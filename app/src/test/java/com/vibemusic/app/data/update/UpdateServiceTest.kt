package com.vibemusic.app.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateServiceTest {

    @Test
    fun `compares semantic versions numerically`() {
        assertTrue(compareVersions("1.1.0", "1.0.9") > 0)
        assertTrue(compareVersions("1.10.0", "1.9.9") > 0)
        assertEquals(0, compareVersions("1.2", "1.2.0"))
        assertEquals(0, compareVersions("v1.2.0", "1.2.0"))
        assertTrue(compareVersions("1.2.0", "1.2.1") < 0)
        assertTrue(isNewerVersion("2.0.0", "1.99.99"))
        assertFalse(isNewerVersion("1.0.0", "1.0.0"))
    }

    @Test
    fun `pre-release ranks below the same stable version`() {
        assertTrue(compareVersions("1.2.0", "1.2.0-beta1") > 0)
        assertTrue(compareVersions("1.2.0-beta1", "1.2.0") < 0)
    }

    @Test
    fun `normalises release tags`() {
        assertEquals("1.1.0", normalizeVersion(" v1.1.0 "))
        assertEquals("2.0", normalizeVersion("V2.0"))
    }

    @Test
    fun `parses the stable release and its apk digest`() {
        val json = """
            {
              "tag_name": "v1.1.0",
              "name": "VibeMusic 1.1.0",
              "body": "Novedades\nSHA-256: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
              "html_url": "https://github.com/DMorenoH0/VibeMusic/releases/tag/v1.1.0",
              "published_at": "2026-11-02T10:00:00Z",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "VibeMusic.apk",
                  "size": 12345678,
                  "browser_download_url": "https://github.com/DMorenoH0/VibeMusic/releases/download/v1.1.0/VibeMusic.apk",
                  "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                }
              ]
            }
        """.trimIndent()

        val release = parseLatestRelease(json)

        assertEquals("v1.1.0", release.tag)
        assertEquals("1.1.0", release.version)
        assertEquals("VibeMusic 1.1.0", release.name)
        assertEquals(12_345_678L, release.apkSize)
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", release.sha256)
        assertTrue(release.apkUrl!!.endsWith("VibeMusic.apk"))
    }

    @Test
    fun `falls back to a hash written in the release body`() {
        val hash = "c".repeat(64)
        val json = """
            {
              "tag_name": "v1.2.0",
              "body": "APK SHA-256: $hash",
              "assets": []
            }
        """.trimIndent()

        val release = parseLatestRelease(json)

        assertEquals(hash, release.sha256)
        assertNull(release.apkUrl)
    }

    @Test
    fun `rejects drafts and pre-releases`() {
        val draft = """{"tag_name":"v1.1.0","draft":true,"assets":[]}"""
        val prerelease = """{"tag_name":"v1.1.0","prerelease":true,"assets":[]}"""

        assertThrows(UpdateException::class.java) { parseLatestRelease(draft) }
        assertThrows(UpdateException::class.java) { parseLatestRelease(prerelease) }
    }

    @Test
    fun `rejects a release without a tag`() {
        assertThrows(UpdateException::class.java) { parseLatestRelease("""{"assets":[]}""") }
    }
}
