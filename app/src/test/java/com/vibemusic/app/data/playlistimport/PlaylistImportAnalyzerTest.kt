package com.vibemusic.app.data.playlistimport

import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImportAnalyzerTest {

    private fun source(
        position: Int,
        title: String,
        artist: String = "Conep",
        durationMs: Long = 180_000
    ) = ImportedPlaylistTrack(
        position = position,
        title = title,
        artists = listOf(artist),
        album = "Mámamelo",
        durationMs = durationMs
    )

    private fun track(
        id: String,
        title: String = "Mámamelo",
        artist: String = "Conep",
        durationMs: Long = 180_000,
        source: TrackSource = TrackSource.AUDIUS,
        uri: String = "https://api.audius.co/v1/tracks/$id/stream"
    ) = Track(
        id = id,
        uri = uri,
        title = title,
        artist = artist,
        album = "Mámamelo",
        durationMs = durationMs,
        source = source,
        downloadUrl = if (source == TrackSource.AUDIUS) {
            "https://api.audius.co/v1/tracks/$id/download"
        } else null
    )

    private fun playlist(vararg tracks: ImportedPlaylistTrack) = ImportedPlaylist(
        platform = PlaylistPlatform.SPOTIFY,
        playlistId = "abc",
        name = "Lista",
        owner = "David",
        artworkUrl = "https://i.scdn.co/image/abc",
        sourceUrl = "https://open.spotify.com/playlist/abc",
        tracks = tracks.toList()
    )

    @Test
    fun `analyses available downloadable and doubtful results`() = runBlocking {
        val catalog = object : TrackCatalog {
            override suspend fun findCandidates(source: ImportedPlaylistTrack): List<Track> =
                when (source.title) {
                    "Mámamelo" -> listOf(track("a"))
                    "Mámamelo Remix" -> listOf(track("b", title = "Mámamelo"))
                    else -> emptyList()
                }
        }
        val result = PlaylistImportAnalyzer(catalog, maxParallelSearches = 1).analyze(
            playlist(
                source(0, "Mámamelo"),
                source(1, "Mámamelo Remix"),
                source(2, "Otra")
            )
        )
        assertEquals(3, result.size)
        assertEquals(ImportStatus.DOWNLOADABLE, result[0].outcome.status)
        assertTrue(result[0].selected)
        assertEquals(ImportStatus.NOT_FOUND, result[1].outcome.status)
        assertEquals(ImportStatus.NOT_FOUND, result[2].outcome.status)
    }

    @Test
    fun `restricted audius match is reported as unavailable`() = runBlocking {
        val catalog = object : TrackCatalog {
            override suspend fun findCandidates(source: ImportedPlaylistTrack): List<Track> =
                listOf(track("a", uri = ""))
        }
        val result = PlaylistImportAnalyzer(catalog, maxParallelSearches = 1).analyze(
            playlist(source(0, "Mámamelo"))
        )
        assertEquals(ImportStatus.UNAVAILABLE, result[0].outcome.status)
        assertFalse(result[0].selected)
        assertFalse(result[0].canAdd)
    }

    @Test
    fun `catalogue failure becomes an error row and does not abort the rest`() = runBlocking {
        val catalog = object : TrackCatalog {
            override suspend fun findCandidates(source: ImportedPlaylistTrack): List<Track> {
                if (source.position == 0) error("Sin conexión con Audius")
                return listOf(track("b"))
            }
        }
        val result = PlaylistImportAnalyzer(catalog, maxParallelSearches = 1).analyze(
            playlist(source(0, "Falla"), source(1, "Mámamelo"))
        )
        assertEquals(ImportStatus.ERROR, result[0].outcome.status)
        assertTrue(result[0].outcome.explanation.contains("Sin conexión"))
        assertEquals(ImportStatus.DOWNLOADABLE, result[1].outcome.status)
    }

    @Test
    fun `local library file wins without a catalogue query`() = runBlocking {
        var searches = 0
        val catalog = object : TrackCatalog {
            override suspend fun findCandidates(source: ImportedPlaylistTrack): List<Track> {
                searches++
                return listOf(track("a"))
            }
        }
        val local = track(
            id = "local:1",
            source = TrackSource.LOCAL,
            uri = "content://media/external/audio/media/1"
        )
        val result = PlaylistImportAnalyzer(catalog, maxParallelSearches = 1).analyze(
            playlist(source(0, "Mámamelo")),
            localTracks = listOf(local)
        )
        assertEquals(0, searches)
        assertEquals(ImportStatus.LOCAL, result[0].outcome.status)
        assertEquals("local:1", result[0].outcome.track?.id)
    }

    @Test
    fun `already downloaded audio is reused instead of duplicated`() = runBlocking {
        val catalog = object : TrackCatalog {
            override suspend fun findCandidates(source: ImportedPlaylistTrack): List<Track> =
                listOf(track("audius:track:a"))
        }
        val downloaded = track(
            id = "audius:track:a",
            title = "Mámamelo (Audius original)",
            source = TrackSource.DOWNLOAD,
            uri = "file:///storage/emulated/0/Music/mamamelo.mp3"
        )
        val result = PlaylistImportAnalyzer(catalog, maxParallelSearches = 1).analyze(
            playlist(source(0, "Mámamelo")),
            downloadedTracks = listOf(downloaded)
        )
        assertEquals(ImportStatus.LOCAL, result[0].outcome.status)
        assertEquals("audius:track:a", result[0].outcome.track?.id)
        assertTrue(result[0].selected)
    }

    @Test
    fun `preserves playlist order for a large list`() = runBlocking {
        val total = 120
        val catalog = object : TrackCatalog {
            override suspend fun findCandidates(source: ImportedPlaylistTrack): List<Track> =
                listOf(track("audius:track:${source.position}", title = source.title))
        }
        val sources = (0 until total).map { source(it, "Canción $it") }
        var lastProgress = 0
        val result = PlaylistImportAnalyzer(catalog, maxParallelSearches = 8).analyze(
            playlist(*sources.toTypedArray())
        ) { completed, reportedTotal, _ ->
            assertEquals(total, reportedTotal)
            lastProgress = completed
        }
        assertEquals(total, result.size)
        assertEquals((0 until total).toList(), result.map { it.source.position })
        assertEquals(total, lastProgress)
    }

    @Test
    fun `duplicate entries are marked and not auto selected`() = runBlocking {
        val catalog = object : TrackCatalog {
            override suspend fun findCandidates(source: ImportedPlaylistTrack): List<Track> =
                listOf(track("audius:track:a"))
        }
        val result = PlaylistImportAnalyzer(catalog, maxParallelSearches = 1).analyze(
            playlist(source(0, "Mámamelo"), source(1, "Mámamelo (Official Audio)"))
        )
        assertEquals(2, result.size)
        assertFalse(result[1].selected)
        assertEquals(0, result[1].duplicateOf)
    }
}
