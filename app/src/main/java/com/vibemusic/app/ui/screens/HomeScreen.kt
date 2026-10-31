package com.vibemusic.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.ScanState
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import com.vibemusic.app.ui.AlbumGroup
import com.vibemusic.app.ui.components.AlbumCard
import com.vibemusic.app.ui.components.CompactTrackCard
import com.vibemusic.app.ui.components.CoverArt
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.MixCard
import com.vibemusic.app.ui.components.PermissionBanner
import com.vibemusic.app.ui.components.SectionHeader
import com.vibemusic.app.ui.components.TrackRow
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.groupAlbums
import com.vibemusic.app.ui.trackPalette
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.viewmodel.VibeViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    vm: VibeViewModel,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val favoritesIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()
    val initializing by vm.initializing.collectAsStateWithLifecycle()

    val albums = remember(tracks) { groupAlbums(tracks) }
    val demos = remember(tracks) { tracks.filter { it.source == TrackSource.DEMO } }
    val hero = demos.firstOrNull() ?: tracks.firstOrNull()

    VibePageBackground {
        if (initializing && tracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VibeRed)
            }
            return@VibePageBackground
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item { HomeHeader(onOpenSearch, onOpenSettings) }

            item {
                PermissionBanner(
                    scanState = scanState,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenAppSettings
                )
            }

            if (tracks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.PlayArrow,
                        title = "Aún no hay música",
                        message = "Concede acceso a tus archivos o importa audio para empezar.",
                        actionLabel = "Escanear biblioteca",
                        onAction = { vm.rescanLibrary() }
                    )
                }
            }

            if (hero != null) {
                item {
                    HeroCard(
                        hero = hero,
                        trackCount = tracks.size,
                        onPlay = { vm.playQueue(tracks, shuffled = true) }
                    )
                }
            }

            if (recent.isNotEmpty()) {
                item {
                    SectionHeader("Escuchado recientemente")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recent, key = { it.id }) { track ->
                            CompactTrackCard(
                                track = track,
                                onClick = {
                                    vm.play(track)
                                    onOpenPlayer()
                                }
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Hecho para ti") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hero != null) {
                        item {
                            MixCard(
                                track = hero,
                                title = "Mezcla Vibe",
                                subtitle = "Todo tu sonido, mezclado",
                                onClick = { vm.playQueue(tracks, shuffled = true) }
                            )
                        }
                    }
                    if (favorites.isNotEmpty()) {
                        item {
                            val first = favorites.first()
                            MixCard(
                                track = first,
                                title = "Tus favoritas",
                                subtitle = "${favorites.size} ${if (favorites.size == 1) "canción que te gusta" else "canciones que te gustan"}",
                                onClick = { vm.playQueue(favorites, shuffled = true) }
                            )
                        }
                    }
                    demos.getOrNull(2)?.let { demo ->
                        item {
                            MixCard(
                                track = demo,
                                title = "Sesión sin conexión",
                                subtitle = "Muestras originales VibeMusic",
                                onClick = { vm.playQueue(demos, shuffled = true) }
                            )
                        }
                    }
                }
            }

            if (albums.isNotEmpty()) {
                item { SectionHeader("Álbumes en tu biblioteca") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(albums, key = { it.name }) { album ->
                            AlbumCard(
                                album = album,
                                onClick = { onOpenAlbum(album.name) },
                                modifier = Modifier.width(150.dp)
                            )
                        }
                    }
                }
            }

            if (demos.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Muestras originales",
                        action = "Ver todo",
                        onAction = { vm.playQueue(demos, shuffled = false) }
                    )
                }
                items(demos.take(4), key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isFavorite = track.id in favoritesIds,
                        isCurrent = player.mediaId == track.id,
                        isPlaying = player.isPlaying,
                        onClick = {
                            vm.playFromList(demos, track)
                            onOpenPlayer()
                        },
                        onFavorite = { vm.toggleFavorite(track) },
                        onMore = { vm.playNext(track) }
                    )
                }
            }

            item {
                Text(
                    text = "Hecho a mano con VibeMusic · audio original libre de regalías",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(onOpenSearch: () -> Unit, onOpenSettings: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VIBE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = VibeCream
                )
                Box(
                    Modifier
                        .padding(start = 4.dp, top = 6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(VibeRed)
                )
                Text(
                    text = "MUSIC",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 3.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = VibeCream)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Ajustes", tint = VibeCream)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = greeting(),
            style = MaterialTheme.typography.headlineLarge,
            color = VibeCream
        )
        Text(
            text = "Tu música. Tu ambiente. Sin ruido.",
            style = MaterialTheme.typography.bodyMedium,
            color = VibeMuted
        )
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}

@Composable
private fun HeroCard(hero: Track, trackCount: Int, onPlay: () -> Unit) {
    val palette = trackPalette(hero)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .height(188.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(palette[1], palette[2], Color(0xFF0A0A0D))
                )
            )
            .clickable(onClick = onPlay)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(palette[0].copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.15f, 0f),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.15f, 0f)
            )
        }
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .fillMaxWidth(0.62f)
        ) {
            Text(
                text = "SESIÓN VIBE",
                style = MaterialTheme.typography.labelSmall,
                color = VibeRed,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tu banda sonora\nde hoy",
                style = MaterialTheme.typography.headlineMedium,
                color = VibeCream,
                lineHeight = 30.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$trackCount ${if (trackCount == 1) "canción" else "canciones"} · mezcla automática",
                style = MaterialTheme.typography.bodySmall,
                color = VibeCream.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = VibeCream
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Reproducir",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black
                    )
                }
            }
        }
        CoverArt(
            track = hero,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(134.dp),
            corner = 18.dp
        )
    }
}
