package com.vibemusic.app.data.recommend

import com.vibemusic.app.data.model.Track
import kotlin.math.ln
import kotlin.random.Random

/**
 * One anonymous playback observation used by the local recommender. It is a
 * plain value object so the engine can be unit tested without Room/Android.
 */
data class ListeningSignal(
    val trackId: String,
    val title: String,
    val artist: String,
    val genre: String?,
    val source: String,
    val artworkUri: String?,
    val durationMs: Long,
    val playedAt: Long,
    val skipped: Boolean,
    val completed: Boolean,
    val positionMs: Long
)

data class RecommendationSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val tracks: List<Track>
)

/**
 * Local, deterministic recommendation engine.
 *
 * No network calls, no analytics and no inferred sensitive traits: it only
 * ranks artists/genres by the user's own playback history, favourite tracks
 * and playlist contents. The daily mix changes once per day and is stable
 * within that day.
 */
object RecommendationEngine {

    private const val DAY_MS = 86_400_000L

    fun buildSections(
        signals: List<ListeningSignal>,
        favorites: List<Track>,
        catalog: List<Track>,
        playlistTracks: List<Track>,
        now: Long = System.currentTimeMillis(),
        limit: Int = 20
    ): List<RecommendationSection> {
        val playable = catalog
            .filter { it.uri.isNotBlank() }
            .distinctBy { it.id }
        if (playable.isEmpty()) return emptyList()

        val byArtist = artistScores(signals, now)
        val genreScores = genreScores(signals, playable)
        val playedIds = signals.filterNot { it.skipped }.map { it.trackId }.toSet()
        val playCounts = signals.filterNot { it.skipped }
            .groupingBy { it.trackId }
            .eachCount()

        val topArtists = byArtist.entries.sortedByDescending { it.value }.map { it.key }
        val topGenres = genreScores.entries.sortedByDescending { it.value }.map { it.key }

        val sections = mutableListOf<RecommendationSection>()

        // Daily mix: catalogue tracks from the most relevant artists/genres,
        // excluding the ones skipped most recently.
        val artistMatched = playable.filter { track ->
            topArtists.take(4).any { it.equals(track.artist, ignoreCase = true) } ||
                track.genre?.let { g -> topGenres.take(3).any { it.equals(g, true) } } == true
        }
        val pool = (artistMatched.ifEmpty { playable })
            .filterNot { track -> recentlySkipped(signals, track.id, now) }
            .distinctBy { it.id }
        val daily = if (pool.isEmpty()) emptyList() else {
            pool.shuffled(Random(now / DAY_MS)).take(limit)
        }
        if (daily.isNotEmpty()) {
            sections += RecommendationSection(
                id = "daily",
                title = "Tu mezcla diaria",
                subtitle = if (signals.isEmpty()) {
                    "Empieza a escuchar y esta mezcla se adaptará a ti"
                } else {
                    "Basada en lo que escuchas · se renueva cada día"
                },
                tracks = daily
            )
        }

        val mostPlayed = playable
            .filter { playCounts[it.id] != null }
            .sortedWith(
                compareByDescending<Track> { playCounts[it.id] ?: 0 }
                    .thenBy { it.title.lowercase() }
            )
            .take(limit)
        if (mostPlayed.size >= 2) {
            sections += RecommendationSection(
                id = "most_played",
                title = "Lo que más escuchas",
                subtitle = "Tus canciones con más reproducciones",
                tracks = mostPlayed
            )
        }

        val favoriteTracks = favorites
            .filter { it.uri.isNotBlank() }
            .distinctBy { it.id }
            .take(limit)
        if (favoriteTracks.isNotEmpty()) {
            sections += RecommendationSection(
                id = "favorites",
                title = "Tus favoritos",
                subtitle = "Lo que has marcado con el corazón",
                tracks = favoriteTracks
            )
        }

        val discoveries = playable
            .filterNot { it.id in playedIds }
            .sortedWith(
                compareByDescending<Track> { track ->
                    topGenres.indexOfFirst { it.equals(track.genre, true) }
                        .takeIf { it >= 0 }?.let { 100 - it } ?: 0
                }.thenBy { it.title.lowercase() }
            )
            .take(limit)
        if (discoveries.isNotEmpty()) {
            sections += RecommendationSection(
                id = "discoveries",
                title = "Descubrimientos",
                subtitle = "Canciones de tu biblioteca que aún no has escuchado",
                tracks = discoveries
            )
        }

        val artistRows = playable
            .filter { track -> topArtists.take(4).any { it.equals(track.artist, true) } }
            .distinctBy { it.id }
            .take(limit)
        if (artistRows.isNotEmpty()) {
            sections += RecommendationSection(
                id = "artists",
                title = "Artistas que repites",
                subtitle = topArtists.take(3).joinToString(" · "),
                tracks = artistRows
            )
        }

        if (topGenres.isNotEmpty()) {
            val related = playable
                .filter { track ->
                    track.genre?.let { genre ->
                        topGenres.any { it.equals(genre, true) }
                    } == true
                }
                .filterNot { it.id in artistRows.map { t -> t.id }.toSet() }
                .distinctBy { it.id }
                .take(limit)
            if (related.size >= 2) {
                sections += RecommendationSection(
                    id = "because",
                    title = "Porque te gusta ${topArtists.firstOrNull() ?: topGenres.first()}",
                    subtitle = "Más música de tu estilo",
                    tracks = related
                )
            }
        }

        val newSounds = playable
            .filter { track ->
                track.genre?.let { genre -> topGenres.none { it.equals(genre, true) } } != false
            }
            .filterNot { it.id in discoveries.map { d -> d.id }.toSet() }
            .distinctBy { it.id }
            .shuffled(Random(now / DAY_MS + 7))
            .take(limit)
        if (newSounds.size >= 2) {
            sections += RecommendationSection(
                id = "new_sounds",
                title = "Nuevos sonidos",
                subtitle = "Estilos que todavía no dominan tu historial",
                tracks = newSounds
            )
        }

        val fromPlaylists = playlistTracks
            .filter { it.uri.isNotBlank() }
            .distinctBy { it.id }
            .take(limit)
        if (fromPlaylists.size >= 2) {
            sections += RecommendationSection(
                id = "playlists",
                title = "Desde tus listas",
                subtitle = "Canciones que guardaste para más tarde",
                tracks = fromPlaylists
            )
        }

        return sections.distinctBy { it.id }
    }

    /** Artist -> relevance score. Skips and old plays weigh less. */
    fun artistScores(
        signals: List<ListeningSignal>,
        now: Long
    ): Map<String, Double> {
        val scores = LinkedHashMap<String, Double>()
        signals.forEach { signal ->
            val artist = signal.artist.trim()
            if (artist.isBlank()) return@forEach
            val ageDays = ((now - signal.playedAt).coerceAtLeast(0L) / DAY_MS).toDouble()
            val recency = 1.0 / (1.0 + ageDays / 14.0)
            val completion = if (signal.durationMs > 0) {
                (signal.positionMs.toDouble() / signal.durationMs).coerceIn(0.0, 1.0)
            } else if (signal.completed) 1.0 else 0.3
            val value = when {
                signal.skipped -> -1.5
                signal.completed -> 2.0
                else -> 1.0
            } + completion
            scores[artist] = (scores[artist] ?: 0.0) + value * recency
        }
        return scores.filterValues { it > 0.0 }
    }

    /** Genre relevance derived from history and from the catalogue metadata. */
    fun genreScores(
        signals: List<ListeningSignal>,
        catalog: List<Track>
    ): Map<String, Double> {
        val genresByTrack = catalog
            .mapNotNull { track -> track.genre?.trim()?.takeIf { it.isNotBlank() }?.let { track.id to it } }
            .toMap()
        val scores = LinkedHashMap<String, Double>()
        signals.forEach { signal ->
            val genre = signal.genre?.trim()?.takeIf { it.isNotBlank() }
                ?: genresByTrack[signal.trackId]
                ?: return@forEach
            val weight = if (signal.skipped) -0.6 else if (signal.completed) 2.0 else 1.0
            scores[genre] = (scores[genre] ?: 0.0) + weight
        }
        return scores.filterValues { it > 0.0 }
    }

    private fun recentlySkipped(
        signals: List<ListeningSignal>,
        trackId: String,
        now: Long
    ): Boolean = signals.any {
        it.trackId == trackId &&
            it.skipped &&
            now - it.playedAt < 2 * DAY_MS
    }

    /** Human-readable confidence for a section, useful in tests/diagnostics. */
    fun strength(signals: List<ListeningSignal>): Double {
        if (signals.isEmpty()) return 0.0
        val total = signals.size.toDouble()
        return (ln(1.0 + total) / ln(11.0)).coerceIn(0.0, 1.0)
    }
}
