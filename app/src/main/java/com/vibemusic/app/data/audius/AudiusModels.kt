package com.vibemusic.app.data.audius

import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small, immutable mirrors of the official Audius API v1 objects.
 *
 * Only fields that VibeMusic actually shows are kept. Parsing is pure and is
 * covered by unit tests with real response shapes, so no endpoint or field is
 * invented.
 */
data class AudiusUser(
    val id: String,
    val name: String,
    val handle: String,
    val bio: String?,
    val artworkUrl: String?,
    val coverUrl: String?,
    val followerCount: Long,
    val followingCount: Long,
    val supporterCount: Long,
    val trackCount: Int,
    val albumCount: Int,
    val playlistCount: Int,
    val verified: Boolean,
    val location: String?,
    val website: String?
) {
    val displayName: String
        get() = name.trim().ifBlank { handle.trim().ifBlank { "Artista de Audius" } }

    val profileUrl: String?
        get() = handle.trim().takeIf { it.isNotBlank() }?.let { "https://audius.co/$it" }
}

data class AudiusAlbumRef(
    val id: String,
    val name: String,
    val permalink: String?
)

data class AudiusTrack(
    val id: String,
    val title: String,
    val user: AudiusUser?,
    val album: AudiusAlbumRef?,
    val durationSeconds: Long,
    val genre: String?,
    val mood: String?,
    val description: String?,
    val artworkUrl: String?,
    val isDownloadable: Boolean,
    val isStreamable: Boolean,
    val isStreamGated: Boolean,
    val isDownloadGated: Boolean,
    val accessStream: Boolean?,
    val accessDownload: Boolean?,
    val streamConditions: JSONObject?,
    val downloadConditions: JSONObject?,
    val playCount: Long,
    val favoriteCount: Long,
    val repostCount: Long,
    val releaseDate: String?,
    val permalink: String?,
    val license: String?
) {
    /**
     * A gated or non-streamable track cannot be played by an anonymous client.
     * VibeMusic filters those out instead of attempting to bypass the gate.
     */
    val playable: Boolean
        get() = isStreamable && !isStreamGated && accessStream != false

    val downloadPermission: AudiusDownloadPermission
        get() {
            if (isDownloadGated || accessDownload == false ||
                (downloadConditions?.length() ?: 0) > 0
            ) {
                return AudiusDownloadPermission(
                    allowed = false,
                    reason = downloadConditionReason(downloadConditions)
                )
            }
            if (!isDownloadable) {
                return AudiusDownloadPermission(
                    allowed = false,
                    reason = "El artista no ha autorizado la descarga de esta pista."
                )
            }
            return AudiusDownloadPermission(allowed = true, reason = null)
        }

    fun toTrack(): Track {
        val permission = downloadPermission
        return Track(
            id = "audius:track:$id",
            uri = if (playable) AudiusEndpoints.stream(id) else "",
            title = title.trim().ifBlank { "Pista sin título" },
            artist = user?.displayName ?: "Artista de Audius",
            album = album?.name?.trim().orEmpty().ifBlank { "Audius" },
            durationMs = durationSeconds.coerceAtLeast(0L) * 1000L,
            artworkUri = artworkUrl,
            source = TrackSource.AUDIUS,
            genre = genre?.trim()?.takeIf { it.isNotBlank() },
            albumId = album?.id?.takeIf { it.isNotBlank() },
            artistId = user?.id?.takeIf { it.isNotBlank() },
            previewOnly = false,
            downloadUrl = if (permission.allowed) AudiusEndpoints.download(id) else null,
            downloadBlockedReason = permission.reason
        )
    }
}

data class AudiusDownloadPermission(
    val allowed: Boolean,
    val reason: String?
)

data class AudiusCollection(
    val id: String,
    val playlistId: String,
    val name: String,
    val description: String?,
    val owner: AudiusUser?,
    val artworkUrl: String?,
    val trackCount: Int,
    val isAlbum: Boolean,
    val totalPlayCount: Long,
    val favoriteCount: Long,
    val releaseDate: String?,
    val permalink: String?
) {
    val displayCreator: String
        get() = owner?.displayName ?: "Audius"

    val webUrl: String?
        get() = permalink?.takeIf { it.isNotBlank() }?.let { "https://audius.co$it" }
}

object AudiusEndpoints {
    const val BASE = "https://api.audius.co/v1"
    const val APP_NAME = "VibeMusic"

    fun stream(trackId: String): String =
        "$BASE/tracks/$trackId/stream?app_name=$APP_NAME"

    fun download(trackId: String): String =
        "$BASE/tracks/$trackId/download?app_name=$APP_NAME"
}

private fun downloadConditionReason(conditions: JSONObject?): String {
    if (conditions == null || conditions.length() == 0) {
        return "El artista ha restringido la descarga de esta pista en Audius."
    }
    if (conditions.has("usdc_purchase")) {
        return "El artista vende esta descarga con USDC. VibeMusic no procesa pagos " +
            "ni desbloquea compras."
    }
    if (conditions.has("follow_user_id")) {
        return "El artista pide que lo sigas en Audius antes de descargar. VibeMusic " +
            "no inicia sesión ni omite ese requisito."
    }
    if (conditions.has("nft_gate") || conditions.has("token_gate")) {
        return "La descarga requiere un NFT o un token de acceso que VibeMusic no puede " +
            "verificar."
    }
    val key = conditions.keys().asSequence().firstOrNull().orEmpty()
    return "La descarga tiene una condición de acceso de Audius que VibeMusic no puede " +
        "verificar (${key.ifBlank { "restringida" }})."
}

// ---------------------------------------------------------------------------
// Pure parsers
// ---------------------------------------------------------------------------

fun parseAudiusTracks(json: String): List<AudiusTrack> {
    val data = JSONObject(json).optJSONArray("data") ?: JSONArray()
    return buildList {
        for (i in 0 until data.length()) {
            data.optJSONObject(i)?.let { add(parseAudiusTrack(it)) }
        }
    }.filter { it.id.isNotBlank() && it.title.isNotBlank() }
}

fun parseAudiusTrackResponse(json: String): AudiusTrack? {
    val root = JSONObject(json)
    val single = root.optJSONObject("data")
    if (single != null) return parseAudiusTrack(single)
    val array = root.optJSONArray("data") ?: return null
    return array.optJSONObject(0)?.let(::parseAudiusTrack)
}

fun parseAudiusUsers(json: String): List<AudiusUser> {
    val data = JSONObject(json).optJSONArray("data") ?: JSONArray()
    return buildList {
        for (i in 0 until data.length()) {
            data.optJSONObject(i)?.let { add(parseAudiusUser(it)) }
        }
    }.filter { it.id.isNotBlank() }
}

fun parseAudiusUserResponse(json: String): AudiusUser? {
    val root = JSONObject(json)
    return root.optJSONObject("data")?.let(::parseAudiusUser)
}

fun parseAudiusCollections(json: String): List<AudiusCollection> {
    val data = JSONObject(json).optJSONArray("data") ?: JSONArray()
    return buildList {
        for (i in 0 until data.length()) {
            data.optJSONObject(i)?.let { add(parseAudiusCollection(it)) }
        }
    }.filter { it.id.isNotBlank() && it.name.isNotBlank() }
}

fun parseAudiusCollectionResponse(json: String): AudiusCollection? {
    val root = JSONObject(json)
    val single = root.optJSONObject("data")
    if (single != null) return parseAudiusCollection(single)
    val array = root.optJSONArray("data") ?: return null
    return array.optJSONObject(0)?.let(::parseAudiusCollection)
}

fun parseAudiusTrack(obj: JSONObject): AudiusTrack {
    val access = obj.optJSONObject("access")
    val backlink = obj.optJSONObject("album_backlink")
    return AudiusTrack(
        id = obj.optString("id"),
        title = obj.optString("title"),
        user = obj.optJSONObject("user")?.let(::parseAudiusUser),
        album = backlink?.let {
            val name = it.optString("playlist_name")
            val id = it.optString("playlist_id")
            if (name.isBlank() && id.isBlank()) null else AudiusAlbumRef(
                id = id,
                name = name,
                permalink = it.optString("permalink").takeIf { value -> value.isNotBlank() }
            )
        },
        durationSeconds = obj.optLong("duration", 0L),
        genre = obj.optString("genre").takeIf { it.isNotBlank() },
        mood = obj.optString("mood").takeIf { it.isNotBlank() },
        description = obj.optString("description").takeIf { it.isNotBlank() },
        artworkUrl = artworkOf(obj.optJSONObject("artwork")),
        isDownloadable = obj.optBoolean("is_downloadable", false),
        isStreamable = obj.optBoolean("is_streamable", true),
        isStreamGated = obj.optBoolean("is_stream_gated", false),
        isDownloadGated = obj.optBoolean("is_download_gated", false),
        accessStream = access?.takeIf { it.has("stream") }?.optBoolean("stream"),
        accessDownload = access?.takeIf { it.has("download") }?.optBoolean("download"),
        streamConditions = obj.optJSONObject("stream_conditions"),
        downloadConditions = obj.optJSONObject("download_conditions"),
        playCount = obj.optLong("play_count", 0L),
        favoriteCount = obj.optLong("favorite_count", 0L),
        repostCount = obj.optLong("repost_count", 0L),
        releaseDate = obj.optString("release_date").takeIf { it.isNotBlank() },
        permalink = obj.optString("permalink").takeIf { it.isNotBlank() },
        license = obj.optString("license").takeIf { it.isNotBlank() }
    )
}

fun parseAudiusUser(obj: JSONObject): AudiusUser = AudiusUser(
    id = obj.optString("id"),
    name = obj.optString("name"),
    handle = obj.optString("handle"),
    bio = obj.optString("bio").takeIf { it.isNotBlank() },
    artworkUrl = artworkOf(obj.optJSONObject("profile_picture")),
    coverUrl = artworkOf(obj.optJSONObject("cover_photo")),
    followerCount = obj.optLong("follower_count", 0L),
    followingCount = obj.optLong("followee_count", 0L),
    supporterCount = obj.optLong("supporter_count", 0L),
    trackCount = obj.optInt("track_count", 0),
    albumCount = obj.optInt("album_count", 0),
    playlistCount = obj.optInt("playlist_count", 0),
    verified = obj.optBoolean("is_verified", false),
    location = obj.optString("location").takeIf { it.isNotBlank() },
    website = obj.optString("website").takeIf { it.isNotBlank() }
)

fun parseAudiusCollection(obj: JSONObject): AudiusCollection = AudiusCollection(
    id = obj.optString("id"),
    playlistId = obj.optString("playlist_id"),
    name = obj.optString("playlist_name"),
    description = obj.optString("description").takeIf { it.isNotBlank() },
    owner = obj.optJSONObject("user")?.let(::parseAudiusUser),
    artworkUrl = artworkOf(obj.optJSONObject("artwork")),
    trackCount = obj.optInt("track_count", 0),
    isAlbum = obj.optBoolean("is_album", false),
    totalPlayCount = obj.optLong("total_play_count", 0L),
    favoriteCount = obj.optLong("favorite_count", 0L),
    releaseDate = obj.optString("release_date").takeIf { it.isNotBlank() },
    permalink = obj.optString("permalink").takeIf { it.isNotBlank() }
)

private fun artworkOf(artwork: JSONObject?): String? {
    if (artwork == null) return null
    return listOf("1000x1000", "480x480", "640x", "150x150")
        .asSequence()
        .map { artwork.optString(it) }
        .firstOrNull { it.startsWith("https://", ignoreCase = true) }
}
