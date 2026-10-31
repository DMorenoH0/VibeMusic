package com.vibemusic.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Downloads a direct audio URL chosen by the user, or imports a local document.
 * No catalog or protected source is scraped: the user is responsible for using
 * audio they are allowed to download. The file is stored privately and played
 * offline afterwards.
 */
class AudioDownloader(private val context: Context) {

    data class FetchedAudio(
        val file: File,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val mime: String
    )

    private val allowedExtensions = setOf(
        "mp3", "m4a", "aac", "ogg", "oga", "opus", "wav", "flac", "webm", "mp4", "3gp"
    )

    private val maxBytes = 250L * 1024L * 1024L

    suspend fun download(
        urlString: String,
        titleHint: String,
        artistHint: String,
        wifiOnly: Boolean,
        onProgress: (Float) -> Unit
    ): FetchedAudio = withContext(Dispatchers.IO) {
        val trimmed = urlString.trim()
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
        ensureNetwork(wifiOnly)

        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 45_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "VibeMusic/1.0 (Android; authorized audio download)")
            setRequestProperty("Accept", "audio/*,application/octet-stream,*/*")
        }

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw AudioException("El servidor respondió HTTP $code. No se pudo descargar.")
            }
            val mime = connection.contentType?.substringBefore(';')?.trim().orEmpty()
            val length = connection.contentLengthLong
            if (length > maxBytes) {
                throw AudioException("El archivo supera el límite de 250 MB.")
            }
            validateMime(mime, url.path.orEmpty())

            val temp = File(context.cacheDir, "download-${UUID.randomUUID()}.tmp")
            var total = 0L
            try {
                connection.inputStream.use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } > 0) {
                            total += read
                            if (total > maxBytes) throw AudioException("El archivo supera el límite de 250 MB.")
                            output.write(buffer, 0, read)
                            if (length > 0) onProgress((total.toFloat() / length).coerceIn(0f, 1f))
                        }
                    }
                }
                if (total == 0L) throw AudioException("El archivo descargado está vacío.")
                onProgress(1f)
                val extension = extensionFor(url.path.orEmpty(), mime)
                finalizeFile(temp, extension, titleHint, artistHint, "Descargas", mime)
            } catch (e: AudioException) {
                temp.delete()
                throw e
            } catch (e: Exception) {
                temp.delete()
                throw AudioException("Error durante la descarga: ${e.message ?: "conexión interrumpida"}")
            }
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
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                        if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                    }
                }
            if (size > maxBytes) throw AudioException("El archivo supera el límite de 250 MB.")
            if (mime.isNotEmpty() && !mime.startsWith("audio/") && mime != "application/octet-stream") {
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
                finalizeFile(
                    temp,
                    extension,
                    name.substringBeforeLast('.'),
                    "",
                    "Importado",
                    mime.ifBlank { "audio/*" }
                )
            } catch (e: AudioException) {
                temp.delete()
                throw e
            } catch (e: Exception) {
                temp.delete()
                throw AudioException("No se pudo importar el archivo: ${e.message ?: "error desconocido"}")
            }
        }

    private fun finalizeFile(
        temp: File,
        extension: String,
        titleHint: String,
        artistHint: String,
        album: String,
        mime: String
    ): FetchedAudio {
        val metadata = readMetadata(temp)
        val duration = metadata.durationMs
        if (duration <= 0L && temp.length() < 2048L) {
            temp.delete()
            throw AudioException("El archivo no contiene audio reproducible.")
        }
        val title = titleHint.trim().ifBlank {
            metadata.title?.trim().orEmpty().ifBlank {
                temp.nameWithoutExtension
            }
        }
        val artist = artistHint.trim().ifBlank {
            metadata.artist?.trim().orEmpty().ifBlank { "Origen externo" }
        }
        val directory = File(context.filesDir, "downloads").apply { mkdirs() }
        val safe = title
            .replace(Regex("[^\\p{L}\\p{N} _.-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(48)
            .ifBlank { "audio" }
        val finalFile = File(directory, "${safe}_${UUID.randomUUID().toString().take(8)}.$extension")
        if (!temp.renameTo(finalFile)) {
            temp.copyTo(finalFile, overwrite = true)
            temp.delete()
        }
        return FetchedAudio(
            file = finalFile,
            title = title,
            artist = artist,
            album = metadata.album?.trim().orEmpty().ifBlank { album },
            durationMs = duration,
            mime = mime
        )
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
            else -> "audio"
        }
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
            throw AudioException("La opción «solo Wi-Fi» está activa. Conéctate a Wi-Fi o desactívala en Ajustes.")
        }
    }

    class AudioException(message: String) : IOException(message)
}
