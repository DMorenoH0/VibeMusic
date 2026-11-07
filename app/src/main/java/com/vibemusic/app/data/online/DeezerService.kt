package com.vibemusic.app.data.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Read-only client for the public Deezer catalogue API.
 *
 * Why Deezer:
 *  - no API key, no subscription and no OAuth for catalogue reads;
 *  - returns title, artist, album, cover, duration and an official 30 s preview;
 *  - previews are promotional clips served by Deezer's own CDN.
 *
 * VibeMusic only streams those previews. It does not download full tracks,
 * bypass DRM or scrape any streaming platform.
 */
class DeezerService {

    private val cache = ResponseCache<String>(64)

    suspend fun search(query: String, limit: Int = 30): List<OnlineTrack> =
        withContext(Dispatchers.IO) {
            val clean = query.trim()
            if (clean.length < 2) return@withContext emptyList()
            val key = "search:${clean.lowercase()}:$limit"
            val body = cache.get(key) ?: HttpJson.get(
                "https://api.deezer.com/search?q=${encode(clean)}&limit=${limit.coerceIn(1, 50)}"
            ).also { cache.put(key, it) }
            parseSearch(body)
        }

    suspend fun album(albumId: String): OnlineAlbum = withContext(Dispatchers.IO) {
        val key = "album:$albumId"
        val body = cache.get(key) ?: HttpJson.get(
            "https://api.deezer.com/album/${encode(albumId)}"
        ).also { cache.put(key, it) }
        parseAlbum(body)
    }

    suspend fun artist(artistId: String): OnlineArtist = withContext(Dispatchers.IO) {
        val key = "artist:$artistId"
        val cached = cache.get(key)
        if (cached != null) return@withContext parseArtist(cached, parseAlbumList("[]"))

        val info = HttpJson.get("https://api.deezer.com/artist/${encode(artistId)}")
        val top = HttpJson.get("https://api.deezer.com/artist/${encode(artistId)}/top?limit=25")
        val albums = HttpJson.get("https://api.deezer.com/artist/${encode(artistId)}/albums?limit=20")
        val merged = JSONObject()
            .put("info", JSONObject(info))
            .put("top", JSONObject(top))
            .put("albums", JSONObject(albums))
            .toString()
        cache.put(key, merged)
        parseArtist(merged, parseAlbumList(albums))
    }

    fun clearCache() = cache.clear()

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

// ---------------------------------------------------------------------------
// Pure parsers (covered by unit tests, no Android dependencies besides org.json)
// ---------------------------------------------------------------------------

fun parseSearch(json: String): List<OnlineTrack> {
    val root = JSONObject(json)
    throwIfApiError(root)
    val data = root.optJSONArray("data") ?: JSONArray()
    return buildList {
        for (i in 0 until data.length()) {
            data.optJSONObject(i)?.let { add(parseTrack(it)) }
        }
    }.filter { it.title.isNotBlank() }
}

fun parseAlbum(json: String): OnlineAlbum {
    val root = JSONObject(json)
    throwIfApiError(root)
    val artist = root.optJSONObject("artist")
    val tracksJson = root.optJSONObject("tracks")?.optJSONArray("data") ?: JSONArray()
    val tracks = buildList {
        for (i in 0 until tracksJson.length()) {
            tracksJson.optJSONObject(i)?.let { add(parseTrack(it)) }
        }
    }
    val cover = coverOf(root)
    val albumTitle = root.optString("title").ifBlank { "Álbum sin título" }
    val albumId = root.optString("id")
    return OnlineAlbum(
        id = albumId,
        title = albumTitle,
        artist = artist?.optString("name").orEmpty().ifBlank { "Artista desconocido" },
        artistId = artist?.optString("id")?.takeIf { it.isNotBlank() },
        artworkUrl = cover,
        releaseDate = root.optString("release_date").takeIf { it.isNotBlank() },
        trackCount = root.optInt("nb_tracks", tracks.size),
        tracks = tracks.map {
            it.copy(
                album = it.album.ifBlank { albumTitle },
                albumId = it.albumId ?: albumId,
                artworkUrl = it.artworkUrl ?: cover
            )
        }
    )
}

fun parseArtist(json: String, albumList: List<OnlineAlbum> = emptyList()): OnlineArtist {
    val root = JSONObject(json)
    val info = root.optJSONObject("info") ?: root
    throwIfApiError(info)
    val top = root.optJSONObject("top")?.let { parseSearch(it.toString()) } ?: emptyList()
    val albumsFromBody = root.optJSONObject("albums")?.let { parseAlbumList(it.toString()) }
    return OnlineArtist(
        id = info.optString("id"),
        name = info.optString("name").ifBlank { "Artista desconocido" },
        artworkUrl = coverOf(info),
        fanCount = info.optLong("nb_fan", 0L),
        albumCount = info.optInt("nb_album", albumsFromBody?.size ?: albumList.size),
        topTracks = top,
        albums = albumsFromBody ?: albumList
    )
}

fun parseAlbumList(json: String): List<OnlineAlbum> {
    val root = JSONObject(json)
    throwIfApiError(root)
    val data = root.optJSONArray("data") ?: JSONArray()
    return buildList {
        for (i in 0 until data.length()) {
            val obj = data.optJSONObject(i) ?: continue
            val artist = obj.optJSONObject("artist")
            add(
                OnlineAlbum(
                    id = obj.optString("id"),
                    title = obj.optString("title").ifBlank { "Álbum sin título" },
                    artist = artist?.optString("name").orEmpty(),
                    artistId = artist?.optString("id")?.takeIf { it.isNotBlank() },
                    artworkUrl = coverOf(obj),
                    releaseDate = obj.optString("release_date").takeIf { it.isNotBlank() },
                    trackCount = obj.optInt("nb_tracks", 0),
                    tracks = emptyList()
                )
            )
        }
    }.filter { it.id.isNotBlank() }
}

private fun parseTrack(obj: JSONObject): OnlineTrack {
    val artist = obj.optJSONObject("artist")
    val album = obj.optJSONObject("album")
    val preview = obj.optString("preview").takeIf { it.startsWith("https://") }
    return OnlineTrack(
        id = obj.optString("id"),
        title = obj.optString("title"),
        artist = artist?.optString("name").orEmpty(),
        artistId = artist?.optString("id")?.takeIf { it.isNotBlank() },
        album = album?.optString("title").orEmpty(),
        albumId = album?.optString("id")?.takeIf { it.isNotBlank() },
        durationMs = obj.optLong("duration", 0L) * 1000L,
        artworkUrl = coverOf(album ?: obj),
        previewUrl = preview
    )
}

private fun coverOf(obj: JSONObject?): String? {
    if (obj == null) return null
    val keys = listOf(
        "cover_xl", "cover_big", "cover_medium", "cover_small",
        "picture_xl", "picture_big", "picture_medium", "picture_small"
    )
    for (key in keys) {
        val value = obj.optString(key)
        if (value.startsWith("https://")) return value
    }
    return null
}

private fun throwIfApiError(obj: JSONObject) {
    val error = obj.opt("error") ?: return
    val message = when (error) {
        is JSONObject -> error.optString("message")
        else -> error.toString()
    }
    throw OnlineApiException(
        when {
            message.contains("quota", true) ->
                "Se alcanzó el límite temporal de búsquedas. Espera unos segundos."
            message.isBlank() -> "El servicio de búsqueda devolvió un error."
            else -> "Búsqueda no disponible: $message"
        }
    )
}
