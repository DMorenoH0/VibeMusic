package com.vibemusic.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class VibeDao {

    // ---------- Favorites ----------
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    abstract fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    abstract suspend fun deleteFavorite(trackId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    abstract suspend fun isFavorite(trackId: String): Boolean

    // ---------- Playlists ----------
    @Query(
        """
        SELECT p.*, (SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount
        FROM playlists p ORDER BY p.updatedAt DESC
        """
    )
    abstract fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Insert
    abstract suspend fun insertPlaylist(entity: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    abstract suspend fun renamePlaylist(id: Long, name: String, updatedAt: Long)

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :id")
    abstract suspend fun touchPlaylist(id: Long, updatedAt: Long)

    @Query("SELECT * FROM playlists WHERE id = :id")
    abstract suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylistOnly(id: Long)

    @Transaction
    open suspend fun deletePlaylist(id: Long) {
        deletePlaylistTracks(id)
        deletePlaylistOnly(id)
    }

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    abstract fun observePlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertPlaylistTrack(entity: PlaylistTrackEntity)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    abstract suspend fun playlistTrackCount(playlistId: Long): Int

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    abstract suspend fun deletePlaylistTrack(playlistId: Long, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    abstract suspend fun deletePlaylistTracks(playlistId: Long)

    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :playlistId")
    abstract suspend fun playlistTrackIds(playlistId: Long): List<String>

    // ---------- Recently played ----------
    @Query("SELECT * FROM recent ORDER BY playedAt DESC LIMIT 20")
    abstract fun observeRecent(): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRecent(entity: RecentEntity)

    @Query(
        """
        DELETE FROM recent WHERE trackId NOT IN
        (SELECT trackId FROM recent ORDER BY playedAt DESC LIMIT 20)
        """
    )
    abstract suspend fun trimRecent()

    @Query("DELETE FROM recent")
    abstract suspend fun clearRecent()

    // ---------- Downloads / imports ----------
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    abstract fun observeDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertDownload(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE trackId = :trackId")
    abstract suspend fun deleteDownload(trackId: String)

    @Query("SELECT * FROM downloads WHERE trackId = :trackId")
    abstract suspend fun getDownload(trackId: String): DownloadEntity?

    @Query("SELECT * FROM downloads")
    abstract suspend fun allDownloads(): List<DownloadEntity>

    // ---------- Data erasure ----------
    @Query("DELETE FROM favorites")
    abstract suspend fun clearFavorites()

    @Query("DELETE FROM playlists")
    abstract suspend fun clearPlaylists()

    @Query("DELETE FROM playlist_tracks")
    abstract suspend fun clearPlaylistTracks()

    @Query("DELETE FROM downloads")
    abstract suspend fun clearDownloads()
}
