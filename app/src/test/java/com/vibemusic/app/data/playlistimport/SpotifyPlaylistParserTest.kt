package com.vibemusic.app.data.playlistimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyPlaylistParserTest {

    private val playlistJson = """
        {
          "name": "Rap español",
          "description": "Selección semanal",
          "owner": {"display_name": "David"},
          "images": [
            {"url": "https://i.scdn.co/image/abc"},
            {"url": "https://i.scdn.co/image/def"}
          ],
          "tracks": {
            "total": 3,
            "next": "https://api.spotify.com/v1/playlists/abc/tracks?offset=100&limit=100",
            "items": [
              {
                "track": {
                  "name": "Mámamelo",
                  "type": "track",
                  "duration_ms": 180000,
                  "external_ids": {"isrc": "ES1234567890"},
                  "artists": [{"name": "Conep"}, {"name": "Invited"}],
                  "album": {"name": "Mámamelo"}
                }
              },
              {
                "track": {
                  "name": "Archivo local",
                  "type": "track",
                  "is_local": true,
                  "duration_ms": 1000,
                  "artists": [{"name": "Nadie"}],
                  "album": {"name": "Local"}
                }
              },
              {
                "track": {
                  "name": "Un episodio",
                  "type": "episode",
                  "duration_ms": 1000,
                  "artists": [{"name": "Podcast"}],
                  "album": {"name": "Podcast"}
                }
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `parses header complete metadata and skips local and episodes`() {
        val page = parseSpotifyPlaylistPage(playlistJson)
        assertEquals("Rap español", page.name)
        assertEquals("David", page.owner)
        assertEquals("Selección semanal", page.description)
        assertEquals("https://i.scdn.co/image/abc", page.artworkUrl)
        assertEquals(1, page.tracks.size)
        assertEquals(2, page.skipped)
        assertEquals("https://api.spotify.com/v1/playlists/abc/tracks?offset=100&limit=100", page.next)

        val track = page.tracks.first()
        assertEquals("Mámamelo", track.title)
        assertEquals(listOf("Conep", "Invited"), track.artists)
        assertEquals("Conep", track.primaryArtist)
        assertEquals("Mámamelo", track.album)
        assertEquals(180_000, track.durationMs)
        assertEquals("ES1234567890", track.isrc)
        assertEquals(0, track.position)
    }

    @Test
    fun `parses subsequent pages with continued position`() {
        val json = """
            {
              "total": 4,
              "next": null,
              "items": [
                {
                  "track": {
                    "name": "Otra",
                    "type": "track",
                    "duration_ms": 200000,
                    "artists": [{"name": "Artista"}],
                    "album": {"name": "Álbum"}
                  }
                }
              ]
            }
        """.trimIndent()
        val page = parseSpotifyTracksPage(json, startPosition = 2)
        assertEquals(1, page.tracks.size)
        assertEquals(2, page.tracks.first().position)
        assertNull(page.next)
    }

    @Test
    fun `handles a playlist without tracks`() {
        val page = parseSpotifyPlaylistPage(
            """{"name":"Vacía","tracks":{"items":[],"total":0},"owner":{}}"""
        )
        assertTrue(page.tracks.isEmpty())
        assertEquals(0, page.total)
        assertNull(page.artworkUrl)
    }
}
