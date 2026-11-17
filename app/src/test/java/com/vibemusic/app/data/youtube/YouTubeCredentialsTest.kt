package com.vibemusic.app.data.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeCredentialsTest {

    @Test
    fun `a stored key wins over the build-time key`() {
        assertEquals(
            "stored-key-1234567890",
            YouTubeCredentials.resolve("build-key", "stored-key-1234567890")
        )
        assertEquals("build-key", YouTubeCredentials.resolve("build-key", null))
        assertEquals("build-key", YouTubeCredentials.resolve("build-key", "   "))
        assertEquals("", YouTubeCredentials.resolve("", null))
        assertEquals("trimmed", YouTubeCredentials.resolve("", "  trimmed  "))
    }

    @Test
    fun `well formed keys reject empty values whitespace and documents`() {
        assertTrue(YouTubeCredentials.isWellFormed("AIzaSyA1234567890abcdefghijklmnopqrstu"))
        assertFalse(YouTubeCredentials.isWellFormed(""))
        assertFalse(YouTubeCredentials.isWellFormed("short"))
        assertFalse(YouTubeCredentials.isWellFormed("AIza key with spaces 123456789"))
        assertFalse(YouTubeCredentials.isWellFormed("x".repeat(201)))
    }

    @Test
    fun `mask only reveals the last four characters`() {
        assertEquals("••••wxyz", YouTubeCredentials.mask("AIzaSyA1234567890wxyz"))
        assertEquals("••••", YouTubeCredentials.mask("abc"))
    }
}
