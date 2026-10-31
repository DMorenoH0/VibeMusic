package com.vibemusic.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val artworkUri: String?,
    val durationMs: Long,
    val source: String,
    val addedAt: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val artworkUri: String?,
    val durationMs: Long,
    val source: String,
    val position: Int,
    val addedAt: Long
)

@Entity(tableName = "recent")
data class RecentEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val artworkUri: String?,
    val durationMs: Long,
    val source: String,
    val playedAt: Long
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val fileName: String,
    val sourceUrl: String,
    val durationMs: Long,
    val downloadedAt: Long,
    val source: String,
    val artworkUri: String? = null
)

data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    val trackCount: Int
)
