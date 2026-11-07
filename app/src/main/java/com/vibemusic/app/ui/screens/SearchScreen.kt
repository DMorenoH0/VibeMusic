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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import com.vibemusic.app.ui.AlbumGroup
import com.vibemusic.app.ui.components.AlbumCard
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.ErrorBanner
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
    onBack: (() -> Unit)? = null,
    autoFocus: Boolean = false,
    onOpenPlayer: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenOnlineAlbum: (String) -> Unit,
    onOpenOnlineArtist: (String) -> Unit
) {
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val online by vm.onlineSearch.collectAsStateWithLifecycle()
    val history by vm.searchHistory.collectAsStateWithLifecycle()
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val genres = remember(tracks) {
        tracks.filter { it.source == TrackSource.DEMO }.mapNotNull { it.genre }.distinct()
    }
    val albums: List<AlbumGroup> = remember(results) { groupAlbums(results) }
    val suggestions = remember(query, online.results, history) {
        val needle = query.trim()
        if (needle.length < 2) emptyList() else {
            (history.filter { it.contains(needle, true) && !it.equals(needle, true) } +
                online.results.map { it.artist }.distinct())
                .filter { it.isNotBlank() }
                .distinct()
                .take(8)
        }
    }

    LaunchedEffect(autoFocus) {
        if (autoFocus) runCatching { focusRequester.requestFocus() }
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
                    .padding(start = if (onBack != null) 6.dp else 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
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
                    if (onBack == null) {
                        Text(
                            "Buscar",
                            style = MaterialTheme.typography.headlineLarge,
                            color = VibeCream
                        )
                        Text(
                            "Tu música y todo el catálogo, en un solo sitio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeMuted
                        )
                        Spacer(Modifier.height(12.dp))
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
                                IconButton(onClick = vm::clearSearch) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Limpiar", tint = VibeMuted)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            vm.submitSearch()
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }

            if (query.isBlank()) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    if (history.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Búsquedas recientes",
                                action = "Borrar",
                                onAction = vm::clearSearchHistory
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(history, key = { it }) { term ->
                                    SuggestionChip(label = term, icon = Icons.Rounded.History) {
                                        vm.submitSearch(term)
                                    }
                                }
                            }
                        }
                    }

                    if (genres.isNotEmpty()) {
                        item { SectionHeader("Explora por género") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(genres) { genre ->
                                    SuggestionChip(label = genre, icon = Icons.Rounded.Search) {
                                        vm.submitSearch(genre)
                                    }
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
                    }

                    item {
                        OnlineSourceNote()
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    if (suggestions.isNotEmpty() && !online.loading) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(suggestions, key = { it }) { term ->
                                    SuggestionChip(label = term, icon = Icons.Rounded.TrendingUp) {
                                        vm.submitSearch(term)
                                    }
                                }
                            }
                        }
                    }

                    if (results.isNotEmpty()) {
                        item {
                            Text(
                                "En tu música · ${results.size} ${if (results.size == 1) "resultado" else "resultados"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VibeMuted,
                                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 2.dp)
                            )
                        }
                        if (albums.isNotEmpty()) {
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
                    }

                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Cloud, contentDescription = null, tint = VibeRed, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Catálogo online · Deezer",
                                style = MaterialTheme.typography.titleSmall,
                                color = VibeCream,
                                modifier = Modifier.weight(1f)
                            )
                            if (online.error != null) {
                                IconButton(onClick = vm::retryOnlineSearch, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Reintentar", tint = VibeRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Text(
                            "Vistas previas oficiales de 30 s. Puedes descubrir música que no está en tu biblioteca.",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    when {
                        online.loading -> item {
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
                                Text("Buscando en el catálogo…", style = MaterialTheme.typography.bodySmall, color = VibeMuted)
                            }
                        }

                        online.error != null -> item {
                            ErrorBanner(
                                message = online.error.orEmpty(),
                                onRetry = vm::retryOnlineSearch
                            )
                        }

                        online.searched && online.results.isEmpty() -> item {
                            EmptyState(
                                icon = Icons.Rounded.Search,
                                title = "Sin resultados online",
                                message = "No encontramos «${online.query}» en el catálogo. Prueba con otro título, artista o álbum."
                            )
                        }

                        else -> items(online.results, key = { it.id }) { track ->
                            TrackRow(
                                track = track,
                                isFavorite = false,
                                isCurrent = player.mediaId == track.id,
                                isPlaying = player.isPlaying,
                                onClick = {
                                    vm.playFromList(online.results, track)
                                    onOpenPlayer()
                                },
                                onFavorite = null,
                                onMore = { onTrackMore(track) }
                            )
                        }
                    }

                    if (results.isEmpty() && !online.loading && online.results.isEmpty() && online.error == null && online.searched) {
                        item {
                            Text(
                                "Tampoco hay coincidencias en tu biblioteca.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeMuted,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineSourceNote() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .background(VibeSurface.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Cloud, contentDescription = null, tint = VibeRed, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "El buscador online usa la API pública de Deezer. Solo reproduce clips promocionales de 30 segundos y no descarga audio protegido.",
            style = MaterialTheme.typography.labelSmall,
            color = VibeMuted
        )
    }
}

@Composable
private fun SuggestionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = VibeSurface,
        onClick = onClick
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = VibeRed,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = VibeCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
