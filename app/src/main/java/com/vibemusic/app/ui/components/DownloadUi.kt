package com.vibemusic.app.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.vibemusic.app.data.download.DownloadProgress
import com.vibemusic.app.data.download.DownloadStatus
import com.vibemusic.app.data.download.SongDownloadResolver
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import java.util.Locale

/** Compact state painted inside every song row. */
data class TrackDownloadUi(
    val downloaded: Boolean = false,
    val active: Boolean = false,
    val percent: Int = 0,
    val failed: Boolean = false
)

/**
 * A single lookup shared through the composition so any [TrackRow] can show the
 * same download state without threading extra parameters through every screen.
 */
val LocalTrackDownloadUi = staticCompositionLocalOf<(Track) -> TrackDownloadUi> {
    { TrackDownloadUi() }
}

object TrackDownloadStatus {

    fun identity(track: Track): String =
        "${track.title.trim().lowercase(Locale.ROOT)}|" +
            track.artist.trim().lowercase(Locale.ROOT)

    fun uiFor(
        track: Track,
        downloadedKeys: Set<String>,
        downloadedIdentities: Set<String>,
        progress: DownloadProgress?
    ): TrackDownloadUi {
        val key = SongDownloadResolver.keyFor(track)
        val downloaded = when {
            track.source == TrackSource.DOWNLOAD || track.source == TrackSource.IMPORTED -> true
            key != null && key in downloadedKeys -> true
            track.source == TrackSource.YOUTUBE || track.source == TrackSource.ONLINE -> false
            identity(track) in downloadedIdentities -> true
            else -> false
        }
        if (downloaded) return TrackDownloadUi(downloaded = true)

        val matches = progress != null && matches(progress, track)
        if (!matches) return TrackDownloadUi()
        return when (progress.status) {
            DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> TrackDownloadUi(
                active = true,
                percent = progress.percent
            )
            DownloadStatus.FAILED -> TrackDownloadUi(failed = true)
            else -> TrackDownloadUi()
        }
    }

    private fun matches(progress: DownloadProgress, track: Track): Boolean {
        if (progress.trackId != null) return progress.trackId == track.id
        return progress.title.equals(track.title, ignoreCase = true) &&
            progress.artist.equals(track.artist, ignoreCase = true)
    }

    fun identityOf(title: String, artist: String): String =
        "${title.trim().lowercase(Locale.ROOT)}|${artist.trim().lowercase(Locale.ROOT)}"
}
