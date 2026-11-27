package com.vibemusic.app.data.playlistimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistLinkParserTest {

    @Test
    fun `accepts spotify playlist url and ignores query`() {
        val result = PlaylistLinkParser.parse(
            "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc123"
        )
        assertTrue(result is PlaylistLinkResult.Valid)
        val link = (result as PlaylistLinkResult.Valid).link
        assertEquals(PlaylistPlatform.SPOTIFY, link.platform)
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", link.playlistId)
    }

    @Test
    fun `accepts spotify uri`() {
        val result = PlaylistLinkParser.parse("spotify:playlist:37i9dQZF1DXcBWIGoYBM5M")
        assertTrue(result is PlaylistLinkResult.Valid)
        assertEquals(
            PlaylistPlatform.SPOTIFY,
            (result as PlaylistLinkResult.Valid).link.platform
        )
    }

    @Test
    fun `rejects spotify track link with a clear message`() {
        val result = PlaylistLinkParser.parse(
            "https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT"
        )
        assertTrue(result is PlaylistLinkResult.Invalid)
        assertTrue((result as PlaylistLinkResult.Invalid).message.contains("playlist"))
    }

    @Test
    fun `accepts youtube music playlist link`() {
        val result = PlaylistLinkParser.parse(
            "https://music.youtube.com/playlist?list=PLABC123def456"
        )
        assertTrue(result is PlaylistLinkResult.Valid)
        val link = (result as PlaylistLinkResult.Valid).link
        assertEquals(PlaylistPlatform.YOUTUBE_MUSIC, link.platform)
        assertEquals("PLABC123def456", link.playlistId)
    }

    @Test
    fun `accepts watch link when it contains a list parameter`() {
        val result = PlaylistLinkParser.parse(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLABC123def456&index=3"
        )
        assertTrue(result is PlaylistLinkResult.Valid)
        assertEquals(
            PlaylistPlatform.YOUTUBE_MUSIC,
            (result as PlaylistLinkResult.Valid).link.platform
        )
    }

    @Test
    fun `rejects a youtube video without playlist`() {
        val result = PlaylistLinkParser.parse(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )
        assertTrue(result is PlaylistLinkResult.Invalid)
    }

    @Test
    fun `rejects unrelated hosts and empty text`() {
        assertTrue(
            PlaylistLinkParser.parse("https://example.com/playlist/abc") is PlaylistLinkResult.Invalid
        )
        assertTrue(PlaylistLinkParser.parse("   ") is PlaylistLinkResult.Invalid)
    }

    @Test
    fun `rejects a short spotify id`() {
        assertTrue(
            PlaylistLinkParser.parse("https://open.spotify.com/playlist/abc") is PlaylistLinkResult.Invalid
        )
    }
}
