package com.vibemusic.app.data.csv

import com.vibemusic.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvSupportTest {

    private fun track(id: String, title: String, artist: String) = Track(
        id = id,
        uri = "file:///music/$id.mp3",
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 180_000,
        source = com.vibemusic.app.data.model.TrackSource.LOCAL
    )

    @Test
    fun `parses quoted fields, escaped quotes and UTF-8 BOM`() {
        val raw = "\uFEFFTitle,Artist,Album\r\n" +
            "\"Song, One\",\"AC/DC\",\"Album \"\"X\"\"\"\r\n" +
            "Plain,Artist,Album\r\n"

        val table = parseCsv(raw)

        assertEquals(',', table.delimiter)
        assertEquals(listOf("Title", "Artist", "Album"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals("Song, One", table.rows[0][0])
        assertEquals("AC/DC", table.rows[0][1])
        assertEquals("Album \"X\"", table.rows[0][2])
        assertEquals("Plain", table.rows[1][0])
    }

    @Test
    fun `detects semicolon and tab delimiters`() {
        assertEquals(';', detectDelimiter("Title;Artist;Album\nA;B;C"))
        assertEquals('\t', detectDelimiter("Title\tArtist\tAlbum\nA\tB\tC"))
    }

    @Test
    fun `guesses English and Spanish header mappings`() {
        val mapping = guessMapping(
            listOf("Track Name", "Artist Name", "Album", "Duration (ms)", "ISRC", "Extra")
        )
        assertEquals(CsvColumnRole.TITLE, mapping[0])
        assertEquals(CsvColumnRole.ARTIST, mapping[1])
        assertEquals(CsvColumnRole.ALBUM, mapping[2])
        assertEquals(CsvColumnRole.DURATION, mapping[3])
        assertEquals(CsvColumnRole.ISRC, mapping[4])
        assertEquals(CsvColumnRole.IGNORE, mapping[5])

        val spanish = guessMapping(listOf("Nombre de la canción", "Artista", "Álbum", "Duración"))
        assertEquals(CsvColumnRole.TITLE, spanish[0])
        assertEquals(CsvColumnRole.ARTIST, spanish[1])
        assertEquals(CsvColumnRole.ALBUM, spanish[2])
        assertEquals(CsvColumnRole.DURATION, spanish[3])
    }

    @Test
    fun `extracts records and matches them against the library without duplicates`() {
        val table = parseCsv(
            "Title,Artist,Album,Duration\n" +
                "Song One,Artist A,Album,3:05\n" +
                "Song One,Artist A,Album,3:05\n" +
                "Missing,Artist B,Album,2:00\n"
        )
        val mapping = guessMapping(table.headers)
        val records = extractRecords(table, mapping)
        assertEquals(3, records.size)
        assertEquals(185_000, records[0].durationMs)

        val library = listOf(track("1", "Song One", "Artist A"))
        val result = matchRecords(records, library)

        assertEquals(1, result.matched.size)
        assertEquals("1", result.matched[0].track.id)
        assertEquals(1, result.unmatched.size)
        assertEquals("Missing", result.unmatched[0].title)
        assertEquals(1, result.duplicates)
    }

    @Test
    fun `export escapes separators and quotes and round-trips through the parser`() {
        val csv = exportCsv(
            listOf(
                Track(
                    id = "1",
                    uri = "file:///1.mp3",
                    title = "Hello, \"World\"",
                    artist = "A;B",
                    album = "Line",
                    durationMs = 65_000
                )
            )
        )
        assertTrue(csv.startsWith("Title,Artist,Album,Duration"))
        val table = parseCsv(csv)
        assertEquals(1, table.rows.size)
        assertEquals("Hello, \"World\"", table.rows[0][0])
        assertEquals("A;B", table.rows[0][1])
        assertEquals("1:05", table.rows[0][3])
        assertEquals("65000", table.rows[0][4])
    }

    @Test
    fun `parses human and millisecond durations`() {
        assertEquals(185_000, parseDuration("3:05"))
        assertEquals(3_725_000, parseDuration("1:02:05"))
        assertEquals(185_000, parseDuration("185"))
        assertEquals(185_000, parseDuration("185000"))
        assertEquals(0, parseDuration(""))
        assertEquals("3:05", formatCsvDuration(185_000))
    }

    @Test
    fun `normalises accents and punctuation for matching`() {
        assertEquals("cancion nina", normalize("Canción  Niña!"))
    }
}
