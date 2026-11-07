package com.vibemusic.app.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest
import java.util.Locale

data class UpdateDownloadState(
    val downloadId: Long? = null,
    val running: Boolean = false,
    val progress: Float = 0f,
    val file: File? = null,
    val verified: Boolean = false,
    val error: String? = null
)

/**
 * Downloads a published APK with Android's DownloadManager and hands it to the
 * official package installer. Nothing is installed silently: the user always
 * confirms in the system dialog.
 */
class UpdateInstaller(private val context: Context) {

    private val manager: DownloadManager? =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    fun startDownload(release: ReleaseInfo): Long {
        val url = release.apkUrl ?: throw UpdateException("La versión publicada no incluye un APK.")
        val host = runCatching { Uri.parse(url).host.orEmpty().lowercase() }.getOrDefault("")
        val fromGitHub = host == "github.com" ||
            host.endsWith(".github.com") ||
            host.endsWith(".githubusercontent.com")
        if (!fromGitHub) {
            throw UpdateException("El APK no procede de GitHub. Descarga cancelada por seguridad.")
        }
        val dm = manager ?: throw UpdateException("El gestor de descargas no está disponible.")
        val relativePath = "updates/VibeMusic-${release.version}.apk"
        val target = File(
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates"),
            "VibeMusic-${release.version}.apk"
        )
        target.parentFile?.mkdirs()
        if (target.exists()) target.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("VibeMusic ${release.version}")
            .setDescription("Descargando actualización…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, relativePath)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setMimeType("application/vnd.android.package-archive")
        request.addRequestHeader("User-Agent", "VibeMusic/${release.version}")
        return dm.enqueue(request)
    }

    fun query(downloadId: Long): UpdateDownloadState {
        val dm = manager ?: return UpdateDownloadState(error = "Gestor de descargas no disponible.")
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
            ?: return UpdateDownloadState(error = "No se pudo consultar la descarga.")
        cursor.use {
            if (!it.moveToFirst()) return UpdateDownloadState(error = "Descarga no encontrada.")
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val localUri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            val progress = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
            return when (status) {
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING ->
                    UpdateDownloadState(downloadId = downloadId, running = true, progress = progress)
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val file = localUri?.let { uriString ->
                        if (uriString.startsWith("file:")) File(Uri.parse(uriString).path.orEmpty())
                        else null
                    }
                    UpdateDownloadState(
                        downloadId = downloadId,
                        running = false,
                        progress = 1f,
                        file = file
                    )
                }
                DownloadManager.STATUS_PAUSED ->
                    UpdateDownloadState(downloadId = downloadId, running = true, progress = progress)
                else -> {
                    val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    UpdateDownloadState(
                        downloadId = downloadId,
                        running = false,
                        error = "La descarga falló (código $reason). Comprueba tu conexión."
                    )
                }
            }
        }
    }

    fun verifySha256(file: File, expected: String?): Boolean {
        if (expected.isNullOrBlank()) return true
        if (!file.exists()) return false
        val actual = runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(Locale.US, it) }
        }.getOrNull() ?: return false
        return actual.equals(expected, ignoreCase = true)
    }

    fun canRequestInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openUnknownSourcesSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun clearDownloads() {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
        if (dir.exists()) dir.deleteRecursively()
    }
}
