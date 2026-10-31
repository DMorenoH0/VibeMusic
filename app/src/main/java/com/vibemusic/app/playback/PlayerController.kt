package com.vibemusic.app.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.vibemusic.app.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val mediaId: String? = null,
    val uri: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUri: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val volume: Float = 1f,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val error: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

data class QueueItem(
    val index: Int,
    val mediaId: String?,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val durationMs: Long
)

/**
 * Single process-wide connection to the Media3 session hosted by
 * [MusicPlaybackService]. The UI observes this object; playback itself keeps
 * running when the activity is destroyed.
 */
object PlayerController {

    

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = friendlyError(error)) }
        }
    }

    fun connect(context: Context) {
        appContext = context.applicationContext
        if (controller != null || controllerFuture != null) return

        val token = SessionToken(
            appContext,
            ComponentName(appContext, MusicPlaybackService::class.java)
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val mediaController = future.get()
                controller = mediaController
                mediaController.addListener(listener)
                _state.update { it.copy(connected = true, error = null) }
                syncState(mediaController)
                startTicker()
            } catch (e: Exception) {
                controllerFuture = null
                _state.update {
                    it.copy(
                        connected = false,
                        error = "No se pudo iniciar el reproductor: ${e.message ?: "error desconocido"}"
                    )
                }
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun retryConnection() {
        val existing = controller
        if (existing != null) {
            _state.update { it.copy(error = null) }
            if (existing.mediaItemCount > 0) {
                existing.prepare()
                existing.play()
            }
            return
        }
        controllerFuture = null
        connect(appContext)
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val player = controller
                if (player != null && player.isPlaying) {
                    _state.update {
                        it.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0),
                            durationMs = player.duration.takeIf { d -> d > 0 } ?: it.durationMs,
                            bufferedMs = player.bufferedPosition.coerceAtLeast(0)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun syncState(player: Player?) {
        if (player == null) return
        val metadata = player.currentMediaItem?.mediaMetadata
        val duration = player.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
        _state.update {
            it.copy(
                connected = true,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                mediaId = player.currentMediaItem?.mediaId,
                uri = player.currentMediaItem?.localConfiguration?.uri?.toString(),
                title = metadata?.title?.toString().orEmpty(),
                artist = metadata?.artist?.toString().orEmpty(),
                album = metadata?.albumTitle?.toString().orEmpty(),
                artworkUri = metadata?.artworkUri?.toString(),
                positionMs = player.currentPosition.coerceAtLeast(0),
                durationMs = duration,
                bufferedMs = player.bufferedPosition.coerceAtLeast(0),
                shuffle = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                volume = player.volume,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem()
            )
        }
        refreshQueue(player)
    }

    private fun refreshQueue(player: Player) {
        val items = ArrayList<QueueItem>(player.mediaItemCount)
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            val metadata = item.mediaMetadata
            items += QueueItem(
                index = i,
                mediaId = item.mediaId,
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                artworkUri = metadata.artworkUri?.toString(),
                durationMs = 0L
            )
        }
        _queue.value = items
    }

    private fun toMediaItem(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .apply { track.artworkUri?.let { setArtworkUri(Uri.parse(it)) } }
            .build()
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.uri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        val player = controller ?: run {
            _state.update { it.copy(error = "El reproductor aún se está iniciando. Inténtalo de nuevo.") }
            return
        }
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        player.setMediaItems(tracks.map(::toMediaItem), safeIndex, 0L)
        player.prepare()
        player.play()
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        val index = queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        playQueue(if (queue.any { it.id == track.id }) queue else listOf(track), index)
    }

    fun togglePlayPause() {
        val player = controller ?: return
        when {
            player.playbackState == Player.STATE_IDLE -> {
                if (player.mediaItemCount > 0) {
                    player.prepare()
                    player.play()
                }
            }
            player.isPlaying -> player.pause()
            else -> player.play()
        }
    }

    fun next() {
        val player = controller ?: return
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun previous() {
        val player = controller ?: return
        if (player.currentPosition > 4_000L) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0)
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0))
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0)) }
    }

    fun seekToFraction(fraction: Float) {
        val player = controller ?: return
        val duration = player.duration.takeIf { it > 0 } ?: return
        seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun setVolume(volume: Float) {
        controller?.volume = volume.coerceIn(0f, 1f)
        _state.update { it.copy(volume = volume.coerceIn(0f, 1f)) }
    }

    fun toggleShuffle() {
        val player = controller ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        syncState(player)
    }

    fun cycleRepeat() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        syncState(player)
    }

    fun enqueue(track: Track) {
        val player = controller ?: return
        val item = toMediaItem(track)
        val current = player.currentMediaItemIndex
        if (player.mediaItemCount == 0) {
            player.setMediaItem(item)
            player.prepare()
        } else {
            player.addMediaItem(current + 1, item)
        }
    }

    fun jumpTo(index: Int) {
        val player = controller ?: return
        if (index in 0 until player.mediaItemCount) {
            player.seekToDefaultPosition(index)
            player.play()
        }
    }

    fun removeFromQueue(index: Int) {
        val player = controller ?: return
        if (index in 0 until player.mediaItemCount) {
            player.removeMediaItem(index)
            syncState(player)
        }
    }

    fun stopAndClear() {
        val player = controller ?: return
        player.stop()
        player.clearMediaItems()
        syncState(player)
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun release() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        ticker?.cancel()
        ticker = null
        _state.value = PlayerUiState()
        _queue.value = emptyList()
    }

    private fun friendlyError(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "El archivo ya no está disponible. Vuelve a escanear la biblioteca."
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
            "Sin permiso para leer este archivo. Revisa los permisos de audio."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "Sin conexión. Este contenido necesita Internet."
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
            "Formato de audio no compatible."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
            "El archivo de audio está dañado o no es válido."
        else -> "No se pudo reproducir: ${error.errorCodeName}"
    }
}
