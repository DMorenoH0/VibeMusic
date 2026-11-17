package com.vibemusic.app.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vibemusic.app.MainActivity
import com.vibemusic.app.R
import com.vibemusic.app.data.MusicRepository
import com.vibemusic.app.data.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps an authorized direct download alive when the
 * activity goes to the background. Progress is published through [DownloadBus]
 * and the pending request is persisted, so returning to the app resumes it.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                job?.cancel()
                job = null
                DownloadBus.publish(
                    DownloadBus.state.value?.copy(status = DownloadStatus.CANCELLED) ?: return START_NOT_STICKY
                )
                DownloadBus.clearPending(this)
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                val request = intent.toRequest() ?: run {
                    stopSelf()
                    return START_NOT_STICKY
                }
                start(request)
                return START_NOT_STICKY
            }

            ACTION_RETRY -> {
                val request = DownloadBus.loadPending(this) ?: run {
                    stopSelf()
                    return START_NOT_STICKY
                }
                start(request)
                return START_NOT_STICKY
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    private fun start(request: DownloadRequest) {
        if (job?.isActive == true) return
        DownloadBus.savePending(this, request)
        DownloadBus.remember(request)
        val initial = DownloadProgress(
            id = System.currentTimeMillis().toString(),
            title = request.title,
            artist = request.artist,
            artworkUrl = request.artworkUrl,
            trackId = request.trackId,
            status = DownloadStatus.QUEUED,
            quality = request.quality
        )
        DownloadBus.publish(initial)
        startForegroundCompat(initial)
        job = scope.launch {
            val repository = MusicRepository(applicationContext, SettingsStore(applicationContext))
            try {
                val track = repository.download(request) { update ->
                    val current = DownloadBus.state.value ?: initial
                    val progress = current.copy(
                        status = DownloadStatus.DOWNLOADING,
                        bytes = update.bytes,
                        totalBytes = update.totalBytes,
                        speedBytesPerSecond = update.speedBytesPerSecond,
                        etaMs = update.etaMs
                    )
                    DownloadBus.publish(progress)
                    updateNotification(progress)
                }
                val done = (DownloadBus.state.value ?: initial).copy(
                    status = DownloadStatus.FINISHED,
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = track.artworkUri,
                    bytes = (DownloadBus.state.value?.bytes ?: 0L),
                    totalBytes = (DownloadBus.state.value?.totalBytes ?: 0L),
                    savedTrackId = track.id,
                    error = null
                )
                DownloadBus.publish(done)
                DownloadBus.clearPending(this@DownloadService)
                DownloadBus.forget()
                updateNotification(done)
                stopForegroundCompat()
                stopSelf()
            } catch (e: CancellationException) {
                val cancelled = (DownloadBus.state.value ?: initial).copy(
                    status = DownloadStatus.CANCELLED
                )
                DownloadBus.publish(cancelled)
                DownloadBus.clearPending(this@DownloadService)
                DownloadBus.forget()
                stopForegroundCompat()
                stopSelf()
            } catch (e: Exception) {
                val failed = (DownloadBus.state.value ?: initial).copy(
                    status = DownloadStatus.FAILED,
                    error = e.message ?: "No se pudo completar la descarga."
                )
                DownloadBus.publish(failed)
                // Keep the request in memory so "Reintentar" works without
                // leaving a pending task that would auto-start on next launch.
                DownloadBus.clearPending(this@DownloadService)
                updateNotification(failed)
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Descargas",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Progreso de las descargas autorizadas de VibeMusic"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundCompat(progress: DownloadProgress) {
        startForeground(NOTIFICATION_ID, buildNotification(progress))
    }

    private fun updateNotification(progress: DownloadProgress) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(progress))
    }

    private fun buildNotification(progress: DownloadProgress): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = when (progress.status) {
            DownloadStatus.FINISHED -> "Descarga completada"
            DownloadStatus.FAILED -> progress.error ?: "Descarga fallida"
            DownloadStatus.CANCELLED -> "Descarga cancelada"
            else -> buildString {
                append(progress.title.ifBlank { "Audio" })
                if (progress.percent > 0) append(" · ${progress.percent} %")
            }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vibe)
            .setContentTitle(if (progress.isActive) "Descargando audio" else "VibeMusic")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(progress.isActive)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(
                100,
                progress.percent,
                progress.totalBytes <= 0 && progress.isActive
            )
            .apply {
                if (progress.status == DownloadStatus.DOWNLOADING) {
                    addAction(0, "Cancelar", cancelIntent)
                }
            }
            .build()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun Intent.toRequest(): DownloadRequest? {
        val url = getStringExtra(EXTRA_URL) ?: return null
        return DownloadRequest(
            url = url,
            title = getStringExtra(EXTRA_TITLE).orEmpty(),
            artist = getStringExtra(EXTRA_ARTIST).orEmpty(),
            album = getStringExtra(EXTRA_ALBUM).orEmpty(),
            artworkUrl = getStringExtra(EXTRA_ARTWORK)?.takeIf { it.isNotBlank() },
            quality = DownloadQuality.byName(getStringExtra(EXTRA_QUALITY)),
            wifiOnly = getBooleanExtra(EXTRA_WIFI_ONLY, true),
            trackId = getStringExtra(EXTRA_TRACK_ID)?.takeIf { it.isNotBlank() },
            localCopy = getBooleanExtra(EXTRA_LOCAL_COPY, false)
        )
    }

    companion object {
        private const val CHANNEL_ID = "vibemusic_downloads"
        private const val NOTIFICATION_ID = 4102
        private const val ACTION_START = "com.vibemusic.app.download.START"
        private const val ACTION_CANCEL = "com.vibemusic.app.download.CANCEL"
        private const val ACTION_RETRY = "com.vibemusic.app.download.RETRY"
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ARTIST = "artist"
        private const val EXTRA_ALBUM = "album"
        private const val EXTRA_ARTWORK = "artwork"
        private const val EXTRA_QUALITY = "quality"
        private const val EXTRA_WIFI_ONLY = "wifiOnly"
        private const val EXTRA_TRACK_ID = "trackId"
        private const val EXTRA_LOCAL_COPY = "localCopy"

        fun start(context: Context, request: DownloadRequest) {
            DownloadBus.savePending(context, request)
            DownloadBus.remember(request)
            val intent = Intent(context, DownloadService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_URL, request.url)
                .putExtra(EXTRA_TITLE, request.title)
                .putExtra(EXTRA_ARTIST, request.artist)
                .putExtra(EXTRA_ALBUM, request.album)
                .putExtra(EXTRA_ARTWORK, request.artworkUrl.orEmpty())
                .putExtra(EXTRA_QUALITY, request.quality.name)
                .putExtra(EXTRA_WIFI_ONLY, request.wifiOnly)
                .putExtra(EXTRA_TRACK_ID, request.trackId.orEmpty())
                .putExtra(EXTRA_LOCAL_COPY, request.localCopy)
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            context.startService(
                Intent(context, DownloadService::class.java).setAction(ACTION_CANCEL)
            )
        }

        fun retry(context: Context) {
            val request = DownloadBus.lastRequest ?: DownloadBus.loadPending(context) ?: return
            start(context, request)
        }

        /** Called when the app opens; resumes an interrupted transfer once. */
        fun resumePending(context: Context) {
            if (DownloadBus.state.value?.isActive == true) return
            val request = DownloadBus.loadPending(context) ?: return
            start(context, request)
        }
    }
}
