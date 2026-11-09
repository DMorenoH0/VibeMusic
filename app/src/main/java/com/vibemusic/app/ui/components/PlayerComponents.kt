package com.vibemusic.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.playback.PlayerUiState
import com.vibemusic.app.ui.theme.VibeBlackSoft
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed

@Composable
fun MiniPlayer(
    track: Track?,
    state: PlayerUiState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (track == null) return@AnimatedVisibility
        Column(
            Modifier
                .fillMaxWidth()
                .background(VibeBlackSoft)
                .clickable(onClick = onOpen)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(VibeRed)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(track = track, modifier = Modifier.size(46.dp), corner = 10.dp, showBrand = false)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(42.dp)) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = VibeRed,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Siguiente",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VibeBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(BottomBarBackground)
            .navigationBarsPadding()
            .height(60.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarTab(
            label = "Home",
            selected = selected == 0,
            selectedIcon = Icons.Rounded.Home,
            unselectedIcon = Icons.Outlined.Home,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f)
        )
        BottomBarTab(
            label = "Samples",
            selected = selected == 1,
            selectedIcon = Icons.Rounded.PlayArrow,
            unselectedIcon = Icons.Outlined.PlayArrow,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f)
        )
        BottomBarTab(
            label = "Explore",
            selected = selected == 2,
            selectedIcon = Icons.Rounded.Explore,
            unselectedIcon = Icons.Outlined.Explore,
            onClick = { onSelect(2) },
            modifier = Modifier.weight(1f)
        )
        BottomBarTab(
            label = "Library",
            selected = selected == 3,
            selectedIcon = Icons.Rounded.LibraryMusic,
            unselectedIcon = Icons.Outlined.LibraryMusic,
            onClick = { onSelect(3) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Minimal YouTube Music style tab: a centred icon over a small label, filled
 * white when active and outlined grey otherwise, with a short colour and
 * scale transition. The four tabs share the width evenly on any screen size.
 */
@Composable
private fun BottomBarTab(
    label: String,
    selected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) BottomBarSelected else BottomBarUnselected,
        animationSpec = tween(durationMillis = 180),
        label = "tabIconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) BottomBarSelected else BottomBarUnselected,
        animationSpec = tween(durationMillis = 180),
        label = "tabLabelColor"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "tabIconScale"
    )

    Column(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .scale(iconScale)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.2.sp,
            maxLines = 1
        )
    }
}

private val BottomBarBackground = Color(0xFF171717)
private val BottomBarSelected = Color.White
private val BottomBarUnselected = Color(0xFFB3B3B3)

