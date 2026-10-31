package com.vibemusic.app.data

import android.content.Context
import android.net.Uri
import com.vibemusic.app.data.db.DownloadEntity
import com.vibemusic.app.data.db.FavoriteEntity
import com.vibemusic.app.data.db.PlaylistEntity
import com.vibemusic.app.data.db.PlaylistWithCount
import com.vibemusic.app.data.db.RecentEntity
import com.vibemusic.app.data.db.VibeMusicDatabase
import com.vibemusic.app.data.db.toFavorite
import com.vibemusic.app.data.db.toRecent
import com.vibemusic.app.data.db.toTrack
import com.vibemusic.app.data.db.toPlaylistTrack
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Ready(val count: Int) : ScanState
    data object PermissionRequired : ScanState
    data class Failed(val message: String) : ScanState
}

class MusicRepository(context: Context, val settings: SettingsStore) {

    private val appContext = context.applicationContext
    private val dao = VibeMusicDatabase.get(appContext).dao()
    private val installer = DemoInstaller(appContext)
    private val scanner = LocalMusicScanner(appContext)
    private val downloader = AudioDownloader(appContext)

    private val _demoTracks = MutableStateFlow<List<Track>>(emptyList())
    val demoTracks: StateFlow<List<Track>> = _demoTracks.asStateFlow()

    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val favoriteEntities: Flow<List<FavoriteEntity>> = dao.observeFavorites()
    val favorites: Flow<List<Track>> = dao.observeFavorites().map { list -> list.map { it.toTrack() } }
    val playlists: Flow<List<PlaylistWithCount>> = dao.observePlaylists()
    val recent: Flow<List<Track>> = dao.observeRecent().map { list -> list.map { it.toTrack() } }
    val downloads: Flow<List<Track>> = dao.observeDownloads().map { list -> list.map { it.toTrack() } }

    val amoled: StateFlow<Boolean> = settings.amoled
    val autoScan: StateFlow<Boolean> = settings.autoScan
    val wifiOnly: StateFlow<Boolean> = settings.wifiOnly

    suspend fun initialize() {
        _demoTracks.value = installer.install()
        when {
            !scanner.hasPermission() -> _scanState.value = ScanState.PermissionRequired
            settings.autoScan.value -> refreshLocal(showScanning = false)
            else -> _scanState.value = ScanState.Idle
        }
    }

    fun hasAudioPermission(): Boolean = scanner.hasPermission()

    fun audioPermission(): String = LocalMusicScanner.audioPermission

    suspend fun refreshLocal(showScanning: Boolean = true) {
        if (!scanner.hasPermission()) {
            _scanState.value = ScanState.PermissionRequired
            return
        }
        if (showScanning) _scanState.value = ScanState.Scanning
        _scanState.value = try {
            val tracks = scanner.scan()
            _localTracks.value = tracks
            ScanState.Ready(tracks.size)
        } catch (e: SecurityException) {
            _scanState.value = ScanState.PermissionRequired
            return
        } catch (e: Exception) {
            ScanState.Failed(e.message ?: "No se pudo leer la biblioteca local.")
        }
    }

    fun markPermissionDenied() {
        _scanState.value = ScanState.PermissionRequired
    }

    // ---------------- Favorites ----------------
    suspend fun toggleFavorite(track: Track) {
        withContext(Dispatchers.IO) {
            if (dao.isFavorite(track.id)) dao.deleteFavorite(track.id)
            else dao.upsertFavorite(track.toFavorite())
        }
    }

    suspend fun removeFavorite(trackId: String) = withContext(Dispatchers.IO) {
        dao.deleteFavorite(trackId)
    }

    // ---------------- Playlists ----------------
    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val clean = name.trim().take(60).ifBlank { "Mi lista" }
        val now = System.currentTimeMillis()
        dao.insertPlaylist(PlaylistEntity(name = clean, createdAt = now, updatedAt = now))
    }

    suspend fun renamePlaylist(id: Long, name: String) = withContext(Dispatchers.IO) {
        dao.renamePlaylist(id, name.trim().take(60).ifBlank { "Mi lista" }, System.currentTimeMillis())
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(id)
    }

    fun observePlaylistTracks(id: Long): Flow<List<Track>> =
        dao.observePlaylistTracks(id).map { list -> list.map { it.toTrack() } }

    suspend fun addToPlaylist(playlistId: Long, track: Track): AddResult = withContext(Dispatchers.IO) {
        val existing = dao.playlistTrackIds(playlistId)
        if (existing.contains(track.id)) return@withContext AddResult.AlreadyThere
        val count = dao.playlistTrackCount(playlistId)
        dao.upsertPlaylistTrack(track.toPlaylistTrack(playlistId, count))
        dao.touchPlaylist(playlistId, System.currentTimeMillis())
        AddResult.Added
    }

    suspend fun removeFromPlaylist(playlistId: Long, trackId: String) = withContext(Dispatchers.IO) {
        dao.deletePlaylistTrack(playlistId, trackId)
        dao.touchPlaylist(playlistId, System.currentTimeMillis())
    }

    enum class AddResult { Added, AlreadyThere }

    // ---------------- Recent ----------------
    suspend fun recordRecent(track: Track) = withContext(Dispatchers.IO) {
        if (track.source == TrackSource.LOCAL && !scanner.hasPermission()) return@withContext
        dao.upsertRecent(track.toRecent())
        dao.trimRecent()
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) { dao.clearRecent() }

    // ---------------- Downloads / imports ----------------
    suspend fun download(
        url: String,
        title: String,
        artist: String,
        onProgress: (Float) -> Unit
    ): Track = withContext(Dispatchers.IO) {
        val fetched = downloader.download(url, title, artist, settings.wifiOnly.value, onProgress)
        storeFetched(fetched, TrackSource.DOWNLOAD, url)
    }

    suspend fun importDocument(uri: Uri, displayName: String?): Track = withContext(Dispatchers.IO) {
        val fetched = downloader.importDocument(uri, displayName)
        storeFetched(fetched, TrackSource.IMPORTED, uri.toString())
    }

    private suspend fun storeFetched(
        fetched: AudioDownloader.FetchedAudio,
        source: TrackSource,
        sourceUrl: String
    ): Track {
        val prefix = if (source == TrackSource.IMPORTED) "import" else "offline"
        val id = "$prefix:${UUID.randomUUID()}"
        val entity = DownloadEntity(
            trackId = id,
            title = fetched.title,
            artist = fetched.artist,
            album = fetched.album,
            fileName = fetched.file.absolutePath,
            sourceUrl = sourceUrl,
            durationMs = fetched.durationMs,
            downloadedAt = System.currentTimeMillis(),
            source = source.name
        )
        dao.upsertDownload(entity)
        return entity.toTrack()
    }

    suspend fun deleteDownload(track: Track) = withContext(Dispatchers.IO) {
        val entity = dao.getDownload(track.id)
        dao.deleteDownload(track.id)
        entity?.let { File(it.fileName).takeIf { file -> file.exists() }?.delete() }
    }

    fun downloadedFileExists(track: Track): Boolean =
        track.fileName?.let { File(it).exists() } ?: false

    // ---------------- Settings ----------------
    fun setAmoled(enabled: Boolean) = settings.setAmoled(enabled)
    fun setAutoScan(enabled: Boolean) = settings.setAutoScan(enabled)
    fun setWifiOnly(enabled: Boolean) = settings.setWifiOnly(enabled)
}
