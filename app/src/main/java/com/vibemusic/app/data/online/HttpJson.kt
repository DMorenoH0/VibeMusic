package com.vibemusic.app.data.online

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal HTTPS JSON client built on HttpURLConnection. It avoids adding a
 * networking library to an app that only needs a handful of public read-only
 * endpoints (Deezer catalogue and GitHub Releases).
 */
object HttpJson {

    private const val USER_AGENT = "VibeMusic/1.2 (Android; open-source music player)"

    @Throws(IOException::class)
    fun get(url: String, timeoutMs: Int = 12_000): String {
        var lastError: IOException? = null
        repeat(2) { attempt ->
            try {
                return request(url, timeoutMs)
            } catch (e: HttpStatusException) {
                // 4xx means the request itself is wrong (invalid key, quota,
                // bad query). Retrying would waste quota and never succeed.
                if (e.statusCode in 400..499) throw e
                lastError = e
                if (attempt == 0) Thread.sleep(350)
            } catch (e: IOException) {
                lastError = e
                if (attempt == 0) Thread.sleep(350)
            }
        }
        throw lastError ?: IOException("No se pudo conectar con el servidor.")
    }

    private fun request(url: String, timeoutMs: Int): String {
        val parsed = URL(url)
        if (parsed.protocol != "https") throw IOException("Solo se admiten conexiones https.")
        val connection = (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "identity")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw HttpStatusException(code, body)
            }
            if (body.isBlank()) throw IOException("El servidor devolvió una respuesta vacía.")
            return body
        } finally {
            connection.disconnect()
        }
    }
}

/** Non-2xx response. Keeps the body so callers can read API error reasons. */
class HttpStatusException(
    val statusCode: Int,
    val responseBody: String
) : IOException("El servidor respondió HTTP $statusCode.")

/** Tiny size-bounded cache so repeated queries do not burn API quota. */
class ResponseCache<T : Any>(private val capacity: Int = 48) {
    private val map = ConcurrentHashMap<String, T>()
    private val order = ArrayDeque<String>()

    @Synchronized
    fun get(key: String): T? = map[key]

    @Synchronized
    fun put(key: String, value: T) {
        if (map.put(key, value) == null) order.addLast(key)
        while (order.size > capacity) {
            val oldest = order.removeFirstOrNull() ?: break
            map.remove(oldest)
        }
    }

    @Synchronized
    fun clear() {
        map.clear()
        order.clear()
    }
}
