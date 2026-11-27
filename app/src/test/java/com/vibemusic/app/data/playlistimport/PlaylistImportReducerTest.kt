package com.vibemusic.app.data.playlistimport

import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImportReducerTest {

    private fun track(id: String, title: String = "Mámamelo", playable: Boolean = true) = Track(
        id = id,
        uri = if (playable) "https://api.audius.co/v1/tracks/$id/stream" else "",
        title = title,
        artist = "Conep",
        album = "Mámamelo",
        durationMs = 180_000,
        source = TrackSource.AUDIUS,
        downloadUrl = "https://api.audius.co/v1/tracks/$id/download"
    )

    private fun item(
        position: Int,
        title: String = "Mámamelo",
        status: ImportStatus = ImportStatus.AVAILABLE,
        track: Track? = track("audius:track:$position")
    ) = PlaylistImportItem(
        source = ImportedPlaylistTrack(
            position = position,
            title = title,
            artists = listOf("Conep"),
            album = "Mámamelo",
            durationMs = 180_000
        ),
        outcome = MatchOutcome(
            status = status,
            track = track,
            candidates = track?.let { matched ->
                listOf(
                    MatchCandidate(
                        track = matched,
                        score = 0.95,
                        titleScore = 1.0,
                        artistScore = 1.0,
                        versionScore = 1.0,
                        durationScore = 1.0,
                        albumScore = 1.0
                    )
                )
            }.orEmpty(),
            explanation = "Prueba",
            confidence = 0.95,
            autoSelected = status == ImportStatus.AVAILABLE
        )
    )

    @Test
    fun `marks repeated source entries as duplicates`() {
        val items = listOf(
            item(0, "Mámamelo"),
            item(1, "Mámamelo (Official Audio)")
        )
        val result = PlaylistImportReducer.markDuplicates(items)
        assertNull(result[0].duplicateOf)
        assertEquals(0, result[1].duplicateOf)
        assertFalse(result[1].selected)
        assertFalse(result[1].canAdd)
    }

    @Test
    fun `marks different versions as different songs`() {
        val items = listOf(
            item(0, "Mámamelo"),
            item(1, "Mámamelo Remix")
        )
        val result = PlaylistImportReducer.markDuplicates(items)
        assertNull(result[1].duplicateOf)
        assertTrue(result[1].canAdd)
    }

    @Test
    fun `marks two sources resolved to the same audio as duplicate`() {
        val shared = track("audius:track:same")
        val items = listOf(
            item(0, "Mámamelo", track = shared),
            item(1, "Mamelo", track = shared)
        )
        val result = PlaylistImportReducer.markDuplicates(items)
        assertEquals(0, result[1].duplicateOf)
    }

    @Test
    fun `toggle only affects addable rows`() {
        val items = listOf(
            item(0),
            item(1, status = ImportStatus.DOUBTFUL, track = track("audius:track:1"))
        )
        val result = PlaylistImportReducer.toggleSelection(items, 0)
        assertTrue(result[0].selected)
        val doubtful = PlaylistImportReducer.toggleSelection(items, 1)
        assertFalse(doubtful[1].selected)
    }

    @Test
    fun `select all skips doubtful and missing rows`() {
        val items = listOf(
            item(0),
            item(1, status = ImportStatus.DOUBTFUL, track = track("audius:track:1")),
            item(2, status = ImportStatus.NOT_FOUND, track = null)
        )
        val result = PlaylistImportReducer.setAllEligible(items, true)
        assertTrue(result[0].selected)
        assertFalse(result[1].selected)
        assertFalse(result[2].selected)
    }

    @Test
    fun `manual candidate selection confirms a doubtful row`() {
        val items = listOf(
            item(
                position = 0,
                status = ImportStatus.DOUBTFUL,
                track = track("audius:track:1")
            )
        )
        val result = PlaylistImportReducer.chooseCandidate(items, 0, "audius:track:1")
        assertEquals(ImportStatus.DOWNLOADABLE, result[0].outcome.status)
        assertTrue(result[0].confirmed)
        assertTrue(result[0].canAdd)
        assertTrue(result[0].selected)
    }

    @Test
    fun `manual selection cannot revive an unplayable candidate`() {
        val unplayable = track("audius:track:1", playable = false)
        val items = listOf(item(0, status = ImportStatus.DOUBTFUL, track = unplayable))
        val result = PlaylistImportReducer.chooseCandidate(items, 0, "audius:track:1")
        assertEquals(ImportStatus.UNAVAILABLE, result[0].outcome.status)
        assertFalse(result[0].canAdd)
        assertFalse(result[0].confirmed)
    }

    @Test
    fun `reclassify applies stricter thresholds without network`() {
        val candidate = MatchCandidate(
            track = track("audius:track:1"),
            score = 0.88,
            titleScore = 0.9,
            artistScore = 1.0,
            versionScore = 1.0,
            durationScore = 1.0,
            albumScore = 1.0
        )
        val items = listOf(
            item(0, status = ImportStatus.AVAILABLE).copy(
                outcome = MatchOutcome(
                    status = ImportStatus.AVAILABLE,
                    track = candidate.track,
                    candidates = listOf(candidate),
                    explanation = "",
                    confidence = 0.88,
                    autoSelected = true
                ),
                selected = true
            )
        )
        val strict = PlaylistImportReducer.reclassify(
            items,
            MatchSensitivity.STRICT.thresholds
        )
        assertEquals(ImportStatus.DOUBTFUL, strict[0].outcome.status)
        assertFalse(strict[0].selected)
        val wide = PlaylistImportReducer.reclassify(
            items,
            MatchSensitivity.WIDE.thresholds
        )
        assertTrue(wide[0].outcome.status in setOf(ImportStatus.AVAILABLE, ImportStatus.DOWNLOADABLE))
    }

    @Test
    fun `summary counts statuses and selection`() {
        val items = listOf(
            item(0).copy(selected = true),
            item(1, status = ImportStatus.DOUBTFUL, track = track("audius:track:1")),
            item(2, status = ImportStatus.NOT_FOUND, track = null)
        )
        val summary = PlaylistImportReducer.summary(items)
        assertEquals(3, summary.total)
        assertEquals(1, summary.selected)
        assertEquals(1, summary.byStatus[ImportStatus.AVAILABLE])
        assertEquals(1, summary.byStatus[ImportStatus.DOUBTFUL])
        assertEquals(1, summary.byStatus[ImportStatus.NOT_FOUND])
    }

    @Test
    fun `library track keeps original metadata and Audius audio rights`() {
        val matched = track("audius:track:1", title = "Mámamelo (Audius)")
        val item = item(0, "Mámamelo (Official Audio)", track = matched).copy(
            selected = true,
            source = ImportedPlaylistTrack(
                position = 0,
                title = "Mámamelo (Official Audio)",
                artists = listOf("Conep", "Invitado"),
                album = "Mámamelo álbum",
                durationMs = 181_500,
                isrc = "ES1234567890"
            )
        )
        val library = item.toLibraryTrack("https://i.scdn.co/image/cover")
        assertNotNull(library)
        assertEquals("Mámamelo (Official Audio)", library?.title)
        assertEquals("Conep, Invitado", library?.artist)
        assertEquals("Mámamelo álbum", library?.album)
        assertEquals(181_500L, library?.durationMs)
        assertEquals("https://api.audius.co/v1/tracks/audius:track:1/download", library?.downloadUrl)
        assertEquals("https://i.scdn.co/image/cover", library?.artworkUri)
        assertEquals("ES1234567890", library?.isrc)
    }

    @Test
    fun `partial keeps analysed rows`() {
        val partial = PlaylistImportReducer.partial(
            listOf(item(0), item(1)),
            total = 10,
            message = "Cancelado"
        )
        assertEquals(2, partial.items.size)
        assertEquals(10, partial.total)
        assertNotNull(partial.message)
    }
}
