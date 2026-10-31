package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import com.vibemusic.app.ui.AlbumGroup
import com.vibemusic.app.ui.ArtistGroup
import com.vibemusic.app.ui.components.AlbumArt
import com.vibemusic.app.ui.components.AlbumCard
import com.vibemusic.app.ui.components.MixCard
import com.vibemusic.app.ui.components.PermissionBanner
import com.vibemusic.app.ui.components.SectionHeader
import com.vibemusic.app.ui.groupAlbums
import com.vibemusic.app.ui.groupArtists
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun ExploreScreen(
    vm: VibeViewModel,
    onOpenSearch: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()
    val demos = remember(tracks) { tracks.filter { it.source == TrackSource.DEMO } }
    val albums = remember(tracks) { groupAlbums(tracks) }
    val artists = remember(tracks) { groupArtists(tracks) }
    val genres = remember(demos) { listOf("Todo") + demos.map { it.genre ?: "Otros" }.distinct() }
    var selectedGenre by rememberSaveable { mutableStateOf("Todo") }

    val filteredDemos = remember(demos, selectedGenre) {
        if (selectedGenre == "Todo") demos else demos.filter { it.genre == selectedGenre }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 560.dp -> 3
            else -> 2
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(8.dp))
                        Text("Explorar", style = MaterialTheme.typography.headlineLarge, color = VibeCream)
                        Text(
                            "Muestras originales y todo lo que hay en tu móvil.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeMuted
                        )
                        Spacer(Modifier.height(14.dp))
                        SearchBarButton(onClick = onOpenSearch)
                    }
                }

                item {
                    PermissionBanner(
                        scanState = scanState,
                        onRequestPermission = onRequestPermission,
                        onOpenSettings = onOpenAppSettings
                    )
                }

                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genres.forEach { genre ->
                            GenreChip(
                                label = genre,
                                selected = genre == selectedGenre,
                                onClick = { selectedGenre = genre }
                            )
                        }
                    }
                }

                if (filteredDemos.isNotEmpty()) {
                    item { SectionHeader("Muestras VibeMusic") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredDemos, key = { it.id }) { track ->
                                MixCard(
                                    track = track,
                                    title = track.title,
                                    subtitle = "${track.artist} · ${track.genre ?: "Original"}",
                                    onClick = {
                                        vm.playFromList(filteredDemos, track)
                                        onOpenPlayer()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "No hay muestras para este género.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibeMuted,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }

                item { SectionHeader("Álbumes") }
                item {
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        albums.chunked(columns).forEach { rowAlbums ->
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                rowAlbums.forEach { album ->
                                    AlbumCard(
                                        album = album,
                                        onClick = { onOpenAlbum(album.name) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowAlbums.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (artists.isNotEmpty()) {
                    item { SectionHeader("Artistas") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(artists, key = { it.name }) { artist ->
                                ArtistCard(artist)
                            }
                        }
                    }
                }

                item {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Las muestras de VibeMusic son instrumentales originales generadas para esta app. No hay catálogo comercial ni extracción desde YouTube.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBarButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VibeSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = VibeMuted, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Buscar canciones, artistas, álbumes...",
            style = MaterialTheme.typography.bodyMedium,
            color = VibeMuted
        )
    }
}

@Composable
private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) VibeRed else VibeSurface,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) VibeCream else VibeMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ArtistCard(artist: ArtistGroup) {
    Column(
        Modifier.width(104.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlbumArt(
            title = artist.name,
            artworkUri = artist.artworkUri,
            seed = artist.seed,
            modifier = Modifier.size(96.dp),
            corner = 48.dp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            artist.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            "${artist.trackCount} ${if (artist.trackCount == 1) "tema" else "temas"}",
            style = MaterialTheme.typography.labelSmall,
            color = VibeMuted
        )
    }
}
