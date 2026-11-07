package com.vibemusic.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.vibemusic.app.ui.theme.VibeThemeStyle
import com.vibemusic.app.ui.theme.paletteForStyle
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun SettingsScreen(
    vm: VibeViewModel,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLegal: (String) -> Unit,
    onOpenCsv: () -> Unit,
    onOpenUpdate: () -> Unit
) {
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()
    val autoScan by vm.autoScan.collectAsStateWithLifecycle()
    val wifiOnly by vm.wifiOnly.collectAsStateWithLifecycle()
    val sleepMinutes by vm.sleepMinutes.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val profile by vm.googleProfile.collectAsStateWithLifecycle()
    val auth by vm.auth.collectAsStateWithLifecycle()

    var confirmClearHistory by remember { mutableStateOf(false) }
    var showWipe by remember { mutableStateOf(false) }
    var wipeAudio by remember { mutableStateOf(false) }
    var showExportPicker by remember { mutableStateOf(false) }
    var exportPlaylistId by remember { mutableStateOf<Long?>(null) }
    val permissionGranted = vm.hasAudioPermission()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val id = exportPlaylistId
        if (uri != null && id != null) vm.exportPlaylist(id, uri)
        exportPlaylistId = null
    }

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

            // ---------------- Appearance ----------------
            item { SectionHeader("Apariencia") }
            item {
                SettingsCard {
                    Row(
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = VibeRed, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Tema de la aplicación",
                                style = MaterialTheme.typography.titleMedium,
                                color = VibeCream
                            )
                            Text(
                                "El cambio se aplica al instante y se guarda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeMuted
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VibeThemeStyle.values().forEach { style ->
                            ThemePreviewCard(
                                style = style,
                                selected = theme == style,
                                onClick = { vm.setTheme(style) }
                            )
                        }
                    }
                }
            }

            // ---------------- Library ----------------
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

            // ---------------- Downloads ----------------
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

            // ---------------- Playback ----------------
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

            // ---------------- Google account ----------------
            item { SectionHeader("Cuenta de Google") }
            item {
                SettingsCard {
                    if (profile != null) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(VibeSurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = VibeRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    profile?.name?.ifBlank { "Cuenta de Google" } ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VibeCream,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    profile?.email.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VibeMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Sesión guardada en este dispositivo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VibeMuted
                                )
                            }
                        }
                        SectionDivider()
                        ActionRow(
                            icon = Icons.Rounded.Logout,
                            title = "Cerrar sesión",
                            subtitle = "Borra el perfil local. La cuenta de Google no se modifica.",
                            onClick = vm::signOutGoogle
                        )
                    } else {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Inicio de sesión opcional",
                                style = MaterialTheme.typography.titleMedium,
                                color = VibeCream
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Ninguna función de reproducción, biblioteca, listas o búsqueda necesita una cuenta. Si inicias sesión, VibeMusic solo guarda tu nombre, correo e identificador de cuenta; el token no se almacena.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeMuted
                            )
                            Spacer(Modifier.height(12.dp))
                            if (vm.googleConfigured) {
                                Surface(
                                    onClick = { vm.signInWithGoogle() },
                                    shape = RoundedCornerShape(50),
                                    color = VibeRed,
                                    enabled = !auth.loading
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (auth.loading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Rounded.AccountCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Iniciar sesión con Google",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = VibeSurfaceHigh
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            "Configuración pendiente",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = VibeCream
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "El inicio de sesión con Google necesita un ID de cliente OAuth (GOOGLE_WEB_CLIENT_ID). Sin esa configuración el botón permanece desactivado y no se envía ningún dato a Google.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VibeMuted
                                        )
                                    }
                                }
                            }
                            auth.error?.let { error ->
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                TextButton(onClick = vm::clearAuthError) {
                                    Text("Entendido", color = VibeRed)
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- CSV ----------------
            item { SectionHeader("Importar y exportar") }
            item {
                SettingsCard {
                    ActionRow(
                        icon = Icons.Rounded.UploadFile,
                        title = "Importar lista desde CSV",
                        subtitle = "Detecta columnas, previsualiza y evita duplicados.",
                        onClick = onOpenCsv
                    )
                    SectionDivider()
                    ActionRow(
                        icon = Icons.Rounded.FileDownload,
                        title = "Exportar una lista a CSV",
                        subtitle = "Compatible con hojas de cálculo y servicios de migración.",
                        onClick = { showExportPicker = true }
                    )
                    SectionDivider()
                    Text(
                        "La compatibilidad con archivos de TuneMyMusic depende de las columnas que incluya cada exportación. VibeMusic reconoce título, artista, álbum, duración e ISRC y permite reasignar columnas manualmente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // ---------------- Updates ----------------
            item { SectionHeader("Actualizaciones") }
            item {
                SettingsCard {
                    ActionRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = when {
                            update.checking -> "Comprobando actualizaciones…"
                            update.error != null -> "No se pudo comprobar"
                            update.available -> "Actualización disponible v${update.release?.version}"
                            update.upToDate -> "Tienes la última versión"
                            else -> "Buscar actualizaciones"
                        },
                        subtitle = when {
                            update.error != null -> update.error.orEmpty()
                            update.available ->
                                "Instalada v${BuildConfig.VERSION_NAME} · nueva v${update.release?.version}"
                            update.upToDate ->
                                "VibeMusic ${BuildConfig.VERSION_NAME} está al día."
                            else ->
                                "Consulta las versiones estables publicadas en GitHub."
                        },
                        onClick = {
                            if (update.available) onOpenUpdate() else vm.checkForUpdates(true)
                        }
                    )
                    if (update.available) {
                        SectionDivider()
                        ActionRow(
                            icon = Icons.Rounded.Download,
                            title = "Ver novedades y descargar",
                            subtitle = "El APK se verifica con SHA-256 cuando GitHub lo publica.",
                            onClick = onOpenUpdate
                        )
                    }
                    SectionDivider()
                    Text(
                        "Solo se tienen en cuenta versiones estables publicadas. La descarga la inicias tú y la instalación se confirma en el diálogo oficial de Android; nunca se instala en segundo plano.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // ---------------- Privacy ----------------
            item { SectionHeader("Privacidad y datos") }
            item {
                SettingsCard {
                    ActionRow(
                        icon = Icons.Rounded.Security,
                        title = "Política de privacidad",
                        subtitle = "Qué datos se guardan, dónde y con quién se comparten.",
                        onClick = { onOpenLegal("privacy") }
                    )
                    SectionDivider()
                    ActionRow(
                        icon = Icons.Rounded.Description,
                        title = "Términos de servicio",
                        subtitle = "Condiciones de uso de VibeMusic.",
                        onClick = { onOpenLegal("terms") }
                    )
                    SectionDivider()
                    ActionRow(
                        icon = Icons.Rounded.Info,
                        title = "Política de cookies",
                        subtitle = "La app Android no usa cookies ni rastreadores.",
                        onClick = { onOpenLegal("cookies") }
                    )
                    SectionDivider()
                    ActionRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Eliminar mis datos",
                        subtitle = "Historial, listas, favoritos, preferencias y caché.",
                        tint = VibeRed,
                        onClick = { showWipe = true }
                    )
                    SectionDivider()
                    ActionRow(
                        icon = Icons.Rounded.CheckCircle,
                        title = "Borrar historial de reproducción",
                        subtitle = "Elimina la lista de escuchado recientemente",
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

            // ---------------- About ----------------
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
                        "Reproductor local con buscador de catálogo, bibliotecas separadas, CSV, temas y actualizaciones verificadas. Las muestras incluidas son instrumentales originales generadas para esta app. No se ofrece un catálogo comercial ilimitado.",
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

    if (showWipe) {
        AlertDialog(
            onDismissRequest = { showWipe = false },
            containerColor = VibeSurfaceHigh,
            titleContentColor = VibeCream,
            textContentColor = VibeMuted,
            title = { Text("Eliminar mis datos") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Se eliminarán de este dispositivo:",
                        style = MaterialTheme.typography.titleSmall,
                        color = VibeCream
                    )
                    WipeBullet("Historial de búsquedas e historial de reproducción.")
                    WipeBullet("Favoritos y listas de reproducción guardadas en VibeMusic.")
                    WipeBullet("Preferencias, tema visual, temporizador y estado del aviso de actualización.")
                    WipeBullet("Perfil de Google guardado localmente y caché interna de la app.")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "No se eliminarán:",
                        style = MaterialTheme.typography.titleSmall,
                        color = VibeCream
                    )
                    WipeBullet("Tus archivos musicales originales del dispositivo.")
                    WipeBullet("Tu cuenta de Google ni los datos que Google conserva. Para borrarlos, usa tu cuenta de Google o cierra sesión antes.")
                    WipeBullet("Datos de Deezer o GitHub: no tienes una cuenta de VibeMusic en esos servicios.")
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { wipeAudio = !wipeAudio },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = wipeAudio,
                            onCheckedChange = { wipeAudio = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = VibeRed,
                                uncheckedColor = VibeMuted,
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            "Borrar también las copias de audio descargadas o importadas por VibeMusic",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeCream
                        )
                    }
                    Text(
                        "Esta opción no toca archivos que no hayan sido creados por la app.",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipe = false
                        vm.wipeAllData(wipeAudio)
                        wipeAudio = false
                    }
                ) { Text("Eliminar datos", color = VibeRed) }
            },
            dismissButton = {
                TextButton(onClick = { showWipe = false }) {
                    Text("Cancelar", color = VibeMuted)
                }
            }
        )
    }

    if (showExportPicker) {
        AlertDialog(
            onDismissRequest = { showExportPicker = false },
            containerColor = VibeSurfaceHigh,
            titleContentColor = VibeCream,
            textContentColor = VibeMuted,
            title = { Text("Exportar a CSV") },
            text = {
                if (playlists.isEmpty()) {
                    Text("Crea una lista con canciones antes de exportarla.")
                } else {
                    Column(
                        Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Elige la lista que quieres guardar como archivo CSV:",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                        Spacer(Modifier.height(8.dp))
                        playlists.forEach { item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showExportPicker = false
                                        exportPlaylistId = item.playlist.id
                                        val safeName = item.playlist.name
                                            .replace(Regex("[^A-Za-z0-9 _-]"), "_")
                                            .ifBlank { "VibeMusic" }
                                        exportLauncher.launch("$safeName.csv")
                                    }
                                    .padding(vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = VibeRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.playlist.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = VibeCream,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${item.trackCount} canciones",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VibeMuted
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportPicker = false }) {
                    Text("Cancelar", color = VibeMuted)
                }
            }
        )
    }
}

@Composable
private fun WipeBullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•", color = VibeRed, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = VibeMuted)
    }
}

@Composable
private fun ThemePreviewCard(
    style: VibeThemeStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = paletteForStyle(style)
    Column(
        Modifier
            .width(118.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(82.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.background)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) VibeRed else palette.line,
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Column(Modifier.padding(10.dp)) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(palette.primary)
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.9f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.surfaceHigh)
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.muted.copy(alpha = 0.55f))
                )
            }
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(VibeRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            style.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) VibeRed else VibeCream,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
