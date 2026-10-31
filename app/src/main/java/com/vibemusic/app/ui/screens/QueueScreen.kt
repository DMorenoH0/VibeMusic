package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.ui.components.AlbumArt
import com.vibemusic.app.ui.components.CoverArt
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.PlayingIndicator
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurfaceHigh
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun QueueScreen(
    vm: VibeViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val queue by vm.queue.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val allTracks by vm.allTracks.collectAsStateWithLifecycle()
    val trackById = remember(allTracks) { allTracks.associateBy { it.id } }
    var confirmClear by remember { mutableStateOf(false) }

    VibePageBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = VibeCream
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Cola de reproducción",
                        style = MaterialTheme.typography.titleLarge,
                        color = VibeCream
                    )
                    Text(
                        text = if (queue.isEmpty()) {
                            "Sin canciones en cola"
                        } else {
                            "${queue.size} ${if (queue.size == 1) "canción" else "canciones"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted
                    )
                }
                if (queue.isNotEmpty()) {
                    Text(
                        "Vaciar",
                        style = MaterialTheme.typography.labelLarge,
                        color = VibeRed,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { confirmClear = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (queue.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Rounded.QueueMusic,
                        title = "La cola está vacía",
                        message = "Reproduce una canción o añade temas desde el menú de cualquier pista."
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    items(queue, key = { "${it.index}:${it.mediaId}" }) { item ->
                        val isCurrent = item.mediaId != null && item.mediaId == player.mediaId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isCurrent) Color.White.copy(alpha = 0.06f) else Color.Transparent)
                                .clickable {
                                    vm.jumpToQueue(item.index)
                                    onOpenPlayer()
                                }
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                                if (isCurrent) {
                                    PlayingIndicator(animated = player.isPlaying)
                                } else {
                                    Text(
                                        "${item.index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VibeMuted
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            val queueTrack = item.mediaId?.let(trackById::get)
                            if (queueTrack != null) {
                                CoverArt(
                                    track = queueTrack,
                                    modifier = Modifier.size(46.dp),
                                    corner = 10.dp,
                                    showBrand = false
                                )
                            } else {
                                AlbumArt(
                                    title = item.title,
                                    artworkUri = item.artworkUri,
                                    seed = item.mediaId ?: item.title,
                                    modifier = Modifier.size(46.dp),
                                    corner = 10.dp,
                                    showBrand = false
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title.ifBlank { "Pista sin título" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isCurrent) VibeRed else VibeCream,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.artist.ifBlank { "VibeMusic" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VibeMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { vm.removeFromQueue(item.index) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Quitar de la cola",
                                    tint = VibeMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = VibeSurfaceHigh,
            titleContentColor = VibeCream,
            textContentColor = VibeMuted,
            title = { Text("Vaciar la cola") },
            text = { Text("Se detendrá la reproducción y se quitarán todas las canciones de la cola.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        vm.clearQueue()
                        onBack()
                    }
                ) { Text("Vaciar", color = VibeRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancelar", color = VibeMuted)
                }
            }
        )
    }
}
