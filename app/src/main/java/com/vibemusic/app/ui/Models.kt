package com.vibemusic.app.ui

import com.vibemusic.app.data.model.Track

data class AlbumGroup(
    val name: String,
    val artist: String,
    val artworkUri: String?,
    val tracks: List<Track>
) {
    val seed: String get() = tracks.firstOrNull()?.id ?: "album:$name"
    val durationMs: Long get() = tracks.sumOf { it.durationMs }
}

data class ArtistGroup(
    val name: String,
    val artworkUri: String?,
    val trackCount: Int,
    val seed: String
)

fun groupAlbums(tracks: List<Track>): List<AlbumGroup> =
    tracks.groupBy { it.album.ifBlank { "Sin álbum" } }
        .map { (album, list) ->
            AlbumGroup(
                name = album,
                artist = list.firstOrNull()?.artist ?: "VibeMusic",
                artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                tracks = list.sortedBy { it.title.lowercase() }
            )
        }
        .sortedBy { it.name.lowercase() }

fun groupArtists(tracks: List<Track>): List<ArtistGroup> =
    tracks.groupBy { it.artist.ifBlank { "Artista desconocido" } }
        .map { (artist, list) ->
            ArtistGroup(
                name = artist,
                artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                trackCount = list.size,
                seed = "artist:$artist"
            )
        }
        .sortedBy { it.name.lowercase() }
