package com.vibemusic.app.data.youtube

import com.vibemusic.app.data.online.HttpJson
import com.vibemusic.app.data.online.HttpStatusException
import com.vibemusic.app.data.online.ResponseCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale

/**
 * A search result from YouTube. Playback is never handled here: the UI opens
 * the official YouTube app or website, so ads, age gates and playback
 * restrictions are always controlled by YouTube.
 */
data class YouTubeVideo(
    val id: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val publishedAt: String = "",
    val description: String = ""
) {
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$id"
    val appUri: String get() = "vnd.youtube:$id"
}

class YouTubeException(
    message: String,
    val kind: Kind = Kind.GENERIC
) : Exception(message) {
    enum class Kind {
        NOT_CONFIGURED,
        INVALID_KEY,
        QUOTA,
        API_DISABLED,
        NETWORK,
        EMPTY_QUERY,
        GENERIC
    }
}

/**
 * Read-only client for YouTube Data API v3. It performs one `search.list`
 * request (100 quota units) and one `videos.list` request (1 unit) per new
 * query, caches successful queries and never retries 4xx responses.
 */
class YouTubeService(
    private val apiKey: String,
    private val fetch: (url: String, timeoutMs: Int) -> String = { url, timeout ->
        HttpJson.get(url, timeout)
    }
) {

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    private val cache = ResponseCache<List<YouTubeVideo>>(capacity = 16)

    suspend fun search(query: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.length < 2) {
            throw YouTubeException(
                "Escribe al menos 2 caracteres para buscar en YouTube.",
                YouTubeException.Kind.EMPTY_QUERY
            )
        }
        if (!isConfigured) {
            throw YouTubeException(
                "YouTube no está configurado. Falta la clave de YouTube Data API v3.",
                YouTubeException.Kind.NOT_CONFIGURED
            )
        }

        val cacheKey = clean.lowercase(Locale.ROOT)
        cache.get(cacheKey)?.let { return@withContext it }

        val searchJson = try {
            fetch(buildSearchUrl(clean, apiKey), 15_000)
        } catch (e: HttpStatusException) {
            throw mapYouTubeError(e.statusCode, e.responseBody)
        } catch (e: IOException) {
            throw YouTubeException(
                "No se pudo conectar con YouTube. Comprueba tu conexión.",
                YouTubeException.Kind.NETWORK
            )
        }

        val ids = parseSearchIds(searchJson)
        if (ids.isEmpty()) {
            cache.put(cacheKey, emptyList())
            return@withContext emptyList()
        }

        val videosJson = try {
            fetch(buildVideosUrl(ids, apiKey), 15_000)
        } catch (e: HttpStatusException) {
            throw mapYouTubeError(e.statusCode, e.responseBody)
        } catch (e: IOException) {
            throw YouTubeException(
                "No se pudieron cargar los detalles de los vídeos de YouTube.",
                YouTubeException.Kind.NETWORK
            )
        }

        val videos = parseYouTubeVideos(searchJson, videosJson)
        cache.put(cacheKey, videos)
        videos
    }

    fun clearCache() = cache.clear()
}

internal fun buildSearchUrl(query: String, key: String): String {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val encodedKey = URLEncoder.encode(key, "UTF-8")
    val fields = "items(id/videoId,snippet(title,channelTitle,publishedAt," +
        "thumbnails/high/url,thumbnails/medium/url,thumbnails/default/url))"
    return "https://www.googleapis.com/youtube/v3/search" +
        "?part=snippet&type=video&maxResults=25&order=relevance" +
        "&q=$encodedQuery&fields=$fields&key=$encodedKey"
}

internal fun buildVideosUrl(ids: List<String>, key: String): String {
    val encodedIds = URLEncoder.encode(ids.joinToString(","), "UTF-8")
    val encodedKey = URLEncoder.encode(key, "UTF-8")
    val fields = "items(id,contentDetails/duration," +
        "snippet(title,channelTitle,publishedAt,description," +
        "thumbnails/high/url,thumbnails/medium/url,thumbnails/default/url))"
    return "https://www.googleapis.com/youtube/v3/videos" +
        "?part=contentDetails,snippet&maxResults=25" +
        "&id=$encodedIds&fields=$fields&key=$encodedKey"
}

internal fun parseSearchIds(json: String): List<String> {
    val items = runCatching { JSONObject(json).optJSONArray("items") }.getOrNull() ?: return emptyList()
    val ids = LinkedHashSet<String>()
    for (i in 0 until items.length()) {
        val item = items.optJSONObject(i) ?: continue
        val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
        if (id.matches(Regex("[A-Za-z0-9_-]{5,20}"))) ids.add(id)
    }
    return ids.toList()
}

/**
 * Combines `search.list` snippets (which control the result order) with
 * `videos.list` details, which are the only source of the ISO-8601 duration.
 */
internal fun parseYouTubeVideos(searchJson: String, videosJson: String): List<YouTubeVideo> {
    val searchVideos = LinkedHashMap<String, YouTubeVideo>()
    val searchItems = runCatching { JSONObject(searchJson).optJSONArray("items") }.getOrNull()
    if (searchItems != null) {
        for (i in 0 until searchItems.length()) {
            val item = searchItems.optJSONObject(i) ?: continue
            val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
            if (!id.matches(Regex("[A-Za-z0-9_-]{5,20}"))) continue
            val snippet = item.optJSONObject("snippet") ?: continue
            searchVideos[id] = YouTubeVideo(
                id = id,
                title = snippet.optString("title").orEmpty().ifBlank { "Vídeo de YouTube" },
                channel = snippet.optString("channelTitle").orEmpty(),
                thumbnailUrl = thumbnailUrl(snippet),
                durationMs = 0L,
                publishedAt = snippet.optString("publishedAt").orEmpty()
            )
        }
    }

    val details = LinkedHashMap<String, YouTubeVideo>()
    val videoItems = runCatching { JSONObject(videosJson).optJSONArray("items") }.getOrNull()
    if (videoItems != null) {
        for (i in 0 until videoItems.length()) {
            val item = videoItems.optJSONObject(i) ?: continue
            val id = item.optString("id").orEmpty()
            if (id.isBlank()) continue
            val snippet = item.optJSONObject("snippet")
            val base = searchVideos[id]
            val title = snippet?.optString("title").orEmpty().ifBlank { base?.title.orEmpty() }
            val channel = snippet?.optString("channelTitle").orEmpty().ifBlank { base?.channel.orEmpty() }
            val thumbnail = snippet?.let { thumbnailUrl(it) }.orEmpty()
                .ifBlank { base?.thumbnailUrl.orEmpty() }
            val duration = parseIsoDurationMs(
                item.optJSONObject("contentDetails")?.optString("duration").orEmpty()
            )
            details[id] = YouTubeVideo(
                id = id,
                title = title.ifBlank { "Vídeo de YouTube" },
                channel = channel,
                thumbnailUrl = thumbnail,
                durationMs = duration,
                publishedAt = snippet?.optString("publishedAt").orEmpty()
                    .ifBlank { base?.publishedAt.orEmpty() },
                description = snippet?.optString("description").orEmpty()
            )
        }
    }

    return searchVideos.keys.mapNotNull { id ->
        val fromSearch = searchVideos[id]
        details[id] ?: fromSearch
    }
}

private fun thumbnailUrl(snippet: JSONObject): String {
    val thumbnails = snippet.optJSONObject("thumbnails") ?: return ""
    for (size in listOf("high", "medium", "default")) {
        val url = thumbnails.optJSONObject(size)?.optString("url").orEmpty()
        if (url.startsWith("https://", ignoreCase = true)) return url
    }
    return ""
}

/** Parses ISO-8601 durations such as PT3M33S or PT1H2M3S. */
internal fun parseIsoDurationMs(raw: String): Long {
    if (raw.isBlank()) return 0L
    val match = Regex("^P(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?$")
        .matchEntire(raw.trim()) ?: return 0L
    val days = match.groupValues[1].toLongOrNull() ?: 0L
    val hours = match.groupValues[2].toLongOrNull() ?: 0L
    val minutes = match.groupValues[3].toLongOrNull() ?: 0L
    val seconds = match.groupValues[4].toDoubleOrNull() ?: 0.0
    val totalSeconds = ((days * 24 + hours) * 60 + minutes) * 60 + seconds
    return (totalSeconds * 1000).toLong()
}

internal fun mapYouTubeError(status: Int, body: String): YouTubeException {
    val reason = runCatching {
        JSONObject(body)
            .getJSONObject("error")
            .getJSONArray("errors")
            .getJSONObject(0)
            .optString("reason")
    }.getOrNull().orEmpty()
    val apiMessage = runCatching {
        JSONObject(body).getJSONObject("error").optString("message")
    }.getOrNull().orEmpty()

    return when {
        reason == "quotaExceeded" || reason == "dailyLimitExceeded" ->
            YouTubeException(
                "Se agotó la cuota diaria de YouTube Data API. Inténtalo más tarde.",
                YouTubeException.Kind.QUOTA
            )
        reason == "accessNotConfigured" ||
            (reason == "forbidden" && apiMessage.contains("not enabled", ignoreCase = true)) ->
            YouTubeException(
                "La YouTube Data API v3 no está habilitada para esta clave en Google Cloud.",
                YouTubeException.Kind.API_DISABLED
            )
        reason == "keyInvalid" || reason == "badRequest" || status == 400 ->
            YouTubeException(
                "La clave de YouTube Data API no es válida. Revisa la configuración.",
                YouTubeException.Kind.INVALID_KEY
            )
        status == 403 ->
            YouTubeException(
                "YouTube rechazó la solicitud. Comprueba la clave y sus restricciones.",
                YouTubeException.Kind.GENERIC
            )
        else ->
            YouTubeException(
                "YouTube respondió HTTP $status. Inténtalo más tarde.",
                YouTubeException.Kind.NETWORK
            )
    }
}
