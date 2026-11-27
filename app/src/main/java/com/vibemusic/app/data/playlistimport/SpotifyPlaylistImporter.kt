package com.vibemusic.app.data.playlistimport

import com.vibemusic.app.data.online.HttpJson
import com.vibemusic.app.data.online.HttpStatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Base64

data class SpotifyCredentials(
    val clientId: String,
    val clientSecret: String
) {
    val configured: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()
}

object SpotifyCredentialsValidator {
    fun validate(clientId: String, clientSecret: String): String? {
        val id = clientId.trim()
        val secret = clientSecret.trim()
        return when {
            id.isEmpty() -> "Escribe el Client ID de tu app de Spotify."
            secret.isEmpty() -> "Escribe el Client Secret de tu app de Spotify."
            id.length < 10 -> "El Client ID parece incompleto."
            secret.length < 10 -> "El Client Secret parece incompleto."
            id.contains(" ") || secret.contains(" ") ->
                "Las credenciales no deben contener espacios."
            else -> null
        }
    }
}

interface SpotifyTransport {
    suspend fun token(clientId: String, clientSecret: String): String
    suspend fun get(url: String, accessToken: String): String
}

class HttpSpotifyTransport : SpotifyTransport {
    override suspend fun token(clientId: String, clientSecret: String): String =
        withContext(Dispatchers.IO) {
            val basic = Base64.getEncoder()
                .encodeToString("$clientId:$clientSecret".toByteArray(Charsets.UTF_8))
            val body = try {
                HttpJson.postForm(
                    url = "https://accounts.spotify.com/api/token",
                    form = mapOf("grant_type" to "client_credentials"),
                    timeoutMs = 15_000,
                    headers = mapOf("Authorization" to "Basic $basic")
                )
            } catch (e: HttpStatusException) {
                val detail = runCatching {
                    JSONObject(e.responseBody).optString("error_description")
                        .ifBlank { JSONObject(e.responseBody).optString("error") }
                }.getOrDefault("")
                throw SpotifyImportException(
                    when (e.statusCode) {
                        400, 401 ->
                            "Spotify rechazó las credenciales. Revisa el Client ID y el Client Secret."
                        403 -> "Spotify no autoriza este tipo de acceso a tu app."
                        429 -> "Spotify está limitando las peticiones. Espera un minuto."
                        else -> "Spotify respondió con un error al iniciar sesión (${e.statusCode})."
                    } + if (detail.isBlank()) "" else " Detalle: $detail"
                )
            } catch (e: IOException) {
                throw SpotifyImportException(
                    "No se pudo conectar con Spotify. Comprueba tu conexión."
                )
            }
            runCatching { JSONObject(body).optString("access_token") }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: throw SpotifyImportException("Spotify no devolvió un token válido.")
        }

    override suspend fun get(url: String, accessToken: String): String =
        withContext(Dispatchers.IO) {
            try {
                HttpJson.get(
                    url = url,
                    timeoutMs = 20_000,
                    headers = mapOf(
                        "Authorization" to "Bearer $accessToken",
                        "Accept" to "application/json"
                    )
                )
            } catch (e: HttpStatusException) {
                throw SpotifyImportException(
                    when (e.statusCode) {
                        401 -> "La sesión con Spotify caducó. Vuelve a analizar la playlist."
                        403 -> "Spotify denegó el acceso a esta playlist."
                        404 -> "La playlist de Spotify no existe o es privada."
                        429 -> "Spotify está limitando las peticiones. Espera un minuto."
                        in 500..599 -> "Spotify no responde en este momento. Inténtalo más tarde."
                        else -> "Spotify respondió con un error (${e.statusCode})."
                    }
                )
            } catch (e: IOException) {
                throw SpotifyImportException(
                    "No se pudo conectar con Spotify. Comprueba tu conexión."
                )
            }
        }
}

class SpotifyPlaylistImporter(
    private val credentials: SpotifyCredentials,
    private val transport: SpotifyTransport = HttpSpotifyTransport()
) {
    suspend fun validateCredentials() {
        if (!credentials.configured) {
            throw SpotifyImportException("Faltan las credenciales de Spotify.")
        }
        transport.token(credentials.clientId, credentials.clientSecret)
    }

    suspend fun import(link: PlaylistLink): ImportedPlaylist {
        if (link.platform != PlaylistPlatform.SPOTIFY) {
            throw SpotifyImportException("El enlace no es de Spotify.")
        }
        if (!credentials.configured) {
            throw SpotifyImportException(
                "Spotify necesita un Client ID y un Client Secret. Añádelos en " +
                    "«Credenciales de Spotify»; se guardan cifrados en este dispositivo."
            )
        }
        val token = transport.token(credentials.clientId, credentials.clientSecret)
        val firstUrl = "https://api.spotify.com/v1/playlists/${encode(link.playlistId)}"
        val firstJson = transport.get(firstUrl, token)
        val firstPage = parseSpotifyPlaylistPage(firstJson)

        val tracks = ArrayList<ImportedPlaylistTrack>()
        firstPage.tracks.forEach { tracks.add(it) }
        var next = firstPage.next
        var pages = 1
        var skipped = firstPage.skipped
        var position = firstPage.tracks.size + firstPage.skipped

        while (!next.isNullOrBlank() && pages < MAX_PAGES) {
            if (!next.startsWith("https://api.spotify.com/v1/playlists/")) {
                throw SpotifyImportException(
                    "Spotify devolvió una dirección de paginación inesperada."
                )
            }
            val pageJson = transport.get(next, token)
            val page = parseSpotifyTracksPage(pageJson, startPosition = position)
            page.tracks.forEach { tracks.add(it) }
            position += page.tracks.size + page.skipped
            skipped += page.skipped
            next = page.next
            pages++
        }

        if (tracks.isEmpty()) {
            throw SpotifyImportException(
                "La playlist de Spotify no contiene canciones accesibles."
            )
        }
        return ImportedPlaylist(
            platform = PlaylistPlatform.SPOTIFY,
            playlistId = link.playlistId,
            name = firstPage.name.ifBlank { "Playlist de Spotify" },
            owner = firstPage.owner.ifBlank { "Spotify" },
            description = firstPage.description,
            artworkUrl = firstPage.artworkUrl,
            sourceUrl = link.url,
            tracks = tracks
        )
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private companion object {
        const val MAX_PAGES = 200 // 20.000 canciones como máximo defensivo
    }
}

// ---------------------------------------------------------------------------
// Pure parsers (unit-tested with representative Spotify payloads)
// ---------------------------------------------------------------------------

data class SpotifyPlaylistPage(
    val name: String,
    val owner: String,
    val description: String?,
    val artworkUrl: String?,
    val tracks: List<ImportedPlaylistTrack>,
    val next: String?,
    val total: Int,
    val skipped: Int
)

data class SpotifyTracksPage(
    val tracks: List<ImportedPlaylistTrack>,
    val next: String?,
    val total: Int,
    val skipped: Int
)

fun parseSpotifyPlaylistPage(json: String): SpotifyPlaylistPage {
    val root = JSONObject(json)
    val tracksObject = root.optJSONObject("tracks") ?: JSONObject()
    val parsed = parseSpotifyItems(tracksObject.optJSONArray("items"), 0)
    return SpotifyPlaylistPage(
        name = root.optString("name").trim(),
        owner = root.optJSONObject("owner")?.optString("display_name").orEmpty().trim(),
        description = root.optString("description").takeIf { it.isNotBlank() },
        artworkUrl = firstImage(root.optJSONArray("images")),
        tracks = parsed.first,
        next = nextPage(tracksObject),
        total = tracksObject.optInt("total", parsed.first.size),
        skipped = parsed.second
    )
}

fun parseSpotifyTracksPage(json: String, startPosition: Int): SpotifyTracksPage {
    val root = JSONObject(json)
    val parsed = parseSpotifyItems(root.optJSONArray("items"), startPosition)
    return SpotifyTracksPage(
        tracks = parsed.first,
        next = nextPage(root),
        total = root.optInt("total", parsed.first.size + parsed.second),
        skipped = parsed.second
    )
}

private fun parseSpotifyItems(
    items: JSONArray?,
    startPosition: Int
): Pair<List<ImportedPlaylistTrack>, Int> {
    if (items == null) return emptyList<ImportedPlaylistTrack>() to 0
    val tracks = ArrayList<ImportedPlaylistTrack>()
    var skipped = 0
    for (i in 0 until items.length()) {
        val wrapper = items.optJSONObject(i) ?: continue
        val track = wrapper.optJSONObject("track")
        val position = startPosition + i
        if (track == null || track.optBoolean("is_local", false)) {
            skipped++
            continue
        }
        if (track.optString("type").equals("episode", ignoreCase = true)) {
            skipped++
            continue
        }
        val title = track.optString("name").trim()
        val artists = buildList {
            val array = track.optJSONArray("artists") ?: JSONArray()
            for (a in 0 until array.length()) {
                val name = array.optJSONObject(a)?.optString("name").orEmpty().trim()
                if (name.isNotBlank()) add(name)
            }
        }
        if (title.isBlank() || artists.isEmpty()) {
            skipped++
            continue
        }
        val album = track.optJSONObject("album")
        tracks.add(
            ImportedPlaylistTrack(
                position = position,
                title = title,
                artists = artists,
                album = album?.optString("name")?.trim()?.takeIf { it.isNotBlank() },
                durationMs = track.optLong("duration_ms", 0L).coerceAtLeast(0L),
                isrc = track.optJSONObject("external_ids")
                    ?.optString("isrc")
                    ?.takeIf { it.isNotBlank() }
            )
        )
    }
    return tracks to skipped
}

private fun nextPage(root: JSONObject): String? {
    if (root.isNull("next")) return null
    return root.optString("next").takeIf { it.isNotBlank() }
}

private fun firstImage(images: JSONArray?): String? {
    if (images == null) return null
    for (i in 0 until images.length()) {
        val url = images.optJSONObject(i)?.optString("url").orEmpty()
        if (url.startsWith("https://", ignoreCase = true)) return url
    }
    return null
}

class SpotifyImportException(message: String) : Exception(message)
