package com.vibemusic.app.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.vibemusic.app.data.download.DownloadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

/**
 * Downloads a direct audio URL chosen by the user, or imports a local document.
 * No catalogue or protected source is scraped: the user downloads only content
 * they are allowed to use.
 *
 * On Android 10+ the finished file is published to the public Music/VibeMusic
 * collection through MediaStore, so other players can see it. If the platform
 * refuses the insert, the file stays in the app-private directory and remains
 * playable offline. Interrupted transfers keep a `.part` file and resume with
 * an HTTP Range request instead of starting from zero.
 */
class AudioDownloader(private val context: Context) {

    data class FetchedAudio(
        val uri: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val mime: String,
        val sizeBytes: Long,
        val artworkUri: String?,
        val fileName: String
    )

    data class ProgressUpdate(
        val bytes: Long,
        val totalBytes: Long,
        val speedBytesPerSecond: Long,
        val etaMs: Long?
    )

    private val allowedExtensions = setOf(
        "mp3", "m4a", "aac", "ogg", "oga", "opus", "wav", "flac", "webm", "mp4", "3gp"
    )

    private val maxBytes = 250L * 1024L * 1024L

    suspend fun download(
        request: DownloadRequest,
        onProgress: (ProgressUpdate) -> Unit
    ): FetchedAudio = withContext(Dispatchers.IO) {
        val trimmed = request.url.trim()
        if (trimmed.isEmpty()) throw AudioException("Introduce una URL de audio directa.")
        val url = try {
            URL(trimmed)
        } catch (_: Exception) {
            throw AudioException("La URL no es válida.")
        }
        if (url.protocol != "https") {
            throw AudioException("Solo se admiten enlaces https seguros.")
        }
        if (url.path.orEmpty().endsWith(".m3u8", ignoreCase = true)) {
            throw AudioException("Las listas HLS (.m3u8) no se pueden guardar como un archivo único.")
        }
        ensureNetwork(request.wifiOnly)

        val part = File(context.cacheDir, "download-${hash(trimmed)}.part")
        var resumeFrom = if (part.exists()) part.length() else 0L
        if (resumeFrom > maxBytes) {
            part.delete()
            resumeFrom = 0L
        }

        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 45_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "VibeMusic/2.0 (Android; authorized audio download)")
            setRequestProperty("Accept", "audio/*,application/octet-stream,*/*")
            setRequestProperty("Accept-Encoding", "identity")
            if (resumeFrom > 0) setRequestProperty("Range", "bytes=$resumeFrom-")
        }

        try {
            val code = connection.responseCode
            if (code == 416 && resumeFrom > 0) {
                // The partial file is larger than the remote object: restart.
                part.delete()
                throw AudioException("La descarga parcial ya no coincide con el archivo. Inténtalo de nuevo.")
            }
            if (code !in 200..299) {
                if (code in 400..499) part.delete()
                throw AudioException("El servidor respondió HTTP $code. No se pudo descargar.")
            }
            val mime = connection.contentType?.substringBefore(';')?.trim().orEmpty()
            val contentLength = connection.contentLengthLong
            val appending = code == 206 && resumeFrom > 0
            if (!appending) resumeFrom = 0L
            val total = when {
                contentLength <= 0 -> 0L
                appending -> resumeFrom + contentLength
                else -> contentLength
            }
            if (total > maxBytes) {
                part.delete()
                throw AudioException("El archivo supera el límite de 250 MB.")
            }
            validateMime(mime, url.path.orEmpty())

            var written = resumeFrom
            val startedAt = System.currentTimeMillis()
            var lastReport = 0L
            try {
                connection.inputStream.use { input ->
                    java.io.FileOutputStream(part, appending).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } > 0) {
                            currentCoroutineContext().ensureActive()
                            written += read
                            if (written > maxBytes) {
                                part.delete()
                                throw AudioException("El archivo supera el límite de 250 MB.")
                            }
                            output.write(buffer, 0, read)
                            val now = System.currentTimeMillis()
                            if (now - lastReport >= 200 || total > 0 && written >= total) {
                                lastReport = now
                                reportProgress(written, total, startedAt, resumeFrom, onProgress)
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Keep the .part file so the next attempt can resume.
                throw e
            } catch (e: AudioException) {
                throw e
            } catch (e: Exception) {
                // Keep the partial file: transient network errors are resumable.
                throw AudioException("Error durante la descarga: ${e.message ?: "conexión interrumpida"}")
            }
            if (written == 0L) {
                part.delete()
                throw AudioException("El archivo descargado está vacío.")
            }
            reportProgress(written, if (total > 0) total else written, startedAt, resumeFrom, onProgress)

            val extension = extensionFor(url.path.orEmpty(), mime)
            val metadata = readMetadata(part)
            // Prefer the original file name from the URL over an opaque temp
            // name, so the saved file keeps a recognisable, sanitised name.
            val urlTitle = runCatching {
                java.net.URLDecoder.decode(
                    url.path.orEmpty().substringAfterLast('/'),
                    "UTF-8"
                )
            }.getOrNull()?.substringBeforeLast('.')?.trim().orEmpty()
            val title = request.title.trim().ifBlank {
                metadata.title?.trim().orEmpty().ifBlank { urlTitle }.ifBlank {
                    part.nameWithoutExtension
                }
            }
            val artist = request.artist.trim().ifBlank {
                metadata.artist?.trim().orEmpty().ifBlank { "Origen externo" }
            }
            val album = request.album.trim().ifBlank {
                metadata.album?.trim().orEmpty().ifBlank { "Descargas" }
            }
            if (metadata.durationMs <= 0L && part.length() < 2048L) {
                part.delete()
                throw AudioException("El archivo no contiene audio reproducible.")
            }
            val artwork = saveArtwork(request.artworkUrl, part, trimmed)
            val stored = publish(part, title, artist, album, metadata.durationMs, mime, extension)
            part.delete()
            FetchedAudio(
                uri = stored.first,
                title = title,
                artist = artist,
                album = album,
                durationMs = metadata.durationMs,
                mime = mime.ifBlank { mimeFor(extension) },
                sizeBytes = stored.second,
                artworkUri = artwork,
                fileName = stored.third
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun importDocument(uri: Uri, displayNameHint: String?): FetchedAudio =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri)?.substringBefore(';').orEmpty()
            var name = displayNameHint.orEmpty()
            var size = -1L
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }
            if (size > maxBytes) throw AudioException("El archivo supera el límite de 250 MB.")
            if (mime.isNotEmpty() && !mime.startsWith("audio/") &&
                mime != "application/octet-stream"
            ) {
                throw AudioException("El archivo seleccionado no parece ser audio ($mime).")
            }
            val extension = name.substringAfterLast('.', "").lowercase().ifBlank {
                extensionFor(name, mime)
            }
            if (!allowedExtensions.contains(extension)) {
                throw AudioException("Extensión no compatible: .$extension")
            }
            val temp = File(context.cacheDir, "import-${UUID.randomUUID()}.tmp")
            try {
                resolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output ->
                        val copied = input.copyTo(output, 64 * 1024)
                        if (copied == 0L) throw AudioException("El archivo está vacío.")
                        if (copied > maxBytes) throw AudioException("El archivo supera el límite de 250 MB.")
                    }
                } ?: throw AudioException("No se pudo abrir el archivo seleccionado.")
                val metadata = readMetadata(temp)
                val title = name.substringBeforeLast('.').trim().ifBlank {
                    metadata.title?.trim().orEmpty().ifBlank { "Audio importado" }
                }
                val artist = metadata.artist?.trim().orEmpty().ifBlank { "Archivo importado" }
                val album = metadata.album?.trim().orEmpty().ifBlank { "Importado" }
                val artwork = saveArtwork(null, temp, "import:$uri")
                val directory = File(context.filesDir, "downloads").apply { mkdirs() }
                val safe = sanitize(title).take(80).ifBlank { "audio" }
                val finalFile = uniqueFile(directory, safe, extension)
                if (!temp.renameTo(finalFile)) {
                    temp.copyTo(finalFile, overwrite = true)
                    temp.delete()
                }
                FetchedAudio(
                    uri = Uri.fromFile(finalFile).toString(),
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = metadata.durationMs,
                    mime = mime.ifBlank { mimeFor(extension) },
                    sizeBytes = finalFile.length(),
                    artworkUri = artwork,
                    fileName = finalFile.absolutePath
                )
            } catch (e: AudioException) {
                temp.delete()
                throw e
            } catch (e: Exception) {
                temp.delete()
                throw AudioException("No se pudo importar el archivo: ${e.message ?: "error desconocido"}")
            }
        }

    /**
     * Publishes the finished temp file to MediaStore on Android 10+. Returns
     * uri, size and the stored path/uri used to delete it later.
     */
    private fun publish(
        temp: File,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        mime: String,
        extension: String
    ): Triple<String, Long, String> {
        val safe = sanitize(title).take(80).ifBlank { "audio" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$safe.$extension")
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.Audio.Media.ALBUM, album)
                put(MediaStore.Audio.Media.DURATION, durationMs)
                put(MediaStore.Audio.Media.MIME_TYPE, mime.ifBlank { mimeFor(extension) })
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/VibeMusic")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = runCatching {
                resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            }.getOrNull()
            if (uri != null) {
                val copied = runCatching {
                    resolver.openOutputStream(uri)?.use { output ->
                        temp.inputStream().use { input -> input.copyTo(output, 64 * 1024) }
                    } != null
                }.getOrDefault(false)
                if (copied) {
                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    runCatching { resolver.update(uri, values, null, null) }
                    return Triple(uri.toString(), temp.length(), uri.toString())
                }
                runCatching { resolver.delete(uri, null, null) }
            }
            // Fall through to app-private storage if MediaStore is unavailable.
        }
        val directory = File(context.filesDir, "downloads").apply { mkdirs() }
        val finalFile = uniqueFile(directory, safe, extension)
        if (!temp.renameTo(finalFile)) {
            temp.copyTo(finalFile, overwrite = true)
        }
        return Triple(Uri.fromFile(finalFile).toString(), finalFile.length(), finalFile.absolutePath)
    }

    fun deleteStored(uriString: String): Boolean {
        if (uriString.isBlank()) return false
        return runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "content") {
                context.contentResolver.delete(uri, null, null) > 0
            } else if (uri.scheme == "file") {
                File(uri.path.orEmpty()).delete()
            } else {
                File(uriString).delete()
            }
        }.getOrDefault(false)
    }

    fun deleteAppAudio(deleteFiles: Boolean) {
        if (!deleteFiles) return
        File(context.filesDir, "downloads").let { dir ->
            if (dir.exists()) dir.deleteRecursively()
        }
        File(context.filesDir, "artwork").let { dir ->
            if (dir.exists()) dir.deleteRecursively()
        }
    }

    // ---------------- Helpers ----------------

    private fun reportProgress(
        written: Long,
        total: Long,
        startedAt: Long,
        resumeFrom: Long,
        onProgress: (ProgressUpdate) -> Unit
    ) {
        val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
        val speed = ((written - resumeFrom).coerceAtLeast(0L) * 1000L) / elapsed
        val eta = if (total > written && speed > 0) {
            ((total - written) * 1000L) / speed
        } else {
            null
        }
        onProgress(ProgressUpdate(written, total, speed, eta))
    }

    private fun saveArtwork(artworkUrl: String?, audio: File, cacheKey: String): String? {
        val directory = File(context.filesDir, "artwork").apply { mkdirs() }
        val target = File(directory, "cover-${hash(cacheKey)}.jpg")
        if (target.exists() && target.length() > 0) return Uri.fromFile(target).toString()

        if (!artworkUrl.isNullOrBlank() && artworkUrl.startsWith("https://", ignoreCase = true)) {
            runCatching {
                val connection = (URL(artworkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 20_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "VibeMusic/2.0 (Android)")
                    setRequestProperty("Accept", "image/*")
                }
                try {
                    if (connection.responseCode in 200..299) {
                        val type = connection.contentType?.lowercase().orEmpty()
                        if (type.startsWith("image/")) {
                            val bytes = connection.inputStream.use { it.readBytes() }
                            if (bytes.size in 1..(5 * 1024 * 1024)) {
                                target.writeBytes(bytes)
                                return Uri.fromFile(target).toString()
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }

        // Fall back to embedded album art, if any.
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(audio.absolutePath)
            val picture = retriever.embeddedPicture
            if (picture != null && picture.isNotEmpty()) {
                target.writeBytes(picture)
                Uri.fromFile(target).toString()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private data class AudioMetadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long
    )

    private fun readMetadata(file: File): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            AudioMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            )
        } catch (_: Exception) {
            AudioMetadata(null, null, null, 0L)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun validateMime(mime: String, path: String) {
        if (mime.isBlank() || mime == "application/octet-stream" || mime == "binary/octet-stream") return
        if (mime == "application/ogg" || mime == "application/x-ogg") return
        if (mime.startsWith("audio/")) return
        if (mime == "video/mp4" && path.endsWith(".m4a", true)) return
        if (mime.startsWith("text/") || mime.contains("html") || mime.contains("json")) {
            throw AudioException("La URL devolvió $mime, no un archivo de audio.")
        }
    }

    private fun extensionFor(path: String, mime: String): String {
        val fromPath = path.substringAfterLast('.', "").lowercase()
        if (fromPath in allowedExtensions) return fromPath
        return when {
            mime.contains("mpeg") -> "mp3"
            mime.contains("mp4") || mime.contains("aac") -> "m4a"
            mime.contains("ogg") || mime.contains("opus") -> "ogg"
            mime.contains("flac") -> "flac"
            mime.contains("wav") -> "wav"
            mime.contains("webm") -> "webm"
            else -> "mp3"
        }
    }

    private fun mimeFor(extension: String): String = when (extension) {
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "aac" -> "audio/aac"
        "ogg", "oga", "opus" -> "audio/ogg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "webm" -> "audio/webm"
        "3gp" -> "audio/3gpp"
        else -> "audio/*"
    }

    private fun sanitize(title: String): String = title
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimEnd('.')

    private fun uniqueFile(directory: File, base: String, extension: String): File {
        var candidate = File(directory, "$base.$extension")
        var index = 2
        while (candidate.exists()) {
            candidate = File(directory, "$base ($index).$extension")
            index++
        }
        return candidate
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun ensureNetwork(wifiOnly: Boolean) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
            ?: throw AudioException("No se pudo comprobar la conexión.")
        val network = manager.activeNetwork ?: throw AudioException("Sin conexión a Internet.")
        val caps = manager.getNetworkCapabilities(network)
            ?: throw AudioException("Sin conexión a Internet.")
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            throw AudioException("Sin conexión a Internet.")
        }
        if (wifiOnly && !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            throw AudioException(
                "La opción «solo Wi-Fi» está activa. Conéctate a Wi-Fi o elige " +
                    "«Wi-Fi y datos móviles» antes de descargar."
            )
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // The UI already warned the user; no extra charge is ever generated
            // by VibeMusic itself.
        }
    }

    class AudioException(message: String) : IOException(message)
}
