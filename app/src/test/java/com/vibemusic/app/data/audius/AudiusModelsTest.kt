package com.vibemusic.app.data.audius

import com.vibemusic.app.data.model.TrackSource
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parsing is tested against the real response shapes returned by
 * https://api.audius.co/v1. The fixtures below keep only the fields the app
 * reads, so a wrong field name fails here instead of in production.
 */
class AudiusModelsTest {

    private val userJson = """
        {
          "id": "u-1",
          "name": "Luna Vale",
          "handle": "lunavale",
          "bio": "Productora madrileña.",
          "profile_picture": {
            "150x150": "https://creatornode.audius.co/artwork/profile150.jpg",
            "480x480": "https://creatornode.audius.co/artwork/profile480.jpg",
            "1000x1000": "https://creatornode.audius.co/artwork/profile1000.jpg"
          },
          "cover_photo": {"640": "https://creatornode.audius.co/artwork/cover640.jpg"},
          "follower_count": 12345,
          "followee_count": 42,
          "supporter_count": 7,
          "track_count": 18,
          "album_count": 3,
          "playlist_count": 2,
          "is_verified": true,
          "location": "Madrid",
          "website": "https://lunavale.example"
        }
    """.trimIndent()

    private fun trackJson(
        id: String = "492515762",
        isDownloadable: Boolean = true,
        isDownloadGated: Boolean = false,
        isStreamGated: Boolean = false,
        accessDownload: Boolean? = true,
        downloadConditions: String? = null
    ): String {
        val access = buildString {
            append("\"stream\": true")
            if (accessDownload != null) append(", \"download\": $accessDownload")
        }
        val conditions = downloadConditions ?: "null"
        return """
            {
              "id": "$id",
              "title": "Dance With The Afterglow",
              "duration": 325,
              "genre": "Electronic",
              "mood": "Energetic",
              "description": "Una pista original.",
              "artwork": {
                "150x150": "https://creatornode.audius.co/artwork/150.jpg",
                "480x480": "https://creatornode.audius.co/artwork/480.jpg",
                "1000x1000": "https://creatornode.audius.co/artwork/1000.jpg"
              },
              "user": $userJson,
              "album_backlink": {
                "playlist_id": "99",
                "playlist_name": "Afterglow EP",
                "permalink": "/lunavale/album/afterglow-ep"
              },
              "is_downloadable": $isDownloadable,
              "is_streamable": true,
              "is_stream_gated": $isStreamGated,
              "is_download_gated": $isDownloadGated,
              "access": { $access },
              "stream_conditions": null,
              "download_conditions": $conditions,
              "play_count": 5421,
              "favorite_count": 210,
              "repost_count": 36,
              "release_date": "2025-01-01T00:00:00Z",
              "permalink": "/lunavale/dance-with-the-afterglow",
              "license": "All rights reserved"
            }
        """.trimIndent()
    }

    @Test
    fun `parses a real track payload with artwork user and album backlink`() {
        val tracks = parseAudiusTracks("""{"data":[${trackJson()}]}""")

        assertEquals(1, tracks.size)
        val track = tracks.first()
        assertEquals("492515762", track.id)
        assertEquals("Dance With The Afterglow", track.title)
        assertEquals(325L, track.durationSeconds)
        assertEquals("Electronic", track.genre)
        assertEquals("Luna Vale", track.user?.name)
        assertEquals("Afterglow EP", track.album?.name)
        assertEquals("99", track.album?.id)
        assertEquals(
            "https://creatornode.audius.co/artwork/1000.jpg",
            track.artworkUrl
        )
        assertTrue(track.playable)
        assertTrue(track.downloadPermission.allowed)
    }

    @Test
    fun `maps a downloadable audius track to a stable app track`() {
        val source = parseAudiusTracks("""{"data":[${trackJson()}]}""").first()
        val track = source.toTrack()

        assertEquals("audius:track:492515762", track.id)
        assertEquals(TrackSource.AUDIUS, track.source)
        assertEquals("Luna Vale", track.artist)
        assertEquals("Afterglow EP", track.album)
        assertEquals("99", track.albumId)
        assertEquals("u-1", track.artistId)
        assertEquals(325_000L, track.durationMs)
        assertEquals(
            "https://api.audius.co/v1/tracks/492515762/stream?app_name=VibeMusic",
            track.uri
        )
        assertEquals(
            "https://api.audius.co/v1/tracks/492515762/download?app_name=VibeMusic",
            track.downloadUrl
        )
        assertNull(track.downloadBlockedReason)
        assertTrue(track.isPersistable)
    }

    @Test
    fun `a track the artist did not mark downloadable is rejected with a reason`() {
        val source = parseAudiusTracks(
            """{"data":[${trackJson(isDownloadable = false, accessDownload = null)}]}"""
        ).first()

        val permission = source.downloadPermission
        assertFalse(permission.allowed)
        assertTrue(permission.reason!!.contains("no ha autorizado"))
        assertNull(source.toTrack().downloadUrl)
    }

    @Test
    fun `usdc purchase gate is reported and never bypassed`() {
        val price = """{"usdc_purchase":{"price":250,"spl_token_amount":250}}"""
        val source = parseAudiusTracks(
            """{"data":[${trackJson(
                isDownloadGated = true,
                accessDownload = false,
                downloadConditions = price
            )}]}"""
        ).first()

        val permission = source.downloadPermission
        assertFalse(permission.allowed)
        assertTrue(permission.reason!!.contains("USDC"))
        assertNull(source.toTrack().downloadUrl)
    }

    @Test
    fun `follow gate explains the unavailable download`() {
        val source = parseAudiusTracks(
            """{"data":[${trackJson(
                isDownloadGated = true,
                accessDownload = false,
                downloadConditions = """{"follow_user_id":123}"""
            )}]}"""
        ).first()

        val permission = source.downloadPermission
        assertFalse(permission.allowed)
        assertTrue(permission.reason!!.contains("sigas"))
    }

    @Test
    fun `download access false blocks the file even without a named condition`() {
        val source = parseAudiusTracks(
            """{"data":[${trackJson(accessDownload = false)}]}"""
        ).first()

        val permission = source.downloadPermission
        assertFalse(permission.allowed)
        assertTrue(permission.reason!!.contains("restringido"))
    }

    @Test
    fun `stream gated track is not exposed to anonymous playback`() {
        val source = parseAudiusTracks(
            """{"data":[${trackJson(isStreamGated = true, accessDownload = true)}]}"""
        ).first()

        assertFalse(source.playable)
        assertTrue(source.toTrack().uri.isBlank())
    }

    @Test
    fun `parses users search with verified artist metadata`() {
        val users = parseAudiusUsers("""{"data":[$userJson]}""")

        assertEquals(1, users.size)
        val artist = users.first()
        assertEquals("Luna Vale", artist.displayName)
        assertEquals("lunavale", artist.handle)
        assertEquals(12345L, artist.followerCount)
        assertEquals(18, artist.trackCount)
        assertEquals("https://audius.co/lunavale", artist.profileUrl)
        assertTrue(artist.verified)
    }

    @Test
    fun `parses a single user detail response`() {
        val user = parseAudiusUserResponse("""{"data":$userJson}""")
        assertEquals("Luna Vale", user?.name)
        assertEquals("Productora madrileña.", user?.bio)
    }

    @Test
    fun `parses album collections using the official playlist shape`() {
        val album = """
            {
              "data": {
                "id": "album-1",
                "playlist_id": "99",
                "playlist_name": "Afterglow EP",
                "description": "Cinco pistas.",
                "user": $userJson,
                "artwork": {"1000x1000": "https://creatornode.audius.co/artwork/album.jpg"},
                "track_count": 5,
                "is_album": true,
                "total_play_count": 900,
                "favorite_count": 80,
                "release_date": "2025-02-02T00:00:00Z",
                "permalink": "/lunavale/album/afterglow-ep"
              }
            }
        """.trimIndent()

        val collection = parseAudiusCollectionResponse(album)

        assertEquals("99", collection?.playlistId)
        assertEquals("Afterglow EP", collection?.name)
        assertTrue(collection!!.isAlbum)
        assertEquals(5, collection.trackCount)
        assertEquals("Luna Vale", collection.displayCreator)
        assertEquals("https://audius.co/lunavale/album/afterglow-ep", collection.webUrl)
    }

    @Test
    fun `parses playlist search arrays distinguishing lists from albums`() {
        val json = """
            {
              "data": [
                {
                  "id": "p-1",
                  "playlist_id": "pl-1",
                  "playlist_name": "Sesión nocturna",
                  "user": $userJson,
                  "artwork": {"480x480": "https://creatornode.audius.co/artwork/list.jpg"},
                  "track_count": 12,
                  "is_album": false,
                  "total_play_count": 0,
                  "favorite_count": 4,
                  "permalink": "/lunavale/playlist/sesion-nocturna"
                }
              ]
            }
        """.trimIndent()

        val lists = parseAudiusCollections(json)

        assertEquals(1, lists.size)
        assertFalse(lists.first().isAlbum)
        assertEquals("Sesión nocturna", lists.first().name)
    }

    @Test
    fun `missing fields do not crash and fall back safely`() {
        val track = parseAudiusTrack(JSONObject("""{"id":"1","title":"Sin datos"}"""))

        assertEquals("1", track.id)
        assertEquals("Artista de Audius", track.toTrack().artist)
        assertEquals("Audius", track.toTrack().album)
        assertNull(track.artworkUrl)
        assertFalse(track.downloadPermission.allowed)
    }
}
