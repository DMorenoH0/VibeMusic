package com.vibemusic.app.data.download

import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import java.util.Locale

/**
 * Result of asking "can this song be saved for offline listening?".
 *
 * Every branch is explicit so the UI never offers a download that would fail
 * later. No source is scraped: only songs with an authorised file (our own
 * samples or a provider that grants download rights) are marked [Available].
 */
sealed interface SongDownloadAvailability {
    data class Available(
        val request: DownloadRequest,
        val label: String = "Descargar canción"
    ) : SongDownloadAvailability

    data class Downloaded(
        val label: String = "Ya descargada · disponible sin conexión"
    ) : SongDownloadAvailability

    data class Unavailable(
        val reason: String,
        val label: String = "No disponible para descarga offline"
    ) : SongDownloadAvailability
}

/**
 * Pure, testable rules that turn a [Track] into a one-tap download request.
 *
 * The same identity key is stored in `DownloadEntity.sourceUrl`, which makes
 * duplicate detection survive restarts without touching the database schema.
 */
object SongDownloadResolver {

    /** Stable key used to remember that a song was already downloaded. */
    fun keyFor(track: Track): String? = when {
        track.source == TrackSource.DEMO -> track.id
        !track.downloadUrl.isNullOrBlank() -> track.downloadUrl.trim()
        else -> null
    }

    /** Lowercase "title|artist" pair, used as a second duplicate guard. */
    fun identity(track: Track): String =
        identityOf(track.title, track.artist)

    fun identityOf(title: String, artist: String): String =
        "${title.trim().lowercase(Locale.ROOT)}|" +
            artist.trim().lowercase(Locale.ROOT)

    fun resolve(
        track: Track,
        downloadedKeys: Set<String> = emptySet(),
        downloadedIdentities: Set<String> = emptySet(),
        quality: DownloadQuality = DownloadQuality.HIGH,
        wifiOnly: Boolean = true
    ): SongDownloadAvailability {
        val key = keyFor(track)
        if (key != null && key in downloadedKeys) return SongDownloadAvailability.Downloaded()
        if (track.source == TrackSource.DOWNLOAD) return SongDownloadAvailability.Downloaded()

        val direct = track.downloadUrl?.trim().orEmpty()
        if (direct.startsWith("https://", ignoreCase = true) &&
            track.source != TrackSource.YOUTUBE &&
            track.source != TrackSource.ONLINE
        ) {
            if (identity(track) in downloadedIdentities) {
                return SongDownloadAvailability.Downloaded()
            }
            return SongDownloadAvailability.Available(
                DownloadRequest(
                    url = direct,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    artworkUrl = track.artworkUri,
                    quality = quality,
                    wifiOnly = wifiOnly,
                    trackId = key ?: track.id
                )
            )
        }

        return when (track.source) {
            TrackSource.DEMO -> {
                if (identity(track) in downloadedIdentities) {
                    SongDownloadAvailability.Downloaded()
                } else {
                    SongDownloadAvailability.Available(
                        DownloadRequest(
                            url = track.uri,
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            artworkUrl = track.artworkUri,
                            quality = DownloadQuality.HIGH,
                            wifiOnly = false,
                            trackId = track.id,
                            localCopy = true
                        )
                    )
                }
            }

            TrackSource.LOCAL -> SongDownloadAvailability.Unavailable(
                reason = "Este archivo ya vive en tu dispositivo y se reproduce sin conexión. " +
                    "VibeMusic no necesita una segunda copia."
            )

            TrackSource.IMPORTED -> SongDownloadAvailability.Downloaded(
                label = "Ya disponible sin conexión · importada"
            )

            TrackSource.DOWNLOAD -> SongDownloadAvailability.Downloaded()

            TrackSource.ONLINE -> SongDownloadAvailability.Unavailable(
                reason = "Las vistas previas de Deezer tienen licencia de reproducción, " +
                    "no de descarga. No se pueden guardar como archivo."
            )

            TrackSource.YOUTUBE -> SongDownloadAvailability.Unavailable(
                reason = "YouTube no autoriza la descarga de audio. " +
                    "Puedes verlo en el reproductor oficial integrado."
            )
        }
    }
}
