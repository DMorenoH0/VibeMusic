package com.vibemusic.app.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMusicScanner(private val context: Context) {

    companion object {
        val audioPermission: String
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        private val albumArtBase: Uri = Uri.parse("content://media/external/audio/albumart")
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED

    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()

        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= 10000"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext() && tracks.size < 5000) {
                val size = cursor.getLong(sizeCol)
                if (size <= 0) continue
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                val fileName = cursor.getString(nameCol)?.trim().orEmpty()
                val title = rawTitle.ifBlank { fileName.substringBeforeLast('.').ifBlank { "Pista sin título" } }
                val artist = cursor.getString(artistCol)?.trim()
                    ?.takeUnless { it.isBlank() || it == "<unknown>" }
                    ?: "Artista desconocido"
                val album = cursor.getString(albumCol)?.trim()
                    ?.takeUnless { it.isBlank() || it == "<unknown>" }
                    ?: "Sin álbum"
                val duration = cursor.getLong(durationCol).coerceAtLeast(0)
                val added = cursor.getLong(addedCol) * 1000L
                tracks += Track(
                    id = "local:$id",
                    uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    ).toString(),
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = duration,
                    artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(albumArtBase, albumId).toString()
                    } else null,
                    source = TrackSource.LOCAL,
                    fileName = fileName,
                    addedAt = added
                )
            }
        }
        tracks
    }
}
