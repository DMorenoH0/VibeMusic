package com.vibemusic.app.data.playlistimport

import com.vibemusic.app.data.audius.AudiusService
import com.vibemusic.app.data.download.SongDownloadResolver
import com.vibemusic.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Finds the audio that best represents one imported playlist entry.
 *
 * The catalogue is an interface so the matching flow can be tested without
 * network access. The production implementation only queries Audius, the
 * provider VibeMusic already uses for full tracks and authorised downloads.
 */
interface TrackCatalog {
    suspend fun findCandidates(source: ImportedPlaylistTrack): List<Track>
}

class AudiusTrackCatalog(
    private val service: AudiusService,
    private val resultLimit: Int = 24
) : TrackCatalog {

    override suspend fun findCandidates(source: ImportedPlaylistTrack): List<Track> {
        val queries = buildList {
            val title = TrackMatchEngine.searchTitle(source.title)
            val artist = source.primaryArtist
            if (artist.isNotBlank()) add("$title $artist")
            val version = TrackMatchEngine.versionProfile(source.title)
            if (version.remix && version.remixerTokens.isNotEmpty()) {
                add("$title $artist ${version.remixerTokens.joinToString(" ")}")
            }
            add(title)
        }.map { it.trim() }.filter { it.length >= 2 }.distinct()

        val found = LinkedHashMap<String, Track>()
        for (query in queries) {
            if (found.size >= resultLimit) break
            val results = try {
                service.searchTracks(query, resultLimit)
            } catch (error: Exception) {
                if (found.isEmpty()) throw error else break
            }
            results.asSequence()
                .map { it.toTrack() }
                .forEach { track -> found.putIfAbsent(track.id, track) }
            // A good first query already fills the candidate set; the extra
            // queries only run when the first one returned too little.
            if (found.size >= 8) break
        }
        return found.values.take(resultLimit)
    }
}

class PlaylistImportAnalyzer(
    private val catalog: TrackCatalog,
    private val maxParallelSearches: Int = 4
) {

    /**
     * Analyses every entry, preserving the playlist order in the returned list.
     * A single failed catalogue query becomes an [ImportStatus.ERROR] row, not
     * a failed import; partial results stay usable when the user cancels.
     */
    suspend fun analyze(
        playlist: ImportedPlaylist,
        sensitivity: MatchSensitivity = MatchSensitivity.BALANCED,
        localTracks: List<Track> = emptyList(),
        downloadedTracks: List<Track> = emptyList(),
        onProgress: (completed: Int, total: Int, item: PlaylistImportItem) -> Unit = { _, _, _ -> }
    ): List<PlaylistImportItem> = coroutineScope {
        val thresholds = sensitivity.thresholds
        val matchPool = (downloadedTracks + localTracks).distinctBy { it.id }
        val offline = matchPool.filter { it.uri.isNotBlank() }
        val offlineIds = offline.associateBy { it.id }
        val offlineIdentities = offline.associateBy { SongDownloadResolver.identity(it) }
        val results = arrayOfNulls<PlaylistImportItem>(playlist.tracks.size)
        val completed = AtomicInteger(0)
        val permits = Semaphore(maxParallelSearches.coerceAtLeast(1))

        playlist.tracks.mapIndexed { index, source ->
            async(Dispatchers.IO) {
                permits.withPermit {
                    val outcome = analyseOne(
                        source = source,
                        thresholds = thresholds,
                        localTracks = matchPool,
                        offlineIds = offlineIds,
                        offlineIdentities = offlineIdentities
                    )
                    val item = PlaylistImportItem(source = source, outcome = outcome)
                    results[index] = item
                    val done = completed.incrementAndGet()
                    onProgress(done, playlist.tracks.size, item)
                }
            }
        }.awaitAll()

        val ordered = results.filterNotNull()
        PlaylistImportReducer.markDuplicates(ordered).map { item ->
            if (item.canAdd && item.outcome.autoSelected) item.copy(selected = true) else item
        }
    }

    private suspend fun analyseOne(
        source: ImportedPlaylistTrack,
        thresholds: MatchThresholds,
        localTracks: List<Track>,
        offlineIds: Map<String, Track>,
        offlineIdentities: Map<String, Track>
    ): MatchOutcome {
        val local = TrackMatchEngine.bestLocalMatch(source, localTracks, thresholds)
        if (local != null) return local

        val candidates = try {
            catalog.findCandidates(source)
        } catch (e: Exception) {
            return MatchOutcome(
                status = ImportStatus.ERROR,
                explanation = e.message?.takeIf { it.isNotBlank() }
                    ?: "No se pudo consultar el catálogo para esta canción.",
                autoSelected = false
            )
        }

        val scored = TrackMatchEngine.scoreAll(source, candidates)
        val outcome = TrackMatchEngine.classify(source, scored, thresholds)
        val matched = outcome.track ?: return outcome

        // The same Audius recording may already be downloaded under its own
        // metadata. Reuse the offline file instead of creating a duplicate.
        val offline = offlineIds[matched.id]
            ?: offlineIdentities[SongDownloadResolver.identity(matched)]
        if (offline != null) {
            return outcome.copy(
                status = ImportStatus.LOCAL,
                track = offline,
                confidence = maxOf(outcome.confidence, 0.99),
                autoSelected = true,
                explanation = "Esta canción ya está en tu dispositivo; se reutiliza " +
                    "el archivo sin descargarla otra vez."
            )
        }
        return outcome
    }
}
