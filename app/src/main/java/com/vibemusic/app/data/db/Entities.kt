package com.vibemusic.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
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
    val addedAt: Long,
    /** Authorised download endpoint snapshot, if the provider granted one. */
    val downloadUrl: String? = null,
    /** Provider explanation when downloads are not allowed. */
    val downloadBlockedReason: String? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val coverUri: String? = null,
    /** Spotify or YouTube Music when the playlist came from the importer. */
    val sourcePlatform: String? = null,
    val sourcePlaylistId: String? = null,
    val sourceUrl: String? = null,
    val sourceOwner: String? = null,
    val sourceDescription: String? = null
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
    val addedAt: Long,
    val downloadUrl: String? = null,
    val downloadBlockedReason: String? = null,
    /** Original catalogue metadata, kept even when the audio came from Audius. */
    val importSourceTitle: String? = null,
    val importSourceArtist: String? = null,
    val importSourceAlbum: String? = null,
    val importSourcePosition: Int? = null,
    val importPlatform: String? = null,
    val matchConfidence: Double? = null
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
    val playedAt: Long,
    val downloadUrl: String? = null,
    val downloadBlockedReason: String? = null
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
    val artworkUri: String? = null,
    val sizeBytes: Long = 0L,
    val mime: String = "",
    val artworkPath: String? = null
)

/**
 * One playback observation. It only ever leaves the device if the user
 * exports it; recommendations are computed locally from this table.
 */
@Entity(
    tableName = "listening_events",
    indices = [Index(value = ["playedAt"])]
)
data class ListeningEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String?,
    val source: String,
    val artworkUri: String?,
    val durationMs: Long,
    val positionMs: Long,
    val playedAt: Long,
    val skipped: Boolean,
    val completed: Boolean
)

data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    val trackCount: Int
)
