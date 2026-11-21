package com.vibemusic.app.data.model

enum class TrackSource {
    DEMO,
    LOCAL,
    DOWNLOAD,
    IMPORTED,
    /** Streaming preview resolved through a public catalogue API. */
    ONLINE,

    /** Full-length track streamed from Audius under its open catalogue terms. */
    AUDIUS,

    /** Reference to a YouTube video played by the official embedded player. */
    YOUTUBE
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
    val addedAt: Long = 0L,
    val albumId: String? = null,
    val artistId: String? = null,
    val previewOnly: Boolean = false,
    /**
     * Direct HTTPS file the provider explicitly authorises for download. It is
     * only set by catalogue entries that grant those rights; preview-only
     * streams and YouTube references leave it null.
     */
    val downloadUrl: String? = null,
    /**
     * Why the provider does not authorise an offline copy. It is shown instead
     * of a download button so the user never starts a transfer that the rights
     * holder has not allowed.
     */
    val downloadBlockedReason: String? = null
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

    /**
     * Deezer preview URLs expire and are not backed by a local file, so they
     * are never written to favourites, playlists or the recent history. Audius
     * tracks use a stable catalogue id and endpoint, so they can be saved.
     */
    val isPersistable: Boolean
        get() = source != TrackSource.ONLINE && uri.isNotBlank()
}

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)
