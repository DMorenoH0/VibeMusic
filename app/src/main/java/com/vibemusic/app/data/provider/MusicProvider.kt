package com.vibemusic.app.data.provider

import com.vibemusic.app.data.model.Track

/**
 * Capabilities are declared, not assumed. The UI can therefore hide a download
 * action for a provider that only offers previews, and the unified search can
 * label every row with the source that actually served it.
 */
enum class ProviderCapability {
    FULL_TRACKS,
    AUTHORIZED_DOWNLOADS,
    PREVIEWS
}

data class ProviderInfo(
    val id: String,
    val displayName: String,
    val capabilities: Set<ProviderCapability>,
    val description: String,
    val legalNote: String
)

data class ProviderArtist(
    val id: String,
    val providerId: String,
    val name: String,
    val handle: String,
    val artworkUrl: String?,
    val bio: String?,
    val followerCount: Long,
    val trackCount: Int,
    val albumCount: Int,
    val verified: Boolean,
    val location: String?,
    val profileUrl: String?
) {
    val seed: String get() = "$providerId:artist:$id"
}

data class ProviderCollection(
    val id: String,
    val providerId: String,
    val name: String,
    val creator: String,
    val creatorId: String?,
    val artworkUrl: String?,
    val description: String?,
    val trackCount: Int,
    val isAlbum: Boolean,
    val totalPlayCount: Long,
    val webUrl: String?
) {
    val seed: String get() = "$providerId:collection:$id"

    val typeLabel: String get() = if (isAlbum) "Álbum" else "Lista"
}

data class ProviderArtistDetail(
    val artist: ProviderArtist,
    val tracks: List<Track>,
    val albums: List<ProviderCollection>,
    val playlists: List<ProviderCollection>
)

data class ProviderCollectionDetail(
    val collection: ProviderCollection,
    val tracks: List<Track>,
    val related: List<Track>
)

data class UnifiedSearchResult(
    val tracks: List<Track>,
    val artists: List<ProviderArtist>,
    val collections: List<ProviderCollection>,
    val previews: List<Track>,
    val warnings: List<String>
) {
    val hasAnything: Boolean
        get() = tracks.isNotEmpty() || artists.isNotEmpty() ||
            collections.isNotEmpty() || previews.isNotEmpty()
}

/**
 * Contract for every music source. Audius is the first implementation; a future
 * Tuned Global integration only needs to implement this interface and register
 * itself, without touching the player, Room schema or screens.
 *
 * Implementations must never invent endpoints, bypass access gates or return
 * content they are not licensed to serve.
 */
interface MusicProvider {
    val info: ProviderInfo

    suspend fun searchTracks(query: String, limit: Int = 30): List<Track>

    suspend fun searchArtists(query: String, limit: Int = 12): List<ProviderArtist>

    /** Albums and playlists, branchable through [ProviderCollection.isAlbum]. */
    suspend fun searchCollections(query: String, limit: Int = 30): List<ProviderCollection>

    suspend fun trending(limit: Int = 24): List<Track>

    suspend fun artist(id: String): ProviderArtistDetail

    suspend fun collection(id: String): ProviderCollectionDetail

    suspend fun related(track: Track, limit: Int = 20): List<Track>
}

/**
 * Single place where providers are registered. Tuned Global is deliberately not
 * registered: there is no commercial connection or API credential for it in
 * this project, so the app must not pretend that it works.
 */
class ProviderRegistry(providers: List<MusicProvider>) {

    val providers: List<MusicProvider> = providers

    val primary: MusicProvider = providers.firstOrNull()
        ?: error("Debe existir al menos un proveedor de música.")

    fun byId(id: String): MusicProvider? = providers.firstOrNull { it.info.id == id }

    val plans: List<String> = listOf(
        "Tuned Global: interfaz preparada, sin conexión comercial ni credenciales en esta versión."
    )
}
