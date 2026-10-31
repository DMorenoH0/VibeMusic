package com.vibemusic.app.ui.screens

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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
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

@Composable
fun PlaylistDetailScreen(
    vm: VibeViewModel,
    playlistId: Long,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onTrackMore: (Track) -> Unit
) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val tracks by remember(playlistId) { vm.playlistTracks(playlistId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val summary = playlists.firstOrNull { it.playlist.id == playlistId }
    val name = summary?.playlist?.name ?: "Lista"
    val totalDuration = tracks.sumOf { it.durationMs }

    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

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
                    seed = "playlist:$playlistId"
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
                        if (tracks.isNotEmpty()) {
                            vm.playQueue(tracks, shuffled = false)
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
                        if (tracks.isNotEmpty()) {
                            vm.playQueue(tracks, shuffled = true)
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
                    message = "Añade canciones desde el menú «⋯» de cualquier pista, en cualquier parte de la app.",
                    actionLabel = "Volver",
                    onAction = onBack
                )
            }
        } else {
            items(tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    isFavorite = track.id in favoriteIds,
                    isCurrent = player.mediaId == track.id,
                    isPlaying = player.isPlaying,
                    onClick = {
                        vm.playFromList(tracks, track)
                        onOpenPlayer()
                    },
                    onFavorite = { vm.toggleFavorite(track) },
                    onMore = { onTrackMore(track) }
                )
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
