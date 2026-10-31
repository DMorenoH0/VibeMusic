package com.vibemusic.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import com.vibemusic.app.ui.components.AlbumCard
import com.vibemusic.app.ui.components.SectionHeader
import com.vibemusic.app.ui.components.TrackRow
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.groupAlbums
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun SearchScreen(
    vm: VibeViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onTrackMore: (Track) -> Unit
) {
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val genres = remember(tracks) {
        tracks.filter { it.source == TrackSource.DEMO }
            .mapNotNull { it.genre }
            .distinct()
    }
    val albums = remember(results) { groupAlbums(results) }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    VibePageBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = VibeCream
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setSearchQuery,
                    singleLine = true,
                    placeholder = {
                        Text("Canciones, artistas, álbumes…", color = VibeMuted)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = VibeMuted)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { vm.setSearchQuery("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar", tint = VibeMuted)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
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
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
            }

            if (query.isBlank()) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    if (genres.isNotEmpty()) {
                        item { SectionHeader("Explora por género") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(genres) { genre ->
                                    SuggestionChip(label = genre) { vm.setSearchQuery(genre) }
                                }
                            }
                        }
                    }
                    if (recent.isNotEmpty()) {
                        item { SectionHeader("Escuchado recientemente") }
                        items(recent, key = { it.id }) { track ->
                            TrackRow(
                                track = track,
                                isFavorite = track.id in favoriteIds,
                                isCurrent = player.mediaId == track.id,
                                isPlaying = player.isPlaying,
                                onClick = {
                                    vm.playFromList(recent, track)
                                    onOpenPlayer()
                                },
                                onFavorite = { vm.toggleFavorite(track) },
                                onMore = { onTrackMore(track) }
                            )
                        }
                    } else {
                        item {
                            Text(
                                "Escribe para buscar en tus canciones locales, descargas y en las muestras originales de VibeMusic.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VibeMuted,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    item {
                        Text(
                            text = if (results.isEmpty()) {
                                "Sin resultados para «$query»"
                            } else {
                                "${results.size} ${if (results.size == 1) "resultado" else "resultados"}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = VibeMuted,
                            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }

                    if (albums.isNotEmpty()) {
                        item { SectionHeader("Álbumes") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(albums, key = { it.name }) { album ->
                                    AlbumCard(
                                        album = album,
                                        onClick = { onOpenAlbum(album.name) },
                                        modifier = Modifier.width(132.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (results.isNotEmpty()) {
                        item { SectionHeader("Canciones") }
                        items(results, key = { it.id }) { track ->
                            TrackRow(
                                track = track,
                                isFavorite = track.id in favoriteIds,
                                isCurrent = player.mediaId == track.id,
                                isPlaying = player.isPlaying,
                                onClick = {
                                    vm.playFromList(results, track)
                                    onOpenPlayer()
                                },
                                onFavorite = { vm.toggleFavorite(track) },
                                onMore = { onTrackMore(track) }
                            )
                        }
                    } else {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Prueba con el nombre de una canción, un artista o un álbum. También puedes buscar un género de las muestras.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VibeMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = VibeSurface,
        onClick = onClick
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = VibeRed,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = VibeCream)
        }
    }
}
