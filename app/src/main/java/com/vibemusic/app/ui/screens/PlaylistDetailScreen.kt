package com.vibemusic.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import com.vibemusic.app.data.youtube.YouTubeVideo
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.PlaylistHero
import com.vibemusic.app.ui.components.TrackRow
import com.vibemusic.app.ui.dialogs.CreatePlaylistDialog
import com.vibemusic.app.ui.formatLongDuration
import com.vibemusic.app.ui.theme.VibeBlack
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurfaceHigh
import com.vibemusic.app.viewmodel.VibeViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaylistDetailScreen(
    vm: VibeViewModel,
    playlistId: Long,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenYouTube: (YouTubeVideo) -> Unit = {}
) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val tracks by remember(playlistId) { vm.playlistTracks(playlistId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val summary = playlists.firstOrNull { it.playlist.id == playlistId }
    val name = summary?.playlist?.name ?: "Lista"
    val totalDuration = tracks.sumOf { it.durationMs }
    val playable = tracks.filter { it.source != TrackSource.YOUTUBE }

    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val picked = uri
            scope.launch {
                val stored = copyPlaylistCover(context, playlistId, picked)
                if (stored != null) vm.setPlaylistCover(playlistId, Uri.parse(stored))
            }
        }
    }

    LaunchedEffect(playlists) {
        if (playlists.isNotEmpty() && summary == null) onBack()
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(VibeBlack),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Box {
                PlaylistHero(
                    name = name,
                    subtitle = "${tracks.size} ${if (tracks.size == 1) "canción" else "canciones"} · ${formatLongDuration(totalDuration)}",
                    seed = "playlist:$playlistId",
                    artworkUri = summary?.playlist?.coverUri
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(6.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = VibeCream
                    )
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        if (playable.isNotEmpty()) {
                            vm.playQueue(playable, shuffled = false)
                            onOpenPlayer()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = VibeRed
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Reproducir", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
                Surface(
                    onClick = {
                        if (playable.isNotEmpty()) {
                            vm.playQueue(playable, shuffled = true)
                            onOpenPlayer()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = VibeSurfaceHigh
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = VibeCream,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .size(18.dp)
                    )
                }
                Surface(
                    onClick = { showRename = true },
                    shape = RoundedCornerShape(50),
                    color = VibeSurfaceHigh
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Renombrar",
                        tint = VibeCream,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .size(18.dp)
                    )
                }
                Surface(
                    onClick = { coverLauncher.launch(arrayOf("image/*")) },
                    shape = RoundedCornerShape(50),
                    color = VibeSurfaceHigh
                ) {
                    Icon(
                        Icons.Rounded.Image,
                        contentDescription = "Cambiar portada",
                        tint = VibeCream,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .size(18.dp)
                    )
                }
                Surface(
                    onClick = { showDelete = true },
                    shape = RoundedCornerShape(50),
                    color = VibeSurfaceHigh
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Eliminar lista",
                        tint = VibeRed,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .size(18.dp)
                    )
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.QueueMusic,
                    title = "Esta lista está vacía",
                    message = "Añade canciones desde el menú «⋯» de cualquier pista, o vídeos desde YouTube.",
                    actionLabel = "Volver",
                    onAction = onBack
                )
            }
        } else {
            items(tracks, key = { it.id }) { track ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrackRow(
                        track = track,
                        modifier = Modifier.weight(1f),
                        isFavorite = track.id in favoriteIds,
                        isCurrent = player.mediaId == track.id,
                        isPlaying = player.isPlaying,
                        onClick = {
                            if (track.source == TrackSource.YOUTUBE) {
                                onOpenYouTube(track.toYouTubeVideo())
                            } else {
                                vm.playFromList(playable, track)
                                onOpenPlayer()
                            }
                        },
                        onFavorite = { vm.toggleFavorite(track) },
                        onMore = { onTrackMore(track) }
                    )
                    Column {
                        IconButton(
                            onClick = {
                                vm.movePlaylistTrack(playlistId, track.id, -1)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ArrowUpward,
                                contentDescription = "Subir",
                                tint = VibeMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                vm.movePlaylistTrack(playlistId, track.id, 1)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ArrowDownward,
                                contentDescription = "Bajar",
                                tint = VibeMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Fin de la lista · ${formatLongDuration(totalDuration)} de música",
                    style = MaterialTheme.typography.labelMedium,
                    color = VibeMuted,
                    modifier = Modifier
                        .padding(20.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }

    if (showRename) {
        CreatePlaylistDialog(
            title = "Renombrar lista",
            initialName = name,
            confirmLabel = "Guardar",
            onDismiss = { showRename = false },
            onConfirm = { newName ->
                showRename = false
                vm.renamePlaylist(playlistId, newName)
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = VibeSurfaceHigh,
            titleContentColor = VibeCream,
            textContentColor = VibeMuted,
            title = { Text("Eliminar «$name»") },
            text = { Text("La lista se borrará. Las canciones seguirán en tu biblioteca.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        vm.deletePlaylist(playlistId)
                        onBack()
                    }
                ) { Text("Eliminar", color = VibeRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancelar", color = VibeMuted)
                }
            }
        )
    }
}

private fun Track.toYouTubeVideo() = YouTubeVideo(
    id = id.removePrefix("youtube:"),
    title = title,
    channel = artist,
    thumbnailUrl = artworkUri.orEmpty(),
    durationMs = durationMs
)

/**
 * Copies the picked image into the app's private storage so the cover keeps
 * working after reboots and even if the original document is moved or deleted.
 */
private suspend fun copyPlaylistCover(context: Context, playlistId: Long, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "playlist-covers").apply { mkdirs() }
            val target = File(dir, "cover-$playlistId-${System.currentTimeMillis()}.img")
            val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            input.use { source ->
                target.outputStream().use { output -> source.copyTo(output, 64 * 1024) }
            }
            if (target.length() <= 0L) {
                target.delete()
                return@runCatching null
            }
            dir.listFiles { file ->
                file.name.startsWith("cover-$playlistId-") && file != target
            }?.forEach { old -> old.delete() }
            Uri.fromFile(target).toString()
        }.getOrNull()
    }


