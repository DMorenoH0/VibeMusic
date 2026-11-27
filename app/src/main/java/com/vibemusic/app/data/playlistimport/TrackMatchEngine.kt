package com.vibemusic.app.data.playlistimport

import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * Pure, deterministic matching engine. No network, no Android APIs: every rule
 * below can be unit-tested with real-world title/artist/duration examples.
 *
 * The central rule is that a close title is never enough. Remix/version markers
 * are compared explicitly and conflicting versions are rejected even when the
 * title is identical.
 */
object TrackMatchEngine {

    private const val DURATION_UNKNOWN = 0.55
    private const val ALBUM_UNKNOWN = 0.6
    private const val ARTIST_UNKNOWN = 0.6

    private val bracketRegex = Regex("\\(([^)]*)\\)|\\[([^\\]]*)]")
    private val featuringRegex = Regex("\\b(?:feat|ft|featuring)\\b.*$", RegexOption.IGNORE_CASE)

    private val noiseTokens = setOf(
        "official", "audio", "video", "videoclip", "clip", "lyric", "lyrics",
        "hd", "hq", "explicit", "clean", "visualizer", "premiere", "music",
        "mv", "m", "v", "topic", "latino", "letra"
    )

    private val versionTokens = setOf(
        "remix", "remixed", "rmx", "bootleg", "flip", "vip", "mashup",
        "slowed", "reverb", "speed", "sped", "nightcore", "instrumental",
        "karaoke", "acapella", "capella", "live", "concert", "cover",
        "tribute", "extended", "acoustic", "unplugged", "remaster",
        "remastered", "remasterizado", "radio", "edit", "version", "demo",
        "mix", "dub", "club", "original"
    )

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    fun normalize(raw: String): String {
        val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFD)
        val withoutMarks = decomposed.replace(Regex("\\p{M}+"), "")
        return withoutMarks
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun baseTitle(raw: String): String {
        var text = bracketRegex.replace(raw, " ")
        text = featuringRegex.replace(text, " ")
        text = text.replace(Regex("\\s[-–—]\\s*(?:official|audio|video|lyrics?|hd|hq)\\b.*$", RegexOption.IGNORE_CASE), " ")
        val tokens = normalize(text).split(' ')
            .filter { it.isNotBlank() && it !in noiseTokens && it !in versionTokens }
        return tokens.joinToString(" ")
    }

    /**
     * Query text for a catalogue search. Unlike [baseTitle] it keeps version
     * words ("remix", "slowed") because they help Audius return the right
     * recording, while promotional noise ("official audio", "video") is
     * removed so it does not waste the search.
     */
    fun searchTitle(raw: String): String {
        val withoutNoiseBrackets = bracketRegex.replace(raw) { match ->
            val inner = match.groupValues[1].ifBlank { match.groupValues[2] }
            val tokens = normalize(inner).split(' ').filter { it.isNotBlank() }
            if (tokens.isNotEmpty() && tokens.all { it in noiseTokens }) " " else " $inner "
        }
        val text = featuringRegex.replace(withoutNoiseBrackets, " ")
        val tokens = normalize(text).split(' ')
            .filter { it.isNotBlank() && it !in noiseTokens }
        return tokens.joinToString(" ").ifBlank { baseTitle(raw) }
    }

    fun versionProfile(raw: String): VersionProfile {
        val text = normalize(bracketRegex.replace(raw, { match ->
            val inner = match.groupValues[1].ifBlank { match.groupValues[2] }
            " $inner "
        }))
        fun has(vararg words: String): Boolean = words.any { word ->
            Regex("(?:^|\\s)${Regex.escape(word)}(?:\\s|$)").containsMatchIn(text)
        }

        val remix = has("remix", "remixed", "rmx", "bootleg", "flip", "vip", "mashup")
        val remixerTokens = if (remix) remixerTokens(raw) else emptyList()
        return VersionProfile(
            remix = remix,
            slowed = has("slowed", "reverb"),
            spedUp = has("sped", "speed", "nightcore"),
            instrumental = has("instrumental", "karaoke", "acapella", "capella"),
            live = has("live", "concert", "session"),
            cover = has("cover", "tribute"),
            extended = has("extended", "dub", "club"),
            acoustic = has("acoustic", "unplugged"),
            remaster = has("remaster", "remastered", "remasterizado"),
            radioEdit = has("radio"),
            demo = has("demo"),
            remixerTokens = remixerTokens
        )
    }

    fun dedupeKey(source: ImportedPlaylistTrack): String {
        val profile = versionProfile(source.title)
        return buildString {
            append(baseTitle(source.title))
            append('|')
            append(profile.signature)
            append('|')
            append(normalize(source.primaryArtist))
        }
    }

    /**
     * Scores one candidate. Returns null when a hard rule rejects it, so an
     * unrelated song can never win only because it is the only result.
     */
    fun score(
        source: ImportedPlaylistTrack,
        candidate: Track
    ): MatchCandidate? {
        val sourceTitle = baseTitle(source.title)
        val candidateTitle = baseTitle(candidate.title)
        if (sourceTitle.isBlank() || candidateTitle.isBlank()) return null

        val titleScore = titleSimilarity(sourceTitle, candidateTitle)
        if (titleScore < 0.58) return null

        val sourceVersion = versionProfile(source.title)
        val candidateVersion = versionProfile(candidate.title)
        val versionResult = versionScore(sourceVersion, candidateVersion)
        if (versionResult.hardReject) return null

        val artistScore = artistSimilarity(source, candidate.artist, sourceVersion)
        if (source.primaryArtist.isNotBlank() && artistScore < 0.45) return null

        val durationScore = durationSimilarity(source.durationMs, candidate.durationMs)
        if (durationScore < 0.0) return null

        val albumScore = albumSimilarity(source.album, candidate.album)
        val isrcEqual = isrcsEqual(source.isrc, candidateIsrc(candidate))
        val isrcConflict = source.isrc.isNullOrBlank().not() &&
            candidateIsrc(candidate).isNullOrBlank().not() &&
            !isrcEqual

        var total =
            0.34 * titleScore +
                0.26 * artistScore +
                0.18 * versionResult.score +
                0.16 * durationScore +
                0.06 * albumScore
        if (isrcEqual) total = maxOf(total, 0.99)
        if (isrcConflict) total *= 0.85

        val reasons = buildList {
            add("Título ${percent(titleScore)}")
            add("Artista ${percent(artistScore)}")
            add(versionResult.reason)
            if (source.durationMs > 0 && candidate.durationMs > 0) {
                add("Duración ${percent(durationScore)}")
            }
            if (isrcEqual) add("ISRC coincide")
            if (isrcConflict) add("ISRC distinto: requiere confirmación")
        }

        return MatchCandidate(
            track = candidate,
            score = total.coerceIn(0.0, 1.0),
            titleScore = titleScore,
            artistScore = artistScore,
            versionScore = versionResult.score,
            durationScore = durationScore,
            albumScore = albumScore,
            reasons = reasons,
            isrcMatched = isrcEqual,
            isrcConflict = isrcConflict,
            autoEligible = !isrcConflict
        )
    }

    fun scoreAll(
        source: ImportedPlaylistTrack,
        candidates: List<Track>
    ): List<MatchCandidate> = candidates
        .asSequence()
        .mapNotNull { score(source, it) }
        .sortedByDescending { it.score }
        .toList()

    fun classify(
        source: ImportedPlaylistTrack,
        candidates: List<MatchCandidate>,
        thresholds: MatchThresholds,
        localCandidateId: String? = null
    ): MatchOutcome {
        val sorted = candidates.sortedByDescending { it.score }
        if (localCandidateId != null) {
            val local = sorted.firstOrNull { it.track.id == localCandidateId }
            if (local != null && local.score >= thresholds.autoAccept) {
                return MatchOutcome(
                    status = ImportStatus.LOCAL,
                    track = local.track,
                    candidates = sorted,
                    explanation = "Este archivo ya está en tu dispositivo; se reutiliza sin duplicarlo.",
                    confidence = local.score,
                    autoSelected = true,
                    localCandidateId = local.track.id
                )
            }
        }

        val top = sorted.firstOrNull()
            ?: return MatchOutcome(
                status = ImportStatus.NOT_FOUND,
                explanation = "Audius no devolvió ninguna coincidencia utilizable.",
                autoSelected = false
            )

        val second = sorted.getOrNull(1)
        val ambiguous = second != null &&
            second.track.id != top.track.id &&
            second.score >= thresholds.review &&
            top.score - second.score < thresholds.ambiguityGap

        val playable = top.track.uri.isNotBlank()
        val downloadable = playable &&
            !top.track.downloadUrl.isNullOrBlank() &&
            top.track.downloadBlockedReason.isNullOrBlank()

        val explanation = when {
            !playable -> "La coincidencia existe pero Audius restringe su reproducción completa."
            ambiguous -> "Hay varias coincidencias posibles con puntuaciones muy parecidas; confirma la correcta."
            top.score >= thresholds.autoAccept && top.autoEligible ->
                if (downloadable) {
                    "Coincidencia de alta confianza y descarga autorizada por Audius."
                } else {
                    "Coincidencia de alta confianza; reproducción completa disponible."
                }
            top.score >= thresholds.review ->
                "Coincidencia posible, pero no alcanza el umbral de alta confianza."
            else ->
                "Ninguna coincidencia alcanza el umbral mínimo de fiabilidad."
        }

        val status = when {
            !playable -> ImportStatus.UNAVAILABLE
            top.score >= thresholds.autoAccept && !ambiguous && top.autoEligible ->
                if (downloadable) ImportStatus.DOWNLOADABLE else ImportStatus.AVAILABLE
            top.score >= thresholds.review -> ImportStatus.DOUBTFUL
            else -> ImportStatus.NOT_FOUND
        }

        return MatchOutcome(
            status = status,
            track = if (status == ImportStatus.NOT_FOUND) null else top.track,
            candidates = sorted,
            explanation = explanation,
            confidence = top.score,
            autoSelected = status == ImportStatus.AVAILABLE ||
                status == ImportStatus.DOWNLOADABLE,
            localCandidateId = null
        )
    }

    fun bestMatch(
        source: ImportedPlaylistTrack,
        candidates: List<Track>,
        thresholds: MatchThresholds
    ): MatchOutcome = classify(source, scoreAll(source, candidates), thresholds)

    /** Strong local hit by title, artist and duration. Never guessed from album. */
    fun bestLocalMatch(
        source: ImportedPlaylistTrack,
        localTracks: List<Track>,
        thresholds: MatchThresholds
    ): MatchOutcome? {
        if (localTracks.isEmpty()) return null
        val candidates = scoreAll(
            source,
            localTracks.filter {
                it.source == TrackSource.LOCAL ||
                    it.source == TrackSource.DOWNLOAD ||
                    it.source == TrackSource.IMPORTED ||
                    it.source == TrackSource.DEMO
            }
        )
        val top = candidates.firstOrNull() ?: return null
        if (top.score < thresholds.autoAccept) return null
        return classify(source, candidates, thresholds, localCandidateId = top.track.id)
    }

    // ---------------------------------------------------------------------
    // Individual comparisons
    // ---------------------------------------------------------------------

    fun titleSimilarity(normalizedSource: String, normalizedCandidate: String): Double {
        if (normalizedSource.isBlank() || normalizedCandidate.isBlank()) return 0.0
        if (normalizedSource == normalizedCandidate) return 1.0
        val a = normalizedSource.split(' ').filter { it.isNotBlank() }.toSet()
        val b = normalizedCandidate.split(' ').filter { it.isNotBlank() }.toSet()
        val jaccard = if (a.isEmpty() || b.isEmpty()) 0.0 else
            a.intersect(b).size.toDouble() / a.union(b).size
        val chars = charSimilarity(normalizedSource, normalizedCandidate)
        var score = 0.55 * jaccard + 0.45 * chars
        if ((normalizedSource.contains(normalizedCandidate) ||
                normalizedCandidate.contains(normalizedSource)) &&
            minOf(normalizedSource.length, normalizedCandidate.length) >= 4
        ) {
            score = maxOf(score, 0.84)
        }
        return score.coerceIn(0.0, 1.0)
    }

    private data class VersionResult(
        val score: Double,
        val hardReject: Boolean,
        val reason: String
    )

    private fun versionScore(source: VersionProfile, candidate: VersionProfile): VersionResult {
        val hardFlags = listOf(
            "remix" to (source.remix to candidate.remix),
            "versión ralentizada" to (source.slowed to candidate.slowed),
            "versión acelerada" to (source.spedUp to candidate.spedUp),
            "instrumental" to (source.instrumental to candidate.instrumental),
            "directo" to (source.live to candidate.live),
            "cover" to (source.cover to candidate.cover),
            "versión extendida" to (source.extended to candidate.extended),
            "acústico" to (source.acoustic to candidate.acoustic),
            "demo" to (source.demo to candidate.demo)
        )
        hardFlags.forEach { (label, pair) ->
            if (pair.first != pair.second) {
                return VersionResult(
                    score = 0.0,
                    hardReject = true,
                    reason = "Versión incompatible: $label"
                )
            }
        }

        var score = 1.0
        val notes = mutableListOf<String>()
        if (source.remix && candidate.remix) {
            val a = source.remixerTokens.toSet()
            val b = candidate.remixerTokens.toSet()
            when {
                a.isEmpty() && b.isEmpty() -> Unit
                a.isEmpty() || b.isEmpty() -> {
                    score *= 0.82
                    notes += "Remezclador no confirmado"
                }
                else -> {
                    val jaccard = a.intersect(b).size.toDouble() / a.union(b).size
                    if (jaccard < 0.5) {
                        return VersionResult(
                            score = 0.0,
                            hardReject = true,
                            reason = "Remezcla diferente"
                        )
                    }
                    score *= 0.8 + 0.2 * jaccard
                    notes += "Remezcla coincide"
                }
            }
        }
        if (source.remaster != candidate.remaster) {
            score *= 0.9
            notes += "Remaster distinto"
        }
        if (source.radioEdit != candidate.radioEdit) {
            score *= 0.78
            notes += "Edición de radio distinta"
        }
        return VersionResult(
            score = score,
            hardReject = false,
            reason = if (notes.isEmpty()) "Versión compatible" else notes.joinToString(" · ")
        )
    }

    private fun artistSimilarity(
        source: ImportedPlaylistTrack,
        candidateArtist: String,
        sourceVersion: VersionProfile
    ): Double {
        val sources = expandArtists(source.artists)
        if (sources.isEmpty() && source.primaryArtist.isBlank()) return ARTIST_UNKNOWN
        val candidate = normalize(candidateArtist)
        if (candidate.isBlank()) return ARTIST_UNKNOWN
        var best = sources.maxOfOrNull { nameSimilarity(it, candidate) } ?: 0.0
        if (sourceVersion.remix && sourceVersion.remixerTokens.isNotEmpty()) {
            val remixer = normalize(sourceVersion.remixerTokens.joinToString(" "))
            best = maxOf(best, nameSimilarity(remixer, candidate))
        }
        return best.coerceIn(0.0, 1.0)
    }

    private fun expandArtists(artists: List<String>): List<String> {
        val parts = artists.flatMap { artist ->
            artist.split(Regex("\\s*(?:,|;|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\s&\\s|\\sx\\s|\\svs\\.?\\s)", RegexOption.IGNORE_CASE))
                .map { normalize(it) }
                .filter { it.isNotBlank() }
        }
        return parts.distinct()
    }

    private fun nameSimilarity(normalizedA: String, normalizedB: String): Double {
        if (normalizedA.isBlank() || normalizedB.isBlank()) return 0.0
        if (normalizedA == normalizedB) return 1.0
        if ((normalizedA.contains(normalizedB) || normalizedB.contains(normalizedA)) &&
            minOf(normalizedA.length, normalizedB.length) >= 3
        ) {
            return 0.9
        }
        val a = normalizedA.split(' ').filter { it.isNotBlank() }.toSet()
        val b = normalizedB.split(' ').filter { it.isNotBlank() }.toSet()
        val jaccard = if (a.isEmpty() || b.isEmpty()) 0.0 else
            a.intersect(b).size.toDouble() / a.union(b).size
        return (0.6 * jaccard + 0.4 * charSimilarity(normalizedA, normalizedB))
            .coerceIn(0.0, 1.0)
    }

    private fun durationSimilarity(sourceMs: Long, candidateMs: Long): Double {
        if (sourceMs <= 0L || candidateMs <= 0L) return DURATION_UNKNOWN
        val diff = abs(sourceMs - candidateMs)
        return when {
            diff <= 2_000L -> 1.0
            diff <= 5_000L -> 0.92
            diff <= 10_000L -> 0.75
            diff <= 15_000L -> 0.50
            diff <= 25_000L -> 0.25
            else -> -1.0 // hard reject: incompatible duration
        }
    }

    private fun albumSimilarity(sourceAlbum: String?, candidateAlbum: String?): Double {
        val a = normalize(sourceAlbum.orEmpty())
        val b = normalize(candidateAlbum.orEmpty())
        if (a.isBlank() || b.isBlank()) return ALBUM_UNKNOWN
        if (a == b) return 1.0
        if (a.contains(b) || b.contains(a)) return 0.85
        val aTokens = a.split(' ').toSet()
        val bTokens = b.split(' ').toSet()
        return if (aTokens.isEmpty() || bTokens.isEmpty()) ALBUM_UNKNOWN else
            aTokens.intersect(bTokens).size.toDouble() / aTokens.union(bTokens).size
    }

    // Audius does not expose ISRC in its public track object, so in practice
    // the comparison only activates when a future catalogue provides it. The
    // importer still keeps Spotify's ISRC for that case.
    private fun candidateIsrc(track: Track): String? = track.isrc

    private fun isrcsEqual(a: String?, b: String?): Boolean {
        if (a.isNullOrBlank() || b.isNullOrBlank()) return false
        return a.uppercase(Locale.ROOT).replace("-", "").replace(" ", "") ==
            b.uppercase(Locale.ROOT).replace("-", "").replace(" ", "")
    }

    private fun charSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val max = maxOf(a.length, b.length)
        if (max == 0) return 0.0
        return 1.0 - levenshtein(a, b).toDouble() / max
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    private fun remixerTokens(raw: String): List<String> {
        val segments = bracketRegex.findAll(raw).map { match ->
            match.groupValues[1].ifBlank { match.groupValues[2] }
        }.toList()
        val source = if (segments.isNotEmpty()) segments.joinToString(" ") else raw
        val tokens = normalize(source)
            .split(' ')
            .filter { it.isNotBlank() && it !in versionTokens && it !in noiseTokens }
        // Ignore generic connective words that do not identify the remixer.
        return tokens.filterNot { it in setOf("the", "a", "an", "by", "and", "de", "del", "la", "el") }
            .distinct()
    }

    private fun percent(value: Double): String =
        "${(value * 100).toInt().coerceIn(0, 100)}%"
}

data class VersionProfile(
    val remix: Boolean,
    val slowed: Boolean,
    val spedUp: Boolean,
    val instrumental: Boolean,
    val live: Boolean,
    val cover: Boolean,
    val extended: Boolean,
    val acoustic: Boolean,
    val remaster: Boolean,
    val radioEdit: Boolean,
    val demo: Boolean,
    val remixerTokens: List<String>
) {
    val signature: String
        get() = buildList {
            if (remix) add("remix")
            if (slowed) add("slowed")
            if (spedUp) add("sped")
            if (instrumental) add("instrumental")
            if (live) add("live")
            if (cover) add("cover")
            if (extended) add("extended")
            if (acoustic) add("acoustic")
            if (remaster) add("remaster")
            if (radioEdit) add("radio")
            if (demo) add("demo")
            if (remix && remixerTokens.isNotEmpty()) {
                add("by:" + remixerTokens.sorted().joinToString("-"))
            }
        }.joinToString("+")
}
