package com.vibemusic.app.data.model

enum class TrackSource {
    DEMO,
    LOCAL,
    DOWNLOAD,
    IMPORTED
}

/**
 * UI / playback representation of one playable audio item. It is deliberately
 * independent from Room entities so that local MediaStore items never need to
 * be written to the database on every scan.
 */
data class Track(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUri: String? = null,
    val source: TrackSource = TrackSource.LOCAL,
    val fileName: String? = null,
    val genre: String? = null,
    val addedAt: Long = 0L
) {
    val subtitle: String
        get() = when {
            artist.isNotBlank() && album.isNotBlank() -> "$artist · $album"
            artist.isNotBlank() -> artist
            album.isNotBlank() -> album
            else -> "VibeMusic"
        }

    val isOffline: Boolean
        get() = source == TrackSource.DEMO ||
            source == TrackSource.DOWNLOAD ||
            source == TrackSource.IMPORTED
}

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)
