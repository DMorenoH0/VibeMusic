package com.vibemusic.app.data.provider

import com.vibemusic.app.data.audius.AudiusCollection
import com.vibemusic.app.data.audius.AudiusService
import com.vibemusic.app.data.audius.AudiusTrack
import com.vibemusic.app.data.audius.AudiusUser
import com.vibemusic.app.data.model.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Audius implementation of [MusicProvider]. It maps official API objects to the
 * app's provider-neutral models and only exposes tracks that an anonymous
 * client can actually stream. Download rights are stored per track, never
 * inferred from the provider as a whole.
 */
class AudiusProvider(
    private val service: AudiusService = AudiusService()
) : MusicProvider {

    override val info = ProviderInfo(
        id = "audius",
        displayName = "Audius",
        capabilities = setOf(
            ProviderCapability.FULL_TRACKS,
            ProviderCapability.AUTHORIZED_DOWNLOADS
        ),
        description = "Catálogo abierto con canciones completas, artistas, álbumes y listas.",
        legalNote = "Audius sirve el audio desde sus propios nodos. VibeMusic respeta " +
            "las condiciones de cada pista y solo descarga cuando el titular lo autoriza."
    )

    override suspend fun searchTracks(query: String, limit: Int): List<Track> =
        service.searchTracks(query, limit)
            .filter { it.playable }
            .map { it.toTrack() }

    override suspend fun searchArtists(query: String, limit: Int): List<ProviderArtist> =
        service.searchUsers(query, limit).map { it.toProviderArtist() }

    /**
     * Audius v1 has no dedicated `/albums/search` route (verified against the
     * live API). Album results therefore come from the official playlist search
     * plus the official `/users/{id}/albums` route of the matching artists.
     */
    override suspend fun searchCollections(query: String, limit: Int): List<ProviderCollection> =
        coroutineScope {
            val direct = service.searchCollections(query, limit)
            val albumsFromArtists = if (direct.none { it.isAlbum }) {
                service.searchUsers(query, 3).flatMap { user ->
                    runCatching { service.userAlbums(user.id, 6) }
                        .getOrDefault(emptyList())
                }
            } else {
                emptyList()
            }
            (direct + albumsFromArtists)
                .distinctBy { it.id }
                .filter { it.trackCount > 0 || it.isAlbum }
                .map { it.toProviderCollection() }
        }

    override suspend fun trending(limit: Int): List<Track> =
        service.trending(limit)
            .filter { it.playable }
            .map { it.toTrack() }

    override suspend fun artist(id: String): ProviderArtistDetail = coroutineScope {
        val user = async { service.user(id) }
        val tracks = async { service.userTracks(id, 50) }
        val albums = async { service.userAlbums(id, 20) }
        val playlists = async { service.userPlaylists(id, 20) }
        val resolved = user.await() ?: throw com.vibemusic.app.data.audius.AudiusApiException(
            "No se pudo cargar el artista de Audius."
        )
        ProviderArtistDetail(
            artist = resolved.toProviderArtist(),
            tracks = tracks.await().filter { it.playable }.map { it.toTrack() },
            albums = albums.await().map { it.toProviderCollection() },
            playlists = playlists.await().map { it.toProviderCollection() }
        )
    }

    override suspend fun collection(id: String): ProviderCollectionDetail = coroutineScope {
        val collection = async { service.collection(id) }
        val tracks = async { service.collectionTracks(id, 100) }
        val resolved = collection.await() ?: throw com.vibemusic.app.data.audius.AudiusApiException(
            "No se pudo cargar la lista de Audius."
        )
        val playable = tracks.await().filter { it.playable }
        val related = playable.firstOrNull()?.let { first ->
            runCatching { service.relatedTracks(first, 12) }.getOrDefault(emptyList())
        }.orEmpty()
        ProviderCollectionDetail(
            collection = resolved.toProviderCollection(),
            tracks = playable.map { it.toTrack() },
            related = related.filter { it.playable }.map { it.toTrack() }
        )
    }

    override suspend fun related(track: Track, limit: Int): List<Track> {
        val id = track.id.removePrefix("audius:track:")
        if (id.isBlank() || id == track.id) return emptyList()
        val source = service.track(id) ?: return emptyList()
        return service.relatedTracks(source, limit)
            .filter { it.playable }
            .map { it.toTrack() }
    }

    fun clearCache() = service.clearCache()
}

private fun AudiusUser.toProviderArtist() = ProviderArtist(
    id = id,
    providerId = "audius",
    name = displayName,
    handle = handle,
    artworkUrl = artworkUrl,
    bio = bio,
    followerCount = followerCount,
    trackCount = trackCount,
    albumCount = albumCount,
    verified = verified,
    location = location,
    profileUrl = profileUrl
)

private fun AudiusCollection.toProviderCollection() = ProviderCollection(
    id = id,
    providerId = "audius",
    name = name,
    creator = displayCreator,
    creatorId = owner?.id,
    artworkUrl = artworkUrl,
    description = description,
    trackCount = trackCount,
    isAlbum = isAlbum,
    totalPlayCount = totalPlayCount,
    webUrl = webUrl
)
