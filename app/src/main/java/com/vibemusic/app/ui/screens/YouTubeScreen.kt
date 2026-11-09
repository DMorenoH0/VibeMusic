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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.youtube.YouTubeVideo
import com.vibemusic.app.ui.components.Artwork
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.ErrorBanner
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.formatVideoDuration
import com.vibemusic.app.ui.paletteFor
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.viewmodel.VibeViewModel

/**
 * Independent YouTube section. It only searches metadata through the official
 * Data API; playback is delegated to the official YouTube player, so ads and
 * playback restrictions are never bypassed.
 */
@Composable
fun YouTubeScreen(
    vm: VibeViewModel,
    onPlayVideo: (YouTubeVideo) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val query by vm.youtubeQuery.collectAsStateWithLifecycle()
    val state by vm.youtubeSearch.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Column {
                        Text(
                            "YouTube",
                            style = MaterialTheme.typography.headlineLarge,
                            color = VibeCream
                        )
                        Text(
                            "Busca y reproduce en el reproductor oficial.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeMuted
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
                    item { YouTubeConfigurationCard() }
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
                                        "${state.results.size} resultados · reproductor oficial",
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
                                    YouTubeResultRow(video = video, onClick = { onPlayVideo(video) })
                                }
                                item { YouTubePlaybackNote() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeResultRow(video: YouTubeVideo, onClick: () -> Unit) {
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
                        contentDescription = "Reproducir en YouTube",
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
                        Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = VibeRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Abrir en YouTube",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeRed
                    )
                }
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
        YouTubeBullet("Al pulsar un vídeo se abre el reproductor oficial de YouTube, con sus anuncios, controles y restricciones.")
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
private fun YouTubeConfigurationCard() {
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
                "YouTube sin configurar",
                style = MaterialTheme.typography.titleMedium,
                color = VibeCream
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Esta sección necesita una clave de YouTube Data API v3. La app no incluye claves en el repositorio y no envía consultas mientras falte la configuración.",
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Configuración para desarrolladores:\n" +
                "1. Crea una clave en Google Cloud con YouTube Data API v3 habilitada.\n" +
                "2. Restríngela al paquete com.vibemusic.app y a la firma de release.\n" +
                "3. Define VIBE_YOUTUBE_API_KEY como secreto de GitHub Actions, variable de entorno o en local.properties (ignorado por Git).",
            style = MaterialTheme.typography.labelSmall,
            color = VibeMuted
        )
    }
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
            "La reproducción se abre en el reproductor oficial de YouTube. No se extrae audio ni se evitan anuncios o restricciones.",
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
