package com.vibemusic.app.data.playlistimport

import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMatchEngineTest {

    private fun source(
        title: String,
        artist: String = "Conep",
        album: String? = "Mámamelo",
        durationMs: Long = 180_000,
        isrc: String? = null
    ) = ImportedPlaylistTrack(
        position = 0,
        title = title,
        artists = listOf(artist),
        album = album,
        durationMs = durationMs,
        isrc = isrc
    )

    private fun track(
        id: String = "audius:track:1",
        title: String,
        artist: String = "Conep",
        album: String = "Mámamelo",
        durationMs: Long = 180_000,
        uri: String = "https://api.audius.co/v1/tracks/1/stream?app_name=VibeMusic",
        downloadUrl: String? = "https://api.audius.co/v1/tracks/1/download?app_name=VibeMusic",
        blocked: String? = null,
        isrc: String? = null
    ) = Track(
        id = id,
        uri = uri,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        source = TrackSource.AUDIUS,
        downloadUrl = downloadUrl,
        downloadBlockedReason = blocked,
        isrc = isrc
    )

    @Test
    fun `exact recording scores very high and is downloadable`() {
        val result = TrackMatchEngine.bestMatch(
            source("Mámamelo"),
            listOf(track(title = "Mámamelo")),
            MatchSensitivity.BALANCED.thresholds
        )
        assertEquals(ImportStatus.DOWNLOADABLE, result.status)
        assertTrue(result.confidence >= 0.99)
        assertTrue(result.autoSelected)
    }

    @Test
    fun `playable but not downloadable is available`() {
        val result = TrackMatchEngine.bestMatch(
            source("Mámamelo"),
            listOf(track(title = "Mámamelo", downloadUrl = null)),
            MatchSensitivity.BALANCED.thresholds
        )
        assertEquals(ImportStatus.AVAILABLE, result.status)
        assertTrue(result.explanation.contains("reproducción", ignoreCase = true))
    }

    @Test
    fun `remix is never replaced by the original recording`() {
        val result = TrackMatchEngine.score(
            source("Mámamelo Remix"),
            track(title = "Mámamelo")
        )
        assertNull(result)
    }

    @Test
    fun `original is never replaced by a remix`() {
        val result = TrackMatchEngine.score(
            source("Mámamelo"),
            track(title = "Mámamelo Remix")
        )
        assertNull(result)
    }

    @Test
    fun `different remixers are incompatible`() {
        val result = TrackMatchEngine.score(
            source("Mámamelo (Conep Remix)"),
            track(title = "Mámamelo (Other Remix)")
        )
        assertNull(result)
    }

    @Test
    fun `slowed version is not the original`() {
        val result = TrackMatchEngine.score(
            source("Mámamelo (Slowed)"),
            track(title = "Mámamelo")
        )
        assertNull(result)
    }

    @Test
    fun `official audio marker does not change the match`() {
        val candidate = TrackMatchEngine.score(
            source("Mámamelo"),
            track(title = "Mámamelo (Official Audio)")
        )
        assertNotNull(candidate)
        assertTrue(candidate!!.score >= 0.95)
    }

    @Test
    fun `incompatible duration is rejected`() {
        val result = TrackMatchEngine.score(
            source("Mámamelo", durationMs = 180_000),
            track(title = "Mámamelo", durationMs = 240_000)
        )
        assertNull(result)
    }

    @Test
    fun `different artist is rejected even with same title`() {
        val result = TrackMatchEngine.score(
            source("Mámamelo", artist = "Conep"),
            track(title = "Mámamelo", artist = "Otro Artista")
        )
        assertNull(result)
    }

    @Test
    fun `isrc match overrides a weaker title`() {
        val result = TrackMatchEngine.score(
            source("Mámamelo", isrc = "ES1234567890"),
            track(title = "Mamelo", isrc = "ES-123-456-7890")
        )
        assertNotNull(result)
        assertTrue(result!!.isrcMatched)
        assertTrue(result.score >= 0.99)
    }

    @Test
    fun `isrc conflict forces confirmation`() {
        val result = TrackMatchEngine.score(
            source("Mámamelo", isrc = "ES1234567890"),
            track(title = "Mámamelo", isrc = "US0000000001")
        )
        assertNotNull(result)
        assertTrue(result!!.isrcConflict)
        assertFalse(result.autoEligible)
    }

    @Test
    fun `ambiguous close candidates need confirmation`() {
        val top = MatchCandidate(
            track = track(id = "audius:track:1", title = "Mámamelo"),
            score = 0.90,
            titleScore = 1.0,
            artistScore = 1.0,
            versionScore = 1.0,
            durationScore = 1.0,
            albumScore = 1.0
        )
        val second = top.copy(
            track = track(id = "audius:track:2", title = "Mámamelo"),
            score = 0.88
        )
        val result = TrackMatchEngine.classify(
            source("Mámamelo"),
            listOf(top, second),
            MatchSensitivity.BALANCED.thresholds
        )
        assertEquals(ImportStatus.DOUBTFUL, result.status)
        assertTrue(result.explanation.contains("parecidas", ignoreCase = true))
    }

    @Test
    fun `unplayable candidate is unavailable`() {
        val result = TrackMatchEngine.bestMatch(
            source("Mámamelo"),
            listOf(track(title = "Mámamelo", uri = "", downloadUrl = null)),
            MatchSensitivity.BALANCED.thresholds
        )
        assertEquals(ImportStatus.UNAVAILABLE, result.status)
    }

    @Test
    fun `empty catalogue returns not found`() {
        val result = TrackMatchEngine.bestMatch(
            source("Mámamelo"),
            emptyList(),
            MatchSensitivity.BALANCED.thresholds
        )
        assertEquals(ImportStatus.NOT_FOUND, result.status)
        assertNull(result.track)
    }

    @Test
    fun `local file is reused without downloading`() {
        val local = track(
            id = "local:1",
            title = "Mámamelo",
            uri = "content://media/external/audio/media/1"
        ).copy(source = TrackSource.LOCAL, downloadUrl = null)
        val result = TrackMatchEngine.bestLocalMatch(
            source("Mámamelo"),
            listOf(local),
            MatchSensitivity.BALANCED.thresholds
        )
        assertNotNull(result)
        assertEquals(ImportStatus.LOCAL, result!!.status)
        assertEquals("local:1", result.track?.id)
    }

    @Test
    fun `dedupe key distinguishes remix slowed and original`() {
        val original = TrackMatchEngine.dedupeKey(source("Mámamelo"))
        val remix = TrackMatchEngine.dedupeKey(source("Mámamelo Remix"))
        val slowed = TrackMatchEngine.dedupeKey(source("Mámamelo (Slowed)"))
        assertTrue(original != remix)
        assertTrue(original != slowed)
        assertTrue(remix != slowed)
    }

    @Test
    fun `search title keeps version words and removes promotional noise`() {
        assertEquals(
            "mamamelo remix",
            TrackMatchEngine.searchTitle("Mámamelo Remix (Official Audio)")
        )
        assertEquals(
            "mamamelo slowed",
            TrackMatchEngine.searchTitle("Mámamelo (Slowed)")
        )
    }
}
