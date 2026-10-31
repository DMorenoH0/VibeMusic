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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.BuildConfig
import com.vibemusic.app.ui.components.SectionHeader
import com.vibemusic.app.ui.components.StatCard
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.ui.theme.VibeSurfaceHigh
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun SettingsScreen(
    vm: VibeViewModel,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()
    val amoled by vm.amoled.collectAsStateWithLifecycle()
    val autoScan by vm.autoScan.collectAsStateWithLifecycle()
    val wifiOnly by vm.wifiOnly.collectAsStateWithLifecycle()
    val sleepMinutes by vm.sleepMinutes.collectAsStateWithLifecycle()

    var confirmClearHistory by remember { mutableStateOf(false) }
    val permissionGranted = vm.hasAudioPermission()

    VibePageBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = VibeCream
                        )
                    }
                    Text(
                        "Ajustes",
                        style = MaterialTheme.typography.headlineLarge,
                        color = VibeCream
                    )
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        icon = Icons.Rounded.MusicNote,
                        value = tracks.size.toString(),
                        label = "Canciones",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Rounded.QueueMusic,
                        value = playlists.size.toString(),
                        label = "Listas",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Rounded.Download,
                        value = downloads.size.toString(),
                        label = "Descargas",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { SectionHeader("Apariencia") }
            item {
                SettingsCard {
                    ToggleRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "Fondo negro puro",
                        subtitle = "Ahorra batería en pantallas OLED.",
                        checked = amoled,
                        onCheckedChange = vm::setAmoled
                    )
                }
            }

            item { SectionHeader("Biblioteca") }
            item {
                SettingsCard {
                    ToggleRow(
                        icon = Icons.Rounded.LibraryMusic,
                        title = "Escanear al abrir",
                        subtitle = "Busca música nueva del dispositivo al iniciar.",
                        checked = autoScan,
                        onCheckedChange = vm::setAutoScan
                    )
                    SectionDivider()
                    ActionRow(
                        icon = if (permissionGranted) Icons.Rounded.CheckCircle else Icons.Rounded.FolderOpen,
                        title = "Permiso de audio",
                        subtitle = if (permissionGranted) {
                            "Concedido · la biblioteca local está disponible"
                        } else {
                            "Necesario para leer tus canciones locales"
                        },
                        tint = if (permissionGranted) VibeCream else VibeRed,
                        onClick = {
                            if (permissionGranted) onOpenAppSettings() else onRequestPermission()
                        }
                    )
                    SectionDivider()
                    ActionRow(
                        icon = Icons.Rounded.FolderOpen,
                        title = "Volver a escanear",
                        subtitle = when (val state = scanState) {
                            is com.vibemusic.app.data.ScanState.Scanning -> "Escaneando…"
                            is com.vibemusic.app.data.ScanState.Ready ->
                                "${state.count} ${if (state.count == 1) "canción local encontrada" else "canciones locales encontradas"}"
                            is com.vibemusic.app.data.ScanState.Failed -> state.message
                            else -> "Actualiza la música de tu dispositivo"
                        },
                        onClick = vm::rescanLibrary
                    )
                    SectionDivider()
                    ActionRow(
                        icon = Icons.Rounded.ChevronRight,
                        title = "Permisos de la app",
                        subtitle = "Notificaciones, archivos y otros permisos del sistema",
                        onClick = onOpenAppSettings
                    )
                }
            }

            item { SectionHeader("Descargas") }
            item {
                SettingsCard {
                    ToggleRow(
                        icon = Icons.Rounded.Wifi,
                        title = "Solo Wi-Fi al descargar",
                        subtitle = "Evita consumir datos móviles en descargas.",
                        checked = wifiOnly,
                        onCheckedChange = vm::setWifiOnly
                    )
                    SectionDivider()
                    Text(
                        "Las descargas usan enlaces directos que tú eliges. La app no extrae audio de YouTube ni de catálogos protegidos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item { SectionHeader("Reproducción") }
            item {
                SettingsCard {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = VibeRed,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Temporizador de sueño",
                                style = MaterialTheme.typography.titleMedium,
                                color = VibeCream
                            )
                            Text(
                                when (sleepMinutes) {
                                    null -> "Desactivado"
                                    else -> "El audio se pausará en $sleepMinutes min"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeMuted
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf<Pair<Int?, String>>(
                            null to "Desactivado",
                            15 to "15 min",
                            30 to "30 min",
                            60 to "60 min"
                        ).forEach { (minutes, label) ->
                            val selected = sleepMinutes == minutes
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) VibeRed else VibeSurfaceHigh,
                                modifier = Modifier.clickable { vm.setSleepTimer(minutes) }
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else VibeMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Datos") }
            item {
                SettingsCard {
                    ActionRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Borrar historial",
                        subtitle = "Elimina la lista de escuchado recientemente",
                        tint = VibeRed,
                        onClick = { confirmClearHistory = true }
                    )
                    SectionDivider()
                    ActionRow(
                        icon = Icons.Rounded.CheckCircle,
                        title = "Favoritos guardados",
                        subtitle = "${favorites.size} ${if (favorites.size == 1) "canción" else "canciones"} · ${playlists.size} ${if (playlists.size == 1) "lista" else "listas"}",
                        onClick = {}
                    )
                }
            }

            item { SectionHeader("Acerca de") }
            item {
                SettingsCard {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(VibeRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "V",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "VibeMusic",
                                style = MaterialTheme.typography.titleMedium,
                                color = VibeCream
                            )
                            Text(
                                "Versión ${BuildConfig.VERSION_NAME} · Media3 ExoPlayer",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeMuted
                            )
                        }
                    }
                    HorizontalDivider(color = VibeLine.copy(alpha = 0.6f))
                    Text(
                        "Reproductor local y sin conexión con estética rojo/negro. Las muestras incluidas son instrumentales originales generadas para esta app. No se ofrece un catálogo comercial ilimitado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            containerColor = VibeSurfaceHigh,
            titleContentColor = VibeCream,
            textContentColor = VibeMuted,
            title = { Text("Borrar historial") },
            text = { Text("Se quitarán las canciones de «Escuchado recientemente». Tus favoritos y listas no se tocan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearHistory = false
                        vm.clearHistory()
                    }
                ) { Text("Borrar", color = VibeRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) {
                    Text("Cancelar", color = VibeMuted)
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = VibeSurface.copy(alpha = 0.62f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = VibeLine.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 52.dp)
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = VibeRed, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = VibeCream)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VibeMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VibeRed,
                uncheckedThumbColor = VibeMuted,
                uncheckedTrackColor = VibeSurfaceHigh
            )
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = VibeCream,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (tint == VibeCream) VibeRed else tint, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = tint)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VibeMuted)
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = VibeMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
