package com.vibemusic.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibemusic.app.BuildConfig
import com.vibemusic.app.audio.AudioProfileStore
import com.vibemusic.app.audio.BluetoothAudioMonitor
import com.vibemusic.app.audio.EqPresets
import com.vibemusic.app.audio.EqualizerController
import com.vibemusic.app.data.MusicRepository
import com.vibemusic.app.data.ScanState
import com.vibemusic.app.data.SettingsStore
import com.vibemusic.app.data.auth.AuthException
import com.vibemusic.app.data.auth.GoogleAuthManager
import com.vibemusic.app.data.csv.CsvColumnRole
import com.vibemusic.app.data.csv.CsvMatch
import com.vibemusic.app.data.csv.CsvMatchResult
import com.vibemusic.app.data.csv.CsvRecord
import com.vibemusic.app.data.csv.CsvTable
import com.vibemusic.app.data.csv.exportCsv
import com.vibemusic.app.data.csv.extractRecords
import com.vibemusic.app.data.csv.guessMapping
import com.vibemusic.app.data.csv.matchRecords
import com.vibemusic.app.data.csv.parseCsv
import com.vibemusic.app.data.db.ListeningEventEntity
import com.vibemusic.app.data.db.DownloadEntity
import com.vibemusic.app.data.db.PlaylistWithCount
import com.vibemusic.app.data.download.DownloadBus
import com.vibemusic.app.data.download.DownloadProgress
import com.vibemusic.app.data.download.DownloadQuality
import com.vibemusic.app.data.download.DownloadRequest
import com.vibemusic.app.data.download.DownloadService
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import com.vibemusic.app.data.recommend.ListeningSignal
import com.vibemusic.app.data.recommend.RecommendationEngine
import com.vibemusic.app.data.recommend.RecommendationSection
import com.vibemusic.app.data.online.DeezerService
import com.vibemusic.app.data.online.OnlineAlbum
import com.vibemusic.app.data.online.OnlineApiException
import com.vibemusic.app.data.online.OnlineArtist
import com.vibemusic.app.data.online.OnlineTrack
import com.vibemusic.app.data.update.GitHubReleaseService
import com.vibemusic.app.data.update.ReleaseInfo
import com.vibemusic.app.data.update.UpdateDownloadState
import com.vibemusic.app.data.update.UpdateException
import com.vibemusic.app.data.update.UpdateInstaller
import com.vibemusic.app.data.update.isNewerVersion
import com.vibemusic.app.data.youtube.YouTubeApiHeaders
import com.vibemusic.app.data.youtube.YouTubeCredentials
import com.vibemusic.app.data.youtube.YouTubeException
import com.vibemusic.app.data.youtube.YouTubeKeyStore
import com.vibemusic.app.data.youtube.YouTubeService
import com.vibemusic.app.data.youtube.YouTubeVideo
import com.vibemusic.app.playback.PlayerController
import com.vibemusic.app.playback.PlayerUiState
import com.vibemusic.app.playback.QueueItem
import com.vibemusic.app.ui.theme.VibeThemeStyle
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.io.File

data class TransferState(
    val visible: Boolean = false,
    val isWorking: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val finishedTitle: String? = null
)

/**
 * Snapshot of the Bluetooth output and the equalisation state. Kept as one
 * state object so the UI cannot show a profile that does not belong to the
 * device currently connected.
 */
data class EqUiState(
    val available: Boolean = false,
    val message: String? = null,
    val bands: List<EqualizerController.Band> = emptyList(),
    val presetName: String = "Balanced",
    val gainsDb: List<Float> = List(EqPresets.anchorsHz.size) { 0f },
    val enabled: Boolean = false,
    val autoApply: Boolean = false,
    val activeDeviceKey: String? = null,
    val activeDeviceName: String? = null,
    val activeDeviceKind: String? = null,
    val manufacturer: String? = null,
    val manufacturerInferred: Boolean = false,
    val codec: String? = null,
    val profiles: List<AudioProfileStore.DeviceProfile> = emptyList()
)

data class OnlineSearchState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<Track> = emptyList(),
    val error: String? = null,
    val searched: Boolean = false
)

data class OnlineAlbumState(
    val loading: Boolean = false,
    val album: OnlineAlbum? = null,
    val error: String? = null
)

data class OnlineArtistState(
    val loading: Boolean = false,
    val artist: OnlineArtist? = null,
    val error: String? = null
)

data class YouTubeSearchState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<YouTubeVideo> = emptyList(),
    val error: String? = null,
    val searched: Boolean = false,
    val configured: Boolean = false
)

data class YouTubeKeyState(
    val checking: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

data class UpdateUiState(
    val checking: Boolean = false,
    val release: ReleaseInfo? = null,
    val error: String? = null,
    val upToDate: Boolean = false,
    val download: UpdateDownloadState = UpdateDownloadState(),
    val installPermission: Boolean = true
) {
    val available: Boolean
        get() = release != null && isNewerVersion(release.version, BuildConfig.VERSION_NAME)
}

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

data class CsvImportState(
    val visible: Boolean = false,
    val fileName: String = "",
    val table: CsvTable? = null,
    val mapping: List<CsvColumnRole> = emptyList(),
    val records: List<CsvRecord> = emptyList(),
    val matches: CsvMatchResult? = null,
    val targetPlaylistId: Long? = null,
    val importing: Boolean = false,
    val error: String? = null,
    val imported: Int = 0,
    val skippedDuplicates: Int = 0,
    val unmatched: List<CsvRecord> = emptyList(),
    val finished: Boolean = false
)

class VibeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application, SettingsStore(application))
    private val settings = repository.settings
    private val deezer = DeezerService()
    private val releases = GitHubReleaseService()
    private val installer = UpdateInstaller(application)
    private val googleAuth = GoogleAuthManager(application)
    private val audioProfiles = AudioProfileStore(application)
    private val bluetoothMonitor = BluetoothAudioMonitor(application)

    private val _eq = MutableStateFlow(EqUiState())
    val eqState: StateFlow<EqUiState> = _eq.asStateFlow()

    val bluetoothOutput: StateFlow<BluetoothAudioMonitor.AudioOutput?> = bluetoothMonitor.output

    val downloadState: StateFlow<DownloadProgress?> = DownloadBus.state
    val recommendationsEnabled: StateFlow<Boolean> = settings.recommendationsEnabled
    val downloadQuality: StateFlow<DownloadQuality> = settings.downloadQuality

    val playerState: StateFlow<PlayerUiState> = PlayerController.state
    val queue: StateFlow<List<QueueItem>> = PlayerController.queue
    val scanState: StateFlow<ScanState> = repository.scanState
    val amoled: StateFlow<Boolean> = repository.amoled
    val autoScan: StateFlow<Boolean> = repository.autoScan
    val wifiOnly: StateFlow<Boolean> = repository.wifiOnly
    val theme: StateFlow<VibeThemeStyle> = settings.theme
    val searchHistory: StateFlow<List<String>> = settings.searchHistory
    val dismissedUpdateVersion: StateFlow<String?> = settings.dismissedUpdateVersion
    val googleProfile = settings.googleProfile
    val googleConfigured: Boolean get() = googleAuth.configured

    val allTracks: StateFlow<List<Track>> = combine(
        repository.demoTracks,
        repository.localTracks,
        repository.downloads
    ) { demo, local, downloads ->
        val byId = LinkedHashMap<String, Track>()
        (downloads + local + demo).forEach { byId[it.id] = it }
        byId.values.sortedBy { it.title.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val deviceTracks: StateFlow<List<Track>> = repository.localTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloadedTracks: StateFlow<List<Track>> = repository.downloads
        .map { list -> list.filter { it.source == TrackSource.DOWNLOAD } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val importedTracks: StateFlow<List<Track>> = repository.downloads
        .map { list -> list.filter { it.source == TrackSource.IMPORTED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Raw download rows so the library can show size and download date. */
    val downloadDetails: StateFlow<List<DownloadEntity>> = repository.downloadEntities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * "Para ti" is computed on-device from listening history, favourites,
     * playlists and the local catalogue. It never performs network requests and
     * returns an empty list when the user disables recommendations.
     */
    val forYouSections: StateFlow<List<RecommendationSection>> = combine(
        repository.listeningEvents,
        favorites,
        allTracks,
        repository.allPlaylistTracksFlow,
        settings.recommendationsEnabled
    ) { events, favouriteTracks, catalog, playlistTracks, enabled ->
        if (!enabled) {
            emptyList()
        } else {
            RecommendationEngine.buildSections(
                signals = events.map { it.toSignal() },
                favorites = favouriteTracks,
                catalog = catalog,
                playlistTracks = playlistTracks
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Cache of online previews so the player/mini-player can show full metadata. */
    private val onlineCache = MutableStateFlow<Map<String, Track>>(emptyMap())

    val currentTrack: StateFlow<Track?> = combine(
        allTracks,
        onlineCache,
        playerState
    ) { tracks, online, player ->
        tracks.firstOrNull { it.id == player.mediaId }
            ?: online[player.mediaId]
            ?: player.mediaId?.let { id ->
                val uri = player.uri.orEmpty()
                val source = when {
                    uri.startsWith("http") -> TrackSource.ONLINE
                    uri.startsWith("file:") -> TrackSource.DOWNLOAD
                    else -> TrackSource.LOCAL
                }
                Track(
                    id = id,
                    uri = uri,
                    title = player.title.ifBlank { "Reproduciendo" },
                    artist = player.artist.ifBlank { "VibeMusic" },
                    album = player.album,
                    durationMs = player.durationMs,
                    artworkUri = player.artworkUri,
                    source = source,
                    previewOnly = source == TrackSource.ONLINE
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

    private val _onlineSearch = MutableStateFlow(OnlineSearchState())
    val onlineSearch: StateFlow<OnlineSearchState> = _onlineSearch.asStateFlow()
    private var onlineSearchJob: Job? = null
    private var lastOnlineQuery = ""

    private val _onlineAlbum = MutableStateFlow(OnlineAlbumState())
    val onlineAlbum: StateFlow<OnlineAlbumState> = _onlineAlbum.asStateFlow()

    private val _onlineArtist = MutableStateFlow(OnlineArtistState())
    val onlineArtist: StateFlow<OnlineArtistState> = _onlineArtist.asStateFlow()

    private val youtubeKeyStore = YouTubeKeyStore(application)
    private val youtubeHeaders = YouTubeApiHeaders.from(application)
    private var youTube = createYouTubeService()
    private val _youtubeKeyState = MutableStateFlow(YouTubeKeyState())
    val youtubeKeyState: StateFlow<YouTubeKeyState> = _youtubeKeyState.asStateFlow()
    private val _youtubeQuery = MutableStateFlow("")
    val youtubeQuery: StateFlow<String> = _youtubeQuery.asStateFlow()
    private val _youtubeSearch = MutableStateFlow(
        YouTubeSearchState(configured = youTube.isConfigured)
    )
    val youtubeSearch: StateFlow<YouTubeSearchState> = _youtubeSearch.asStateFlow()
    private var youtubeSearchJob: Job? = null
    private var lastYouTubeQuery = ""

    val youtubeConfigured: Boolean get() = youTube.isConfigured
    val youtubeKeyStored: Boolean get() = youtubeKeyStore.hasStoredKey()
    val youtubeUsesProxy: Boolean get() = youTube.mode == YouTubeService.Mode.PROXY

    private fun createYouTubeService(): YouTubeService {
        val key = YouTubeCredentials.resolve(BuildConfig.YOUTUBE_API_KEY, youtubeKeyStore.load())
        return YouTubeService(
            apiKey = key,
            proxyUrl = BuildConfig.YOUTUBE_PROXY_URL,
            extraHeaders = youtubeHeaders
        )
    }

    private val _update = MutableStateFlow(UpdateUiState())
    val update: StateFlow<UpdateUiState> = _update.asStateFlow()
    private var updateDownloadJob: Job? = null

    private val _auth = MutableStateFlow(AuthUiState())
    val auth: StateFlow<AuthUiState> = _auth.asStateFlow()

    private val _csvImport = MutableStateFlow(CsvImportState())
    val csvImport: StateFlow<CsvImportState> = _csvImport.asStateFlow()

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
        observeListening()
        startAudioMonitoring()
        _update.update { it.copy(installPermission = installer.canRequestInstall()) }
        viewModelScope.launch {
            delay(2_500)
            while (true) {
                checkForUpdates(manual = false)
                delay(6 * 60 * 60 * 1000L)
            }
        }
    }

    // ---------------- Playback ----------------
    fun play(track: Track) {
        cacheOnline(listOf(track))
        PlayerController.playTrack(track, allTracks.value)
    }

    fun playQueue(tracks: List<Track>, shuffled: Boolean = false) {
        if (tracks.isEmpty()) {
            notify("No hay canciones para reproducir.")
            return
        }
        val list = if (shuffled) tracks.shuffled() else tracks
        cacheOnline(list)
        PlayerController.playQueue(list, 0)
    }

    fun playFromList(tracks: List<Track>, track: Track) {
        cacheOnline(tracks)
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        PlayerController.playQueue(tracks, index)
    }

    private fun cacheOnline(tracks: List<Track>) {
        val online = tracks.filter { it.source == TrackSource.ONLINE }
        if (online.isEmpty()) return
        onlineCache.update { cache -> cache + online.associateBy { it.id } }
        if (onlineCache.value.size > 120) {
            onlineCache.update { it.entries.toList().takeLast(100).associate { entry -> entry.key to entry.value } }
        }
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
        cacheOnline(listOf(track))
        PlayerController.enqueue(track)
        notify(
            if (track.source == TrackSource.ONLINE) {
                "Vista previa añadida a la cola: ${track.title}"
            } else {
                "Añadido a la cola: ${track.title}"
            }
        )
    }

    // ---------------- Favorites ----------------
    fun toggleFavorite(track: Track) {
        if (!track.isPersistable) {
            notify("Las vistas previas online no se guardan en favoritos. Descarga o importa el audio para conservarlo.")
            return
        }
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
        if (!track.isPersistable) {
            notify("Las vistas previas online no se guardan en listas. Descarga o importa el audio para conservarlo.")
            return
        }
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

    fun setPlaylistCover(id: Long, uri: Uri?) {
        viewModelScope.launch {
            repository.setPlaylistCover(id, uri?.toString())
            notify(if (uri == null) "Portada restablecida" else "Portada actualizada")
        }
    }

    fun movePlaylistTrack(playlistId: Long, trackId: String, delta: Int) {
        viewModelScope.launch { repository.movePlaylistTrack(playlistId, trackId, delta) }
    }

    /** Adds a YouTube reference to a playlist. It never downloads the video. */
    fun addYouTubeToPlaylist(playlistId: Long, video: YouTubeVideo) {
        viewModelScope.launch {
            when (repository.addToPlaylist(playlistId, video.toTrackReference())) {
                MusicRepository.AddResult.Added -> notify("Vídeo añadido a la lista")
                MusicRepository.AddResult.AlreadyThere -> notify("Ese vídeo ya está en la lista")
            }
        }
    }

    fun createPlaylistWithYouTube(
        name: String,
        video: YouTubeVideo,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            when (repository.addToPlaylist(id, video.toTrackReference())) {
                MusicRepository.AddResult.Added -> notify("Lista creada con el vídeo")
                MusicRepository.AddResult.AlreadyThere -> notify("Lista creada")
            }
            onCreated(id)
        }
    }

    private fun YouTubeVideo.toTrackReference() = Track(
        id = "youtube:$id",
        uri = watchUrl,
        title = title,
        artist = channel,
        album = "YouTube",
        durationMs = durationMs,
        artworkUri = thumbnailUrl,
        source = TrackSource.YOUTUBE,
        addedAt = System.currentTimeMillis()
    )

    // ---------------- Online search ----------------
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        scheduleOnlineSearch(query)
    }

    /** Called by the keyboard “Search” action or a suggestion chip. */
    fun submitSearch(query: String = _searchQuery.value) {
        val clean = query.trim()
        _searchQuery.value = clean
        if (clean.length < 2) {
            _onlineSearch.value = OnlineSearchState(query = clean)
            return
        }
        settings.addSearchHistory(clean)
        onlineSearchJob?.cancel()
        onlineSearchJob = viewModelScope.launch { runOnlineSearch(clean) }
    }

    fun clearSearch() {
        onlineSearchJob?.cancel()
        _searchQuery.value = ""
        _onlineSearch.value = OnlineSearchState()
        lastOnlineQuery = ""
    }

    fun clearSearchHistory() {
        settings.clearSearchHistory()
        notify("Historial de búsquedas borrado")
    }

    fun removeSearchHistory(query: String) = settings.removeSearchHistory(query)

    private fun scheduleOnlineSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) {
            onlineSearchJob?.cancel()
            _onlineSearch.value = OnlineSearchState(query = clean)
            lastOnlineQuery = ""
            return
        }
        _onlineSearch.update { it.copy(query = clean) }
        onlineSearchJob?.cancel()
        onlineSearchJob = viewModelScope.launch {
            delay(500)
            runOnlineSearch(clean)
        }
    }

    private suspend fun runOnlineSearch(query: String) {
        if (lastOnlineQuery == query && _onlineSearch.value.searched && _onlineSearch.value.error == null) return
        lastOnlineQuery = query
        _onlineSearch.value = OnlineSearchState(query = query, loading = true)
        try {
            val results = deezer.search(query)
            val mapped = results.map { it.toTrack() }
            cacheOnline(mapped)
            _onlineSearch.value = OnlineSearchState(
                query = query,
                loading = false,
                results = mapped,
                searched = true
            )
            if (results.isEmpty()) {
                // Nothing to add to history: an empty search is not useful to repeat.
            } else {
                settings.addSearchHistory(query)
            }
        } catch (e: OnlineApiException) {
            _onlineSearch.value = OnlineSearchState(
                query = query,
                loading = false,
                error = e.message,
                searched = true
            )
        } catch (e: Exception) {
            _onlineSearch.value = OnlineSearchState(
                query = query,
                loading = false,
                error = "Sin conexión o el buscador no responde. Tus canciones locales siguen disponibles.",
                searched = true
            )
        }
    }

    fun retryOnlineSearch() {
        submitSearch(_onlineSearch.value.query.ifBlank { _searchQuery.value })
    }

    // ---------------- YouTube ----------------
    fun setYouTubeQuery(query: String) {
        _youtubeQuery.value = query
        if (query.isBlank()) {
            youtubeSearchJob?.cancel()
            _youtubeSearch.value = YouTubeSearchState(configured = youTube.isConfigured)
            lastYouTubeQuery = ""
        }
    }

    /**
     * YouTube searches are only sent when the user explicitly submits them.
     * Typing never spends quota; a query is cached after the first success.
     */
    fun submitYouTubeSearch(query: String = _youtubeQuery.value) {
        val clean = query.trim()
        _youtubeQuery.value = clean
        if (clean.length < 2) {
            youtubeSearchJob?.cancel()
            _youtubeSearch.value = YouTubeSearchState(
                query = clean,
                configured = youTube.isConfigured
            )
            return
        }
        youtubeSearchJob?.cancel()
        youtubeSearchJob = viewModelScope.launch { runYouTubeSearch(clean) }
    }

    fun clearYouTubeSearch() {
        youtubeSearchJob?.cancel()
        _youtubeQuery.value = ""
        _youtubeSearch.value = YouTubeSearchState(configured = youTube.isConfigured)
        lastYouTubeQuery = ""
    }

    fun retryYouTubeSearch() {
        submitYouTubeSearch(_youtubeSearch.value.query.ifBlank { _youtubeQuery.value })
    }

    /**
     * Validates a key against Google with a 1-unit request and only stores it
     * when the API accepts it. The key is encrypted with Android Keystore and
     * is never logged.
     */
    fun saveYouTubeApiKey(raw: String) {
        val key = raw.trim()
        if (!YouTubeCredentials.isWellFormed(key)) {
            _youtubeKeyState.value = YouTubeKeyState(
                error = "La clave no tiene un formato válido. Pega la clave completa " +
                    "de Google Cloud (normalmente empieza por AIza)."
            )
            return
        }
        viewModelScope.launch {
            _youtubeKeyState.value = YouTubeKeyState(checking = true)
            val candidate = YouTubeService(key, "", youtubeHeaders)
            try {
                candidate.validate()
                youtubeKeyStore.save(key)
                youTube = candidate
                _youtubeSearch.value = _youtubeSearch.value.copy(configured = true)
                lastYouTubeQuery = ""
                _youtubeKeyState.value = YouTubeKeyState(success = true)
                notify("Clave de YouTube verificada y guardada en este dispositivo.")
            } catch (e: YouTubeException) {
                _youtubeKeyState.value = YouTubeKeyState(
                    error = e.message ?: "No se pudo verificar la clave."
                )
            } catch (e: Exception) {
                _youtubeKeyState.value = YouTubeKeyState(
                    error = "No se pudo verificar la clave. Comprueba tu conexión."
                )
            }
        }
    }

    fun clearYouTubeApiKey() {
        youtubeKeyStore.clear()
        youTube = createYouTubeService()
        lastYouTubeQuery = ""
        _youtubeKeyState.value = YouTubeKeyState()
        _youtubeSearch.value = _youtubeSearch.value.copy(configured = youTube.isConfigured)
        notify("Clave de YouTube eliminada del dispositivo.")
    }

    fun dismissYouTubeKeyState() {
        _youtubeKeyState.value = YouTubeKeyState()
    }

    private suspend fun runYouTubeSearch(query: String) {
        if (lastYouTubeQuery == query && _youtubeSearch.value.searched &&
            _youtubeSearch.value.error == null
        ) {
            return
        }
        lastYouTubeQuery = query
        _youtubeSearch.value = YouTubeSearchState(
            query = query,
            loading = true,
            configured = youTube.isConfigured
        )
        try {
            val results = youTube.search(query)
            _youtubeSearch.value = YouTubeSearchState(
                query = query,
                loading = false,
                results = results,
                searched = true,
                configured = true
            )
        } catch (e: YouTubeException) {
            _youtubeSearch.value = YouTubeSearchState(
                query = query,
                loading = false,
                error = e.message ?: "No se pudo buscar en YouTube.",
                searched = true,
                configured = youTube.isConfigured
            )
        } catch (e: Exception) {
            _youtubeSearch.value = YouTubeSearchState(
                query = query,
                loading = false,
                error = "Sin conexión o YouTube no responde. Tu música local sigue disponible.",
                searched = true,
                configured = youTube.isConfigured
            )
        }
    }

    fun loadOnlineAlbum(albumId: String) {
        if (albumId.isBlank()) return
        _onlineAlbum.value = OnlineAlbumState(loading = true)
        viewModelScope.launch {
            try {
                val album = deezer.album(albumId)
                val mapped = album.tracks.map { it.toTrack() }
                cacheOnline(mapped)
                _onlineAlbum.value = OnlineAlbumState(album = album)
            } catch (e: Exception) {
                _onlineAlbum.value = OnlineAlbumState(
                    error = e.message ?: "No se pudo cargar el álbum."
                )
            }
        }
    }

    fun loadOnlineArtist(artistId: String) {
        if (artistId.isBlank()) return
        _onlineArtist.value = OnlineArtistState(loading = true)
        viewModelScope.launch {
            try {
                val artist = deezer.artist(artistId)
                val mapped = artist.topTracks.map { it.toTrack() }
                cacheOnline(mapped)
                _onlineArtist.value = OnlineArtistState(artist = artist)
            } catch (e: Exception) {
                _onlineArtist.value = OnlineArtistState(
                    error = e.message ?: "No se pudo cargar el artista."
                )
            }
        }
    }

    // ---------------- Updates ----------------
    fun checkForUpdates(manual: Boolean = true) {
        if (_update.value.checking) return
        _update.update { it.copy(checking = true, error = null) }
        viewModelScope.launch {
            try {
                val release = releases.latestRelease()
                settings.markUpdateChecked()
                val newer = isNewerVersion(release.version, BuildConfig.VERSION_NAME)
                _update.update {
                    it.copy(
                        checking = false,
                        release = release,
                        upToDate = !newer,
                        error = null
                    )
                }
                if (manual) {
                    notify(
                        if (newer) "Actualización disponible: v${release.version}"
                        else "Ya tienes la última versión (${BuildConfig.VERSION_NAME})"
                    )
                }
            } catch (e: UpdateException) {
                _update.update { it.copy(checking = false, error = e.message) }
                if (manual) notify(e.message ?: "No se pudo comprobar si hay actualizaciones.")
            } catch (e: Exception) {
                val message = "No se pudo comprobar si hay actualizaciones. Revisa tu conexión."
                _update.update { it.copy(checking = false, error = message) }
                if (manual) notify(message)
            }
        }
    }

    fun dismissUpdateNotice() {
        val release = _update.value.release ?: return
        settings.dismissUpdate(release.version)
    }

    fun downloadUpdate() {
        val release = _update.value.release ?: return
        if (_update.value.download.running) return
        try {
            val id = installer.startDownload(release)
            _update.update {
                it.copy(
                    download = UpdateDownloadState(downloadId = id, running = true),
                    installPermission = installer.canRequestInstall()
                )
            }
            pollUpdateDownload(id, release)
        } catch (e: Exception) {
            _update.update {
                it.copy(download = UpdateDownloadState(error = e.message ?: "No se pudo iniciar la descarga."))
            }
        }
    }

    private fun pollUpdateDownload(downloadId: Long, release: ReleaseInfo) {
        updateDownloadJob?.cancel()
        updateDownloadJob = viewModelScope.launch {
            val deadline = System.currentTimeMillis() + 15 * 60_000L
            while (System.currentTimeMillis() < deadline) {
                delay(800)
                val state = installer.query(downloadId)
                if (state.running) {
                    _update.update { it.copy(download = state) }
                    continue
                }
                if (state.error != null) {
                    _update.update { it.copy(download = state) }
                    notify(state.error)
                    return@launch
                }
                val file = state.file
                if (file == null || !file.exists()) {
                    _update.update {
                        it.copy(download = state.copy(error = "El APK descargado no se encontró en el almacenamiento."))
                    }
                    return@launch
                }
                val verified = installer.verifySha256(file, release.sha256)
                if (!verified) {
                    file.delete()
                    _update.update {
                        it.copy(
                            download = state.copy(
                                file = null,
                                verified = false,
                                error = "La verificación SHA-256 falló. El archivo se eliminó por seguridad."
                            )
                        )
                    }
                    notify("La verificación del APK falló. Descarga cancelada.")
                    return@launch
                }
                _update.update {
                    it.copy(
                        download = state.copy(verified = release.sha256 != null),
                        installPermission = installer.canRequestInstall()
                    )
                }
                notify("Actualización descargada. Pulsa «Instalar» para confirmar.")
                return@launch
            }
            _update.update {
                it.copy(download = it.download.copy(running = false, error = "La descarga tardó demasiado. Inténtalo de nuevo."))
            }
        }
    }

    fun installDownloadedUpdate() {
        val file = _update.value.download.file
        if (file == null || !file.exists()) {
            notify("Descarga primero la actualización.")
            return
        }
        if (!installer.canRequestInstall()) {
            installer.openUnknownSourcesSettings()
            notify("Permite instalar apps desconocidas para VibeMusic y vuelve a intentarlo.")
            return
        }
        runCatching { installer.install(file) }
            .onFailure { notify("No se pudo abrir el instalador: ${it.message}") }
    }

    fun openInstallPermissionSettings() = installer.openUnknownSourcesSettings()

    // ---------------- Google account ----------------
    fun signInWithGoogle() {
        if (_auth.value.loading) return
        _auth.value = AuthUiState(loading = true)
        viewModelScope.launch {
            try {
                val profile = googleAuth.signIn()
                settings.saveGoogleProfile(profile)
                _auth.value = AuthUiState()
                notify("Sesión iniciada como ${profile.name.ifBlank { profile.email }}")
            } catch (e: AuthException) {
                _auth.value = AuthUiState(error = e.message)
            } catch (e: Exception) {
                _auth.value = AuthUiState(error = "No se pudo iniciar sesión: ${e.message ?: "error desconocido"}")
            }
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            googleAuth.signOut()
            settings.clearGoogleProfile()
            _auth.value = AuthUiState()
            notify("Sesión de Google cerrada")
        }
    }

    fun clearAuthError() {
        _auth.update { it.copy(error = null) }
    }

    // ---------------- CSV import / export ----------------
    fun openCsvFile(uri: Uri, displayName: String?) {
        _csvImport.value = CsvImportState(visible = true, fileName = displayName ?: "archivo.csv")
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readBytes()
                        if (bytes.size > 8 * 1024 * 1024) throw IllegalArgumentException("El CSV supera el límite de 8 MB.")
                        String(bytes, Charsets.UTF_8)
                    } ?: throw IllegalArgumentException("No se pudo abrir el archivo.")
                }
                val table = parseCsv(text)
                if (table.headers.isEmpty()) {
                    throw IllegalArgumentException("El archivo no contiene filas CSV reconocibles.")
                }
                val mapping = guessMapping(table.headers)
                val records = extractRecords(table, mapping)
                if (records.isEmpty()) {
                    throw IllegalArgumentException(
                        "No se detectó una columna de título. Asigna manualmente las columnas."
                    )
                }
                val matches = matchRecords(records, allTracks.value)
                _csvImport.value = CsvImportState(
                    visible = true,
                    fileName = displayName ?: "archivo.csv",
                    table = table,
                    mapping = mapping,
                    records = records,
                    matches = matches
                )
            } catch (e: Exception) {
                _csvImport.update {
                    it.copy(error = e.message ?: "No se pudo leer el CSV.")
                }
            }
        }
    }

    fun updateCsvMapping(index: Int, role: CsvColumnRole) {
        val state = _csvImport.value
        val table = state.table ?: return
        if (index !in state.mapping.indices) return
        val mapping = state.mapping.toMutableList()
        if (role != CsvColumnRole.IGNORE) {
            // A role can only be assigned to one column at a time.
            for (i in mapping.indices) if (i != index && mapping[i] == role) mapping[i] = CsvColumnRole.IGNORE
        }
        mapping[index] = role
        val records = extractRecords(table, mapping)
        _csvImport.value = state.copy(
            mapping = mapping,
            records = records,
            matches = matchRecords(records, allTracks.value),
            error = if (records.isEmpty()) "Selecciona una columna de título para continuar." else null
        )
    }

    fun setCsvTargetPlaylist(playlistId: Long?) {
        _csvImport.update { it.copy(targetPlaylistId = playlistId) }
    }

    fun importCsv(createNewName: String? = null) {
        val state = _csvImport.value
        val matches = state.matches ?: return
        if (state.records.isEmpty()) {
            _csvImport.update { it.copy(error = "No hay canciones para importar.") }
            return
        }
        _csvImport.update { it.copy(importing = true, error = null) }
        viewModelScope.launch {
            try {
                val playlistId = state.targetPlaylistId ?: repository.createPlaylist(
                    createNewName?.takeIf { it.isNotBlank() }
                        ?: state.fileName.substringBeforeLast('.').take(60)
                )
                val existing = repository.playlistTracksOnce(playlistId).map { it.id }.toSet()
                var imported = 0
                var duplicates = 0
                matches.matched.forEach { match: CsvMatch ->
                    if (match.track.id in existing) {
                        duplicates++
                    } else {
                        repository.addToPlaylist(playlistId, match.track)
                        imported++
                    }
                }
                _csvImport.update {
                    it.copy(
                        importing = false,
                        finished = true,
                        imported = imported,
                        skippedDuplicates = duplicates + matches.duplicates,
                        unmatched = matches.unmatched
                    )
                }
                notify("Importación completada: $imported ${if (imported == 1) "canción" else "canciones"}")
            } catch (e: Exception) {
                _csvImport.update {
                    it.copy(importing = false, error = e.message ?: "No se pudo importar la lista.")
                }
            }
        }
    }

    fun dismissCsvImport() {
        _csvImport.value = CsvImportState()
    }

    fun exportPlaylist(playlistId: Long, uri: Uri) {
        viewModelScope.launch {
            try {
                val tracks = repository.playlistTracksOnce(playlistId)
                if (tracks.isEmpty()) {
                    notify("La lista está vacía; no hay nada que exportar.")
                    return@launch
                }
                val csv = exportCsv(tracks)
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openOutputStream(uri)?.use { output ->
                        output.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("No se pudo crear el archivo.")
                }
                notify("Lista exportada: ${tracks.size} ${if (tracks.size == 1) "canción" else "canciones"}")
            } catch (e: Exception) {
                notify("No se pudo exportar: ${e.message ?: "error desconocido"}")
            }
        }
    }

    // ---------------- Equalizer / Bluetooth ----------------
    private fun startAudioMonitoring() {
        bluetoothMonitor.start()
        EqualizerController.ensureReady()
        _eq.value = _eq.value.copy(
            available = EqualizerController.available,
            message = EqualizerController.message,
            bands = EqualizerController.bands(),
            autoApply = audioProfiles.autoApply,
            gainsDb = audioProfiles.customGains,
            profiles = audioProfiles.profiles().values.toList()
        )
        viewModelScope.launch {
            bluetoothMonitor.output.collect { output ->
                _eq.update {
                    it.copy(
                        activeDeviceKey = output?.key,
                        activeDeviceName = output?.name,
                        activeDeviceKind = output?.kind,
                        manufacturer = output?.manufacturer,
                        manufacturerInferred = output?.manufacturerInferred == true,
                        codec = output?.codec
                    )
                }
                val device = output ?: return@collect
                val profile = audioProfiles.profileFor(device.key) ?: return@collect
                _eq.update {
                    it.copy(presetName = profile.presetName, gainsDb = profile.gainsDb)
                }
                if (audioProfiles.autoApply && EqualizerController.available) {
                    if (EqualizerController.apply(profile.gainsDb)) {
                        _eq.update { it.copy(enabled = true) }
                    }
                }
            }
        }
    }

    fun applyEqPreset(name: String) {
        val preset = EqPresets.byName(name)
        applyEqGains(preset.gainsDb, preset.name)
    }

    fun applyEqGains(gainsDb: List<Float>, presetName: String = "Custom") {
        val clamped = if (gainsDb.size == EqPresets.anchorsHz.size) {
            gainsDb.map { it.coerceIn(-12f, 12f) }
        } else {
            List(EqPresets.anchorsHz.size) { 0f }
        }
        val applied = EqualizerController.apply(clamped)
        if (!applied) {
            _eq.update {
                it.copy(
                    presetName = presetName,
                    gainsDb = clamped,
                    enabled = false,
                    message = EqualizerController.message
                        ?: "Este dispositivo no permite aplicar el ecualizador global."
                )
            }
            notify("Ajuste guardado, pero el dispositivo no expone un ecualizador global.")
            return
        }
        if (presetName == "Custom") audioProfiles.customGains = clamped
        val device = _eq.value.activeDeviceKey
        if (device != null) {
            audioProfiles.saveProfile(device, presetName, clamped, _eq.value.autoApply)
        }
        _eq.update {
            it.copy(
                presetName = presetName,
                gainsDb = clamped,
                enabled = true,
                message = null,
                profiles = audioProfiles.profiles().values.toList()
            )
        }
        notify("Ecualización aplicada: $presetName")
    }

    fun setEqEnabled(enabled: Boolean) {
        EqualizerController.setEnabled(enabled)
        _eq.update { it.copy(enabled = enabled) }
    }

    fun setEqAutoApply(enabled: Boolean) {
        audioProfiles.autoApply = enabled
        _eq.update { it.copy(autoApply = enabled) }
        if (enabled) {
            val device = _eq.value.activeDeviceKey
            val profile = device?.let { audioProfiles.profileFor(it) }
            if (profile != null && EqualizerController.available) {
                if (EqualizerController.apply(profile.gainsDb)) {
                    _eq.update { it.copy(enabled = true, presetName = profile.presetName) }
                }
            }
        }
    }

    fun saveEqForActiveDevice() {
        val device = _eq.value.activeDeviceKey
        if (device == null) {
            notify("Conecta unos auriculares o un altavoz para guardar su perfil.")
            return
        }
        audioProfiles.saveProfile(
            device,
            _eq.value.presetName,
            _eq.value.gainsDb,
            _eq.value.autoApply
        )
        _eq.update { it.copy(profiles = audioProfiles.profiles().values.toList()) }
        notify("Perfil guardado para ${_eq.value.activeDeviceName ?: device}")
    }

    fun removeEqProfile(deviceKey: String) {
        audioProfiles.removeProfile(deviceKey)
        _eq.update { it.copy(profiles = audioProfiles.profiles().values.toList()) }
        notify("Perfil eliminado")
    }

    fun resetEq() {
        val flat = List(EqPresets.anchorsHz.size) { 0f }
        EqualizerController.setEnabled(false)
        audioProfiles.customGains = flat
        _eq.update {
            it.copy(
                presetName = "Balanced",
                gainsDb = flat,
                enabled = false
            )
        }
        notify("Ecualizador restablecido")
    }

    fun refreshBluetooth() = bluetoothMonitor.refresh()

    fun hasBluetoothPermission(): Boolean =
        BluetoothAudioMonitor.hasBluetoothPermission(getApplication())

    fun setDeviceModel(model: String) {
        val device = _eq.value.activeDeviceKey
        if (device == null) {
            notify("Conecta unos auriculares o un altavoz para asociarle un modelo.")
            return
        }
        audioProfiles.setDeviceModel(device, model)
        _eq.update { it.copy(profiles = audioProfiles.profiles().values.toList()) }
        notify("Modelo guardado para este dispositivo")
    }

    // ---------------- Listening history (local) ----------------
    private fun observeListening() {
        viewModelScope.launch {
            var active: Track? = null
            var maxPosition = 0L
            var duration = 0L
            playerState.collect { state ->
                val id = state.mediaId
                val track = id?.let { mediaId ->
                    allTracks.value.firstOrNull { it.id == mediaId }
                        ?: onlineCache.value[mediaId]
                }
                if (track == null) {
                    persistListening(active, maxPosition, duration)
                    active = null
                    maxPosition = 0L
                    duration = 0L
                    return@collect
                }
                if (active?.id != track.id) {
                    persistListening(active, maxPosition, duration)
                    active = track
                    maxPosition = 0L
                    duration = if (state.durationMs > 0) state.durationMs else track.durationMs
                } else if (state.durationMs > 0) {
                    duration = state.durationMs
                }
                if (state.positionMs > maxPosition) maxPosition = state.positionMs
            }
        }
    }

    private fun persistListening(track: Track?, positionMs: Long, durationMs: Long) {
        if (track == null) return
        if (track.source == TrackSource.ONLINE || track.source == TrackSource.YOUTUBE) return
        if (positionMs < 5_000) return
        val knownDuration = if (durationMs > 0) durationMs else track.durationMs
        val completed = knownDuration > 0 && positionMs >= knownDuration - 5_000
        val skipped = !completed && knownDuration > 0 && positionMs < knownDuration / 2
        viewModelScope.launch {
            repository.recordListening(track, positionMs, skipped, completed)
        }
    }

    private fun ListeningEventEntity.toSignal() = ListeningSignal(
        trackId = trackId,
        title = title,
        artist = artist,
        genre = genre,
        source = source,
        artworkUri = artworkUri,
        durationMs = durationMs,
        playedAt = playedAt,
        skipped = skipped,
        completed = completed,
        positionMs = positionMs
    )

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
    /**
     * Starts an authorized direct download in a foreground service so it keeps
     * running when the activity is backgrounded. Progress is exposed through
     * [downloadState]; the request survives process death and resumes with an
     * HTTP Range request.
     */
    fun startDownload(request: DownloadRequest) {
        val current = DownloadBus.state.value
        if (current?.isActive == true) {
            notify("Ya hay una descarga en curso. Espera o cancélala.")
            return
        }
        DownloadService.start(getApplication(), request)
    }

    fun cancelDownload() {
        DownloadService.cancel(getApplication())
    }

    fun retryDownload() {
        if (DownloadBus.lastRequest == null && DownloadBus.loadPending(getApplication()) == null) {
            notify("No hay ninguna descarga pendiente que reintentar.")
            return
        }
        DownloadService.retry(getApplication())
    }

    fun resumePendingDownload() {
        DownloadService.resumePending(getApplication())
    }

    fun dismissDownloadBanner() {
        if (DownloadBus.state.value?.isActive != true) DownloadBus.clear()
    }

    fun setDownloadQuality(quality: DownloadQuality) = settings.setDownloadQuality(quality)

    fun setRecommendationsEnabled(enabled: Boolean) {
        settings.setRecommendationsEnabled(enabled)
        notify(if (enabled) "Recomendaciones activadas" else "Recomendaciones desactivadas")
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

    fun clearListeningHistory() {
        viewModelScope.launch {
            repository.clearListeningHistory()
            notify("Historial de escucha borrado")
        }
    }

    fun setAmoled(enabled: Boolean) = repository.setAmoled(enabled)
    fun setAutoScan(enabled: Boolean) = repository.setAutoScan(enabled)
    fun setWifiOnly(enabled: Boolean) = repository.setWifiOnly(enabled)

    fun setTheme(style: VibeThemeStyle) {
        settings.setTheme(style)
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

    // ---------------- Privacy ----------------
    fun wipeAllData(deleteAppAudio: Boolean, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val removed = repository.wipeUserData(deleteAppAudio)
                audioProfiles.clear()
                DownloadBus.clear()
                DownloadBus.clearPending(getApplication())
                _eq.value = EqUiState(
                    available = EqualizerController.available,
                    message = EqualizerController.message
                )
                _csvImport.value = CsvImportState()
                _onlineSearch.value = OnlineSearchState()
                _searchQuery.value = ""
                youtubeSearchJob?.cancel()
                youTube.clearCache()
                _youtubeQuery.value = ""
                _youtubeSearch.value = YouTubeSearchState(configured = youTube.isConfigured)
                lastYouTubeQuery = ""
                _update.value = UpdateUiState(installPermission = installer.canRequestInstall())
                _auth.value = AuthUiState()
                notify(
                    if (deleteAppAudio) {
                        "Datos eliminados. Se borraron $removed archivos gestionados por VibeMusic."
                    } else {
                        "Datos de VibeMusic eliminados. Tus archivos musicales no se han tocado."
                    }
                )
                onDone(removed)
            } catch (e: Exception) {
                notify("No se pudieron borrar todos los datos: ${e.message}")
            }
        }
    }

    fun appVersion(): String = BuildConfig.VERSION_NAME

    fun lastUpdateCheck(): Long = settings.lastUpdateCheck()

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
        bluetoothMonitor.stop()
        sleepJob?.cancel()
        onlineSearchJob?.cancel()
        youtubeSearchJob?.cancel()
        updateDownloadJob?.cancel()
    }
}
