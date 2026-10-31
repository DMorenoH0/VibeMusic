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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.components.AlbumArt
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.TrackRow
import com.vibemusic.app.ui.formatLongDuration
import com.vibemusic.app.ui.paletteFor
import com.vibemusic.app.ui.trackPalette
import com.vibemusic.app.ui.theme.VibeBlack
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurfaceHigh
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun AlbumDetailScreen(
    vm: VibeViewModel,
    albumName: String,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onTrackMore: (Track) -> Unit
) {
    val allTracks by vm.allTracks.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()

    val tracks = remember(allTracks, albumName) {
        allTracks.filter { it.album.ifBlank { "Sin álbum" } == albumName }
            .sortedBy { it.title.lowercase() }
    }
    val artist = tracks.firstOrNull()?.artist.orEmpty().ifBlank { "VibeMusic" }
    val artworkUri = tracks.firstNotNullOfOrNull { it.artworkUri }
    val cover = tracks.firstOrNull()
    val palette = remember(albumName, cover?.id) {
        cover?.let { trackPalette(it) } ?: paletteFor("album:$albumName")
    }
    val seed = cover?.id ?: "album:$albumName"
    val totalDuration = tracks.sumOf { it.durationMs }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(VibeBlack),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(268.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(palette[1], palette[2], VibeBlack)
                        )
                    )
            ) {
                AlbumArt(
                    title = albumName,
                    artworkUri = artworkUri,
                    seed = seed,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 22.dp)
                        .size(142.dp),
                    corner = 20.dp,
                    palette = palette
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, end = 20.dp, bottom = 22.dp)
                        .fillMaxWidth(0.58f)
                ) {
                    Text(
                        "ÁLBUM",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeRed,
                        letterSpacing = 1.6.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        albumName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = VibeCream,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibeCream.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${tracks.size} ${if (tracks.size == 1) "tema" else "temas"} · ${formatLongDuration(totalDuration)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted
                    )
                }
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
                        Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Reproducir",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
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
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = null,
                            tint = VibeCream,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Aleatorio",
                            style = MaterialTheme.typography.labelLarge,
                            color = VibeCream
                        )
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Album,
                    title = "Álbum vacío",
                    message = "No se encontraron canciones para este álbum.",
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
                    showAlbum = false,
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
                    "Álbum · ${tracks.size} ${if (tracks.size == 1) "pista" else "pistas"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = VibeMuted,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(20.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }
}
