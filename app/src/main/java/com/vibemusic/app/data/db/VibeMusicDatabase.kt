package com.vibemusic.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        RecentEntity::class,
        DownloadEntity::class,
        ListeningEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VibeMusicDatabase : RoomDatabase() {
    abstract fun dao(): VibeDao

    companion object {
        @Volatile
        private var instance: VibeMusicDatabase? = null

        /**
         * Keeps favourites, playlists and downloads when upgrading from 1.2.x.
         * New columns get safe defaults and the listening table starts empty.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN coverUri TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN sizeBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE downloads ADD COLUMN mime TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE downloads ADD COLUMN artworkPath TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS listening_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trackId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL,
                        genre TEXT,
                        source TEXT NOT NULL,
                        artworkUri TEXT,
                        durationMs INTEGER NOT NULL,
                        positionMs INTEGER NOT NULL,
                        playedAt INTEGER NOT NULL,
                        skipped INTEGER NOT NULL,
                        completed INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_events_playedAt " +
                        "ON listening_events(playedAt)"
                )
            }
        }

        fun get(context: Context): VibeMusicDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VibeMusicDatabase::class.java,
                    "vibemusic.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
