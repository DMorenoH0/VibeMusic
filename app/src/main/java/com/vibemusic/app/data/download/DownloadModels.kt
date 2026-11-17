package com.vibemusic.app.data.download

/** Quality is a user preference, not a promise: direct files are never re-encoded. */
enum class DownloadQuality(val label: String, val explanation: String) {
    HIGH("Alta", "Archivo original, sin recodificar"),
    MEDIUM("Media", "Solo si la fuente ofrece una versión menor"),
    LOW("Baja", "Solo si la fuente ofrece una versión menor");

    companion object {
        fun byName(name: String?): DownloadQuality =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: HIGH
    }
}

data class DownloadRequest(
    val url: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String? = null,
    val quality: DownloadQuality = DownloadQuality.HIGH,
    val wifiOnly: Boolean = true,
    /** Stable identity of the song in the catalogue, used to avoid duplicates. */
    val trackId: String? = null,
    /**
     * True when the audio is already an authorised copy inside the app (the
     * bundled original samples). No network is used and the file is published
     * to the offline library in one step.
     */
    val localCopy: Boolean = false
)

enum class DownloadStatus { QUEUED, DOWNLOADING, FINISHED, FAILED, CANCELLED }

data class DownloadProgress(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val trackId: String? = null,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val bytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSecond: Long = 0L,
    val etaMs: Long? = null,
    val error: String? = null,
    val quality: DownloadQuality = DownloadQuality.HIGH,
    val savedTrackId: String? = null,
    val resumable: Boolean = true
) {
    val fraction: Float
        get() = if (totalBytes > 0) (bytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val percent: Int get() = (fraction * 100).toInt()

    val isActive: Boolean
        get() = status == DownloadStatus.QUEUED || status == DownloadStatus.DOWNLOADING

    val displayName: String
        get() = title.ifBlank { "Audio" }
}
