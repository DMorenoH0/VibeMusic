package com.vibemusic.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opens a real 3.0 database file (schema v3, written with raw SQL), lets Room
 * run MIGRATION_3_4 and checks that the user's existing library survived and
 * that the new importer columns are usable.
 */
@RunWith(AndroidJUnit4::class)
class VibeMusicMigration3To4Test {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "vibemusic-migration-3-4.db"
    private var database: VibeMusicDatabase? = null

    @Before
    fun createVersion3Database() {
        context.deleteDatabase(dbName)
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.execSQL(
            "CREATE TABLE IF NOT EXISTS `favorites` (`trackId` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, " +
                "`uri` TEXT NOT NULL, `artworkUri` TEXT, `durationMs` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, `downloadUrl` TEXT, " +
                "`downloadBlockedReason` TEXT, PRIMARY KEY(`trackId`))"
        )
        raw.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "`coverUri` TEXT)"
        )
        raw.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_tracks` (`playlistId` INTEGER NOT NULL, " +
                "`trackId` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, " +
                "`album` TEXT NOT NULL, `uri` TEXT NOT NULL, `artworkUri` TEXT, " +
                "`durationMs` INTEGER NOT NULL, `source` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, `downloadUrl` TEXT, " +
                "`downloadBlockedReason` TEXT, PRIMARY KEY(`playlistId`, `trackId`))"
        )
        raw.execSQL(
            "CREATE TABLE IF NOT EXISTS `recent` (`trackId` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`artist` TEXT NOT NULL, `album` TEXT NOT NULL, `uri` TEXT NOT NULL, " +
                "`artworkUri` TEXT, `durationMs` INTEGER NOT NULL, `source` TEXT NOT NULL, " +
                "`playedAt` INTEGER NOT NULL, `downloadUrl` TEXT, `downloadBlockedReason` TEXT, " +
                "PRIMARY KEY(`trackId`))"
        )
        raw.execSQL(
            "CREATE TABLE IF NOT EXISTS `downloads` (`trackId` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, " +
                "`fileName` TEXT NOT NULL, `sourceUrl` TEXT NOT NULL, " +
                "`durationMs` INTEGER NOT NULL, `downloadedAt` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, `artworkUri` TEXT, " +
                "`sizeBytes` INTEGER NOT NULL DEFAULT 0, `mime` TEXT NOT NULL DEFAULT '', " +
                "`artworkPath` TEXT, PRIMARY KEY(`trackId`))"
        )
        raw.execSQL(
            "CREATE TABLE IF NOT EXISTS `listening_events` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trackId` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, " +
                "`genre` TEXT, `source` TEXT NOT NULL, `artworkUri` TEXT, " +
                "`durationMs` INTEGER NOT NULL, `positionMs` INTEGER NOT NULL, " +
                "`playedAt` INTEGER NOT NULL, `skipped` INTEGER NOT NULL, " +
                "`completed` INTEGER NOT NULL)"
        )
        raw.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_listening_events_playedAt` " +
                "ON `listening_events` (`playedAt`)"
        )

        raw.execSQL(
            "INSERT INTO favorites (trackId, title, artist, album, uri, artworkUri, " +
                "durationMs, source, addedAt, downloadUrl, downloadBlockedReason) VALUES " +
                "('audius:track:1', 'Mámamelo', 'Conep', 'Mámamelo', " +
                "'https://api.audius.co/v1/tracks/1/stream', NULL, 180000, 'AUDIUS', 10, " +
                "'https://api.audius.co/v1/tracks/1/download', NULL)"
        )
        raw.execSQL(
            "INSERT INTO playlists (id, name, createdAt, updatedAt, coverUri) VALUES " +
                "(1, 'Lista antigua', 5, 5, NULL)"
        )
        raw.execSQL(
            "INSERT INTO playlist_tracks (playlistId, trackId, title, artist, album, uri, " +
                "artworkUri, durationMs, source, position, addedAt, downloadUrl, " +
                "downloadBlockedReason) VALUES (1, 'audius:track:1', 'Mámamelo', 'Conep', " +
                "'Mámamelo', 'https://api.audius.co/v1/tracks/1/stream', NULL, 180000, " +
                "'AUDIUS', 0, 11, NULL, NULL)"
        )
        raw.execSQL(
            "INSERT INTO recent (trackId, title, artist, album, uri, artworkUri, durationMs, " +
                "source, playedAt, downloadUrl, downloadBlockedReason) VALUES " +
                "('audius:track:1', 'Mámamelo', 'Conep', 'Mámamelo', " +
                "'https://api.audius.co/v1/tracks/1/stream', NULL, 180000, 'AUDIUS', 12, " +
                "NULL, NULL)"
        )
        raw.execSQL(
            "INSERT INTO downloads (trackId, title, artist, album, fileName, sourceUrl, " +
                "durationMs, downloadedAt, source, artworkUri, sizeBytes, mime, artworkPath) " +
                "VALUES ('audius:track:1', 'Mámamelo', 'Conep', 'Mámamelo', 'mamamelo.mp3', " +
                "'https://api.audius.co/v1/tracks/1/download', 180000, 13, 'AUDIUS', NULL, " +
                "12345, 'audio/mpeg', NULL)"
        )
        raw.execSQL(
            "INSERT INTO listening_events (trackId, title, artist, album, genre, source, " +
                "artworkUri, durationMs, positionMs, playedAt, skipped, completed) VALUES " +
                "('audius:track:1', 'Mámamelo', 'Conep', 'Mámamelo', NULL, 'AUDIUS', NULL, " +
                "180000, 180000, 14, 0, 1)"
        )
        raw.version = 3
        raw.close()

        database = Room.databaseBuilder(context, VibeMusicDatabase::class.java, dbName)
            .addMigrations(
                VibeMusicDatabase.MIGRATION_1_2,
                VibeMusicDatabase.MIGRATION_2_3,
                VibeMusicDatabase.MIGRATION_3_4
            )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun cleanUp() {
        database?.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun upgradingFromV3KeepsUserData() = runBlocking {
        val dao = database!!.dao()
        assertTrue(dao.isFavorite("audius:track:1"))
        assertEquals(1, dao.playlistTrackCount(1))
        assertNotNull(dao.getDownload("audius:track:1"))
        assertEquals(1, dao.listeningEventsOnce().size)

        val playlist = dao.getPlaylist(1)
        assertNotNull(playlist)
        assertEquals("Lista antigua", playlist!!.name)
        assertNull(playlist.sourcePlatform)
        assertNull(playlist.sourceUrl)

        val track = dao.allPlaylistTracks().first { it.playlistId == 1L }
        assertEquals("Mámamelo", track.title)
        assertNull(track.importSourceTitle)
        assertNull(track.importSourcePosition)
        assertNull(track.matchConfidence)
    }

    @Test
    fun importedPlaylistKeepsOriginMetadataAndOrder() = runBlocking {
        val dao = database!!.dao()
        val playlistId = dao.insertImportedPlaylist(
            PlaylistEntity(
                name = "Rap español",
                createdAt = 20,
                updatedAt = 20,
                coverUri = "https://i.scdn.co/image/abc",
                sourcePlatform = "SPOTIFY",
                sourcePlaylistId = "37i9dQZF1DXcBWIGoYBM5M",
                sourceUrl = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M",
                sourceOwner = "David",
                sourceDescription = "Selección semanal"
            ),
            listOf(
                PlaylistTrackEntity(
                    playlistId = 0,
                    trackId = "audius:track:9",
                    title = "Mámamelo (Official Audio)",
                    artist = "Conep",
                    album = "Mámamelo",
                    uri = "https://api.audius.co/v1/tracks/9/stream",
                    artworkUri = "https://i.scdn.co/image/abc",
                    durationMs = 181_000,
                    source = "AUDIUS",
                    position = 0,
                    addedAt = 21,
                    downloadUrl = "https://api.audius.co/v1/tracks/9/download",
                    importSourceTitle = "Mámamelo (Official Audio)",
                    importSourceArtist = "Conep",
                    importSourceAlbum = "Mámamelo",
                    importSourcePosition = 4,
                    importPlatform = "SPOTIFY",
                    matchConfidence = 0.97
                )
            )
        )

        val stored = dao.getPlaylist(playlistId)
        assertNotNull(stored)
        assertEquals("SPOTIFY", stored!!.sourcePlatform)
        assertEquals("David", stored.sourceOwner)
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", stored.sourcePlaylistId)

        val imported = dao.allPlaylistTracks().first { it.playlistId == playlistId }
        assertEquals(4, imported.importSourcePosition)
        assertEquals(0.97, imported.matchConfidence!!, 0.0001)
        assertEquals("SPOTIFY", imported.importPlatform)
    }
}
