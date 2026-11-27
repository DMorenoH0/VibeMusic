package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.playlistimport.ImportStatus
import com.vibemusic.app.data.playlistimport.MatchSensitivity
import com.vibemusic.app.data.playlistimport.PlaylistImportItem
import com.vibemusic.app.ui.components.AlbumArt
import com.vibemusic.app.ui.components.ErrorBanner
import com.vibemusic.app.ui.components.InfoPill
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeGold
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.ui.theme.VibeSurfaceHigh
import com.vibemusic.app.viewmodel.PlaylistImportUiState
import com.vibemusic.app.viewmodel.VibeViewModel
import java.util.Locale

/**
 * Explore → Descargas → Importar playlist.
 *
 * Spotify and YouTube Music are read strictly as metadata through their
 * official APIs. Audio always comes from Audius, so every row shows the real
 * state of that audio: playable, downloadable, already local, doubtful or not
 * available. Nothing is added without a concrete matched recording.
 */
@Composable
fun PlaylistImportScreen(
    vm: VibeViewModel,
    onBack: () -> Unit,
    onOpenPlaylist: (Long) -> Unit
) {
    val state by vm.playlistImport.collectAsStateWithLifecycle()
    val youtubeKeyState by vm.youtubeKeyState.collectAsStateWithLifecycle()
    val spotifyOpen = remember { mutableStateOf(false) }
    val youtubeOpen = remember { mutableStateOf(false) }
    var confirmAdd by remember { mutableStateOf(false) }

    VibePageBackground {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = if (state.hasResults) 120.dp else 32.dp)
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
                        Column {
                            Text(
                                "Importar playlist",
                                style = MaterialTheme.typography.headlineLarge,
                                color = VibeCream
                            )
                            Text(
                                "Spotify o YouTube Music · audio de Audius",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeMuted
                            )
                        }
                    }
                }

                item { LinkCard(state = state, vm = vm) }

                state.linkError?.let { message ->
                    item { ErrorBanner(message = message) }
                }
                state.error?.let { message ->
                    item { ErrorBanner(message = message) }
                }

                item {
                    CredentialsCard(
                        state = state,
                        vm = vm,
                        youtubeChecking = youtubeKeyState.checking,
                        youtubeError = youtubeKeyState.error,
                        youtubeSuccess = youtubeKeyState.success,
                        spotifyOpen = spotifyOpen.value,
                        youtubeOpen = youtubeOpen.value,
                        onToggleSpotify = { spotifyOpen.value = !spotifyOpen.value },
                        onToggleYouTube = { youtubeOpen.value = !youtubeOpen.value }
                    )
                }

                if (state.analyzing) {
                    item { AnalysisProgress(state = state, onCancel = vm::cancelPlaylistImportAnalysis) }
                }

                val playlist = state.playlist
                if (playlist != null && state.items.isNotEmpty()) {
                    item {
                        SummaryCard(
                            state = state,
                            onSensitivity = vm::setPlaylistImportSensitivity,
                            onSelectAll = { vm.selectAllPlaylistImportItems(true) },
                            onClearSelection = { vm.selectAllPlaylistImportItems(false) }
                        )
                    }
                    if (state.partial) {
                        item {
                            InfoPill(
                                text = "Importación interrumpida · ${state.items.size} de ${state.total} analizadas",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (state.importedPlaylistId != null) {
                        item {
                            ImportSuccessCard(
                                state = state,
                                onOpenPlaylist = { id -> onOpenPlaylist(id) },
                                onAnother = vm::resetPlaylistImport
                            )
                        }
                    }

                    statusSections().forEach { (status, label) ->
                        val group = state.items.filter { it.outcome.status == status }
                        if (group.isEmpty()) return@forEach
                        item(key = "header-$status") {
                            StatusSectionHeader(status = status, label = label, count = group.size)
                        }
                        itemsIndexed(group, key = { index, item -> "$status-${item.source.position}-$index" }) { _, item ->
                            ImportItemRow(
                                item = item,
                                playlistArtwork = playlist.artworkUrl,
                                onToggle = { vm.togglePlaylistImportSelection(item.source.position) },
                                onChooseCandidate = { trackId ->
                                    vm.choosePlaylistImportCandidate(item.source.position, trackId)
                                }
                            )
                        }
                    }

                    item {
                        Text(
                            "Spotify y YouTube Music solo aportan metadatos. La reproducción y la " +
                                "descarga se resuelven en Audius y respetan los permisos de cada pista. " +
                                "Las vistas previas de 30 s nunca se tratan como canciones completas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        )
                    }
                } else if (!state.analyzing && state.error == null && playlist == null) {
                    item {
                        EmptyHint()
                    }
                }
            }

            if (state.hasResults && state.importedPlaylistId == null) {
                AddBar(
                    state = state,
                    onAdd = { confirmAdd = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }
        }

        if (confirmAdd && state.hasResults && state.importedPlaylistId == null) {
            AlertDialog(
                onDismissRequest = { confirmAdd = false },
                title = { Text("Añadir a mi biblioteca") },
                text = {
                    Text(
                        "Se añadirán ${state.selectedCount} de ${state.items.size} canciones " +
                            "a una lista nueva, en el orden original. Las canciones no " +
                            "seleccionadas y las dudosas sin confirmar se omitirán."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmAdd = false
                            vm.addSelectedPlaylistTracks()
                        }
                    ) { Text("Añadir") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmAdd = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Link + progress
// ---------------------------------------------------------------------------

@Composable
private fun LinkCard(state: PlaylistImportUiState, vm: VibeViewModel) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = VibeSurface.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = VibeRed, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Enlace de la playlist", style = MaterialTheme.typography.titleMedium, color = VibeCream)
                Spacer(Modifier.weight(1f))
                val platform = state.platform
                if (platform != null) {
                    InfoPill(text = platform.label)
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.url,
                onValueChange = vm::setPlaylistImportUrl,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.linkError != null,
                label = { Text("URL o URI de la playlist") },
                placeholder = { Text("https://open.spotify.com/playlist/…") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = vm::analyzePlaylistImport,
                    enabled = state.canAnalyze,
                    shape = RoundedCornerShape(50)
                ) {
                    if (state.analyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = VibeCream
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (state.analyzing) "Analizando…" else "Analizar playlist")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    when (state.platform) {
                        com.vibemusic.app.data.playlistimport.PlaylistPlatform.SPOTIFY ->
                            "Se leerán todas las canciones con paginación."
                        com.vibemusic.app.data.playlistimport.PlaylistPlatform.YOUTUBE_MUSIC ->
                            "Solo metadatos oficiales; nunca se descarga audio de YouTube."
                        null -> "Pega el enlace completo de la playlist."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun AnalysisProgress(state: PlaylistImportUiState, onCancel: () -> Unit) {
    val fraction = if (state.total > 0) {
        (state.analysed.toFloat() / state.total.toFloat()).coerceIn(0f, 1f)
    } else 0f
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = VibeSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (state.total > 0) "Buscando audio en Audius · ${state.analysed} de ${state.total}"
                else "Leyendo la playlist…",
                style = MaterialTheme.typography.titleSmall,
                color = VibeCream
            )
            Spacer(Modifier.height(10.dp))
            if (state.total > 0) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = VibeRed,
                    trackColor = VibeSurfaceHigh
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = VibeRed,
                    trackColor = VibeSurfaceHigh
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.total > 0) "${(fraction * 100).toInt()}% · puedes cancelar y conservar lo analizado"
                    else "La lista puede tardar según su tamaño.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCancel) { Text("Cancelar", color = VibeRed) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Credentials
// ---------------------------------------------------------------------------

@Composable
private fun CredentialsCard(
    state: PlaylistImportUiState,
    vm: VibeViewModel,
    youtubeChecking: Boolean,
    youtubeError: String?,
    youtubeSuccess: Boolean,
    spotifyOpen: Boolean,
    youtubeOpen: Boolean,
    onToggleSpotify: () -> Unit,
    onToggleYouTube: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = VibeSurface.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Key, contentDescription = null, tint = VibeGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Credenciales", style = MaterialTheme.typography.titleMedium, color = VibeCream)
                    Text(
                        "Se guardan cifradas en este dispositivo. Nunca se incluyen en el APK ni en GitHub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            CredentialToggle(
                title = "Spotify",
                detail = if (state.spotifyConfigured) "Credenciales guardadas" else "Client ID y Client Secret",
                configured = state.spotifyConfigured,
                open = spotifyOpen,
                onClick = onToggleSpotify
            )
            if (spotifyOpen) {
                SpotifyCredentialFields(state = state, vm = vm)
            }
            CredentialToggle(
                title = "YouTube Data API v3",
                detail = if (state.youtubeConfigured) "Clave disponible" else "Necesaria para leer playlists",
                configured = state.youtubeConfigured,
                open = youtubeOpen,
                onClick = onToggleYouTube
            )
            if (youtubeOpen) {
                YouTubeKeyFields(
                    vm = vm,
                    configured = state.youtubeConfigured,
                    checking = youtubeChecking,
                    error = youtubeError,
                    success = youtubeSuccess
                )
            }
        }
    }
}

@Composable
private fun CredentialToggle(
    title: String,
    detail: String,
    configured: Boolean,
    open: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (configured) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = if (configured) Color(0xFF4ADE80) else VibeMuted,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = VibeCream)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = VibeMuted)
        }
        TextButton(onClick = onClick) {
            Text(if (open) "Ocultar" else "Configurar", color = VibeRed)
        }
    }
}

@Composable
private fun SpotifyCredentialFields(state: PlaylistImportUiState, vm: VibeViewModel) {
    var clientId by remember { mutableStateOf("") }
    var clientSecret by remember { mutableStateOf("") }
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            "Crea una app en developer.spotify.com, copia su Client ID y Client Secret y pégalos aquí. " +
                "Solo se usan para leer playlists públicas con el flujo client credentials.",
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it },
            label = { Text("Client ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = clientSecret,
            onValueChange = { clientSecret = it },
            label = { Text("Client Secret") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        state.spotifyError?.let { message ->
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = VibeRed)
        }
        if (state.spotifySaved) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Credenciales verificadas y guardadas.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4ADE80)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { vm.saveSpotifyCredentials(clientId, clientSecret) },
                enabled = !state.spotifyChecking && clientId.isNotBlank() && clientSecret.isNotBlank(),
                shape = RoundedCornerShape(50)
            ) {
                if (state.spotifyChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.spotifyChecking) "Verificando…" else "Guardar credenciales")
            }
            if (state.spotifyConfigured) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { vm.clearSpotifyCredentials(); clientId = ""; clientSecret = "" },
                    shape = RoundedCornerShape(50)
                ) { Text("Eliminar") }
            }
        }
    }
}

@Composable
private fun YouTubeKeyFields(
    vm: VibeViewModel,
    configured: Boolean,
    checking: Boolean,
    error: String?,
    success: Boolean
) {
    var apiKey by remember { mutableStateOf("") }
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            "La misma clave de YouTube Data API v3 que usa el buscador. Se comprueba con una " +
                "petición de 1 unidad de cuota antes de guardarla.",
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Clave de API") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        error?.let { message ->
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = VibeRed)
        }
        if (success) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Clave verificada y guardada cifrada.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4ADE80)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { vm.saveYouTubeApiKey(apiKey) },
                enabled = !checking && apiKey.isNotBlank(),
                shape = RoundedCornerShape(50)
            ) {
                if (checking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (checking) "Verificando…" else "Guardar clave")
            }
            if (configured) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { vm.clearYouTubeApiKey(); apiKey = "" },
                    shape = RoundedCornerShape(50)
                ) { Text("Eliminar") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Results
// ---------------------------------------------------------------------------

@Composable
private fun SummaryCard(
    state: PlaylistImportUiState,
    onSensitivity: (MatchSensitivity) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    val playlist = state.playlist ?: return
    val byStatus = state.summary
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = VibeSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleLarge,
                color = VibeCream,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${playlist.owner} · ${playlist.trackCount} canciones · ${playlist.platform.label}",
                style = MaterialTheme.typography.bodySmall,
                color = VibeMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Disponibles", (byStatus[ImportStatus.AVAILABLE] ?: 0) + (byStatus[ImportStatus.DOWNLOADABLE] ?: 0), Modifier.weight(1f))
                Metric("Locales", byStatus[ImportStatus.LOCAL] ?: 0, Modifier.weight(1f))
                Metric("Dudosas", byStatus[ImportStatus.DOUBTFUL] ?: 0, Modifier.weight(1f))
                Metric("Sin audio", (byStatus[ImportStatus.NOT_FOUND] ?: 0) + (byStatus[ImportStatus.UNAVAILABLE] ?: 0), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text("Precisión de la búsqueda", style = MaterialTheme.typography.labelMedium, color = VibeMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MatchSensitivity.entries.forEach { sensitivity ->
                    FilterChip(
                        selected = state.sensitivity == sensitivity,
                        onClick = { onSensitivity(sensitivity) },
                        label = { Text(sensitivity.label) }
                    )
                }
            }
            Text(
                state.sensitivity.description,
                style = MaterialTheme.typography.bodySmall,
                color = VibeMuted
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelectAll) {
                    Text("Seleccionar disponibles", color = VibeRed)
                }
                TextButton(onClick = onClearSelection) {
                    Text("Quitar selección", color = VibeMuted)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${state.selectedCount} seleccionadas",
                    style = MaterialTheme.typography.labelMedium,
                    color = VibeCream
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = VibeCream,
            fontWeight = FontWeight.Bold
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = VibeMuted, maxLines = 1)
    }
}

@Composable
private fun StatusSectionHeader(status: ImportStatus, label: String, count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(statusColor(status), RoundedCornerShape(50))
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = VibeCream)
        Spacer(Modifier.weight(1f))
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = VibeMuted)
    }
}

@Composable
private fun ImportItemRow(
    item: PlaylistImportItem,
    playlistArtwork: String?,
    onToggle: () -> Unit,
    onChooseCandidate: (String) -> Unit
) {
    val track = item.outcome.track
    val artwork = track?.artworkUri ?: playlistArtwork
    val enabled = item.canAdd
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = item.selected,
                onCheckedChange = { onToggle() },
                enabled = enabled
            )
            AlbumArt(
                title = item.source.displayTitle,
                artworkUri = artwork,
                seed = item.source.videoId ?: item.source.displayTitle,
                modifier = Modifier.size(48.dp),
                corner = 10.dp,
                showBrand = false
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.source.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VibeCream,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append(item.source.artistLine.ifBlank { "Artista desconocido" })
                        item.source.album?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                        if (item.source.durationMs > 0L) {
                            append(" · ${formatDuration(item.source.durationMs)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(item.outcome.status)
                    if (item.outcome.confidence > 0.0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${(item.outcome.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted
                        )
                    }
                    item.duplicateOf?.let { position ->
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Repite la posición ${position + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeGold
                        )
                    }
                }
            }
        }
        Text(
            item.outcome.explanation,
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted,
            modifier = Modifier.padding(start = 48.dp, top = 2.dp, end = 4.dp)
        )
        if (item.outcome.status == ImportStatus.DOUBTFUL && !item.confirmed) {
            val candidates = item.outcome.candidates
                .filter { it.track.uri.isNotBlank() }
                .take(3)
            if (candidates.isNotEmpty()) {
                Column(Modifier.padding(start = 48.dp, top = 4.dp)) {
                    Text(
                        "Elige la versión correcta:",
                        style = MaterialTheme.typography.labelMedium,
                        color = VibeCream
                    )
                    candidates.forEach { candidate ->
                        TextButton(
                            onClick = { onChooseCandidate(candidate.track.id) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${candidate.track.title} — ${candidate.track.artist} " +
                                    "(${(candidate.score * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeRed,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Text(
                    "No hay una versión reproducible entre las candidatas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(start = 48.dp, top = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 48.dp)
                .height(1.dp)
                .background(VibeLine.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun StatusBadge(status: ImportStatus) {
    Surface(
        shape = RoundedCornerShape(50),
        color = statusColor(status).copy(alpha = 0.16f)
    ) {
        Text(
            status.label,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor(status),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AddBar(
    state: PlaylistImportUiState,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VibeSurfaceHigh,
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${state.selectedCount} de ${state.items.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = VibeCream
                )
                Text(
                    "se añadirán en el orden original",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted
                )
            }
            Button(
                onClick = onAdd,
                enabled = state.selectedCount > 0 && !state.importing,
                shape = RoundedCornerShape(50)
            ) {
                if (state.importing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (state.importing) "Añadiendo…" else "Añadir seleccionadas")
            }
        }
    }
}

@Composable
private fun ImportSuccessCard(
    state: PlaylistImportUiState,
    onOpenPlaylist: (Long) -> Unit,
    onAnother: () -> Unit
) {
    val id = state.importedPlaylistId ?: return
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF14351F),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Playlist añadida a tu biblioteca",
                    style = MaterialTheme.typography.titleMedium,
                    color = VibeCream
                )
            }
            Text(
                "${state.importedCount} canciones guardadas en el orden original" +
                    if (state.skipped > 0) " · ${state.skipped} omitidas" else "",
                style = MaterialTheme.typography.bodySmall,
                color = VibeMuted
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onOpenPlaylist(id) },
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Abrir playlist")
                }
                OutlinedButton(onClick = onAnother, shape = RoundedCornerShape(50)) {
                    Text("Importar otra")
                }
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = VibeSurface.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Cómo funciona",
                style = MaterialTheme.typography.titleMedium,
                color = VibeCream
            )
            Spacer(Modifier.height(6.dp))
            listOf(
                "1. Pega el enlace de una playlist pública.",
                "2. VibeMusic lee título, artistas, álbum y duración desde la API oficial.",
                "3. Cada canción se compara con Audius por título, artista, versión, duración, álbum e ISRC.",
                "4. Las coincidencias de alta confianza se marcan; las dudosas necesitan tu confirmación.",
                "5. Puedes seleccionar solo una parte y añadirla a tu biblioteca."
            ).forEach { line ->
                Text(line, style = MaterialTheme.typography.bodySmall, color = VibeMuted)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun statusSections(): List<Pair<ImportStatus, String>> = listOf(
    ImportStatus.AVAILABLE to "Disponibles",
    ImportStatus.DOWNLOADABLE to "Descargables",
    ImportStatus.LOCAL to "Locales · ya en tu dispositivo",
    ImportStatus.DOUBTFUL to "Coincidencia dudosa · confirma la versión",
    ImportStatus.NOT_FOUND to "No encontradas",
    ImportStatus.UNAVAILABLE to "No disponibles",
    ImportStatus.ERROR to "Error de consulta"
)

@Composable
private fun statusColor(status: ImportStatus): Color = when (status) {
    ImportStatus.AVAILABLE -> Color(0xFF4ADE80)
    ImportStatus.DOWNLOADABLE -> VibeGold
    ImportStatus.LOCAL -> Color(0xFF60A5FA)
    ImportStatus.DOUBTFUL -> Color(0xFFFBBF24)
    ImportStatus.NOT_FOUND -> VibeMuted
    ImportStatus.UNAVAILABLE -> Color(0xFFF87171)
    ImportStatus.ERROR -> Color(0xFFF87171)
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
