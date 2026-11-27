package com.vibemusic.app.data.playlistimport

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubePlaylistImporterTest {

    private val link = PlaylistLink(
        platform = PlaylistPlatform.YOUTUBE_MUSIC,
        playlistId = "PLABC123def456",
        url = "https://music.youtube.com/playlist?list=PLABC123def456"
    )

    private fun item(position: Int, videoId: String, title: String, channel: String = "Conep - Topic") = """
        {"snippet":{"position":$position,"title":"$title",
        "videoOwnerChannelTitle":"$channel"},
        "contentDetails":{"videoId":"$videoId"}}
    """.trimIndent()

    @Test
    fun `imports every page and fills durations without extracting audio`() = runBlocking {
        val seen = mutableListOf<String>()
        val importer = YouTubePlaylistImporter("KEY") { url, _ ->
            seen += url
            when {
                url.contains("/youtube/v3/playlists?") -> """
                    {"items":[{"snippet":{"title":"Mix","description":"Prueba",
                    "channelTitle":"David","thumbnails":{"high":{"url":"https://i.ytimg.com/vi/a/hq.jpg"}}}}]}
                """.trimIndent()

                url.contains("/youtube/v3/playlistItems") && url.contains("pageToken=NEXT") -> """
                    {"items":[${item(2, "vid00000003", "Tres")}]}
                """.trimIndent()

                url.contains("/youtube/v3/playlistItems") -> """
                    {"nextPageToken":"NEXT","items":[
                    ${item(0, "vid00000001", "Conep - Mámamelo")},
                    ${item(1, "vid00000002", "Dos")}]}
                """.trimIndent()

                url.contains("/youtube/v3/videos") -> """
                    {"items":[
                    {"id":"vid00000001","contentDetails":{"duration":"PT3M"}},
                    {"id":"vid00000002","contentDetails":{"duration":"PT2M"}},
                    {"id":"vid00000003","contentDetails":{"duration":"PT1M"}}]}
                """.trimIndent()

                else -> error("URL inesperada: $url")
            }
        }

        val playlist = importer.import(link)
        assertEquals("Mix", playlist.name)
        assertEquals("David", playlist.owner)
        assertEquals(3, playlist.trackCount)
        assertEquals(listOf(0, 1, 2), playlist.tracks.map { it.position })
        assertEquals("Mámamelo", playlist.tracks[0].title)
        assertEquals("Conep", playlist.tracks[0].artistLine)
        assertEquals(180_000, playlist.tracks[0].durationMs)
        assertEquals(120_000, playlist.tracks[1].durationMs)
        assertEquals(60_000, playlist.tracks[2].durationMs)
        assertTrue(seen.any { it.contains("pageToken=NEXT") })
        assertTrue(seen.any { it.contains("/youtube/v3/videos") })
    }

    @Test
    fun `missing api key fails with a clear message`() = runBlocking {
        val error = runCatching {
            YouTubePlaylistImporter("").import(link)
        }.exceptionOrNull()
        assertTrue(error is YouTubePlaylistImportException)
        assertTrue(error?.message?.contains("clave", ignoreCase = true) == true)
    }

    @Test
    fun `private or empty playlist fails without inventing songs`() = runBlocking {
        val importer = YouTubePlaylistImporter("KEY") { url, _ ->
            if (url.contains("/playlists?")) """{"items":[]}""" else """{"items":[]}"""
        }
        val error = runCatching { importer.import(link) }.exceptionOrNull()
        assertTrue(error is YouTubePlaylistImportException)
    }
}
