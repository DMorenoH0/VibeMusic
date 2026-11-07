package com.vibemusic.app.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GoogleAuthManagerTest {

    @Test
    fun `extracts profile fields from the token payload`() {
        val payload = """{"sub":"1234567890","name":"David Moreno","email":"david@example.com"}"""

        val profile = profileFromPayload(payload)

        assertEquals("1234567890", profile.id)
        assertEquals("David Moreno", profile.name)
        assertEquals("david@example.com", profile.email)
    }

    @Test
    fun `falls back to the email prefix when the name is missing`() {
        val profile = profileFromPayload("""{"sub":"1","email":"user@example.com"}""")

        assertEquals("user", profile.name)
    }

    @Test
    fun `rejects payloads without an account identifier`() {
        assertThrows(AuthException::class.java) {
            profileFromPayload("""{"name":"No id","email":"x@example.com"}""")
        }
    }

    @Test
    fun `rejects malformed JSON`() {
        assertThrows(AuthException::class.java) { profileFromPayload("not-json") }
    }
}
