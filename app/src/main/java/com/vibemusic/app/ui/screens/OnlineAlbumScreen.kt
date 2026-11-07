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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.components.AlbumArt
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.ErrorBanner
import com.vibemusic.app.ui.components.TrackRow
import com.vibemusic.app.ui.formatLongDuration
import com.vibemusic.app.ui.paletteFor
import com.vibemusic.app.ui.theme.VibeBlack
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurfaceHigh
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun OnlineAlbumScreen(
    vm: VibeViewModel,
    albumId: String,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenArtist: (String) -> Unit
) {
    val state by vm.onlineAlbum.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()

    LaunchedEffect(albumId) { vm.loadOnlineAlbum(albumId) }

    val album = state.album
    val palette = paletteFor("deezer:album:$albumId")

    if (state.loading && album == null) {
        Box(Modifier.fillMaxSize().background(VibeBlack), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VibeRed)
        }
        return
    }

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
                    .background(Brush.linearGradient(listOf(palette[1], palette[2], VibeBlack)))
            ) {
                AlbumArt(
                    title = album?.title ?: "Álbum",
                    artworkUri = album?.artworkUrl,
                    seed = "deezer:album:$albumId",
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
                        "ÁLBUM ONLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeRed,
                        letterSpacing = 1.6.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        album?.title ?: "Cargando…",
                        style = MaterialTheme.typography.headlineMedium,
                        color = VibeCream,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    if (album != null) {
                        Text(
                            album.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeCream.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${album.tracks.size} ${if (album.tracks.size == 1) "tema" else "temas"} · ${formatLongDuration(album.totalDurationMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted
                        )
                    }
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

        state.error?.let { message ->
            item {
                ErrorBanner(message = message, onRetry = { vm.loadOnlineAlbum(albumId) })
            }
        }

        if (album != null) {
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
                            vm.playQueue(album.tracks.map { it.toTrack() }, shuffled = false)
                            onOpenPlayer()
                        },
                        shape = RoundedCornerShape(50),
                        color = VibeRed
                    ) {
                        Row(
                            Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reproducir", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                    Surface(
                        onClick = {
                            vm.playQueue(album.tracks.map { it.toTrack() }, shuffled = true)
                            onOpenPlayer()
                        },
                        shape = RoundedCornerShape(50),
                        color = VibeSurfaceHigh
                    ) {
                        Row(
                            Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = VibeCream, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Aleatorio", style = MaterialTheme.typography.labelLarge, color = VibeCream)
                        }
                    }
                    if (album.artistId != null) {
                        Text(
                            "Ver artista",
                            style = MaterialTheme.typography.labelLarge,
                            color = VibeRed,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clickable { onOpenArtist(album.artistId) }
                        )
                    }
                }
            }

            item {
                Text(
                    "Vistas previas de 30 s · catálogo Deezer",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            if (album.tracks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Album,
                        title = "Álbum sin pistas",
                        message = "La fuente no devolvió pistas reproducibles para este álbum."
                    )
                }
            } else {
                items(album.tracks.map { it.toTrack() }, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = player.mediaId == track.id,
                        isPlaying = player.isPlaying,
                        showAlbum = false,
                        onClick = {
                            vm.playFromList(album.tracks.map { it.toTrack() }, track)
                            onOpenPlayer()
                        },
                        onFavorite = null,
                        onMore = { onTrackMore(track) }
                    )
                }
            }
        }

        item { Spacer(Modifier.navigationBarsPadding().height(12.dp)) }
    }
}
