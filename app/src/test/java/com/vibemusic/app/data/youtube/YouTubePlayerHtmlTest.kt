package com.vibemusic.app.data.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubePlayerHtmlTest {

    private val template = "<div id=\"player\"></div>videoId: '__VIDEO_ID__'"

    @Test
    fun `renders the validated video id into the template`() {
        val html = renderYouTubePlayerHtml(template, "dQw4w9WgXcQ")
        assertTrue(html.contains("dQw4w9WgXcQ"))
        assertTrue(!html.contains("__VIDEO_ID__"))
    }

    @Test
    fun `rejects ids that could inject markup or script`() {
        assertEquals("", renderYouTubePlayerHtml(template, "</script><script>alert(1)</script>"))
        assertEquals("", renderYouTubePlayerHtml(template, "abc"))
        assertEquals("", renderYouTubePlayerHtml(template, "id with spaces"))
    }

    @Test
    fun `private or removed videos get a clear message`() {
        val message = youtubePlayerErrorMessage(100)
        assertTrue(message.contains("privado"))
        assertTrue(message.contains("eliminado"))
    }

    @Test
    fun `embedding disabled videos explain the restriction`() {
        assertTrue(youtubePlayerErrorMessage(101).contains("no permite"))
        assertTrue(youtubePlayerErrorMessage(150).contains("no permite"))
    }

    @Test
    fun `timeout and unknown errors are described`() {
        assertTrue(youtubePlayerErrorMessage(-1).contains("no respondió"))
        assertTrue(youtubePlayerErrorMessage(-2).contains("no pudo reproducir"))
        assertTrue(youtubePlayerErrorMessage(123).contains("123"))
    }
}
