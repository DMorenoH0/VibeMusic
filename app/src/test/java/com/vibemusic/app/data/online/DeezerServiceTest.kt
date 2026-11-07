package com.vibemusic.app.data.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeezerServiceTest {

    @Test
    fun `parses search results with preview, cover and duration`() {
        val json = """
            {
              "data": [
                {
                  "id": 3135556,
                  "title": "Harder, Better, Faster, Stronger",
                  "duration": 224,
                  "preview": "https://cdns-preview-d.dzcdn.net/stream/c-demo.mp3",
                  "artist": {"id": 27, "name": "Daft Punk"},
                  "album": {"id": 302127, "title": "Discovery", "cover_big": "https://e-cdns-images.dzcdn.net/cover.jpg"}
                }
              ]
            }
        """.trimIndent()

        val tracks = parseSearch(json)

        assertEquals(1, tracks.size)
        val track = tracks[0]
        assertEquals("Harder, Better, Faster, Stronger", track.title)
        assertEquals("Daft Punk", track.artist)
        assertEquals("Discovery", track.album)
        assertEquals(224_000, track.durationMs)
        assertEquals("27", track.artistId)
        assertEquals("302127", track.albumId)
        assertTrue(track.playable)
        assertTrue(track.previewUrl!!.startsWith("https://"))

        val mapped = track.toTrack()
        assertEquals(com.vibemusic.app.data.model.TrackSource.ONLINE, mapped.source)
        assertFalse(mapped.isPersistable)
        assertTrue(mapped.previewOnly)
    }

    @Test
    fun `marks tracks without a preview as not playable`() {
        val json = """
            {"data":[{"id":1,"title":"No preview","duration":100,"artist":{"name":"A"},"album":{"title":"B"}}]}
        """.trimIndent()

        val track = parseSearch(json).single()

        assertFalse(track.playable)
        assertFalse(track.toTrack().isPersistable)
    }

    @Test
    fun `maps API quota errors to a friendly message`() {
        val json = """{"error":{"type":"Exception","message":"Quota limit exceeded","code":4}}"""

        val error = assertThrows(OnlineApiException::class.java) { parseSearch(json) }

        assertTrue(error.message!!.contains("límite"))
    }

    @Test
    fun `parses album detail and fills missing per-track metadata`() {
        val json = """
            {
              "id": 302127,
              "title": "Discovery",
              "release_date": "2001-03-07",
              "nb_tracks": 14,
              "cover_xl": "https://e-cdns-images.dzcdn.net/discovery.jpg",
              "artist": {"id": 27, "name": "Daft Punk"},
              "tracks": {
                "data": [
                  {"id": 1, "title": "One More Time", "duration": 320, "preview": "https://cdn/1.mp3"},
                  {"id": 2, "title": "Aerodynamic", "duration": 212, "preview": "https://cdn/2.mp3"}
                ]
              }
            }
        """.trimIndent()

        val album = parseAlbum(json)

        assertEquals("302127", album.id)
        assertEquals("Discovery", album.title)
        assertEquals("Daft Punk", album.artist)
        assertEquals(2, album.tracks.size)
        assertEquals("Discovery", album.tracks[0].album)
        assertEquals("302127", album.tracks[0].albumId)
        assertEquals("https://e-cdns-images.dzcdn.net/discovery.jpg", album.tracks[0].artworkUrl)
        assertEquals(532_000, album.totalDurationMs)
    }

    @Test
    fun `parses merged artist payload with top tracks and albums`() {
        val json = """
            {
              "info": {"id": 27, "name": "Daft Punk", "nb_fan": 3500000, "nb_album": 5,
                       "picture_big": "https://cdn/artist.jpg"},
              "top": {"data": [{"id": 1, "title": "One More Time", "duration": 320,
                                "preview": "https://cdn/1.mp3", "artist": {"id": 27, "name": "Daft Punk"}}]},
              "albums": {"data": [{"id": 302127, "title": "Discovery", "nb_tracks": 14,
                                   "artist": {"id": 27, "name": "Daft Punk"}}]}
            }
        """.trimIndent()

        val artist = parseArtist(json)

        assertEquals("Daft Punk", artist.name)
        assertEquals(3_500_000L, artist.fanCount)
        assertEquals(5, artist.albumCount)
        assertEquals(1, artist.topTracks.size)
        assertEquals(1, artist.albums.size)
        assertEquals("Discovery", artist.albums[0].title)
    }
}
