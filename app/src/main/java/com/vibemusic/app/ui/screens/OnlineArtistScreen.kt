package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.online.OnlineAlbum
import com.vibemusic.app.ui.components.AlbumArt
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.ErrorBanner
import com.vibemusic.app.ui.components.TrackRow
import com.vibemusic.app.ui.paletteFor
import com.vibemusic.app.ui.theme.VibeBlack
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun OnlineArtistScreen(
    vm: VibeViewModel,
    artistId: String,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    val state by vm.onlineArtist.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val artist = state.artist
    val palette = paletteFor("deezer:artist:$artistId")

    LaunchedEffect(artistId) { vm.loadOnlineArtist(artistId) }

    if (state.loading && artist == null) {
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
                    .height(290.dp)
                    .background(Brush.verticalGradient(listOf(palette[1], palette[2], VibeBlack)))
            ) {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AlbumArt(
                        title = artist?.name ?: "Artista",
                        artworkUri = artist?.artworkUrl,
                        seed = "deezer:artist:$artistId",
                        modifier = Modifier
                            .size(132.dp)
                            .clip(CircleShape),
                        corner = 66.dp,
                        palette = palette
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        artist?.name ?: "Cargando…",
                        style = MaterialTheme.typography.headlineMedium,
                        color = VibeCream,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    if (artist != null) {
                        Text(
                            buildString {
                                append("Artista online")
                                if (artist.albumCount > 0) append(" · ${artist.albumCount} álbumes")
                                if (artist.fanCount > 0) append(" · ${compactCount(artist.fanCount)} seguidores")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted,
                            letterSpacing = 0.6.sp
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
            item { ErrorBanner(message = message, onRetry = { vm.loadOnlineArtist(artistId) }) }
        }

        if (artist != null) {
            if (artist.topTracks.isNotEmpty()) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                vm.playQueue(artist.topTracks.map { it.toTrack() }, shuffled = true)
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
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Vistas previas de 30 s",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted
                        )
                    }
                }

                item {
                    Text(
                        "Populares",
                        style = MaterialTheme.typography.titleLarge,
                        color = VibeCream,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(artist.topTracks.map { it.toTrack() }, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = player.mediaId == track.id,
                        isPlaying = player.isPlaying,
                        onClick = {
                            vm.playFromList(artist.topTracks.map { it.toTrack() }, track)
                            onOpenPlayer()
                        },
                        onFavorite = null,
                        onMore = { onTrackMore(track) }
                    )
                }
            } else {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Person,
                        title = "Sin pistas",
                        message = "El artista no tiene vistas previas disponibles ahora mismo."
                    )
                }
            }

            if (artist.albums.isNotEmpty()) {
                item {
                    Text(
                        "Álbumes",
                        style = MaterialTheme.typography.titleLarge,
                        color = VibeCream,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(artist.albums, key = { it.id }) { album ->
                            OnlineAlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.navigationBarsPadding().height(12.dp)) }
    }
}

@Composable
private fun OnlineAlbumCard(album: OnlineAlbum, onClick: () -> Unit) {
    Column(
        Modifier
            .width(132.dp)
            .clickable(onClick = onClick)
    ) {
        AlbumArt(
            title = album.title,
            artworkUri = album.artworkUrl,
            seed = "deezer:album:${album.id}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            corner = 16.dp
        )
        Spacer(Modifier.height(7.dp))
        Text(
            album.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            album.releaseDate?.take(4).orEmpty().ifBlank { "Álbum" },
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted,
            maxLines = 1
        )
    }
}

private fun compactCount(value: Long): String = when {
    value >= 1_000_000 -> "%.1f M".format(value / 1_000_000f)
    value >= 1_000 -> "%.1f K".format(value / 1_000f)
    else -> value.toString()
}
