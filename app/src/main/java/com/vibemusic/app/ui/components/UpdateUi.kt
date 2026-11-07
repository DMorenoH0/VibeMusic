package com.vibemusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.ui.theme.VibeSurfaceHigh
import com.vibemusic.app.viewmodel.UpdateUiState

@Composable
fun UpdateBanner(
    installedVersion: String,
    latestVersion: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x22FF3B30))
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VibeRed.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = VibeRed, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Actualización disponible",
                style = MaterialTheme.typography.titleSmall,
                color = VibeCream,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "v$latestVersion · tienes la v$installedVersion",
                style = MaterialTheme.typography.bodySmall,
                color = VibeMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Descartar aviso", tint = VibeMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun UpdateDialog(
    installedVersion: String,
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRelease: (String) -> Unit
) {
    val release = state.release ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VibeSurfaceHigh,
        titleContentColor = VibeCream,
        textContentColor = VibeMuted,
        title = {
            Column {
                Text("Actualización disponible")
                Text(
                    "v${release.version} · instalada v$installedVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted
                )
            }
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Novedades",
                    style = MaterialTheme.typography.titleSmall,
                    color = VibeCream
                )
                Text(
                    release.notes.ifBlank { "Esta versión incluye mejoras y correcciones." }.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted
                )
                if (release.apkSize > 0) {
                    Text(
                        "APK · ${"%.1f".format(release.apkSize / 1024f / 1024f)} MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted
                    )
                }
                if (release.sha256 != null) {
                    Text(
                        "Verificación SHA-256 disponible",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted
                    )
                }
                if (state.download.running) {
                    LinearProgressIndicator(
                        progress = { state.download.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = VibeRed,
                        trackColor = VibeSurface
                    )
                    Text(
                        "Descargando… ${(state.download.progress * 100).toInt()} %",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted
                    )
                }
                state.download.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (state.download.file != null && state.download.error == null) {
                    Text(
                        if (state.download.verified) {
                            "Descarga verificada. Pulsa Instalar y confirma en el sistema."
                        } else {
                            "Descarga completada. Pulsa Instalar y confirma en el sistema."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeCream
                    )
                    if (!state.installPermission) {
                        Text(
                            "Android necesita permiso para instalar apps de esta fuente.",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted
                        )
                        TextButton(onClick = onOpenSettings) {
                            Text("Abrir ajustes de instalación", color = VibeRed)
                        }
                    }
                }
                Text(
                    "La descarga la inicias tú y la instalación se confirma en el diálogo oficial de Android. VibeMusic no instala nada en segundo plano.",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeMuted
                )
            }
        },
        confirmButton = {
            when {
                state.download.running -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = VibeRed,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Descargando", color = VibeMuted)
                    }
                }
                state.download.file != null -> {
                    TextButton(onClick = onInstall) { Text("Instalar", color = VibeRed) }
                }
                else -> {
                    TextButton(onClick = onDownload) { Text("Descargar APK", color = VibeRed) }
                }
            }
        },
        dismissButton = {
            Row {
                if (!release.htmlUrl.isBlank()) {
                    TextButton(onClick = { onOpenRelease(release.htmlUrl) }) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = VibeMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("GitHub", color = VibeMuted)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cerrar", color = VibeMuted) }
            }
        }
    )
}
