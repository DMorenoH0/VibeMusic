package com.vibemusic.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.vibemusic.app.ui.components.CoverArt
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.ErrorBanner
import com.vibemusic.app.ui.formatDuration
import com.vibemusic.app.ui.theme.AccentPalettes
import com.vibemusic.app.ui.theme.VibeBlack
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.trackPalette
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun NowPlayingScreen(
    vm: VibeViewModel,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onTrackMore: (com.vibemusic.app.data.model.Track) -> Unit
) {
    val track by vm.currentTrack.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val queue by vm.queue.collectAsStateWithLifecycle()

    val current = track
    if (current == null) {
        NowPlayingEmpty(onBack = onBack)
        return
    }

    val palette = remember(current.id) { trackPalette(current) }
    val transition = rememberInfiniteTransition(label = "now_playing_glow")
    val pulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    val glowAlpha = if (state.isPlaying) 0.22f + pulse * 0.30f else 0.18f

    Box(
        Modifier
            .fillMaxSize()
            .background(VibeBlack)
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette[1].copy(alpha = 0.62f),
                        VibeBlack,
                        Color.Black
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette[0].copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.75f, size.height * 0.06f),
                    radius = size.width * 0.9f
                ),
                radius = size.width * 0.9f,
                center = Offset(size.width * 0.75f, size.height * 0.06f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette[0].copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.08f, size.height * 0.78f),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.08f, size.height * 0.78f)
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val coverSize = minOf(maxWidth * 0.72f, 320.dp)
            val scroll = rememberScrollState()

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Cerrar reproductor",
                            tint = VibeCream,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        "REPRODUCIENDO",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted,
                        letterSpacing = 1.6.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onTrackMore(current) }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Más opciones",
                            tint = VibeCream
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(coverSize * 1.28f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(palette[0].copy(alpha = glowAlpha), Color.Transparent)
                                )
                            )
                    )
                    CoverArt(
                        track = current,
                        modifier = Modifier
                            .size(coverSize)
                            .shadow(24.dp, RoundedCornerShape(28.dp)),
                        corner = 28.dp
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = VibeCream,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = current.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VibeMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(22.dp))

                var dragging by remember { mutableStateOf(false) }
                var dragValue by remember { mutableFloatStateOf(0f) }
                Slider(
                    value = if (dragging) dragValue else state.progress,
                    onValueChange = {
                        dragging = true
                        dragValue = it
                    },
                    onValueChangeFinished = {
                        vm.seekToFraction(dragValue)
                        dragging = false
                    },
                    enabled = state.durationMs > 0,
                    colors = vibeSliderColors()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatDuration(
                            if (dragging && state.durationMs > 0) {
                                (state.durationMs * dragValue).toLong()
                            } else {
                                state.positionMs
                            }
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = VibeMuted
                    )
                    Text(
                        formatDuration(state.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = VibeMuted
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = vm::toggleShuffle) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = if (state.shuffle) VibeRed else VibeCream,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = vm::previous, modifier = Modifier.size(52.dp)) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = VibeCream,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Surface(
                        onClick = vm::togglePlayPause,
                        shape = CircleShape,
                        color = VibeRed,
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(18.dp, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (state.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }
                    IconButton(onClick = vm::next, modifier = Modifier.size(52.dp)) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Siguiente",
                            tint = VibeCream,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    IconButton(onClick = vm::cycleRepeat) {
                        val repeatOn = state.repeatMode != Player.REPEAT_MODE_OFF
                        Icon(
                            imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                                Icons.Rounded.RepeatOne
                            } else {
                                Icons.Rounded.Repeat
                            },
                            contentDescription = "Repetir",
                            tint = if (repeatOn) VibeRed else VibeCream,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { vm.toggleFavorite(current) }) {
                        Icon(
                            imageVector = if (current.id in favoriteIds) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Rounded.FavoriteBorder
                            },
                            contentDescription = "Favorito",
                            tint = if (current.id in favoriteIds) VibeRed else VibeCream,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Surface(
                        onClick = onOpenQueue,
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.07f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = VibeCream,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = "${queue.size} en cola",
                                style = MaterialTheme.typography.labelLarge,
                                color = VibeCream
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            vm.setVolume(if (state.volume > 0.01f) 0f else 1f)
                        }
                    ) {
                        Icon(
                            imageVector = if (state.volume > 0.01f) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = "Silenciar",
                            tint = VibeCream,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = VibeMuted,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = state.volume,
                        onValueChange = vm::setVolume,
                        colors = vibeSliderColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${(state.volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted,
                        modifier = Modifier.width(34.dp),
                        textAlign = TextAlign.End
                    )
                }

                state.error?.let { message ->
                    ErrorBanner(
                        message = message,
                        onRetry = vm::retryPlayer,
                        onDismiss = vm::clearPlayerError
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun NowPlayingEmpty(onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(VibeBlack)
            .background(
                Brush.verticalGradient(
                    listOf(AccentPalettes[0][1].copy(alpha = 0.35f), VibeBlack, Color.Black)
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(6.dp)) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Cerrar reproductor",
                    tint = VibeCream,
                    modifier = Modifier.size(30.dp)
                )
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "Nada reproduciendo",
                    message = "Elige una canción de tu biblioteca o de las muestras originales para empezar.",
                    actionLabel = "Elegir música",
                    onAction = onBack
                )
            }
        }
    }
}

@Composable
private fun vibeSliderColors() = SliderDefaults.colors(
    thumbColor = VibeRed,
    activeTrackColor = VibeRed,
    inactiveTrackColor = VibeLine,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
    disabledThumbColor = VibeMuted,
    disabledActiveTrackColor = VibeLine,
    disabledInactiveTrackColor = VibeLine
)
