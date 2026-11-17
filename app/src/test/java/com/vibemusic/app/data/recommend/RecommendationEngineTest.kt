package com.vibemusic.app.data.recommend

import com.vibemusic.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {

    private val day = 86_400_000L
    private val now = 1_700_000_000_000L

    private fun track(
        id: String,
        artist: String,
        genre: String? = null,
        title: String = "Song $id"
    ) = Track(
        id = id,
        uri = "file:///music/$id.mp3",
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 180_000L,
        genre = genre
    )

    private fun signal(
        track: Track,
        playedAt: Long = now,
        skipped: Boolean = false,
        completed: Boolean = true,
        positionMs: Long = track.durationMs
    ) = ListeningSignal(
        trackId = track.id,
        title = track.title,
        artist = track.artist,
        genre = track.genre,
        source = "LOCAL",
        artworkUri = null,
        durationMs = track.durationMs,
        playedAt = playedAt,
        skipped = skipped,
        completed = completed,
        positionMs = positionMs
    )

    @Test
    fun `empty catalogue produces no sections`() {
        val sections = RecommendationEngine.buildSections(
            signals = emptyList(),
            favorites = emptyList(),
            catalog = emptyList(),
            playlistTracks = emptyList(),
            now = now
        )
        assertTrue(sections.isEmpty())
    }

    @Test
    fun `daily mix is stable within the same day and changes the next day`() {
        val catalog = (1..40).map { track("t$it", "Artist ${it % 7}", "Genre ${it % 5}") }
        val signals = catalog.take(4).map { signal(it) }

        val first = RecommendationEngine.buildSections(signals, emptyList(), catalog, emptyList(), now)
        val second = RecommendationEngine.buildSections(signals, emptyList(), catalog, emptyList(), now)
        val nextDay = RecommendationEngine.buildSections(
            signals,
            emptyList(),
            catalog,
            emptyList(),
            now + day
        )

        val firstDaily = first.first { it.id == "daily" }.tracks.map { it.id }
        val secondDaily = second.first { it.id == "daily" }.tracks.map { it.id }
        val nextDaily = nextDay.first { it.id == "daily" }.tracks.map { it.id }

        assertEquals(firstDaily, secondDaily)
        assertFalse(
            "The daily mix should be renewed on a different day",
            firstDaily == nextDaily
        )
    }

    @Test
    fun `recently skipped track is excluded from the daily mix`() {
        val catalog = (1..10).map { track("t$it", "Artist") }
        val skipped = catalog.first()
        val signals = catalog.drop(1).map { signal(it) } + signal(
            skipped,
            playedAt = now - 60_000L,
            skipped = true,
            completed = false,
            positionMs = 4_000L
        )

        val daily = RecommendationEngine
            .buildSections(signals, emptyList(), catalog, emptyList(), now)
            .first { it.id == "daily" }
            .tracks
            .map { it.id }

        assertFalse(daily.contains(skipped.id))
    }

    @Test
    fun `most played orders by number of completed plays`() {
        val a = track("a", "Artist A", "Pop")
        val b = track("b", "Artist B", "Rock")
        val catalog = listOf(a, b) + track("c", "Artist C", "Jazz")
        val signals = listOf(signal(a), signal(a), signal(a), signal(b))

        val mostPlayed = RecommendationEngine
            .buildSections(signals, emptyList(), catalog, emptyList(), now)
            .first { it.id == "most_played" }
            .tracks

        assertEquals(listOf("a", "b"), mostPlayed.map { it.id })
    }

    @Test
    fun `favorites and playlist contents appear in their own sections`() {
        val favorite = track("f", "Artist F", "Pop")
        val playlistTrack = track("p", "Artist P", "Rock")
        val secondPlaylistTrack = track("p2", "Artist Q", "Jazz")
        val catalog = listOf(favorite, playlistTrack, secondPlaylistTrack)

        val sections = RecommendationEngine.buildSections(
            signals = emptyList(),
            favorites = listOf(favorite),
            catalog = catalog,
            playlistTracks = listOf(playlistTrack, secondPlaylistTrack, playlistTrack),
            now = now
        )

        assertEquals(listOf("f"), sections.first { it.id == "favorites" }.tracks.map { it.id })
        assertEquals(
            listOf("p", "p2"),
            sections.first { it.id == "playlists" }.tracks.map { it.id }
        )
    }

    @Test
    fun `discoveries only contain tracks that were never played`() {
        val played = track("played", "Artist A", "Pop")
        val unplayed = track("new", "Artist B", "Pop")
        val catalog = listOf(played, unplayed)

        val discoveries = RecommendationEngine
            .buildSections(listOf(signal(played)), emptyList(), catalog, emptyList(), now)
            .first { it.id == "discoveries" }
            .tracks
            .map { it.id }

        assertEquals(listOf("new"), discoveries)
    }

    @Test
    fun `artist scores ignore skipped-only artists and reward completions`() {
        val liked = track("l", "Liked Artist")
        val skipped = track("s", "Skipped Artist")
        val scores = RecommendationEngine.artistScores(
            listOf(
                signal(liked),
                signal(liked),
                signal(skipped, skipped = true, completed = false, positionMs = 2_000L)
            ),
            now
        )

        assertTrue(scores.getValue("Liked Artist") > 0.0)
        assertFalse(scores.containsKey("Skipped Artist"))
    }

    @Test
    fun `genre scores fall back to catalogue metadata when the signal has no genre`() {
        val tagged = track("g", "Artist", "Electronic")
        val scores = RecommendationEngine.genreScores(
            listOf(signal(tagged).copy(genre = null)),
            listOf(tagged)
        )
        assertEquals(2.0, scores.getValue("Electronic"), 0.001)
    }

    @Test
    fun `strength grows with the number of signals and is bounded`() {
        assertEquals(0.0, RecommendationEngine.strength(emptyList()), 0.0)
        val few = RecommendationEngine.strength((1..2).map { signal(track("s$it", "A")) })
        val many = RecommendationEngine.strength((1..10).map { signal(track("s$it", "A")) })
        assertTrue(many > few)
        assertTrue(many <= 1.0)
    }
}
