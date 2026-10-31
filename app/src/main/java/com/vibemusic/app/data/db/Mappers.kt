package com.vibemusic.app.data.db

import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource

fun Track.toFavorite(now: Long = System.currentTimeMillis()) = FavoriteEntity(
    trackId = id,
    title = title,
    artist = artist,
    album = album,
    uri = uri,
    artworkUri = artworkUri,
    durationMs = durationMs,
    source = source.name,
    addedAt = now
)

fun FavoriteEntity.toTrack() = Track(
    id = trackId,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artworkUri = artworkUri,
    source = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.LOCAL),
    addedAt = addedAt
)

fun Track.toPlaylistTrack(playlistId: Long, position: Int, now: Long = System.currentTimeMillis()) =
    PlaylistTrackEntity(
        playlistId = playlistId,
        trackId = id,
        title = title,
        artist = artist,
        album = album,
        uri = uri,
        artworkUri = artworkUri,
        durationMs = durationMs,
        source = source.name,
        position = position,
        addedAt = now
    )

fun PlaylistTrackEntity.toTrack() = Track(
    id = trackId,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artworkUri = artworkUri,
    source = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.LOCAL),
    addedAt = addedAt
)

fun Track.toRecent(now: Long = System.currentTimeMillis()) = RecentEntity(
    trackId = id,
    title = title,
    artist = artist,
    album = album,
    uri = uri,
    artworkUri = artworkUri,
    durationMs = durationMs,
    source = source.name,
    playedAt = now
)

fun RecentEntity.toTrack() = Track(
    id = trackId,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artworkUri = artworkUri,
    source = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.LOCAL),
    addedAt = playedAt
)

fun DownloadEntity.toTrack() = Track(
    id = trackId,
    uri = android.net.Uri.fromFile(java.io.File(fileName)).toString(),
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artworkUri = artworkUri,
    source = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.DOWNLOAD),
    fileName = fileName,
    addedAt = downloadedAt
)
