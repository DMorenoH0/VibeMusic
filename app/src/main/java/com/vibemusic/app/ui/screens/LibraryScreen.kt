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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.components.AlbumCard
import com.vibemusic.app.ui.components.EmptyState
import com.vibemusic.app.ui.components.PermissionBanner
import com.vibemusic.app.ui.components.PlaylistCard
import com.vibemusic.app.ui.components.TrackRow
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.groupAlbums
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.viewmodel.VibeViewModel

private val libraryTabs = listOf("Canciones", "Álbumes", "Listas", "Favoritos", "Descargas")

@Composable
fun LibraryScreen(
    vm: VibeViewModel,
    onOpenPlayer: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onImportFiles: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onDownload: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val tracks by vm.allTracks.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()
    val initializing by vm.initializing.collectAsStateWithLifecycle()

    val albums = remember(tracks) { groupAlbums(tracks) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    VibePageBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            LibraryHeader(
                onOpenSearch = onOpenSearch,
                onOpenSettings = onOpenSettings,
                onImportFiles = onImportFiles,
                onDownload = onDownload,
                onCreatePlaylist = onCreatePlaylist
            )

            PermissionBanner(
                scanState = scanState,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenAppSettings
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(libraryTabs.size) { index ->
                    TabChip(
                        label = libraryTabs[index],
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }

            if (initializing && tracks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VibeRed)
                }
                return@Column
            }

            Box(Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> SongsTab(
                        tracks = tracks,
                        favoriteIds = favoriteIds,
                        currentId = player.mediaId,
                        isPlaying = player.isPlaying,
                        onPlay = { track ->
                            vm.playFromList(tracks, track)
                            onOpenPlayer()
                        },
                        onFavorite = vm::toggleFavorite,
                        onMore = onTrackMore,
                        onRescan = {
                            if (vm.hasAudioPermission()) vm.rescanLibrary() else onRequestPermission()
                        }
                    )

                    1 -> AlbumsTab(
                        albums = albums,
                        onOpenAlbum = onOpenAlbum
                    )

                    2 -> PlaylistsTab(
                        playlists = playlists,
                        onOpenPlaylist = onOpenPlaylist,
                        onCreatePlaylist = onCreatePlaylist
                    )

                    3 -> FavoritesTab(
                        favorites = favorites,
                        favoriteIds = favoriteIds,
                        currentId = player.mediaId,
                        isPlaying = player.isPlaying,
                        onPlay = { track ->
                            vm.playFromList(favorites, track)
                            onOpenPlayer()
                        },
                        onFavorite = vm::toggleFavorite,
                        onMore = onTrackMore
                    )

                    else -> DownloadsTab(
                        downloads = downloads,
                        favoriteIds = favoriteIds,
                        currentId = player.mediaId,
                        isPlaying = player.isPlaying,
                        onPlay = { track ->
                            vm.playFromList(downloads, track)
                            onOpenPlayer()
                        },
                        onFavorite = vm::toggleFavorite,
                        onMore = onTrackMore,
                        onDownload = onDownload
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onImportFiles: () -> Unit,
    onDownload: () -> Unit,
    onCreatePlaylist: () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Biblioteca",
                    style = MaterialTheme.typography.headlineLarge,
                    color = VibeCream
                )
                Text(
                    "Todo tu audio, en un solo lugar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VibeMuted
                )
            }
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = VibeCream)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Ajustes", tint = VibeCream)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(VibeSurface)
                .clickable(onClick = onOpenSearch)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = VibeMuted, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Buscar en tu biblioteca…",
                style = MaterialTheme.typography.bodyMedium,
                color = VibeMuted
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderAction(Icons.Rounded.FolderOpen, "Importar", onImportFiles)
            HeaderAction(Icons.Rounded.Download, "Descargar", onDownload)
            HeaderAction(Icons.Rounded.Add, "Nueva lista", onCreatePlaylist)
        }
    }
}

@Composable
private fun HeaderAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = VibeSurface,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = VibeRed, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = VibeCream)
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) VibeRed else VibeSurface,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else VibeMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CountLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = VibeMuted,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SongsTab(
    tracks: List<Track>,
    favoriteIds: Set<String>,
    currentId: String?,
    isPlaying: Boolean,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onMore: (Track) -> Unit,
    onRescan: () -> Unit
) {
    if (tracks.isEmpty()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "Tu biblioteca está vacía",
                    message = "Escanea tu música local, importa archivos o usa las muestras originales de VibeMusic.",
                    actionLabel = "Escanear ahora",
                    onAction = onRescan
                )
            }
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { CountLine("${tracks.size} ${if (tracks.size == 1) "canción en total" else "canciones en total"}") }
        items(tracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                isFavorite = track.id in favoriteIds,
                isCurrent = currentId == track.id,
                isPlaying = isPlaying,
                onClick = { onPlay(track) },
                onFavorite = { onFavorite(track) },
                onMore = { onMore(track) }
            )
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<com.vibemusic.app.ui.AlbumGroup>,
    onOpenAlbum: (String) -> Unit
) {
    if (albums.isEmpty()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Album,
                    title = "Sin álbumes",
                    message = "Los álbumes aparecen cuando hay canciones con metadatos de álbum."
                )
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(albums.size) { index ->
            val album = albums[index]
            AlbumCard(
                album = album,
                onClick = { onOpenAlbum(album.name) }
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<com.vibemusic.app.data.db.PlaylistWithCount>,
    onOpenPlaylist: (Long) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    if (playlists.isEmpty()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.QueueMusic,
                    title = "Crea tu primera lista",
                    message = "Organiza canciones por ambiente, estado de ánimo o lo que quieras.",
                    actionLabel = "Nueva lista",
                    onAction = onCreatePlaylist
                )
            }
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(playlists, key = { it.playlist.id }) { item ->
            PlaylistCard(
                playlist = com.vibemusic.app.data.model.PlaylistSummary(
                    id = item.playlist.id,
                    name = item.playlist.name,
                    trackCount = item.trackCount,
                    createdAt = item.playlist.createdAt,
                    updatedAt = item.playlist.updatedAt
                ),
                onClick = { onOpenPlaylist(item.playlist.id) }
            )
        }
    }
}

@Composable
private fun FavoritesTab(
    favorites: List<Track>,
    favoriteIds: Set<String>,
    currentId: String?,
    isPlaying: Boolean,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onMore: (Track) -> Unit
) {
    if (favorites.isEmpty()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Favorite,
                    title = "Aún no hay favoritas",
                    message = "Toca el corazón de una canción para guardarla aquí."
                )
            }
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { CountLine("${favorites.size} ${if (favorites.size == 1) "canción favorita" else "canciones favoritas"}") }
        items(favorites, key = { it.id }) { track ->
            TrackRow(
                track = track,
                isFavorite = track.id in favoriteIds,
                isCurrent = currentId == track.id,
                isPlaying = isPlaying,
                onClick = { onPlay(track) },
                onFavorite = { onFavorite(track) },
                onMore = { onMore(track) }
            )
        }
    }
}

@Composable
private fun DownloadsTab(
    downloads: List<Track>,
    favoriteIds: Set<String>,
    currentId: String?,
    isPlaying: Boolean,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onMore: (Track) -> Unit,
    onDownload: () -> Unit
) {
    if (downloads.isEmpty()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Download,
                    title = "Sin descargas",
                    message = "Descarga un enlace directo o importa archivos para escucharlos sin conexión.",
                    actionLabel = "Descargar audio",
                    onAction = onDownload
                )
            }
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { CountLine("${downloads.size} ${if (downloads.size == 1) "archivo disponible" else "archivos disponibles"} sin conexión") }
        items(downloads, key = { it.id }) { track ->
            TrackRow(
                track = track,
                isFavorite = track.id in favoriteIds,
                isCurrent = currentId == track.id,
                isPlaying = isPlaying,
                onClick = { onPlay(track) },
                onFavorite = { onFavorite(track) },
                onMore = { onMore(track) }
            )
        }
    }
}
