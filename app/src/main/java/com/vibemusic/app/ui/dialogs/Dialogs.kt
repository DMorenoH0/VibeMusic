package com.vibemusic.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.RadioButton
import com.vibemusic.app.data.db.PlaylistWithCount
import com.vibemusic.app.data.download.DownloadProgress
import com.vibemusic.app.data.download.DownloadQuality
import com.vibemusic.app.data.download.DownloadRequest
import com.vibemusic.app.data.download.DownloadStatus
import com.vibemusic.app.data.download.SongDownloadAvailability
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import com.vibemusic.app.ui.components.AlbumArt
import com.vibemusic.app.ui.components.CoverArt
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.ui.theme.VibeSurfaceHigh
import com.vibemusic.app.viewmodel.TransferState
import java.util.Locale

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = "Nueva lista",
    initialName: String = "",
    confirmLabel: String = "Crear"
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VibeSurfaceHigh,
        titleContentColor = VibeCream,
        textContentColor = VibeMuted,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                singleLine = true,
                label = { Text("Nombre de la lista") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = vibeTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text(confirmLabel, color = if (name.isNotBlank()) VibeRed else VibeMuted) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = VibeMuted) }
        }
    )
}

@Composable
fun DownloadDialog(
    onDismiss: () -> Unit,
    onDownload: (DownloadRequest) -> Unit,
    initialQuality: DownloadQuality = DownloadQuality.HIGH,
    initialWifiOnly: Boolean = true
) {
    var url by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var qualityName by rememberSaveable { mutableStateOf(initialQuality.name) }
    var wifiOnly by rememberSaveable { mutableStateOf(initialWifiOnly) }
    val quality = DownloadQuality.byName(qualityName)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VibeSurfaceHigh,
        titleContentColor = VibeCream,
        textContentColor = VibeMuted,
        title = { Text("Añadir enlace autorizado") },
        text = {
            Column(
                Modifier
                    .height(440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Para canciones con fuente autorizada usa «Descargar canción» en su menú: " +
                        "se descargan con un solo toque, con portada y metadatos. " +
                        "Este formulario es solo para un enlace https directo que tengas derecho a usar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    label = { Text("URL https://…") },
                    colors = vibeTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("Título (opcional)") },
                    colors = vibeTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    singleLine = true,
                    label = { Text("Artista (opcional)") },
                    colors = vibeTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "CALIDAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeMuted
                )
                DownloadQuality.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { qualityName = option.name }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = quality == option,
                            onClick = { qualityName = option.name }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(option.label, color = VibeCream, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                option.explanation,
                                color = VibeMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text("CONEXIÓN", style = MaterialTheme.typography.labelSmall, color = VibeMuted)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { wifiOnly = true }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = wifiOnly, onClick = { wifiOnly = true })
                    Text("Solo Wi-Fi", color = VibeCream, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { wifiOnly = false }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = !wifiOnly, onClick = { wifiOnly = false })
                    Text("Wi-Fi y datos móviles", color = VibeCream, style = MaterialTheme.typography.bodyMedium)
                }
                if (!wifiOnly) {
                    Text(
                        "La descarga mediante datos móviles puede consumir tu tarifa de datos y generar cargos adicionales según tu operador.",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeRed
                    )
                }
                Text(
                    "Descarga solo contenido que tengas derecho a usar. VibeMusic no aloja audio ni extrae catálogos de plataformas de streaming.",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeMuted
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDownload(
                        DownloadRequest(
                            url = url.trim(),
                            title = title.trim(),
                            artist = artist.trim(),
                            quality = quality,
                            wifiOnly = wifiOnly
                        )
                    )
                },
                enabled = url.isNotBlank()
            ) { Text("Descargar", color = if (url.isNotBlank()) VibeRed else VibeMuted) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = VibeMuted) }
        }
    )
}

@Composable
fun DownloadProgressDialog(
    progress: DownloadProgress?,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    if (progress == null) return
    val active = progress.isActive
    AlertDialog(
        onDismissRequest = { if (!active) onDismiss() },
        containerColor = VibeSurfaceHigh,
        titleContentColor = VibeCream,
        textContentColor = VibeMuted,
        title = {
            Text(
                when (progress.status) {
                    DownloadStatus.FINISHED -> "Descarga completada"
                    DownloadStatus.FAILED -> "Descarga interrumpida"
                    DownloadStatus.CANCELLED -> "Descarga cancelada"
                    else -> "Descargando audio"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(
                        title = progress.title.ifBlank { "Audio" },
                        artworkUri = progress.artworkUrl,
                        seed = progress.id,
                        modifier = Modifier.size(64.dp),
                        corner = 12.dp,
                        showBrand = false
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            progress.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = VibeCream,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            progress.artist.ifBlank { "VibeMusic" },
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (active) {
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = VibeRed,
                        trackColor = VibeSurface
                    )
                    Text(
                        "${progress.percent} % · ${formatBytes(progress.bytes)} de ${formatBytes(progress.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${formatBytes(progress.speedBytesPerSecond)}/s",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted
                        )
                        Text(
                            formatEta(progress.etaMs)?.let { "Quedan $it" } ?: "Calculando…",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted
                        )
                    }
                }

                progress.error?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "La parte descargada se conserva: al reintentar se reanuda si el servidor lo permite.",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted
                    )
                }

                if (progress.status == DownloadStatus.FINISHED) {
                    Text(
                        "${progress.displayName} ya está en tu biblioteca y se puede reproducir sin Internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeCream
                    )
                }
            }
        },
        confirmButton = {
            when {
                active -> TextButton(onClick = onCancel) { Text("Cancelar", color = VibeRed) }
                progress.status == DownloadStatus.FAILED ->
                    TextButton(onClick = onRetry) { Text("Reintentar", color = VibeRed) }
                else -> TextButton(onClick = onDismiss) { Text("Cerrar", color = VibeRed) }
            }
        },
        dismissButton = if (active) {
            null
        } else {
            { TextButton(onClick = onDismiss) { Text("Cerrar", color = VibeMuted) } }
        }
    )
}

private fun formatBytes(bytes: Long): String {
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

private fun formatEta(etaMs: Long?): String? {
    if (etaMs == null || etaMs <= 0L) return null
    val seconds = (etaMs / 1000L).coerceAtLeast(1L)
    val minutes = seconds / 60L
    val rest = seconds % 60L
    return if (minutes > 0) "$minutes min $rest s" else "$rest s"
}

@Composable
fun TransferDialog(
    transfer: TransferState,
    onDismiss: () -> Unit
) {
    if (!transfer.visible) return
    AlertDialog(
        onDismissRequest = { if (!transfer.isWorking) onDismiss() },
        containerColor = VibeSurfaceHigh,
        titleContentColor = VibeCream,
        textContentColor = VibeMuted,
        title = {
            Text(
                when {
                    transfer.isWorking -> "Descargando audio"
                    transfer.error != null -> "No se pudo completar"
                    else -> "Listo"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    transfer.isWorking -> {
                        LinearProgressIndicator(
                            progress = { transfer.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = VibeRed,
                            trackColor = VibeSurface
                        )
                        Text(
                            "Progreso: ${(transfer.progress.coerceIn(0f, 1f) * 100).toInt()} %",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                        Text(
                            "Mantén la app abierta y no bloquees la pantalla.",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted
                        )
                    }

                    transfer.error != null -> {
                        Text(
                            transfer.error.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> {
                        Text(
                            "Guardado: ${transfer.finishedTitle ?: "archivo de audio"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeCream
                        )
                        Text(
                            "Ya está disponible en Biblioteca › Descargas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !transfer.isWorking
            ) { Text("Cerrar", color = if (!transfer.isWorking) VibeRed else VibeMuted) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionSheet(
    track: Track,
    playlists: List<PlaylistWithCount>,
    isFavorite: Boolean,
    playlistId: Long?,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onRemoveFromPlaylist: (Long) -> Unit,
    onDeleteDownload: () -> Unit,
    onNewPlaylist: () -> Unit,
    onOpenAlbum: (() -> Unit)?,
    onOpenArtist: (() -> Unit)? = null,
    download: SongDownloadAvailability,
    onDownload: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VibeSurfaceHigh,
        contentColor = VibeCream,
        dragHandle = { BottomSheetDefaults.DragHandle(color = VibeLine) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(track = track, modifier = Modifier.size(58.dp), corner = 12.dp, showBrand = false)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = VibeLine.copy(alpha = 0.6f))

            SheetAction(Icons.Rounded.PlayArrow, "Reproducir ahora", onClick = onPlay)
            SheetAction(Icons.Rounded.SkipNext, "Reproducir a continuación", onClick = onPlayNext)
            SheetAction(
                Icons.Rounded.PlaylistAdd,
                "Añadir a la cola",
                note = "Se añade al final, sin interrumpir lo que suena.",
                onClick = onAddToQueue
            )
            SheetAction(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (isFavorite) {
                    "Eliminar de Canciones que te gustan"
                } else {
                    "Añadir a Canciones que te gustan"
                },
                tint = if (isFavorite) VibeRed else VibeCream,
                onClick = onToggleFavorite
            )
            if (onOpenAlbum != null) {
                SheetAction(Icons.Rounded.Album, "Ver álbum", onClick = onOpenAlbum)
            }
            if (onOpenArtist != null) {
                SheetAction(Icons.Rounded.Person, "Ver artista", onClick = onOpenArtist)
            }
            when (download) {
                is SongDownloadAvailability.Available -> SheetAction(
                    Icons.Rounded.Download,
                    download.label,
                    tint = VibeRed,
                    note = "Un toque: portada y metadatos originales, disponible sin conexión.",
                    onClick = onDownload
                )

                is SongDownloadAvailability.Downloaded -> SheetStatus(
                    Icons.Rounded.CheckCircle,
                    download.label,
                    "Puedes reproducirla sin Internet desde Descargas.",
                    tint = VibeRed
                )

                is SongDownloadAvailability.Unavailable -> SheetStatus(
                    Icons.Rounded.CloudOff,
                    download.label,
                    download.reason,
                    tint = VibeMuted
                )
            }
            if (playlistId != null) {
                SheetAction(
                    Icons.Rounded.Delete,
                    "Quitar de esta lista",
                    tint = VibeRed,
                    onClick = { onRemoveFromPlaylist(playlistId) }
                )
            }
            if (track.source == TrackSource.DOWNLOAD || track.source == TrackSource.IMPORTED) {
                SheetAction(
                    Icons.Rounded.Delete,
                    "Eliminar archivo descargado",
                    tint = VibeRed,
                    onClick = onDeleteDownload
                )
            }

            HorizontalDivider(color = VibeLine.copy(alpha = 0.6f))

            Text(
                "AÑADIR A UNA LISTA",
                style = MaterialTheme.typography.labelSmall,
                color = VibeMuted,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
            )
            SheetAction(Icons.Rounded.Add, "Nueva lista", onClick = onNewPlaylist)
            playlists.forEach { item ->
                SheetAction(
                    Icons.Rounded.QueueMusic,
                    item.playlist.name,
                    onClick = { onAddToPlaylist(item.playlist.id) }
                )
            }
            if (playlists.isEmpty()) {
                Text(
                    "Todavía no tienes listas. Crea la primera con «Nueva lista».",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = VibeCream,
    note: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
            if (!note.isNullOrBlank()) {
                Text(
                    note,
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeMuted
                )
            }
        }
    }
}

@Composable
private fun SheetStatus(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    note: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
            Text(
                note,
                style = MaterialTheme.typography.labelSmall,
                color = VibeMuted
            )
        }
    }
}

@Composable
private fun vibeTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VibeRed,
    unfocusedBorderColor = VibeLine,
    focusedContainerColor = VibeSurface,
    unfocusedContainerColor = VibeSurface,
    cursorColor = VibeRed,
    focusedTextColor = VibeCream,
    unfocusedTextColor = VibeCream,
    focusedLabelColor = VibeRed,
    unfocusedLabelColor = VibeMuted
)
