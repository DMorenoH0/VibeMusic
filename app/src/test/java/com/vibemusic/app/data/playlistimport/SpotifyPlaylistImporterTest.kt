package com.vibemusic.app.data.playlistimport

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyPlaylistImporterTest {

    private val link = PlaylistLink(
        platform = PlaylistPlatform.SPOTIFY,
        playlistId = "37i9dQZF1DXcBWIGoYBM5M",
        url = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"
    )

    private class FakeTransport(
        private val pages: Map<String, String>
    ) : SpotifyTransport {
        var tokenCalls = 0
        var getCalls = 0
        override suspend fun token(clientId: String, clientSecret: String): String {
            tokenCalls++
            assertEquals("id", clientId)
            assertEquals("secret", clientSecret)
            return "token"
        }

        override suspend fun get(url: String, accessToken: String): String {
            getCalls++
            assertEquals("token", accessToken)
            return pages[url] ?: error("URL inesperada: $url")
        }
    }

    private fun item(title: String, artist: String = "Conep", duration: Long = 180000) = """
        {"track":{"name":"$title","type":"track","duration_ms":$duration,
        "artists":[{"name":"$artist"}],"album":{"name":"Álbum"}}}
    """.trimIndent()

    private fun playlistPage(
        next: String?,
        items: List<String>,
        name: String = "Lista"
    ) = """
        {"name":"$name","owner":{"display_name":"David"},
        "images":[{"url":"https://i.scdn.co/image/cover"}],
        "tracks":{"total":${items.size},"next":${next?.let { "\"$it\"" } ?: "null"},
        "items":[${items.joinToString(",")}]}}
    """.trimIndent()

    private fun tracksPage(next: String?, items: List<String>) = """
        {"total":${items.size},"next":${next?.let { "\"$it\"" } ?: "null"},
        "items":[${items.joinToString(",")}]}
    """.trimIndent()

    @Test
    fun `imports all pages preserving order`() = runBlocking {
        val page1 = playlistPage(
            next = "https://api.spotify.com/v1/playlists/${link.playlistId}/tracks?offset=2",
            items = listOf(item("Uno"), item("Dos"))
        )
        val page2 = tracksPage(
            next = null,
            items = listOf(item("Tres"), item("Cuatro"))
        )
        val transport = FakeTransport(
            mapOf(
                "https://api.spotify.com/v1/playlists/${link.playlistId}" to page1,
                "https://api.spotify.com/v1/playlists/${link.playlistId}/tracks?offset=2" to page2
            )
        )
        val playlist = SpotifyPlaylistImporter(
            SpotifyCredentials("id", "secret"),
            transport
        ).import(link)

        assertEquals("Lista", playlist.name)
        assertEquals("David", playlist.owner)
        assertEquals("https://i.scdn.co/image/cover", playlist.artworkUrl)
        assertEquals(4, playlist.trackCount)
        assertEquals(listOf("Uno", "Dos", "Tres", "Cuatro"), playlist.tracks.map { it.title })
        assertEquals(listOf(0, 1, 2, 3), playlist.tracks.map { it.position })
        assertEquals(1, transport.tokenCalls)
        assertEquals(2, transport.getCalls)
    }

    @Test
    fun `keeps the original playlist slot when a local file is skipped`() = runBlocking {
        val local = """
            {"track":{"name":"Archivo local","type":"track","is_local":true,
            "duration_ms":1000,"artists":[{"name":"Nadie"}],"album":{"name":"Local"}}}
        """.trimIndent()
        val page1 = playlistPage(
            next = null,
            items = listOf(item("Uno"), local, item("Dos"))
        )
        val transport = FakeTransport(
            mapOf("https://api.spotify.com/v1/playlists/${link.playlistId}" to page1)
        )
        val playlist = SpotifyPlaylistImporter(
            SpotifyCredentials("id", "secret"),
            transport
        ).import(link)
        assertEquals(listOf("Uno", "Dos"), playlist.tracks.map { it.title })
        assertEquals(listOf(0, 2), playlist.tracks.map { it.position })
    }

    @Test
    fun `imports a large playlist across pages`() = runBlocking {
        val firstBatch = (0 until 100).map { item("Canción $it") }
        val secondBatch = (100 until 120).map { item("Canción $it") }
        val next = "https://api.spotify.com/v1/playlists/${link.playlistId}/tracks?offset=100"
        val transport = FakeTransport(
            mapOf(
                "https://api.spotify.com/v1/playlists/${link.playlistId}" to
                    playlistPage(next, firstBatch),
                next to tracksPage(null, secondBatch)
            )
        )
        val playlist = SpotifyPlaylistImporter(
            SpotifyCredentials("id", "secret"),
            transport
        ).import(link)
        assertEquals(120, playlist.trackCount)
        assertEquals("Canción 0", playlist.tracks.first().title)
        assertEquals("Canción 119", playlist.tracks.last().title)
        assertEquals((0 until 120).toList(), playlist.tracks.map { it.position })
    }

    @Test
    fun `rejects missing credentials before touching the network`() = runBlocking {
        val transport = FakeTransport(emptyMap())
        val error = runCatching {
            SpotifyPlaylistImporter(SpotifyCredentials("", ""), transport).import(link)
        }.exceptionOrNull()
        assertTrue(error is SpotifyImportException)
        assertEquals(0, transport.tokenCalls)
    }

    @Test
    fun `rejects an unexpected pagination host`() = runBlocking {
        val page1 = playlistPage(
            next = "https://evil.example/steal",
            items = listOf(item("Uno"))
        )
        val transport = FakeTransport(
            mapOf("https://api.spotify.com/v1/playlists/${link.playlistId}" to page1)
        )
        val error = runCatching {
            SpotifyPlaylistImporter(SpotifyCredentials("id", "secret"), transport).import(link)
        }.exceptionOrNull()
        assertTrue(error is SpotifyImportException)
        assertTrue(error?.message?.contains("paginación") == true)
    }
}
