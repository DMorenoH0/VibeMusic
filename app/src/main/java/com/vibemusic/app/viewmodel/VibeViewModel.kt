package com.vibemusic.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibemusic.app.data.MusicRepository
import com.vibemusic.app.data.ScanState
import com.vibemusic.app.data.SettingsStore
import com.vibemusic.app.data.db.PlaylistWithCount
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import com.vibemusic.app.playback.PlayerController
import com.vibemusic.app.playback.PlayerUiState
import com.vibemusic.app.playback.QueueItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransferState(
    val visible: Boolean = false,
    val isWorking: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val finishedTitle: String? = null
)

class VibeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application, SettingsStore(application))

    val playerState: StateFlow<PlayerUiState> = PlayerController.state
    val queue: StateFlow<List<QueueItem>> = PlayerController.queue
    val scanState: StateFlow<ScanState> = repository.scanState
    val amoled: StateFlow<Boolean> = repository.amoled
    val autoScan: StateFlow<Boolean> = repository.autoScan
    val wifiOnly: StateFlow<Boolean> = repository.wifiOnly

    val allTracks: StateFlow<List<Track>> = combine(
        repository.demoTracks,
        repository.localTracks,
        repository.downloads
    ) { demo, local, downloads ->
        val byId = LinkedHashMap<String, Track>()
        (downloads + local + demo).forEach { byId[it.id] = it }
        byId.values.sortedBy { it.title.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<Track>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = repository.favorites
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists: StateFlow<List<PlaylistWithCount>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recent: StateFlow<List<Track>> = repository.recent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads: StateFlow<List<Track>> = repository.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentTrack: StateFlow<Track?> = combine(allTracks, playerState) { tracks, player ->
        tracks.firstOrNull { it.id == player.mediaId } ?: player.mediaId?.let { id ->
            Track(
                id = id,
                uri = player.uri.orEmpty(),
                title = player.title.ifBlank { "Reproduciendo" },
                artist = player.artist.ifBlank { "VibeMusic" },
                album = player.album,
                durationMs = player.durationMs,
                artworkUri = player.artworkUri,
                source = if (player.uri?.startsWith("file:") == true) {
                    TrackSource.DOWNLOAD
                } else {
                    TrackSource.LOCAL
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Track>> = combine(_searchQuery, allTracks) { query, tracks ->
        if (query.isBlank()) emptyList() else {
            val needle = normalize(query)
            tracks.filter {
                normalize(it.title).contains(needle) ||
                    normalize(it.artist).contains(needle) ||
                    normalize(it.album).contains(needle) ||
                    normalize(it.genre.orEmpty()).contains(needle)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _transfer = MutableStateFlow(TransferState())
    val transfer: StateFlow<TransferState> = _transfer.asStateFlow()

    private val _initializing = MutableStateFlow(true)
    val initializing: StateFlow<Boolean> = _initializing.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private val _sleepMinutes = MutableStateFlow<Int?>(null)
    val sleepMinutes: StateFlow<Int?> = _sleepMinutes.asStateFlow()
    private var sleepJob: Job? = null

    init {
        PlayerController.connect(application)
        viewModelScope.launch {
            repository.initialize()
            _initializing.value = false
        }
        viewModelScope.launch {
            currentTrack
                .map { it?.id }
                .distinctUntilChanged()
                .collect { id ->
                    if (id != null) {
                        currentTrack.value?.let { repository.recordRecent(it) }
                    }
                }
        }
    }

    // ---------------- Playback ----------------
    fun play(track: Track) {
        PlayerController.playTrack(track, allTracks.value)
    }

    fun playQueue(tracks: List<Track>, shuffled: Boolean = false) {
        if (tracks.isEmpty()) {
            notify("No hay canciones para reproducir.")
            return
        }
        val list = if (shuffled) tracks.shuffled() else tracks
        PlayerController.playQueue(list, 0)
    }

    fun playFromList(tracks: List<Track>, track: Track) {
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        PlayerController.playQueue(tracks, index)
    }

    fun togglePlayPause() = PlayerController.togglePlayPause()
    fun next() = PlayerController.next()
    fun previous() = PlayerController.previous()
    fun seekTo(ms: Long) = PlayerController.seekTo(ms)
    fun seekToFraction(fraction: Float) = PlayerController.seekToFraction(fraction)
    fun setVolume(volume: Float) = PlayerController.setVolume(volume)
    fun toggleShuffle() = PlayerController.toggleShuffle()
    fun cycleRepeat() = PlayerController.cycleRepeat()
    fun jumpToQueue(index: Int) = PlayerController.jumpTo(index)
    fun removeFromQueue(index: Int) = PlayerController.removeFromQueue(index)
    fun clearQueue() = PlayerController.stopAndClear()
    fun clearPlayerError() = PlayerController.clearError()
    fun retryPlayer() = PlayerController.retryConnection()

    fun playNext(track: Track) {
        PlayerController.enqueue(track)
        notify("Añadido a la cola: ${track.title}")
    }

    // ---------------- Favorites ----------------
    fun toggleFavorite(track: Track) {
        if (track.uri.isBlank()) {
            notify("No se puede guardar: la pista no tiene un archivo válido.")
            return
        }
        val wasFavorite = track.id in favoriteIds.value
        viewModelScope.launch {
            repository.toggleFavorite(track)
            notify(if (wasFavorite) "Quitado de favoritos" else "Añadido a favoritos")
        }
    }

    // ---------------- Playlists ----------------
    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            notify("Lista creada")
            onCreated(id)
        }
    }

    fun addToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            when (repository.addToPlaylist(playlistId, track)) {
                MusicRepository.AddResult.Added -> notify("Añadido a la lista")
                MusicRepository.AddResult.AlreadyThere -> notify("Ya está en esa lista")
            }
        }
    }

    fun removeFromPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.removeFromPlaylist(playlistId, track.id)
            notify("Quitado de la lista")
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch {
            repository.renamePlaylist(id, name)
            notify("Lista renombrada")
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
            notify("Lista eliminada")
        }
    }

    fun playlistTracks(id: Long) = repository.observePlaylistTracks(id)

    // ---------------- Library scanning ----------------
    fun hasAudioPermission(): Boolean = repository.hasAudioPermission()

    fun audioPermission(): String = repository.audioPermission()

    fun onAudioPermissionGranted() {
        viewModelScope.launch { repository.refreshLocal(showScanning = true) }
    }

    fun onAudioPermissionDenied() {
        repository.markPermissionDenied()
        notify("Sin permiso de audio. Puedes concederlo en Ajustes del sistema.")
    }

    fun rescanLibrary() {
        if (!repository.hasAudioPermission()) {
            repository.markPermissionDenied()
            notify("Concede el permiso de audio para escanear tu música.")
            return
        }
        viewModelScope.launch {
            repository.refreshLocal(showScanning = true)
            val state = repository.scanState.value
            if (state is ScanState.Ready) notify(
                "${state.count} ${if (state.count == 1) "canción local encontrada" else "canciones locales encontradas"}"
            )
            if (state is ScanState.Failed) notify(state.message)
        }
    }

    // ---------------- Downloads / imports ----------------
    fun startDownload(url: String, title: String, artist: String) {
        if (_transfer.value.isWorking) return
        viewModelScope.launch {
            _transfer.value = TransferState(visible = true, isWorking = true)
            try {
                val track = repository.download(url, title, artist) { progress ->
                    _transfer.update { it.copy(progress = progress) }
                }
                _transfer.value = TransferState(
                    visible = true,
                    isWorking = false,
                    progress = 1f,
                    finishedTitle = track.title
                )
                notify("Descarga completa: ${track.title}")
            } catch (e: Exception) {
                _transfer.update {
                    it.copy(
                        isWorking = false,
                        error = e.message ?: "No se pudo descargar el archivo."
                    )
                }
            }
        }
    }

    fun importDocument(uri: Uri, displayName: String?) {
        if (_transfer.value.isWorking) return
        viewModelScope.launch {
            _transfer.value = TransferState(visible = true, isWorking = true)
            try {
                val track = repository.importDocument(uri, displayName)
                _transfer.value = TransferState(
                    visible = true,
                    isWorking = false,
                    progress = 1f,
                    finishedTitle = track.title
                )
                notify("Importado: ${track.title}")
            } catch (e: Exception) {
                _transfer.update {
                    it.copy(
                        isWorking = false,
                        error = e.message ?: "No se pudo importar el archivo."
                    )
                }
            }
        }
    }

    fun importDocuments(uris: List<Uri>) {
        if (uris.isEmpty() || _transfer.value.isWorking) return
        viewModelScope.launch {
            _transfer.value = TransferState(visible = true, isWorking = true)
            var imported = 0
            var lastTitle: String? = null
            var lastError: String? = null
            for (uri in uris) {
                try {
                    val track = repository.importDocument(uri, null)
                    imported++
                    lastTitle = track.title
                } catch (e: Exception) {
                    lastError = e.message ?: "No se pudo importar un archivo."
                }
            }
            _transfer.value = TransferState(
                visible = true,
                isWorking = false,
                progress = 1f,
                finishedTitle = if (imported > 0) {
                    if (imported == 1) lastTitle else "$imported archivos importados"
                } else null,
                error = lastError
            )
            if (imported > 0) notify("Importación completa ($imported)")
            if (lastError != null && imported == 0) notify(lastError)
        }
    }

    fun deleteDownload(track: Track) {
        viewModelScope.launch {
            repository.deleteDownload(track)
            notify("Archivo eliminado: ${track.title}")
        }
    }

    fun dismissTransfer() {
        if (!_transfer.value.isWorking) _transfer.value = TransferState()
    }

    fun clearTransferError() {
        _transfer.update { it.copy(error = null) }
    }

    // ---------------- History / settings ----------------
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            notify("Historial borrado")
        }
    }

    fun setAmoled(enabled: Boolean) = repository.setAmoled(enabled)
    fun setAutoScan(enabled: Boolean) = repository.setAutoScan(enabled)
    fun setWifiOnly(enabled: Boolean) = repository.setWifiOnly(enabled)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        sleepJob = null
        _sleepMinutes.value = minutes
        if (minutes == null) {
            notify("Temporizador desactivado")
            return
        }
        notify("El audio se detendrá en $minutes min")
        sleepJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            if (PlayerController.state.value.isPlaying) PlayerController.togglePlayPause()
            _sleepMinutes.value = null
            notify("Temporizador finalizado: reproducción pausada")
        }
    }

    fun notify(message: String) {
        _messages.tryEmit(message)
    }

    private fun normalize(value: String): String {
        val lower = value.lowercase()
        val decomposed = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{Mn}+"), "")
    }

    override fun onCleared() {
        super.onCleared()
        sleepJob?.cancel()
    }
}
