package com.vibemusic.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        RecentEntity::class,
        DownloadEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VibeMusicDatabase : RoomDatabase() {
    abstract fun dao(): VibeDao

    companion object {
        @Volatile
        private var instance: VibeMusicDatabase? = null

        fun get(context: Context): VibeMusicDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VibeMusicDatabase::class.java,
                    "vibemusic.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
