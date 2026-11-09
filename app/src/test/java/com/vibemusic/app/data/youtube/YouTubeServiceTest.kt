package com.vibemusic.app.data.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeServiceTest {

    private val searchJson = """
        {
          "items": [
            {
              "id": {"videoId": "dQw4w9WgXcQ"},
              "snippet": {
                "title": "Rick Astley - Never Gonna Give You Up",
                "channelTitle": "Rick Astley",
                "publishedAt": "2009-10-25T06:57:33Z",
                "thumbnails": {
                  "default": {"url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/default.jpg"},
                  "high": {"url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"}
                }
              }
            },
            {
              "id": {"videoId": "5NV6Rdv1a3I"},
              "snippet": {
                "title": "Daft Punk - Get Lucky",
                "channelTitle": "Daft Punk",
                "publishedAt": "2013-04-19T00:00:00Z",
                "thumbnails": {
                  "medium": {"url": "https://i.ytimg.com/vi/5NV6Rdv1a3I/mqdefault.jpg"}
                }
              }
            },
            {
              "id": {"videoId": "not valid!"},
              "snippet": {"title": "Ignorado", "channelTitle": "X"}
            }
          ]
        }
    """.trimIndent()

    private val videosJson = """
        {
          "items": [
            {
              "id": "5NV6Rdv1a3I",
              "contentDetails": {"duration": "PT4M8S"},
              "snippet": {
                "title": "Daft Punk - Get Lucky (Official Audio)",
                "channelTitle": "Daft Punk",
                "publishedAt": "2013-04-19T00:00:00Z",
                "description": "Official audio",
                "thumbnails": {"high": {"url": "https://i.ytimg.com/vi/5NV6Rdv1a3I/hqdefault.jpg"}}
              }
            },
            {
              "id": "dQw4w9WgXcQ",
              "contentDetails": {"duration": "PT3M33S"},
              "snippet": {
                "title": "Rick Astley - Never Gonna Give You Up (Video)",
                "channelTitle": "Rick Astley",
                "publishedAt": "2009-10-25T06:57:33Z",
                "description": "Official video",
                "thumbnails": {"high": {"url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"}}
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `search url encodes query and requests 25 videos`() {
        val url = buildSearchUrl("Daft Punk & Pharrell", "KEY-123")
        assertTrue(url.startsWith("https://www.googleapis.com/youtube/v3/search?"))
        assertTrue(url.contains("type=video"))
        assertTrue(url.contains("maxResults=25"))
        assertTrue(url.contains("q=Daft+Punk+%26+Pharrell"))
        assertTrue(url.contains("key=KEY-123"))
        assertTrue(url.contains("part=snippet"))
    }

    @Test
    fun `videos url joins ids and asks for content details`() {
        val url = buildVideosUrl(listOf("abc123", "def456"), "KEY")
        assertTrue(url.startsWith("https://www.googleapis.com/youtube/v3/videos?"))
        assertTrue(url.contains("part=contentDetails,snippet"))
        assertTrue(url.contains("id=abc123%2Cdef456"))
    }

    @Test
    fun `search ids preserve order and drop invalid ids`() {
        assertEquals(listOf("dQw4w9WgXcQ", "5NV6Rdv1a3I"), parseSearchIds(searchJson))
        assertEquals(emptyList<String>(), parseSearchIds("""{"items":[]}"""))
        assertEquals(emptyList<String>(), parseSearchIds("not json"))
    }

    @Test
    fun `videos combine search order with durations and details`() {
        val videos = parseYouTubeVideos(searchJson, videosJson)
        assertEquals(2, videos.size)
        assertEquals("dQw4w9WgXcQ", videos[0].id)
        assertEquals("Rick Astley - Never Gonna Give You Up (Video)", videos[0].title)
        assertEquals("Rick Astley", videos[0].channel)
        assertEquals(213_000L, videos[0].durationMs)
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", videos[0].thumbnailUrl)
        assertEquals(248_000L, videos[1].durationMs)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", videos[0].watchUrl)
    }

    @Test
    fun `iso 8601 durations are parsed`() {
        assertEquals(213_000L, parseIsoDurationMs("PT3M33S"))
        assertEquals(45_000L, parseIsoDurationMs("PT45S"))
        assertEquals(3_723_000L, parseIsoDurationMs("PT1H2M3S"))
        assertEquals(93_600_000L, parseIsoDurationMs("P1DT2H"))
        assertEquals(0L, parseIsoDurationMs(""))
        assertEquals(0L, parseIsoDurationMs("3:33"))
    }

    @Test
    fun `quota errors are reported clearly`() {
        val body = """
            {"error":{"code":403,"message":"Quota exceeded","errors":[
              {"reason":"quotaExceeded","domain":"youtube.quota"}]}}
        """.trimIndent()
        val error = mapYouTubeError(403, body)
        assertEquals(YouTubeException.Kind.QUOTA, error.kind)
        assertTrue(error.message.orEmpty().contains("cuota"))
    }

    @Test
    fun `invalid key and disabled api are mapped`() {
        val invalid = mapYouTubeError(
            400,
            """{"error":{"code":400,"errors":[{"reason":"keyInvalid"}]}}"""
        )
        assertEquals(YouTubeException.Kind.INVALID_KEY, invalid.kind)

        val disabled = mapYouTubeError(
            403,
            """{"error":{"code":403,"errors":[{"reason":"accessNotConfigured"}]}}"""
        )
        assertEquals(YouTubeException.Kind.API_DISABLED, disabled.kind)
    }
}
