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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.components.AlbumArt
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
fun AudiusCollectionScreen(
    vm: VibeViewModel,
    collectionId: String,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenArtist: (String) -> Unit
) {
    val state by vm.audiusCollection.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val detail = state.detail
    val seed = "audius:collection:$collectionId"
    val palette = paletteFor(seed)

    LaunchedEffect(collectionId) { vm.loadAudiusCollection(collectionId) }

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
                        state.error ?: "No se pudo cargar la lista.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibeMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = VibeRed,
                        onClick = { vm.loadAudiusCollection(collectionId) }
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

    val collection = detail.collection
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
                    .height(300.dp)
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
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AlbumArt(
                        title = collection.name,
                        artworkUri = collection.artworkUrl,
                        seed = seed,
                        modifier = Modifier.size(124.dp),
                        corner = 16.dp,
                        showBrand = false,
                        palette = palette
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            collection.typeLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeRed
                        )
                        Text(
                            collection.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = VibeCream,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            collection.creator,
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeCream,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            buildString {
                                append("${collection.trackCount} pistas")
                                if (collection.totalPlayCount > 0) {
                                    append(" · ${formatCompactCount(collection.totalPlayCount)} reproducciones")
                                }
                                append(" · Audius")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted
                        )
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CollectionPill(
                    icon = Icons.Rounded.PlayArrow,
                    label = "Reproducir",
                    enabled = detail.tracks.isNotEmpty()
                ) {
                    vm.playFromList(detail.tracks, detail.tracks.first())
                    onOpenPlayer()
                }
                CollectionPill(
                    icon = Icons.Rounded.Shuffle,
                    label = "Mezclar",
                    enabled = detail.tracks.isNotEmpty()
                ) {
                    vm.playQueue(detail.tracks, shuffled = true)
                    onOpenPlayer()
                }
                Spacer(Modifier.weight(1f))
                val creatorId = collection.creatorId
                if (!creatorId.isNullOrBlank()) {
                    Text(
                        text = "Ver artista",
                        style = MaterialTheme.typography.labelLarge,
                        color = VibeRed,
                        modifier = Modifier
                            .clickable { onOpenArtist(creatorId) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        if (!collection.description.isNullOrBlank()) {
            item {
                Text(
                    collection.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }

        if (detail.tracks.isEmpty()) {
            item {
                Text(
                    "Esta lista no tiene pistas reproducibles ahora mismo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        } else {
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
        }

        if (detail.related.isNotEmpty()) {
            item { SectionHeader("También te puede gustar") }
            items(detail.related, key = { "related:${it.id}" }) { track ->
                TrackRow(
                    track = track,
                    isFavorite = track.id in favoriteIds,
                    isCurrent = player.mediaId == track.id,
                    isPlaying = player.isPlaying,
                    onClick = {
                        vm.playFromList(detail.related, track)
                        onOpenPlayer()
                    },
                    onFavorite = { vm.toggleFavorite(track) },
                    onMore = { onTrackMore(track) }
                )
            }
        }

        item {
            Text(
                "Audio servido por Audius. VibeMusic no omite condiciones de acceso ni " +
                    "extrae audio de otras plataformas.",
                style = MaterialTheme.typography.labelSmall,
                color = VibeMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun CollectionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = if (enabled) VibeRed else VibeRed.copy(alpha = 0.35f),
        onClick = { if (enabled) onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}
