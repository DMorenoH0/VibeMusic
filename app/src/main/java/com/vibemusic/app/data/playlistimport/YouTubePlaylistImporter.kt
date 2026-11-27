package com.vibemusic.app.data.playlistimport

import com.vibemusic.app.data.online.HttpJson
import com.vibemusic.app.data.online.HttpStatusException
import com.vibemusic.app.data.youtube.mapYouTubeError
import com.vibemusic.app.data.youtube.parseIsoDurationMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale

/**
 * Reads playlist metadata through the official YouTube Data API v3.
 *
 * Only metadata is requested: `playlistItems.list` (1 quota unit per page) and
 * `videos.list` (1 unit per 50 videos) give title, channel, position and
 * duration. VibeMusic never requests audio streams or extracts media from
 * YouTube; playback always uses the official embedded player, and imported
 * songs are matched against Audius separately.
 */
class YouTubePlaylistImporter(
    private val apiKey: String,
    private val fetch: (url: String, timeoutMs: Int) -> String = { url, timeout ->
        HttpJson.get(url, timeout)
    }
) {

    val configured: Boolean get() = apiKey.trim().isNotEmpty()

    suspend fun import(link: PlaylistLink): ImportedPlaylist = withContext(Dispatchers.IO) {
        if (link.platform != PlaylistPlatform.YOUTUBE_MUSIC) {
            throw YouTubePlaylistImportException("El enlace no es de YouTube Music.")
        }
        if (!configured) {
            throw YouTubePlaylistImportException(
                "YouTube Music necesita una clave de YouTube Data API v3. " +
                    "Añádela en «Credenciales de YouTube» o crea el APK con la clave incluida."
            )
        }
        val details = try {
            fetch(buildPlaylistDetailsUrl(link.playlistId, apiKey), 20_000)
        } catch (e: HttpStatusException) {
            throw mapImportError(e.statusCode, e.responseBody)
        } catch (e: IOException) {
            throw YouTubePlaylistImportException(
                "No se pudo conectar con YouTube. Comprueba tu conexión."
            )
        }
        val header = parsePlaylistHeader(details)
            ?: throw YouTubePlaylistImportException(
                "La playlist no existe, es privada o no pertenece a este canal."
            )

        val collected = LinkedHashMap<String, ImportedPlaylistTrack>()
        var pageToken: String? = null
        var pages = 0
        var position = 0
        while (pages < MAX_PAGES) {
            val json = try {
                fetch(buildPlaylistItemsUrl(link.playlistId, apiKey, pageToken), 20_000)
            } catch (e: HttpStatusException) {
                throw mapImportError(e.statusCode, e.responseBody)
            } catch (e: IOException) {
                throw YouTubePlaylistImportException(
                    "No se pudieron leer las canciones de la playlist de YouTube."
                )
            }
            val page = parsePlaylistItemsPage(json, fallbackStart = position)
            page.tracks.forEach { track ->
                val key = "${track.videoId}:${track.position}"
                collected[key] = track
            }
            position = maxOf(position, page.nextPosition)
            pageToken = page.nextPageToken
            pages++
            if (pageToken.isNullOrBlank()) break
        }

        if (collected.isEmpty()) {
            throw YouTubePlaylistImportException(
                "La playlist de YouTube Music no contiene vídeos accesibles."
            )
        }

        val withDurations = fillDurations(collected.values.toList(), apiKey, fetch)
        ImportedPlaylist(
            platform = PlaylistPlatform.YOUTUBE_MUSIC,
            playlistId = link.playlistId,
            name = header.name,
            owner = header.owner,
            description = header.description,
            artworkUrl = header.artworkUrl,
            sourceUrl = link.url,
            tracks = withDurations.sortedBy { it.position }
        )
    }

    private companion object {
        const val MAX_PAGES = 400 // 20.000 canciones, igual que el límite defensivo de Spotify
    }
}

// ---------------------------------------------------------------------------
// Pure parsers and helpers, unit-tested with representative API payloads
// ---------------------------------------------------------------------------

data class YouTubePlaylistHeader(
    val name: String,
    val owner: String,
    val description: String?,
    val artworkUrl: String?
)

data class YouTubeItemsPage(
    val tracks: List<ImportedPlaylistTrack>,
    val nextPageToken: String?,
    val nextPosition: Int
)

fun parsePlaylistHeader(json: String): YouTubePlaylistHeader? {
    val items = JSONObject(json).optJSONArray("items") ?: return null
    val item = items.optJSONObject(0) ?: return null
    val snippet = item.optJSONObject("snippet") ?: return null
    val name = snippet.optString("title").trim()
    if (name.isBlank()) return null
    return YouTubePlaylistHeader(
        name = name,
        owner = snippet.optString("channelTitle").trim().ifBlank { "YouTube Music" },
        description = snippet.optString("description").trim().takeIf { it.isNotBlank() },
        artworkUrl = youTubeThumbnail(snippet)
    )
}

fun parsePlaylistItemsPage(json: String, fallbackStart: Int = 0): YouTubeItemsPage {
    val root = JSONObject(json)
    val items = root.optJSONArray("items")
    val tracks = ArrayList<ImportedPlaylistTrack>()
    var maxPosition = fallbackStart - 1
    if (items != null) {
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val snippet = item.optJSONObject("snippet") ?: continue
            val videoId = item.optJSONObject("contentDetails")
                ?.optString("videoId").orEmpty()
                .ifBlank { snippet.optJSONObject("resourceId")?.optString("videoId").orEmpty() }
            if (!videoId.matches(Regex("[A-Za-z0-9_-]{5,20}"))) continue
            val rawTitle = snippet.optString("title").trim()
            val officialPosition = snippet.optInt("position", -1)
            val position = if (officialPosition >= 0) officialPosition else fallbackStart + i
            maxPosition = maxOf(maxPosition, position)
            if (isUnavailableTitle(rawTitle)) continue
            val channel = snippet.optString("videoOwnerChannelTitle")
                .ifBlank { snippet.optString("channelTitle") }
                .trim()
            val split = splitYouTubeMusicTitle(rawTitle, channel)
            tracks.add(
                ImportedPlaylistTrack(
                    position = position,
                    title = split.second,
                    artists = listOf(split.first),
                    album = null,
                    durationMs = 0L,
                    videoId = videoId
                )
            )
        }
    }
    val next = root.optString("nextPageToken").takeIf { it.isNotBlank() }
    return YouTubeItemsPage(
        tracks = tracks,
        nextPageToken = next,
        nextPosition = maxPosition + 1
    )
}

/**
 * Splits "Artist - Song" when the left segment is plausible, and otherwise
 * treats the channel as the artist. The full original title is kept in the
 * song title, so "(Official Audio)" and versions are never silently dropped.
 */
fun splitYouTubeMusicTitle(rawTitle: String, channel: String): Pair<String, String> {
    val title = rawTitle.trim()
    val separators = listOf(" - ", " – ", " — ")
    for (separator in separators) {
        val index = title.indexOf(separator)
        if (index <= 0 || index >= title.length - separator.length) continue
        val left = title.substring(0, index).trim()
        val right = title.substring(index + separator.length).trim()
        if (left.length in 2..80 && right.length >= 1) {
            return cleanChannelArtist(left) to right
        }
    }
    return cleanChannelArtist(channel).ifBlank { "Artista desconocido" } to
        (title.ifBlank { "Canción sin título" })
}

private fun cleanChannelArtist(raw: String): String {
    var value = raw.trim()
    val suffixes = listOf(" - Topic", "VEVO", " - Official")
    suffixes.forEach { suffix ->
        if (value.length > suffix.length && value.endsWith(suffix, ignoreCase = true)) {
            value = value.dropLast(suffix.length).trim()
        }
    }
    return value
}

private fun isUnavailableTitle(title: String): Boolean {
    val clean = title.trim().lowercase(Locale.ROOT)
    return clean == "deleted video" ||
        clean == "private video" ||
        clean == "vídeo eliminado" ||
        clean == "vídeo privado"
}

private fun youTubeThumbnail(snippet: JSONObject): String? {
    val thumbnails = snippet.optJSONObject("thumbnails") ?: return null
    for (size in listOf("high", "medium", "default")) {
        val url = thumbnails.optJSONObject(size)?.optString("url").orEmpty()
        if (url.startsWith("https://", ignoreCase = true)) return url
    }
    return null
}

/**
 * Adds durations in batches of 50 ids, the documented maximum of `videos.list`.
 * A failed batch leaves those durations at 0; matching then relies on title,
 * artist and version instead of inventing a length.
 */
fun fillDurations(
    tracks: List<ImportedPlaylistTrack>,
    apiKey: String,
    fetch: (url: String, timeoutMs: Int) -> String = { url, timeout -> HttpJson.get(url, timeout) }
): List<ImportedPlaylistTrack> {
    val byId = tracks.mapNotNull { track -> track.videoId?.let { it to track } }.toMap()
    val ids = byId.keys.toList()
    if (ids.isEmpty()) return tracks
    val durations = HashMap<String, Long>()
    ids.chunked(50).forEach { chunk ->
        val body = runCatching { fetch(buildVideosUrl(chunk, apiKey), 20_000) }.getOrNull()
            ?: return@forEach
        val items = runCatching { JSONObject(body).optJSONArray("items") }.getOrNull()
            ?: return@forEach
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val ms = parseIsoDurationMs(
                item.optJSONObject("contentDetails")?.optString("duration").orEmpty()
            )
            if (id.isNotBlank() && ms > 0L) durations[id] = ms
        }
    }
    return tracks.map { track ->
        val ms = track.videoId?.let { durations[it] } ?: 0L
        if (ms > 0L) track.copy(durationMs = ms) else track
    }
}

internal fun buildPlaylistDetailsUrl(playlistId: String, key: String): String {
    val fields = "items(snippet(title,description,channelTitle," +
        "thumbnails/high/url,thumbnails/medium/url,thumbnails/default/url)," +
        "contentDetails/itemCount)"
    return "https://www.googleapis.com/youtube/v3/playlists" +
        "?part=snippet,contentDetails&maxResults=1" +
        "&id=${encode(playlistId)}&fields=${encode(fields)}&key=${encode(key)}"
}

internal fun buildPlaylistItemsUrl(
    playlistId: String,
    key: String,
    pageToken: String?
): String {
    val fields = "nextPageToken,pageInfo/totalResults,items(" +
        "snippet(position,title,channelTitle,videoOwnerChannelTitle," +
        "thumbnails/high/url,thumbnails/medium/url,thumbnails/default/url)," +
        "contentDetails/videoId)"
    val token = pageToken?.takeIf { it.isNotBlank() }
        ?.let { "&pageToken=${encode(it)}" }.orEmpty()
    return "https://www.googleapis.com/youtube/v3/playlistItems" +
        "?part=snippet,contentDetails&maxResults=50" +
        "&playlistId=${encode(playlistId)}$token" +
        "&fields=${encode(fields)}&key=${encode(key)}"
}

private fun buildVideosUrl(ids: List<String>, key: String): String {
    val fields = "items(id,contentDetails/duration)"
    return "https://www.googleapis.com/youtube/v3/videos" +
        "?part=contentDetails&maxResults=50" +
        "&id=${encode(ids.joinToString(","))}" +
        "&fields=${encode(fields)}&key=${encode(key)}"
}

private fun encode(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")

private fun mapImportError(status: Int, body: String): YouTubePlaylistImportException {
    val mapped = mapYouTubeError(status, body)
    val message = when (mapped.kind) {
        com.vibemusic.app.data.youtube.YouTubeException.Kind.QUOTA ->
            "Se agotó la cuota diaria de YouTube Data API. La playlist no se puede leer ahora."
        com.vibemusic.app.data.youtube.YouTubeException.Kind.INVALID_KEY ->
            "La clave de YouTube Data API no es válida o está restringida para este APK."
        com.vibemusic.app.data.youtube.YouTubeException.Kind.API_DISABLED ->
            "La YouTube Data API v3 no está habilitada para esa clave en Google Cloud."
        else -> mapped.message ?: "YouTube respondió con un error."
    }
    return YouTubePlaylistImportException(message)
}

class YouTubePlaylistImportException(message: String) : Exception(message)
