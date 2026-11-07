package com.vibemusic.app.data.csv

import com.vibemusic.app.data.model.Track
import java.text.Normalizer

enum class CsvColumnRole { TITLE, ARTIST, ALBUM, DURATION, ISRC, IGNORE }

data class CsvTable(
    val headers: List<String>,
    val rows: List<List<String>>,
    val delimiter: Char
)

data class CsvRecord(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long = 0L,
    val isrc: String = ""
)

data class CsvMatch(
    val record: CsvRecord,
    val track: Track
)

data class CsvMatchResult(
    val matched: List<CsvMatch>,
    val unmatched: List<CsvRecord>,
    val duplicates: Int
)

/**
 * Small RFC 4180 reader/writer. It supports quoted fields, escaped quotes,
 * embedded line breaks, UTF-8 BOM and comma, semicolon or tab delimiters, which
 * covers the CSV files exported by most playlist migration services.
 */
fun parseCsv(raw: String, maxRows: Int = 5_000): CsvTable {
    val text = raw.removePrefix("\uFEFF")
    if (text.isBlank()) return CsvTable(emptyList(), emptyList(), ',')
    val delimiter = detectDelimiter(text)
    val rows = ArrayList<List<String>>()
    var field = StringBuilder()
    var row = ArrayList<String>()
    var inQuotes = false
    var i = 0
    while (i < text.length && rows.size <= maxRows) {
        val c = text[i]
        when {
            inQuotes -> {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            }

            c == '"' -> inQuotes = true
            c == delimiter -> {
                row.add(field.toString().trim())
                field = StringBuilder()
            }

            c == '\r' -> {
                // handled together with the optional following \n
            }

            c == '\n' -> {
                row.add(field.toString().trim())
                field = StringBuilder()
                if (row.any { it.isNotBlank() }) rows.add(row)
                row = ArrayList()
            }

            else -> field.append(c)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString().trim())
        if (row.any { it.isNotBlank() }) rows.add(row)
    }
    if (rows.isEmpty()) return CsvTable(emptyList(), emptyList(), delimiter)
    val headers = rows.first().map { it.trim() }
    val body = rows.drop(1).map { values ->
        List(headers.size) { index -> values.getOrElse(index) { "" } }
    }
    return CsvTable(headers, body, delimiter)
}

fun detectDelimiter(text: String): Char {
    val firstLine = text.lineSequence().firstOrNull().orEmpty()
    val candidates = listOf(',' to firstLine.count { it == ',' }, ';' to firstLine.count { it == ';' }, '\t' to firstLine.count { it == '\t' })
    return candidates.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first ?: ','
}

fun guessMapping(headers: List<String>): List<CsvColumnRole> =
    headers.map { header ->
        val key = normalize(header).replace("_", " ").trim()
        when {
            key in setOf("title", "track", "track name", "song", "song title", "nombre", "nombre de la cancion", "titulo", "cancion") -> CsvColumnRole.TITLE
            key in setOf("artist", "artist name", "artists", "band", "artista", "nombre del artista") -> CsvColumnRole.ARTIST
            key in setOf("album", "album name", "album title", "disco", "nombre del album") -> CsvColumnRole.ALBUM
            key in setOf("duration", "length", "time", "duracion", "duration ms", "duration (ms)") -> CsvColumnRole.DURATION
            key in setOf("isrc", "isrc code", "isrc code(s)") -> CsvColumnRole.ISRC
            else -> CsvColumnRole.IGNORE
        }
    }

fun extractRecords(table: CsvTable, mapping: List<CsvColumnRole>): List<CsvRecord> {
    val titleIndex = mapping.indexOfFirst { it == CsvColumnRole.TITLE }
    val artistIndex = mapping.indexOfFirst { it == CsvColumnRole.ARTIST }
    val albumIndex = mapping.indexOfFirst { it == CsvColumnRole.ALBUM }
    val durationIndex = mapping.indexOfFirst { it == CsvColumnRole.DURATION }
    val isrcIndex = mapping.indexOfFirst { it == CsvColumnRole.ISRC }
    if (titleIndex < 0) return emptyList()
    return table.rows.mapNotNull { row ->
        val title = row.getOrElse(titleIndex) { "" }.trim()
        if (title.isBlank()) return@mapNotNull null
        CsvRecord(
            title = title,
            artist = row.getOrElse(artistIndex) { "" }.trim().takeIf { artistIndex >= 0 }.orEmpty(),
            album = row.getOrElse(albumIndex) { "" }.trim().takeIf { albumIndex >= 0 }.orEmpty(),
            durationMs = row.getOrElse(durationIndex) { "" }.takeIf { durationIndex >= 0 }
                ?.let(::parseDuration)
                ?: 0L,
            isrc = row.getOrElse(isrcIndex) { "" }.trim().takeIf { isrcIndex >= 0 }.orEmpty()
        )
    }
}

fun matchRecords(records: List<CsvRecord>, library: List<Track>): CsvMatchResult {
    val byTitleArtist = HashMap<String, MutableList<Track>>()
    val byTitle = HashMap<String, MutableList<Track>>()
    library.forEach { track ->
        byTitleArtist.getOrPut("${normalize(track.title)}|${normalize(track.artist)}") { mutableListOf() }.add(track)
        byTitle.getOrPut(normalize(track.title)) { mutableListOf() }.add(track)
    }
    val used = HashSet<String>()
    val matched = ArrayList<CsvMatch>()
    val unmatched = ArrayList<CsvRecord>()
    var duplicates = 0
    val seen = HashSet<String>()
    records.forEach { record ->
        val key = "${normalize(record.title)}|${normalize(record.artist)}"
        if (!seen.add(key)) {
            duplicates++
            return@forEach
        }
        val candidates = byTitleArtist[key].orEmpty().filterNot { it.id in used }
            .ifEmpty {
                byTitle[normalize(record.title)].orEmpty().filterNot { it.id in used }
            }
        val best = candidates.firstOrNull()
        if (best != null) {
            used.add(best.id)
            matched.add(CsvMatch(record, best))
        } else {
            unmatched.add(record)
        }
    }
    return CsvMatchResult(matched, unmatched, duplicates)
}

fun exportCsv(tracks: List<Track>): String {
    val header = listOf("Title", "Artist", "Album", "Duration", "Duration (ms)", "ISRC")
    val rows = tracks.map { track ->
        listOf(
            track.title,
            track.artist,
            track.album,
            formatCsvDuration(track.durationMs),
            track.durationMs.toString(),
            ""
        )
    }
    return buildString {
        append(header.joinToString(",") { escapeCsv(it) })
        append("\r\n")
        rows.forEach { row ->
            append(row.joinToString(",") { escapeCsv(it) })
            append("\r\n")
        }
    }
}

fun escapeCsv(value: String): String {
    val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' }
    val escaped = value.replace("\"", "\"\"")
    return if (needsQuotes) "\"$escaped\"" else escaped
}

fun formatCsvDuration(durationMs: Long): String {
    if (durationMs <= 0) return ""
    val total = durationMs / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

fun parseDuration(value: String): Long {
    val clean = value.trim()
    if (clean.isEmpty()) return 0L
    if (clean.all { it.isDigit() }) {
        val number = clean.toLongOrNull() ?: return 0L
        // A bare number can be milliseconds (TuneMyMusic) or seconds.
        return if (number > 4_000) number else number * 1000L
    }
    val parts = clean.split(':')
    if (parts.size in 2..3) {
        val numbers = parts.map { it.toLongOrNull() ?: return 0L }
        var seconds = 0L
        numbers.forEach { seconds = seconds * 60 + it }
        return seconds * 1000L
    }
    return 0L
}

fun normalize(value: String): String {
    val lower = value.lowercase().trim()
    val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return decomposed
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
