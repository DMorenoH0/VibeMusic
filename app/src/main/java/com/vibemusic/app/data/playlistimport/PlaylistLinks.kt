package com.vibemusic.app.data.playlistimport

import java.net.URI
import java.net.URLDecoder

/**
 * Playlist origins VibeMusic can read through their official APIs. Audio is
 * always resolved later from Audius; Spotify and YouTube Music only provide
 * metadata and are never used as audio sources.
 */
enum class PlaylistPlatform(val label: String, val shortLabel: String) {
    SPOTIFY("Spotify", "Spotify"),
    YOUTUBE_MUSIC("YouTube Music", "YouTube Music")
}

data class PlaylistLink(
    val platform: PlaylistPlatform,
    val playlistId: String,
    val url: String
)

sealed interface PlaylistLinkResult {
    data class Valid(val link: PlaylistLink) : PlaylistLinkResult

    data class Invalid(val message: String) : PlaylistLinkResult
}

/**
 * Strict parser for public playlist links. It accepts only the documented URL
 * shapes of each platform and rejects track/album links or arbitrary hosts, so
 * a pasted string never becomes an API request to an unexpected domain.
 */
object PlaylistLinkParser {

    private val spotifyId = Regex("^[A-Za-z0-9]{22}$")
    private val youtubePlaylistId = Regex("^[A-Za-z0-9_-]{10,100}$")
    private val spotifyUri = Regex("^spotify:playlist:([A-Za-z0-9]{22})$", RegexOption.IGNORE_CASE)

    fun detect(raw: String): PlaylistPlatform? = when (parse(raw)) {
        is PlaylistLinkResult.Valid -> (parse(raw) as PlaylistLinkResult.Valid).link.platform
        is PlaylistLinkResult.Invalid -> null
    }

    fun parse(raw: String): PlaylistLinkResult {
        val clean = raw.trim()
        if (clean.isEmpty()) {
            return PlaylistLinkResult.Invalid(
                "Pega el enlace de una playlist de Spotify o de YouTube Music."
            )
        }
        spotifyUri.matchEntire(clean)?.let { match ->
            val id = match.groupValues[1]
            return PlaylistLinkResult.Valid(
                PlaylistLink(
                    platform = PlaylistPlatform.SPOTIFY,
                    playlistId = id,
                    url = "https://open.spotify.com/playlist/$id"
                )
            )
        }
        parseSpotifyUrl(clean)?.let { return it }
        parseYouTubeUrl(clean)?.let { return it }
        return PlaylistLinkResult.Invalid(
            "El enlace no es de una playlist de Spotify ni de YouTube Music. " +
                "Copia el enlace completo desde «Compartir → Copiar enlace»."
        )
    }

    private fun parseSpotifyUrl(raw: String): PlaylistLinkResult? {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        if (host != "open.spotify.com" && host != "play.spotify.com") return null
        val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
        val playlistIndex = segments.indexOfFirst { it.equals("playlist", ignoreCase = true) }
        if (playlistIndex < 0) {
            val other = listOf("track", "album", "artist", "episode", "show")
                .firstOrNull { kind -> segments.any { it.equals(kind, ignoreCase = true) } }
            return PlaylistLinkResult.Invalid(
                if (other == null) {
                    "El enlace de Spotify no apunta a una playlist."
                } else {
                    "Ese enlace de Spotify es de tipo «$other». Solo se pueden importar playlists."
                }
            )
        }
        val id = segments.getOrNull(playlistIndex + 1).orEmpty()
        if (!spotifyId.matches(id)) {
            return PlaylistLinkResult.Invalid(
                "El identificador de la playlist de Spotify no es válido."
            )
        }
        return PlaylistLinkResult.Valid(
            PlaylistLink(
                platform = PlaylistPlatform.SPOTIFY,
                playlistId = id,
                url = "https://open.spotify.com/playlist/$id"
            )
        )
    }

    private fun parseYouTubeUrl(raw: String): PlaylistLinkResult? {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val allowedHosts = setOf(
            "music.youtube.com",
            "www.youtube.com",
            "youtube.com",
            "m.youtube.com"
        )
        if (host !in allowedHosts) return null
        val path = uri.path.orEmpty()
        val isPlaylistPath = path.equals("/playlist", ignoreCase = true)
        val isWatchPath = path.equals("/watch", ignoreCase = true)
        if (!isPlaylistPath && !isWatchPath) {
            return PlaylistLinkResult.Invalid(
                "El enlace de YouTube no apunta a una playlist."
            )
        }
        val list = queryParam(uri.rawQuery, "list")
        if (list.isNullOrBlank() || !youtubePlaylistId.matches(list)) {
            return PlaylistLinkResult.Invalid(
                "El enlace de YouTube no incluye una playlist válida (parámetro «list»)."
            )
        }
        return PlaylistLinkResult.Valid(
            PlaylistLink(
                platform = PlaylistPlatform.YOUTUBE_MUSIC,
                playlistId = list,
                url = "https://music.youtube.com/playlist?list=$list"
            )
        )
    }

    private fun queryParam(rawQuery: String?, name: String): String? {
        val query = rawQuery ?: return null
        return query.split('&').firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=', "")
            if (!key.equals(name, ignoreCase = true)) return@firstNotNullOfOrNull null
            runCatching {
                URLDecoder.decode(part.substringAfter('=', ""), "UTF-8")
            }.getOrDefault(part.substringAfter('=', ""))
        }
    }
}
