package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import com.vibemusic.app.ui.components.CoverArt
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.formatDuration
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.viewmodel.VibeViewModel

/**
 * Samples tab: the original instrumental pieces bundled with VibeMusic. They
 * are ordinary local files, so they play in full, offline and in the
 * background with the same player, queue and notifications as any other
 * local track. Nothing here streams or downloads from YouTube.
 */
@Composable
fun SamplesScreen(
    vm: VibeViewModel,
    onOpenPlayer: () -> Unit
) {
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val demos = remember(tracks) { tracks.filter { it.source == TrackSource.DEMO } }
    val genres = remember(demos) {
        listOf("Todo") + demos.map { it.genre ?: "Otros" }.distinct()
    }
    var selectedGenre by rememberSaveable { mutableStateOf("Todo") }
    val filtered = remember(demos, selectedGenre) {
        if (selectedGenre == "Todo") demos else demos.filter { it.genre == selectedGenre }
    }

    VibePageBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Samples",
                        style = MaterialTheme.typography.headlineLarge,
                        color = VibeCream
                    )
                    Text(
                        "Muestras originales incluidas en la app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibeMuted
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = VibeRed,
                            modifier = Modifier.clickable(enabled = filtered.isNotEmpty()) {
                                filtered.firstOrNull()?.let { first ->
                                    vm.playFromList(filtered, first)
                                    onOpenPlayer()
                                }
                            }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Reproducir todo",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${filtered.size} ${if (filtered.size == 1) "tema" else "temas"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = VibeMuted
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre ->
                        SampleGenreChip(
                            label = genre,
                            selected = genre == selectedGenre,
                            onClick = { selectedGenre = genre }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.PlayArrow,
                        title = "Sin muestras",
                        message = "Las muestras se instalan al abrir la app por primera vez. Vuelve a iniciarla si faltan."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { track ->
                    SampleRow(
                        track = track,
                        onClick = {
                            vm.playFromList(filtered, track)
                            onOpenPlayer()
                        }
                    )
                }
            }

            item {
                Text(
                    "Instrumentales originales generadas para VibeMusic. Se guardan en el dispositivo y se " +
                        "reproducen completas, sin conexión y en segundo plano. No proceden de YouTube.",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun SampleRow(track: Track, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            CoverArt(
                track = track,
                modifier = Modifier.size(54.dp),
                corner = 12.dp,
                showBrand = false
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleSmall,
                color = VibeCream,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${track.artist} · ${track.genre ?: "Original"}",
                style = MaterialTheme.typography.bodySmall,
                color = VibeMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = VibeMuted
        )
    }
}

@Composable
private fun SampleGenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) VibeRed else VibeSurface,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) VibeCream else VibeMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
