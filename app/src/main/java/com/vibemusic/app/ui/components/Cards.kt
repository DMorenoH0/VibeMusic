package com.vibemusic.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibemusic.app.data.ScanState
import com.vibemusic.app.data.model.PlaylistSummary
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.AlbumGroup
import com.vibemusic.app.ui.formatDuration
import com.vibemusic.app.ui.paletteFor
import com.vibemusic.app.ui.theme.VibeBlack
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.ui.theme.VibeSurfaceHigh

@Composable
fun VibePageBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x33FF3B30), Color.Transparent),
                    center = Offset(size.width * 0.82f, -size.height * 0.04f),
                    radius = size.width * 0.9f
                ),
                radius = size.width * 0.9f,
                center = Offset(size.width * 0.82f, -size.height * 0.04f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1A7A1030), Color.Transparent),
                    center = Offset(-size.width * 0.1f, size.height * 0.55f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(-size.width * 0.1f, size.height * 0.55f)
            )
        }
        content()
    }
}

@Composable
fun AlbumCard(
    album: AlbumGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        val cover = album.tracks.firstOrNull()
        if (cover != null) {
            CoverArt(
                track = cover,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                corner = 16.dp
            )
        } else {
            AlbumArt(
                title = album.name,
                artworkUri = album.artworkUri,
                seed = album.seed,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                corner = 16.dp
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.artist} · ${album.tracks.size} ${if (album.tracks.size == 1) "tema" else "temas"}",
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VibeSurface.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(paletteFor(playlist.name)[0], paletteFor(playlist.name)[2])
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = VibeCream, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${playlist.trackCount} ${if (playlist.trackCount == 1) "canción" else "canciones"}",
                style = MaterialTheme.typography.bodySmall,
                color = VibeMuted
            )
        }
    }
}

@Composable
fun PermissionBanner(
    scanState: ScanState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (scanState !is ScanState.PermissionRequired && scanState !is ScanState.Failed) return
    val isPermission = scanState is ScanState.PermissionRequired
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1FFF3B30))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.FolderOpen,
            contentDescription = null,
            tint = VibeRed,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isPermission) "Tu música local, aquí" else "No se pudo leer la biblioteca",
                style = MaterialTheme.typography.titleSmall,
                color = VibeCream
            )
            Text(
                text = if (isPermission) {
                    "Concede acceso al audio para escanear canciones del móvil."
                } else {
                    (scanState as ScanState.Failed).message
                },
                style = MaterialTheme.typography.bodySmall,
                color = VibeMuted
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isPermission) "Permitir" else "Ajustes",
            style = MaterialTheme.typography.labelLarge,
            color = VibeRed,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { if (isPermission) onRequestPermission() else onOpenSettings() }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22FF3B30))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = VibeCream,
            modifier = Modifier.weight(1f)
        )
        if (onRetry != null) {
            Text(
                "Reintentar",
                style = MaterialTheme.typography.labelLarge,
                color = VibeRed,
                modifier = Modifier
                    .clickable(onClick = onRetry)
                    .padding(8.dp)
            )
        }
        if (onDismiss != null) {
            Text(
                "Cerrar",
                style = MaterialTheme.typography.labelMedium,
                color = VibeMuted,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun MixCard(
    track: Track,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(190.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        CoverArt(track = track, modifier = Modifier.size(190.dp), corner = 18.dp, showBrand = false)
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.18f),
                            0.68f to Color.Black.copy(alpha = 0.82f),
                            1f to Color.Black.copy(alpha = 0.97f)
                        )
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = VibeCream,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = VibeCream.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
fun PlaylistHero(
    name: String,
    subtitle: String,
    seed: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(
                Brush.linearGradient(
                    listOf(paletteFor(seed)[0].copy(alpha = 0.85f), paletteFor(seed)[1], VibeBlack)
                )
            )
    ) {
        AlbumArt(
            title = name,
            artworkUri = null,
            seed = seed,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp, top = 20.dp)
                .size(150.dp),
            corner = 18.dp
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                "LISTA DE REPRODUCCIÓN",
                style = MaterialTheme.typography.labelSmall,
                color = VibeRed
            )
            Spacer(Modifier.height(4.dp))
            Text(
                name,
                style = MaterialTheme.typography.headlineMedium,
                color = VibeCream,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.62f)
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = VibeCream.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RowInfo(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = VibeMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = VibeCream, textAlign = TextAlign.End)
    }
}

@Composable
fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VibeSurface.copy(alpha = 0.6f))
            .padding(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = VibeRed, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = VibeCream)
        Text(label, style = MaterialTheme.typography.bodySmall, color = VibeMuted)
    }
}
