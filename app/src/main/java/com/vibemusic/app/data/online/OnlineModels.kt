package com.vibemusic.app.data.online

import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource

/**
 * A catalogue result. [previewUrl] is the official 30-second preview supplied by
 * the Deezer public API; VibeMusic never downloads or reconstructs full tracks.
 */
data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String?,
    val album: String,
    val albumId: String?,
    val durationMs: Long,
    val artworkUrl: String?,
    val previewUrl: String?
) {
    val playable: Boolean get() = !previewUrl.isNullOrBlank()

    fun toTrack(): Track = Track(
        id = "deezer:track:$id",
        uri = previewUrl.orEmpty(),
        title = title.ifBlank { "Pista sin título" },
        artist = artist.ifBlank { "Artista desconocido" },
        album = album,
        durationMs = durationMs,
        artworkUri = artworkUrl,
        source = TrackSource.ONLINE,
        albumId = albumId,
        artistId = artistId,
        previewOnly = true
    )
}

data class OnlineAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String?,
    val artworkUrl: String?,
    val releaseDate: String?,
    val trackCount: Int,
    val tracks: List<OnlineTrack>
) {
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

data class OnlineArtist(
    val id: String,
    val name: String,
    val artworkUrl: String?,
    val fanCount: Long,
    val albumCount: Int,
    val topTracks: List<OnlineTrack>,
    val albums: List<OnlineAlbum>
)

class OnlineApiException(message: String) : Exception(message)
