package com.vibemusic.app.data.audius

import com.vibemusic.app.data.online.HttpJson
import com.vibemusic.app.data.online.HttpStatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Read-only client for the official Audius API v1 (https://api.audius.co/v1).
 *
 * Audius publishes this API for catalogue reads without an API key. VibeMusic
 * identifies itself with `app_name=VibeMusic`, caches responses for a few
 * minutes and never sends user identity, listening history or local library
 * contents. Downloads are only requested when the track object explicitly
 * grants `is_downloadable` and does not carry an access gate.
 */
class AudiusService {

    private data class CacheEntry(val body: String, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    suspend fun searchTracks(query: String, limit: Int = 30): List<AudiusTrack> =
        withContext(Dispatchers.IO) {
            val clean = query.trim()
            if (clean.length < 2) return@withContext emptyList()
            parseAudiusTracks(
                get(
                    "tracks/search",
                    mapOf("query" to clean, "limit" to limit.coerceIn(1, 100)),
                    SEARCH_TTL_MS
                )
            )
        }

    suspend fun searchUsers(query: String, limit: Int = 12): List<AudiusUser> =
        withContext(Dispatchers.IO) {
            val clean = query.trim()
            if (clean.length < 2) return@withContext emptyList()
            parseAudiusUsers(
                get(
                    "users/search",
                    mapOf("query" to clean, "limit" to limit.coerceIn(1, 100)),
                    SEARCH_TTL_MS
                )
            )
        }

    suspend fun searchCollections(query: String, limit: Int = 30): List<AudiusCollection> =
        withContext(Dispatchers.IO) {
            val clean = query.trim()
            if (clean.length < 2) return@withContext emptyList()
            parseAudiusCollections(
                get(
                    "playlists/search",
                    mapOf("query" to clean, "limit" to limit.coerceIn(1, 100)),
                    SEARCH_TTL_MS
                )
            )
        }

    suspend fun trending(limit: Int = 24, genre: String? = null): List<AudiusTrack> =
        withContext(Dispatchers.IO) {
            val params = buildMap<String, Any> {
                put("limit", limit.coerceIn(1, 100))
                genre?.trim()?.takeIf { it.isNotBlank() }?.let { put("genre", it) }
            }
            parseAudiusTracks(get("tracks/trending", params, TRENDING_TTL_MS))
        }

    suspend fun track(id: String): AudiusTrack? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        parseAudiusTrackResponse(get("tracks/$id", emptyMap(), DETAIL_TTL_MS))
    }

    suspend fun user(id: String): AudiusUser? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        parseAudiusUserResponse(get("users/$id", emptyMap(), DETAIL_TTL_MS))
    }

    suspend fun collection(id: String): AudiusCollection? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        parseAudiusCollectionResponse(get("playlists/$id", emptyMap(), DETAIL_TTL_MS))
    }

    suspend fun userTracks(id: String, limit: Int = 50): List<AudiusTrack> =
        withContext(Dispatchers.IO) {
            if (id.isBlank()) return@withContext emptyList()
            parseAudiusTracks(
                get(
                    "users/$id/tracks",
                    mapOf("limit" to limit.coerceIn(1, 100)),
                    DETAIL_TTL_MS
                )
            )
        }

    suspend fun userAlbums(id: String, limit: Int = 20): List<AudiusCollection> =
        withContext(Dispatchers.IO) {
            if (id.isBlank()) return@withContext emptyList()
            parseAudiusCollections(
                get(
                    "users/$id/albums",
                    mapOf("limit" to limit.coerceIn(1, 100)),
                    DETAIL_TTL_MS
                )
            )
        }

    suspend fun userPlaylists(id: String, limit: Int = 20): List<AudiusCollection> =
        withContext(Dispatchers.IO) {
            if (id.isBlank()) return@withContext emptyList()
            parseAudiusCollections(
                get(
                    "users/$id/playlists",
                    mapOf("limit" to limit.coerceIn(1, 100)),
                    DETAIL_TTL_MS
                )
            )
        }

    suspend fun collectionTracks(id: String, limit: Int = 100): List<AudiusTrack> =
        withContext(Dispatchers.IO) {
            if (id.isBlank()) return@withContext emptyList()
            parseAudiusTracks(
                get(
                    "playlists/$id/tracks",
                    mapOf("limit" to limit.coerceIn(1, 100)),
                    DETAIL_TTL_MS
                )
            )
        }

    /**
     * Audius does not expose a `/related` endpoint in v1. Related music is
     * derived from two official sources: the same artist's tracks and a search
     * for the track's own genre. Nothing is fabricated.
     */
    suspend fun relatedTracks(track: AudiusTrack, limit: Int = 20): List<AudiusTrack> {
        val result = LinkedHashMap<String, AudiusTrack>()
        val artistId = track.user?.id
        if (!artistId.isNullOrBlank()) {
            runCatching { userTracks(artistId, 30) }.getOrDefault(emptyList())
                .filter { it.id != track.id && it.playable }
                .forEach { result[it.id] = it }
        }
        val genre = track.genre
        if (!genre.isNullOrBlank()) {
            runCatching { searchTracks(genre, 40) }.getOrDefault(emptyList())
                .filter {
                    it.id != track.id &&
                        it.playable &&
                        it.genre?.equals(genre, ignoreCase = true) == true
                }
                .forEach { result.putIfAbsent(it.id, it) }
        }
        if (result.isEmpty()) {
            runCatching { trending(limit) }.getOrDefault(emptyList())
                .filter { it.id != track.id && it.playable }
                .forEach { result[it.id] = it }
        }
        return result.values.take(limit)
    }

    fun clearCache() = cache.clear()

    // -----------------------------------------------------------------------

    private fun get(path: String, params: Map<String, Any>, ttlMs: Long): String {
        val key = buildString {
            append(path)
            params.toSortedMap().forEach { (k, v) -> append("|$k=$v") }
        }
        val now = System.currentTimeMillis()
        cache[key]?.let { entry ->
            if (entry.expiresAt > now) return entry.body
            cache.remove(key)
        }
        if (cache.size > MAX_CACHE_ENTRIES) {
            cache.entries.removeIf { it.value.expiresAt <= now }
            if (cache.size > MAX_CACHE_ENTRIES) cache.clear()
        }

        val url = buildString {
            append(AudiusEndpoints.BASE)
            append('/')
            append(path)
            append("?app_name=")
            append(encode(AudiusEndpoints.APP_NAME))
            params.forEach { (name, value) ->
                append('&')
                append(encode(name))
                append('=')
                append(encode(value.toString()))
            }
        }
        val body = try {
            HttpJson.get(
                url = url,
                timeoutMs = 20_000,
                headers = mapOf(
                    "User-Agent" to "VibeMusic/3.0 (Android; Audius open API)",
                    "Accept" to "application/json"
                )
            )
        } catch (e: HttpStatusException) {
            throw AudiusApiException(
                when (e.statusCode) {
                    401, 403 ->
                        "Audius no permite esta consulta sin una cuenta autorizada."
                    404 -> "Audius no encontró esa pista, artista o lista."
                    429 ->
                        "Audius está limitando las peticiones. Espera unos segundos y " +
                            "vuelve a intentarlo."
                    in 500..599 ->
                        "Audius no responde en este momento. Inténtalo de nuevo en un rato."
                    else -> "Audius respondió con un error (${e.statusCode})."
                }
            )
        } catch (e: IOException) {
            throw AudiusApiException(
                "Sin conexión con Audius. Comprueba tu red; tu música local sigue disponible."
            )
        }
        cache[key] = CacheEntry(body, now + ttlMs)
        return body
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    companion object {
        private const val SEARCH_TTL_MS = 3L * 60L * 1000L
        private const val TRENDING_TTL_MS = 5L * 60L * 1000L
        private const val DETAIL_TTL_MS = 10L * 60L * 1000L
        private const val MAX_CACHE_ENTRIES = 96
    }
}

class AudiusApiException(message: String) : Exception(message)
