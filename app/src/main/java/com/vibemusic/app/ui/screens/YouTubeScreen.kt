package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.db.PlaylistWithCount
import com.vibemusic.app.data.youtube.YouTubeVideo
import com.vibemusic.app.ui.components.Artwork
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.ErrorBanner
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.dialogs.CreatePlaylistDialog
import com.vibemusic.app.ui.formatVideoDuration
import com.vibemusic.app.ui.paletteFor
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.viewmodel.VibeViewModel

/**
 * YouTube section. It only searches metadata through the official Data API;
 * playback opens the official embedded player inside VibeMusic, so ads,
 * branding and playback restrictions are never bypassed.
 */
@Composable
fun YouTubeScreen(
    vm: VibeViewModel,
    onPlayVideo: (YouTubeVideo) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val query by vm.youtubeQuery.collectAsStateWithLifecycle()
    val state by vm.youtubeSearch.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    var showKeyDialog by remember { mutableStateOf(false) }
    var actionVideo by remember { mutableStateOf<YouTubeVideo?>(null) }
    var createPlaylistForVideo by remember { mutableStateOf<YouTubeVideo?>(null) }

    VibePageBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Volver",
                                tint = VibeCream
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Box(
                        Modifier
                            .size(34.dp)
                            .background(VibeRed, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.SmartDisplay,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "YouTube",
                            style = MaterialTheme.typography.headlineLarge,
                            color = VibeCream
                        )
                        Text(
                            "Busca y reproduce dentro de VibeMusic con el reproductor oficial.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeMuted
                        )
                    }
                    IconButton(onClick = {
                        vm.dismissYouTubeKeyState()
                        showKeyDialog = true
                    }) {
                        Icon(
                            Icons.Rounded.Key,
                            contentDescription = "Configurar clave de YouTube",
                            tint = VibeMuted
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setYouTubeQuery,
                    singleLine = true,
                    placeholder = {
                        Text("Canciones, artistas o álbumes…", color = VibeMuted)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = VibeMuted)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = vm::clearYouTubeSearch) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Limpiar",
                                        tint = VibeMuted
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    vm.submitYouTubeSearch()
                                    keyboard?.hide()
                                },
                                enabled = state.configured && !state.loading
                            ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = "Buscar en YouTube",
                                    tint = if (state.configured) VibeRed else VibeMuted
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        vm.submitYouTubeSearch()
                        keyboard?.hide()
                    }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibeRed,
                        unfocusedBorderColor = VibeLine,
                        focusedContainerColor = VibeSurface,
                        unfocusedContainerColor = VibeSurface,
                        cursorColor = VibeRed,
                        focusedTextColor = VibeCream,
                        unfocusedTextColor = VibeCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                if (!state.configured) {
                    item { YouTubeConfigurationCard(vm) }
                } else if (query.isBlank()) {
                    item { YouTubeIntroCard() }
                } else {
                    when {
                        state.loading -> item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = VibeRed,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Buscando en YouTube…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VibeMuted
                                )
                            }
                        }

                        state.error != null -> item {
                            ErrorBanner(
                                message = state.error.orEmpty(),
                                onRetry = vm::retryYouTubeSearch
                            )
                        }

                        state.searched && state.results.isEmpty() -> item {
                            EmptyState(
                                icon = Icons.Rounded.SmartDisplay,
                                title = "Sin resultados en YouTube",
                                message = "No encontramos «${state.query}». Prueba con otro título, artista o álbum."
                            )
                        }

                        else -> {
                            if (state.results.isNotEmpty()) {
                                item {
                                    Text(
                                        "${state.results.size} resultados · reproductor integrado",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VibeMuted,
                                        modifier = Modifier.padding(
                                            start = 20.dp,
                                            top = 8.dp,
                                            bottom = 4.dp
                                        )
                                    )
                                }
                                items(state.results, key = { it.id }) { video ->
                                    YouTubeResultRow(
                                        video = video,
                                        onClick = { onPlayVideo(video) },
                                        onMore = { actionVideo = video }
                                    )
                                }
                                item { YouTubePlaybackNote() }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showKeyDialog) {
        YouTubeKeyDialog(
            vm = vm,
            onDismiss = { showKeyDialog = false }
        )
    }

    actionVideo?.let { video ->
        YouTubeAddToPlaylistSheet(
            video = video,
            playlists = playlists,
            onDismiss = { actionVideo = null },
            onAdd = { id ->
                vm.addYouTubeToPlaylist(id, video)
                actionVideo = null
            },
            onNew = {
                createPlaylistForVideo = video
                actionVideo = null
            }
        )
    }

    createPlaylistForVideo?.let { video ->
        CreatePlaylistDialog(
            title = "Nueva lista con este vídeo",
            onDismiss = { createPlaylistForVideo = null },
            onConfirm = { name ->
                createPlaylistForVideo = null
                vm.createPlaylistWithYouTube(name, video)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YouTubeAddToPlaylistSheet(
    video: YouTubeVideo,
    playlists: List<PlaylistWithCount>,
    onDismiss: () -> Unit,
    onAdd: (Long) -> Unit,
    onNew: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VibeSurface,
        contentColor = VibeCream,
        dragHandle = { BottomSheetDefaults.DragHandle(color = VibeLine) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = VibeCream,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    video.channel,
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HorizontalDivider(color = VibeLine.copy(alpha = 0.6f))
            YouTubeSheetAction(Icons.Rounded.Add, "Nueva lista", onNew)
            playlists.forEach { item ->
                YouTubeSheetAction(
                    Icons.Rounded.QueueMusic,
                    item.playlist.name,
                    onClick = { onAdd(item.playlist.id) }
                )
            }
            if (playlists.isEmpty()) {
                Text(
                    "Todavía no tienes listas. Crea la primera con «Nueva lista».",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
            Text(
                "Se guarda el enlace del vídeo para abrirlo en el reproductor oficial. " +
                    "VibeMusic no descarga ni extrae el audio.",
                style = MaterialTheme.typography.labelSmall,
                color = VibeMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun YouTubeSheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = VibeCream, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = VibeCream)
    }
}

@Composable
private fun YouTubeResultRow(
    video: YouTubeVideo,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Artwork(
                    title = video.title,
                    artworkUri = video.thumbnailUrl.ifBlank { null },
                    seed = video.id,
                    palette = paletteFor(video.id),
                    modifier = Modifier
                        .width(132.dp)
                        .height(74.dp),
                    corner = 10.dp,
                    showBrand = false
                )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .background(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f),
                            RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Reproducir dentro de VibeMusic",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                val duration = formatVideoDuration(video.durationMs)
                if (duration.isNotEmpty()) {
                    Text(
                        duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = VibeCream,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    video.channel,
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = VibeRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Reproducir en VibeMusic",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeRed
                    )
                }
            }
            IconButton(onClick = onMore, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "Añadir a una lista",
                    tint = VibeMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun YouTubeIntroCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .background(VibeSurface, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = VibeRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Cómo funciona",
                style = MaterialTheme.typography.titleMedium,
                color = VibeCream
            )
        }
        Spacer(Modifier.height(10.dp))
        YouTubeBullet("Busca por título, artista o álbum y pulsa Buscar. La consulta solo se envía cuando tú lo pides.")
        YouTubeBullet("Los resultados muestran miniatura, canal y duración mediante YouTube Data API v3.")
        YouTubeBullet("Al pulsar un vídeo se abre el reproductor oficial integrado en VibeMusic, con sus anuncios, controles y restricciones.")
        YouTubeBullet("VibeMusic no extrae audio, no descarga vídeos y no reproduce YouTube en segundo plano.")
        Spacer(Modifier.height(8.dp))
        Text(
            "Tu música local, la cola y las notificaciones multimedia siguen funcionando aparte, sin depender de YouTube.",
            style = MaterialTheme.typography.labelSmall,
            color = VibeMuted
        )
    }
}

@Composable
private fun YouTubeConfigurationCard(vm: VibeViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .background(VibeSurface, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.SmartDisplay,
                contentDescription = null,
                tint = VibeRed,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Conecta YouTube",
                style = MaterialTheme.typography.titleMedium,
                color = VibeCream
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Añade tu clave de YouTube Data API v3 para buscar vídeos reales. " +
                "Se guarda cifrada en este dispositivo y solo se envía a Google " +
                "cuando pulsas Buscar.",
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted
        )
        Spacer(Modifier.height(14.dp))
        YouTubeKeyForm(vm)
        Spacer(Modifier.height(14.dp))
        Text(
            "Cómo obtenerla: Google Cloud → YouTube Data API v3 → Credenciales → " +
                "Clave de API. Restríngela al paquete com.vibemusic.app y a la firma " +
                "de esta app. También puedes compilar con VIBE_YOUTUBE_API_KEY o " +
                "VIBE_YOUTUBE_PROXY_URL.",
            style = MaterialTheme.typography.labelSmall,
            color = VibeMuted
        )
    }
}

@Composable
private fun YouTubeKeyForm(vm: VibeViewModel) {
    val keyState by vm.youtubeKeyState.collectAsStateWithLifecycle()
    var key by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = key,
        onValueChange = {
            key = it
            if (keyState.error != null || keyState.success) vm.dismissYouTubeKeyState()
        },
        singleLine = true,
        label = { Text("Clave de API de YouTube", color = VibeMuted) },
        placeholder = { Text("AIza…", color = VibeMuted) },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Ocultar clave" else "Mostrar clave",
                    tint = VibeMuted
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VibeRed,
            unfocusedBorderColor = VibeLine,
            focusedContainerColor = VibeSurface,
            unfocusedContainerColor = VibeSurface,
            cursorColor = VibeRed,
            focusedTextColor = VibeCream,
            unfocusedTextColor = VibeCream
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(10.dp))
    Button(
        onClick = { vm.saveYouTubeApiKey(key) },
        enabled = key.isNotBlank() && !keyState.checking,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (keyState.checking) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text("Comprobando…")
        } else {
            Text("Guardar y comprobar")
        }
    }

    keyState.error?.let { error ->
        Spacer(Modifier.height(8.dp))
        Text(
            error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    if (keyState.success) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Clave verificada. Escribe una búsqueda para empezar.",
            style = MaterialTheme.typography.bodySmall,
            color = VibeRed
        )
    }
    if (vm.youtubeKeyStored) {
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = vm::clearYouTubeApiKey) {
            Text("Quitar clave de este dispositivo", color = VibeMuted)
        }
    }
}

@Composable
private fun YouTubeKeyDialog(vm: VibeViewModel, onDismiss: () -> Unit) {
    val keyState by vm.youtubeKeyState.collectAsStateWithLifecycle()

    LaunchedEffect(keyState.success) {
        if (keyState.success && vm.youtubeConfigured) onDismiss()
    }

    AlertDialog(
        onDismissRequest = {
            vm.dismissYouTubeKeyState()
            onDismiss()
        },
        containerColor = VibeSurface,
        title = {
            Text(
                if (vm.youtubeConfigured) "Clave de YouTube" else "Conectar YouTube",
                color = VibeCream
            )
        },
        text = {
            Column {
                Text(
                    if (vm.youtubeUsesProxy) {
                        "Esta compilación usa un servicio intermediario: no necesitas clave en el dispositivo."
                    } else {
                        "Pega tu clave de YouTube Data API v3. Se comprueba con Google " +
                            "antes de guardarse cifrada en este dispositivo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeMuted
                )
                if (!vm.youtubeUsesProxy) {
                    Spacer(Modifier.height(12.dp))
                    YouTubeKeyForm(vm)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.dismissYouTubeKeyState()
                onDismiss()
            }) {
                Text("Cerrar", color = VibeRed)
            }
        }
    )
}

@Composable
private fun YouTubePlaybackNote() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .background(VibeSurface.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.SmartDisplay,
            contentDescription = null,
            tint = VibeRed,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "La reproducción usa el reproductor oficial de YouTube integrado en VibeMusic. No se extrae audio ni se evitan anuncios o restricciones.",
            style = MaterialTheme.typography.labelSmall,
            color = VibeMuted
        )
    }
}

@Composable
private fun YouTubeBullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•", color = VibeRed, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = VibeMuted)
    }
}
