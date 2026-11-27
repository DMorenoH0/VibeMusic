package com.vibemusic.app.data.playlistimport

/**
 * Deterministic transformations over an analysis result. Keeping them pure
 * makes selection, duplicate detection, threshold changes and interrupted
 * imports testable without Android, Room or network.
 */
object PlaylistImportReducer {

    fun markDuplicates(items: List<PlaylistImportItem>): List<PlaylistImportItem> {
        val firstBySource = HashMap<String, Int>()
        val firstByTrack = HashMap<String, Int>()
        return items.map { item ->
            val sourceKey = TrackMatchEngine.dedupeKey(item.source)
            val usableTrack = item.outcome.track
                ?.takeIf { item.outcome.status != ImportStatus.NOT_FOUND }
            val trackKey = usableTrack?.id
            val duplicateOf = firstBySource[sourceKey]
                ?: trackKey?.let { firstByTrack[it] }
            if (duplicateOf == null) {
                firstBySource[sourceKey] = item.source.position
                if (trackKey != null) firstByTrack[trackKey] = item.source.position
                item.copy(duplicateOf = null)
            } else {
                item.copy(
                    duplicateOf = duplicateOf,
                    selected = false,
                    outcome = item.outcome.copy(
                        autoSelected = false,
                        explanation = item.outcome.explanation +
                            " · Repite la canción de la posición $duplicateOf."
                    )
                )
            }
        }
    }

    /**
     * Re-applies a new sensitivity without repeating network requests. The
     * candidate scores do not depend on thresholds; only classification does.
     */
    fun reclassify(
        items: List<PlaylistImportItem>,
        thresholds: MatchThresholds
    ): List<PlaylistImportItem> {
        val recalculated = items.map { item ->
            if (item.outcome.status == ImportStatus.ERROR) {
                item
            } else {
                val outcome = TrackMatchEngine.classify(
                    source = item.source,
                    candidates = item.outcome.candidates,
                    thresholds = thresholds,
                    localCandidateId = item.outcome.localCandidateId
                )
                val updated = item.copy(
                    outcome = outcome,
                    confirmed = false,
                    selected = outcome.autoSelected
                )
                if (item.selected && updated.canAdd) updated.copy(selected = true) else updated
            }
        }
        return markDuplicates(recalculated)
    }

    fun toggleSelection(
        items: List<PlaylistImportItem>,
        position: Int
    ): List<PlaylistImportItem> = items.map { item ->
        if (item.source.position != position || !item.canAdd) {
            item
        } else {
            item.copy(selected = !item.selected)
        }
    }

    fun setAllEligible(
        items: List<PlaylistImportItem>,
        selected: Boolean
    ): List<PlaylistImportItem> = items.map { item ->
        if (item.canAdd) item.copy(selected = selected) else item
    }

    /**
     * Confirms one explicit candidate for a doubtful row. This is the only way
     * a doubtful match can be added: the user must pick a concrete audio track.
     */
    fun chooseCandidate(
        items: List<PlaylistImportItem>,
        position: Int,
        trackId: String
    ): List<PlaylistImportItem> = items.map { item ->
        if (item.source.position != position) return@map item
        val candidate = item.outcome.candidates.firstOrNull { it.track.id == trackId }
            ?: return@map item
        val playable = candidate.track.uri.isNotBlank()
        val downloadable = playable &&
            !candidate.track.downloadUrl.isNullOrBlank() &&
            candidate.track.downloadBlockedReason.isNullOrBlank()
        val status = when {
            !playable -> ImportStatus.UNAVAILABLE
            downloadable -> ImportStatus.DOWNLOADABLE
            else -> ImportStatus.AVAILABLE
        }
        item.copy(
            outcome = item.outcome.copy(
                status = status,
                track = candidate.track,
                confidence = candidate.score,
                autoSelected = false,
                explanation = "Coincidencia elegida manualmente. " +
                    "Título ${(candidate.titleScore * 100).toInt()}%, " +
                    "artista ${(candidate.artistScore * 100).toInt()}%."
            ),
            confirmed = playable,
            selected = playable,
            duplicateOf = null
        )
    }

    fun summary(items: List<PlaylistImportItem>): PlaylistImportSummary = PlaylistImportSummary(
        total = items.size,
        selected = items.count { it.selected },
        byStatus = items.groupingBy { it.outcome.status }.eachCount()
    )

    /** Keeps the already-analysed rows when an import is cancelled mid-way. */
    fun partial(
        analysed: List<PlaylistImportItem>,
        total: Int,
        message: String
    ): PlaylistImportPartial = PlaylistImportPartial(
        items = markDuplicates(analysed),
        total = total,
        message = message
    )
}

data class PlaylistImportPartial(
    val items: List<PlaylistImportItem>,
    val total: Int,
    val message: String
)
