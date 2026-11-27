package com.vibemusic.app.data.playlistimport

import com.vibemusic.app.data.model.Track

/**
 * Metadata read from Spotify or YouTube Music. It is deliberately independent
 * from Audius: the imported playlist describes what the user asked for, while
 * [MatchOutcome] describes the audio that VibeMusic actually found.
 */
data class ImportedPlaylistTrack(
    val position: Int,
    val title: String,
    val artists: List<String>,
    val album: String?,
    val durationMs: Long,
    val isrc: String? = null,
    val videoId: String? = null
) {
    val primaryArtist: String
        get() = artists.firstOrNull()?.trim().orEmpty()

    val artistLine: String
        get() = artists.map { it.trim() }.filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")

    val displayTitle: String
        get() = title.trim().ifBlank { "Canción sin título" }
}

data class ImportedPlaylist(
    val platform: PlaylistPlatform,
    val playlistId: String,
    val name: String,
    val owner: String,
    val description: String? = null,
    val artworkUrl: String? = null,
    val sourceUrl: String,
    val tracks: List<ImportedPlaylistTrack>
) {
    val trackCount: Int get() = tracks.size
}

/** Why a playlist row has the state it has. */
enum class ImportStatus(val label: String) {
    AVAILABLE("Disponible"),
    DOWNLOADABLE("Descargable"),
    LOCAL("Local"),
    DOUBTFUL("Coincidencia dudosa"),
    NOT_FOUND("No encontrado"),
    UNAVAILABLE("No disponible"),

    /** The catalogue could not be queried; this is not a "not found" result. */
    ERROR("Error de consulta")
}

/** One scored Audius or local candidate. */
data class MatchCandidate(
    val track: Track,
    val score: Double,
    val titleScore: Double,
    val artistScore: Double,
    val versionScore: Double,
    val durationScore: Double,
    val albumScore: Double,
    val reasons: List<String> = emptyList(),
    val isrcMatched: Boolean = false,
    val isrcConflict: Boolean = false,
    val autoEligible: Boolean = true
)

data class MatchOutcome(
    val status: ImportStatus,
    val track: Track? = null,
    val candidates: List<MatchCandidate> = emptyList(),
    val explanation: String,
    val confidence: Double = 0.0,
    val autoSelected: Boolean = false,
    val localCandidateId: String? = null
) {
    val hasUsableTrack: Boolean
        get() = track != null && status in setOf(
            ImportStatus.AVAILABLE,
            ImportStatus.DOWNLOADABLE,
            ImportStatus.LOCAL
        )
}

data class PlaylistImportItem(
    val source: ImportedPlaylistTrack,
    val outcome: MatchOutcome,
    val selected: Boolean = false,
    val confirmed: Boolean = false,
    /** Position of the first identical entry, when this row repeats it. */
    val duplicateOf: Int? = null
) {
    val canAdd: Boolean
        get() = when (outcome.status) {
            ImportStatus.AVAILABLE,
            ImportStatus.DOWNLOADABLE,
            ImportStatus.LOCAL -> outcome.track != null && duplicateOf == null

            ImportStatus.DOUBTFUL -> confirmed && outcome.track != null && duplicateOf == null
            else -> false
        }

    val statusLabel: String
        get() = outcome.status.label
}

enum class MatchSensitivity(val label: String, val description: String) {
    STRICT(
        label = "Estricta",
        description = "Solo coincidencias casi idénticas (título, artista, versión y duración)."
    ),
    BALANCED(
        label = "Equilibrada",
        description = "Acepta automáticamente coincidencias claras y pide confirmación en las dudosas."
    ),
    WIDE(
        label = "Amplia",
        description = "Explora más resultados; casi todo lo dudoso requiere confirmación."
    );

    val thresholds: MatchThresholds
        get() = when (this) {
            STRICT -> MatchThresholds(autoAccept = 0.91, review = 0.70, ambiguityGap = 0.04)
            BALANCED -> MatchThresholds(autoAccept = 0.86, review = 0.62, ambiguityGap = 0.05)
            WIDE -> MatchThresholds(autoAccept = 0.80, review = 0.55, ambiguityGap = 0.07)
        }
}

/**
 * Thresholds are deliberately data, not constants buried in the matcher, so a
 * future settings screen can tune precision without touching scoring code.
 */
data class MatchThresholds(
    val autoAccept: Double = 0.86,
    val review: Double = 0.62,
    val ambiguityGap: Double = 0.05
) {
    init {
        require(autoAccept in 0.0..1.0) { "autoAccept debe estar entre 0 y 1" }
        require(review in 0.0..1.0) { "review debe estar entre 0 y 1" }
        require(review <= autoAccept) { "review no puede superar autoAccept" }
    }
}

data class PlaylistImportSummary(
    val total: Int = 0,
    val selected: Int = 0,
    val byStatus: Map<ImportStatus, Int> = emptyMap()
) {
    val analysisComplete: Boolean get() = total > 0
}

/**
 * One confirmed row ready to be stored. [track] points at the playable audio
 * (Audius or a local file); [source] keeps what the original playlist said.
 */
data class ImportedPlaylistEntry(
    val source: ImportedPlaylistTrack,
    val track: Track,
    val confidence: Double
)

/**
 * Builds the library track: the original title, artist, album, duration and
 * ISRC are preserved, while the URI and download rights come from the matched
 * audio source. That is why an imported "Mámamelo Remix" never silently shows
 * the metadata of a different Audius recording.
 */
fun PlaylistImportItem.toLibraryTrack(playlistArtworkUrl: String? = null): Track? {
    val matched = outcome.track ?: return null
    if (!canAdd) return null
    return matched.copy(
        title = source.displayTitle,
        artist = source.artistLine.ifBlank { matched.artist },
        album = source.album?.trim().orEmpty().ifBlank { matched.album },
        durationMs = source.durationMs.takeIf { it > 0L } ?: matched.durationMs,
        artworkUri = matched.artworkUri ?: playlistArtworkUrl,
        isrc = matched.isrc ?: source.isrc
    )
}
