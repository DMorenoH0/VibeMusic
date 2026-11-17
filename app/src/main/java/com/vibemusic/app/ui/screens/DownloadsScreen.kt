package com.vibemusic.app.ui.screens

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.TrackRow
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.viewmodel.VibeViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline library of every file VibeMusic downloaded or imported. Shows size
 * and date so a large download can be spotted and deleted; playback works
 * without a network connection because the file already lives on the device.
 */
@Composable
fun DownloadsScreen(
    vm: VibeViewModel,
    onOpenPlayer: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val details by vm.downloadDetails.collectAsStateWithLifecycle()
    val detailById = remember(details) { details.associateBy { it.trackId } }
    val totalBytes = remember(details) { details.sumOf { it.sizeBytes } }

    VibePageBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Volver",
                                tint = VibeCream
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Descargas",
                            style = MaterialTheme.typography.headlineMedium,
                            color = VibeCream
                        )
                        Text(
                            "${downloads.size} ${if (downloads.size == 1) "archivo" else "archivos"} · " +
                                formatSize(totalBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                    }
                    IconButton(onClick = onImport) {
                        Icon(
                            Icons.Rounded.FolderOpen,
                            contentDescription = "Importar archivos",
                            tint = VibeCream
                        )
                    }
                    IconButton(onClick = onDownload) {
                        Icon(
                            Icons.Rounded.CloudDownload,
                            contentDescription = "Descargar enlace",
                            tint = VibeRed
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionChip(
                        icon = Icons.Rounded.Add,
                        label = "Añadir por enlace",
                        onClick = onDownload,
                        modifier = Modifier.weight(1f)
                    )
                    ActionChip(
                        icon = Icons.Rounded.FolderOpen,
                        label = "Importar audio",
                        onClick = onImport,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (downloads.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.FileDownload,
                        title = "Sin descargas todavía",
                        message = "Abre una canción y elige «Descargar canción» para guardarla con su portada " +
                            "y metadatos. Las descargas se reproducen sin conexión.",
                        actionLabel = "Añadir por enlace",
                        onAction = onDownload
                    )
                }
            } else {
                items(downloads, key = { it.id }) { track ->
                    val entity = detailById[track.id]
                    val caption = remember(entity) {
                        if (entity == null) {
                            null
                        } else {
                            "${formatDate(entity.downloadedAt)} · ${formatSize(entity.sizeBytes)}"
                        }
                    }
                    TrackRow(
                        track = track,
                        caption = caption,
                        onClick = {
                            vm.playFromList(downloads, track)
                            onOpenPlayer()
                        },
                        onMore = { onTrackMore(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = VibeSurface,
        modifier = modifier.clip(RoundedCornerShape(50))
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = VibeRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = VibeCream, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return if (index == 0) "$bytes B"
    else String.format(Locale.getDefault(), "%.1f %s", value, units[index])
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Fecha desconocida"
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        .format(Date(timestamp))
}
