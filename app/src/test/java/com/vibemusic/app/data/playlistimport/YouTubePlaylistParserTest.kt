package com.vibemusic.app.data.playlistimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubePlaylistParserTest {

    @Test
    fun `parses playlist header`() {
        val json = """
            {
              "items": [{
                "snippet": {
                  "title": "Mix de tarde",
                  "description": "Canciones para trabajar",
                  "channelTitle": "David",
                  "thumbnails": {
                    "high": {"url": "https://i.ytimg.com/vi/abc/hqdefault.jpg"}
                  }
                },
                "contentDetails": {"itemCount": 12}
              }]
            }
        """.trimIndent()
        val header = parsePlaylistHeader(json)
        assertEquals("Mix de tarde", header?.name)
        assertEquals("David", header?.owner)
        assertEquals("Canciones para trabajar", header?.description)
        assertEquals("https://i.ytimg.com/vi/abc/hqdefault.jpg", header?.artworkUrl)
    }

    @Test
    fun `returns null for private or missing playlist`() {
        assertNull(parsePlaylistHeader("""{"items":[]}"""))
        assertNull(parsePlaylistHeader("""{"error":{"code":404}}"""))
    }

    @Test
    fun `parses items positions and next page token`() {
        val json = """
            {
              "nextPageToken": "CAUQAA",
              "items": [
                {
                  "snippet": {
                    "position": 0,
                    "title": "Conep - Mámamelo (Official Video)",
                    "videoOwnerChannelTitle": "Conep - Topic",
                    "thumbnails": {"high": {"url": "https://i.ytimg.com/vi/one/hq.jpg"}}
                  },
                  "contentDetails": {"videoId": "vid00000001"}
                },
                {
                  "snippet": {
                    "position": 1,
                    "title": "Otra canción",
                    "videoOwnerChannelTitle": "ArtistaVEVO"
                  },
                  "contentDetails": {"videoId": "vid00000002"}
                },
                {
                  "snippet": {
                    "position": 2,
                    "title": "Private video",
                    "videoOwnerChannelTitle": "Canal"
                  },
                  "contentDetails": {"videoId": "vid00000003"}
                }
              ]
            }
        """.trimIndent()
        val page = parsePlaylistItemsPage(json)
        assertEquals(2, page.tracks.size)
        assertEquals("CAUQAA", page.nextPageToken)
        assertEquals(3, page.nextPosition)

        assertEquals("Mámamelo (Official Video)", page.tracks[0].title)
        assertEquals("Conep", page.tracks[0].artists.single())
        assertEquals(0, page.tracks[0].position)
        assertEquals("vid00000001", page.tracks[0].videoId)

        assertEquals("Otra canción", page.tracks[1].title)
        assertEquals("Artista", page.tracks[1].artists.single())
        assertEquals(1, page.tracks[1].position)
    }

    @Test
    fun `unavailable video leaves its original position free`() {
        val json = """
            {
              "items": [
                {
                  "snippet": {"position": 0, "title": "Uno", "videoOwnerChannelTitle": "A"},
                  "contentDetails": {"videoId": "vid00000001"}
                },
                {
                  "snippet": {"position": 1, "title": "Private video", "videoOwnerChannelTitle": "A"},
                  "contentDetails": {"videoId": "vid00000002"}
                },
                {
                  "snippet": {"position": 2, "title": "Tres", "videoOwnerChannelTitle": "A"},
                  "contentDetails": {"videoId": "vid00000003"}
                }
              ]
            }
        """.trimIndent()
        val page = parsePlaylistItemsPage(json)
        assertEquals(listOf(0, 2), page.tracks.map { it.position })
        assertEquals(3, page.nextPosition)
    }

    @Test
    fun `splits artist from title only when the left side is plausible`() {
        val (artist, title) = splitYouTubeMusicTitle(
            "Conep - Mámamelo",
            "Conep - Topic"
        )
        assertEquals("Conep", artist)
        assertEquals("Mámamelo", title)

        val (channelArtist, channelTitle) = splitYouTubeMusicTitle(
            "Mámamelo (Official Audio)",
            "Conep - Topic"
        )
        assertEquals("Conep", channelArtist)
        assertEquals("Mámamelo (Official Audio)", channelTitle)

        val (vevoArtist, vevoTitle) = splitYouTubeMusicTitle(
            "Sin separador",
            "ConepVEVO"
        )
        assertEquals("Conep", vevoArtist)
        assertEquals("Sin separador", vevoTitle)
    }

    @Test
    fun `fills durations in batches without inventing unknown lengths`() {
        val tracks = listOf(
            ImportedPlaylistTrack(0, "Uno", listOf("A"), null, 0, videoId = "v1"),
            ImportedPlaylistTrack(1, "Dos", listOf("B"), null, 0, videoId = "v2")
        )
        val urls = mutableListOf<String>()
        val result = fillDurations(tracks, "KEY") { url, _ ->
            urls += url
            """
                {"items":[
                  {"id":"v1","contentDetails":{"duration":"PT3M33S"}},
                  {"id":"v2","contentDetails":{"duration":"PT1H2M3S"}}
                ]}
            """.trimIndent()
        }
        assertEquals(1, urls.size)
        assertTrue(urls.first().contains("id=v1%2Cv2"))
        assertEquals(213_000, result[0].durationMs)
        assertEquals(3_723_000, result[1].durationMs)
    }

    @Test
    fun `failed duration batch leaves zero without throwing`() {
        val tracks = listOf(
            ImportedPlaylistTrack(0, "Uno", listOf("A"), null, 0, videoId = "v1")
        )
        val result = fillDurations(tracks, "KEY") { _, _ -> error("sin red") }
        assertEquals(0, result.single().durationMs)
    }

    @Test
    fun `builds a paginated official playlist items url`() {
        val url = buildPlaylistItemsUrl("PL123", "KEY", "TOKEN")
        assertTrue(url.startsWith("https://www.googleapis.com/youtube/v3/playlistItems"))
        assertTrue(url.contains("playlistId=PL123"))
        assertTrue(url.contains("pageToken=TOKEN"))
        assertTrue(url.contains("maxResults=50"))
        assertTrue(url.contains("key=KEY"))
    }
}
