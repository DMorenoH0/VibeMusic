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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Verified
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.components.AlbumArt
import com.vibemusic.app.ui.components.ProviderCollectionCard
import com.vibemusic.app.ui.components.SectionHeader
import com.vibemusic.app.ui.components.TrackRow
import com.vibemusic.app.ui.components.formatCompactCount
import com.vibemusic.app.ui.paletteFor
import com.vibemusic.app.ui.theme.VibeBlack
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun AudiusArtistScreen(
    vm: VibeViewModel,
    artistId: String,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenCollection: (String) -> Unit
) {
    val state by vm.audiusArtist.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val detail = state.detail
    val seed = "audius:artist:$artistId"
    val palette = paletteFor(seed)

    LaunchedEffect(artistId) { vm.loadAudiusArtist(artistId) }

    if (detail == null) {
        Box(Modifier.fillMaxSize().background(VibeBlack)) {
            if (state.loading) {
                CircularProgressIndicator(
                    color = VibeRed,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        state.error ?: "No se pudo cargar el artista.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibeMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = VibeRed,
                        onClick = { vm.loadAudiusArtist(artistId) }
                    ) {
                        Text(
                            "Reintentar",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(6.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = VibeCream)
            }
        }
        return
    }

    val artist = detail.artist
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(VibeBlack),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(322.dp)
                    .background(
                        Brush.verticalGradient(listOf(palette[1], palette[2], VibeBlack))
                    )
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(6.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = VibeCream
                    )
                }
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AlbumArt(
                        title = artist.name,
                        artworkUri = artist.artworkUrl,
                        seed = seed,
                        modifier = Modifier
                            .size(132.dp)
                            .clip(CircleShape),
                        corner = 66.dp,
                        showBrand = false,
                        palette = palette
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            artist.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = VibeCream,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (artist.verified) {
                            Spacer(Modifier.width(5.dp))
                            Icon(
                                Icons.Rounded.Verified,
                                contentDescription = "Artista verificado",
                                tint = VibeRed,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    if (!artist.handle.isBlank()) {
                        Text(
                            "@${artist.handle} · Audius",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        buildString {
                            append("${formatCompactCount(artist.followerCount)} seguidores")
                            append(" · ${artist.trackCount} pistas")
                            if (artist.albumCount > 0) append(" · ${artist.albumCount} álbumes")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryPill(
                            icon = Icons.Rounded.PlayArrow,
                            label = "Reproducir",
                            enabled = detail.tracks.isNotEmpty()
                        ) {
                            vm.playFromList(detail.tracks, detail.tracks.first())
                            onOpenPlayer()
                        }
                        SecondaryPill(
                            icon = Icons.Rounded.Shuffle,
                            label = "Mezclar",
                            enabled = detail.tracks.isNotEmpty()
                        ) {
                            vm.playQueue(detail.tracks, shuffled = true)
                            onOpenPlayer()
                        }
                    }
                }
            }
        }

        if (!artist.bio.isNullOrBlank()) {
            item {
                Text(
                    artist.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }

        if (detail.albums.isNotEmpty()) {
            item { SectionHeader("Álbumes") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(detail.albums, key = { it.seed }) { collection ->
                        ProviderCollectionCard(
                            collection = collection,
                            onClick = { onOpenCollection(collection.id) }
                        )
                    }
                }
            }
        }

        if (detail.playlists.isNotEmpty()) {
            item { SectionHeader("Listas") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(detail.playlists, key = { it.seed }) { collection ->
                        ProviderCollectionCard(
                            collection = collection,
                            onClick = { onOpenCollection(collection.id) }
                        )
                    }
                }
            }
        }

        if (detail.tracks.isNotEmpty()) {
            item { SectionHeader("Canciones completas") }
            items(detail.tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    isFavorite = track.id in favoriteIds,
                    isCurrent = player.mediaId == track.id,
                    isPlaying = player.isPlaying,
                    onClick = {
                        vm.playFromList(detail.tracks, track)
                        onOpenPlayer()
                    },
                    onFavorite = { vm.toggleFavorite(track) },
                    onMore = { onTrackMore(track) }
                )
            }
        } else {
            item {
                Text(
                    "Este artista no tiene pistas reproducibles en Audius ahora mismo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }

        item {
            Text(
                "Datos y audio servidos por la API oficial de Audius. Las descargas solo se " +
                    "ofrecen cuando el artista las autoriza.",
                style = MaterialTheme.typography.labelSmall,
                color = VibeMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun PrimaryPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (enabled) VibeRed else VibeRed.copy(alpha = 0.35f),
        onClick = { if (enabled) onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SecondaryPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = VibeCream.copy(alpha = 0.12f),
        onClick = { if (enabled) onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = VibeCream, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = VibeCream, style = MaterialTheme.typography.labelLarge)
        }
    }
}
