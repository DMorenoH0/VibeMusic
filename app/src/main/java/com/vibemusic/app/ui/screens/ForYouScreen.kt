package com.vibemusic.app.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.recommend.RecommendationSection
import com.vibemusic.app.ui.components.AudiusTrackRail
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.TrackRow
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.viewmodel.VibeViewModel

/**
 * "Para ti": local recommendations computed from playback history, favourites
 * and playlists. Nothing is uploaded; disabling the feature hides every
 * generated section and stops new signals from being stored.
 */
@Composable
fun ForYouScreen(
    vm: VibeViewModel,
    onOpenPlayer: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenSettings: () -> Unit,
    onExplore: () -> Unit
) {
    val sections by vm.forYouSections.collectAsStateWithLifecycle()
    val enabled by vm.recommendationsEnabled.collectAsStateWithLifecycle()
    val audius by vm.audiusDiscovery.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadAudiusDiscovery() }

    VibePageBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item { ForYouHeader(onOpenSettings) }

            if (!enabled) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Shield,
                        title = "Recomendaciones desactivadas",
                        message = "VibeMusic puede crear mezclas con tu historial, favoritos y listas " +
                            "sin enviar nada a Internet. Tú decides si quieres activarlo.",
                        actionLabel = "Activar recomendaciones",
                        onAction = { vm.setRecommendationsEnabled(true) }
                    )
                }
            } else if (sections.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Tu mezcla está tomando forma",
                        message = "Escucha algunas canciones, marca favoritos y crea listas. " +
                            "Las recomendaciones se calculan en este dispositivo.",
                        actionLabel = "Explorar música",
                        onAction = onExplore
                    )
                }
            } else {
                sections.forEach { section ->
                    item(key = "header-${section.id}") {
                        RecommendationHeader(
                            section = section,
                            onPlayAll = {
                                vm.playQueue(section.tracks)
                                onOpenPlayer()
                            }
                        )
                    }
                    items(
                        items = section.tracks.take(5),
                        key = { track -> "${section.id}-${track.id}" }
                    ) { track ->
                        TrackRow(
                            track = track,
                            isFavorite = false,
                            onClick = {
                                vm.playFromList(section.tracks, track)
                                onOpenPlayer()
                            },
                            onMore = { onTrackMore(track) }
                        )
                    }
                }
                item {
                    PrivacyNote()
                }
            }

            item {
                AudiusTrackRail(
                    title = "Descubre en Audius",
                    tracks = audius.tracks,
                    loading = audius.loading,
                    error = audius.error,
                    onRetry = vm::refreshAudiusDiscovery,
                    onPlay = { track ->
                        vm.playFromList(audius.tracks, track)
                        onOpenPlayer()
                    }
                )
            }
        }
    }
}

@Composable
private fun ForYouHeader(onOpenSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Para ti",
                style = MaterialTheme.typography.headlineLarge,
                color = VibeCream
            )
            Text(
                "Mezclas creadas solo para ti, en tu dispositivo",
                style = MaterialTheme.typography.bodySmall,
                color = VibeMuted
            )
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = "Privacidad de las recomendaciones",
                    tint = VibeRed
                )
            }
        }
    }
}

@Composable
private fun RecommendationHeader(
    section: RecommendationSection,
    onPlayAll: () -> Unit
) {
    Column(Modifier.padding(top = 10.dp, bottom = 2.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                section.title,
                style = MaterialTheme.typography.titleLarge,
                color = VibeCream,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = VibeSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onPlayAll)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = VibeCream,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Reproducir",
                        style = MaterialTheme.typography.labelMedium,
                        color = VibeCream
                    )
                }
            }
        }
        Text(
            section.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 22.dp)
            .clip(RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Rounded.Shield,
            contentDescription = null,
            tint = VibeMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "Estas recomendaciones se calculan en tu teléfono con tu historial, " +
                "favoritos y listas. No se envían a ningún servidor ni se usan para " +
                "inferir características personales.",
            style = MaterialTheme.typography.labelSmall,
            color = VibeMuted
        )
    }
}