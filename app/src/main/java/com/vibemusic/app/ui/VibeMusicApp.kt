package com.vibemusic.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import com.vibemusic.app.ui.components.MiniPlayer
import com.vibemusic.app.ui.components.UpdateBanner
import com.vibemusic.app.ui.components.UpdateDialog
import com.vibemusic.app.ui.components.VibeBottomBar
import com.vibemusic.app.ui.dialogs.CreatePlaylistDialog
import com.vibemusic.app.ui.dialogs.DownloadDialog
import com.vibemusic.app.ui.dialogs.OnlineTrackActionSheet
import com.vibemusic.app.ui.dialogs.TrackActionSheet
import com.vibemusic.app.ui.dialogs.TransferDialog
import com.vibemusic.app.ui.screens.AlbumDetailScreen
import com.vibemusic.app.ui.screens.CsvImportScreen
import com.vibemusic.app.ui.screens.ExploreScreen
import com.vibemusic.app.ui.screens.HomeScreen
import com.vibemusic.app.ui.screens.LegalScreen
import com.vibemusic.app.ui.screens.LibraryScreen
import com.vibemusic.app.ui.screens.NowPlayingScreen
import com.vibemusic.app.ui.screens.OnlineAlbumScreen
import com.vibemusic.app.ui.screens.OnlineArtistScreen
import com.vibemusic.app.ui.screens.PlaylistDetailScreen
import com.vibemusic.app.ui.screens.QueueScreen
import com.vibemusic.app.ui.screens.SearchScreen
import com.vibemusic.app.ui.screens.SettingsScreen
import com.vibemusic.app.viewmodel.VibeViewModel

/**
 * Root composable. The main shell has four tabs (Inicio, Buscar, Explorar and
 * Biblioteca) and every detail view is a full-screen overlay on top of it.
 */
@Composable
fun VibeMusicApp(vm: VibeViewModel) {
    val context = LocalContext.current
    val player by vm.playerState.collectAsStateWithLifecycle()
    val current by vm.currentTrack.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val transfer by vm.transfer.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val dismissedUpdate by vm.dismissedUpdateVersion.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(0) }
    var route by rememberSaveable { mutableStateOf<String?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }

    var actionTrack by remember { mutableStateOf<Track?>(null) }
    var actionPlaylistId by remember { mutableStateOf<Long?>(null) }
    var pendingPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var showCreatePlaylist by rememberSaveable { mutableStateOf(false) }
    var showDownload by rememberSaveable { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) {
        vm.messages.collect { message -> snackbar.showSnackbar(message) }
    }

    val openExternal: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Playback still works if the user hides the notification. */ }

    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.onAudioPermissionGranted()
            requestNotifications()
        } else {
            vm.onAudioPermissionDenied()
        }
    }

    val requestAudioPermission: () -> Unit = {
        if (vm.hasAudioPermission()) {
            vm.onAudioPermissionGranted()
            requestNotifications()
        } else {
            audioLauncher.launch(vm.audioPermission())
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) vm.importDocuments(uris)
    }

    val openAppSettings: () -> Unit = {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    BackHandler(enabled = route != null || showUpdateDialog) {
        if (showUpdateDialog) showUpdateDialog = false else route = null
    }

    val updateAvailable = update.available &&
        update.release?.version != dismissedUpdate

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeScreen(
                        vm = vm,
                        onOpenSearch = { tab = 1 },
                        onOpenSettings = { route = "settings" },
                        onOpenPlayer = { route = "player" },
                        onOpenAlbum = { album -> route = "album:$album" },
                        onRequestPermission = requestAudioPermission,
                        onOpenAppSettings = openAppSettings
                    )

                    1 -> SearchScreen(
                        vm = vm,
                        onBack = null,
                        onOpenPlayer = { route = "player" },
                        onOpenAlbum = { album -> route = "album:$album" },
                        onTrackMore = { track ->
                            actionPlaylistId = null
                            actionTrack = track
                        },
                        onOpenOnlineAlbum = { id -> route = "onlineAlbum:$id" },
                        onOpenOnlineArtist = { id -> route = "onlineArtist:$id" }
                    )

                    2 -> ExploreScreen(
                        vm = vm,
                        onOpenSearch = { tab = 1 },
                        onOpenAlbum = { album -> route = "album:$album" },
                        onOpenPlayer = { route = "player" },
                        onRequestPermission = requestAudioPermission,
                        onOpenAppSettings = openAppSettings
                    )

                    else -> LibraryScreen(
                        vm = vm,
                        onOpenPlayer = { route = "player" },
                        onTrackMore = { track ->
                            actionPlaylistId = null
                            actionTrack = track
                        },
                        onOpenAlbum = { album -> route = "album:$album" },
                        onOpenPlaylist = { id -> route = "playlist:$id" },
                        onOpenSearch = { tab = 1 },
                        onOpenSettings = { route = "settings" },
                        onImportFiles = { importLauncher.launch(arrayOf("audio/*")) },
                        onCreatePlaylist = {
                            pendingPlaylistTrack = null
                            showCreatePlaylist = true
                        },
                        onDownload = { showDownload = true },
                        onRequestPermission = requestAudioPermission,
                        onOpenAppSettings = openAppSettings
                    )
                }
            }

            if (route == null && updateAvailable) {
                UpdateBanner(
                    installedVersion = vm.appVersion(),
                    latestVersion = update.release?.version.orEmpty(),
                    onOpen = { showUpdateDialog = true },
                    onDismiss = vm::dismissUpdateNotice
                )
            }

            current?.let { playing ->
                if (route != "player") {
                    MiniPlayer(
                        track = playing,
                        state = player,
                        onOpen = { route = "player" },
                        onToggle = vm::togglePlayPause,
                        onNext = vm::next
                    )
                }
            }
            VibeBottomBar(selected = tab, onSelect = { tab = it })
        }

        val overlayModifier = Modifier
            .fillMaxSize()
            .zIndex(2f)

        when {
            route == "player" -> Box(overlayModifier) {
                NowPlayingScreen(
                    vm = vm,
                    onBack = { route = null },
                    onOpenQueue = { route = "queue" },
                    onTrackMore = { track ->
                        actionPlaylistId = null
                        actionTrack = track
                    }
                )
            }

            route == "queue" -> Box(overlayModifier) {
                QueueScreen(
                    vm = vm,
                    onBack = { route = null },
                    onOpenPlayer = { route = "player" }
                )
            }

            route == "settings" -> Box(overlayModifier) {
                SettingsScreen(
                    vm = vm,
                    onBack = { route = null },
                    onRequestPermission = requestAudioPermission,
                    onOpenAppSettings = openAppSettings,
                    onOpenLegal = { key -> route = "legal:$key" },
                    onOpenCsv = { route = "csv" },
                    onOpenUpdate = { showUpdateDialog = true }
                )
            }

            route == "csv" -> Box(overlayModifier) {
                CsvImportScreen(
                    vm = vm,
                    onBack = { route = "settings" }
                )
            }

            route?.startsWith("legal:") == true -> {
                val key = route.orEmpty().removePrefix("legal:")
                Box(overlayModifier) {
                    LegalScreen(
                        initial = key,
                        onBack = { route = "settings" }
                    )
                }
            }

            route?.startsWith("onlineAlbum:") == true -> {
                val albumId = route.orEmpty().removePrefix("onlineAlbum:")
                Box(overlayModifier) {
                    OnlineAlbumScreen(
                        vm = vm,
                        albumId = albumId,
                        onBack = { route = null },
                        onOpenPlayer = { route = "player" },
                        onTrackMore = { track ->
                            actionPlaylistId = null
                            actionTrack = track
                        },
                        onOpenArtist = { id -> route = "onlineArtist:$id" }
                    )
                }
            }

            route?.startsWith("onlineArtist:") == true -> {
                val artistId = route.orEmpty().removePrefix("onlineArtist:")
                Box(overlayModifier) {
                    OnlineArtistScreen(
                        vm = vm,
                        artistId = artistId,
                        onBack = { route = null },
                        onOpenPlayer = { route = "player" },
                        onTrackMore = { track ->
                            actionPlaylistId = null
                            actionTrack = track
                        },
                        onOpenAlbum = { id -> route = "onlineAlbum:$id" }
                    )
                }
            }

            route?.startsWith("album:") == true -> {
                val album = route.orEmpty().removePrefix("album:")
                Box(overlayModifier) {
                    AlbumDetailScreen(
                        vm = vm,
                        albumName = album,
                        onBack = { route = null },
                        onOpenPlayer = { route = "player" },
                        onTrackMore = { track ->
                            actionPlaylistId = null
                            actionTrack = track
                        }
                    )
                }
            }

            route?.startsWith("playlist:") == true -> {
                val id = route.orEmpty().removePrefix("playlist:").toLongOrNull()
                if (id != null) {
                    Box(overlayModifier) {
                        PlaylistDetailScreen(
                            vm = vm,
                            playlistId = id,
                            onBack = { route = null },
                            onOpenPlayer = { route = "player" },
                            onTrackMore = { track ->
                                actionPlaylistId = id
                                actionTrack = track
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (route == null) 96.dp else 28.dp
                )
                .zIndex(4f)
        )
    }

    actionTrack?.let { track ->
        if (track.source == TrackSource.ONLINE) {
            OnlineTrackActionSheet(
                track = track,
                onDismiss = {
                    actionTrack = null
                    actionPlaylistId = null
                },
                onPlay = {
                    vm.play(track)
                    actionTrack = null
                    actionPlaylistId = null
                    route = "player"
                },
                onPlayNext = {
                    vm.playNext(track)
                    actionTrack = null
                    actionPlaylistId = null
                },
                onOpenAlbum = track.albumId?.let { albumId ->
                    {
                        actionTrack = null
                        actionPlaylistId = null
                        route = "onlineAlbum:$albumId"
                    }
                },
                onOpenArtist = track.artistId?.let { artistId ->
                    {
                        actionTrack = null
                        actionPlaylistId = null
                        route = "onlineArtist:$artistId"
                    }
                }
            )
        } else {
            val playlistId = actionPlaylistId
            TrackActionSheet(
                track = track,
                playlists = playlists,
                isFavorite = track.id in favoriteIds,
                playlistId = playlistId,
                onDismiss = {
                    actionTrack = null
                    actionPlaylistId = null
                },
                onPlay = {
                    vm.play(track)
                    actionTrack = null
                    actionPlaylistId = null
                    route = "player"
                },
                onPlayNext = {
                    vm.playNext(track)
                    actionTrack = null
                    actionPlaylistId = null
                },
                onToggleFavorite = {
                    vm.toggleFavorite(track)
                    actionTrack = null
                    actionPlaylistId = null
                },
                onAddToPlaylist = { id ->
                    vm.addToPlaylist(id, track)
                    actionTrack = null
                    actionPlaylistId = null
                },
                onRemoveFromPlaylist = { id ->
                    vm.removeFromPlaylist(id, track)
                    actionTrack = null
                    actionPlaylistId = null
                },
                onDeleteDownload = {
                    vm.deleteDownload(track)
                    actionTrack = null
                    actionPlaylistId = null
                },
                onNewPlaylist = {
                    pendingPlaylistTrack = track
                    actionTrack = null
                    actionPlaylistId = null
                    showCreatePlaylist = true
                },
                onOpenAlbum = if (track.album.isBlank()) null else {
                    {
                        val album = track.album
                        actionTrack = null
                        actionPlaylistId = null
                        route = "album:$album"
                    }
                }
            )
        }
    }

    if (showUpdateDialog && update.release != null) {
        UpdateDialog(
            installedVersion = vm.appVersion(),
            state = update,
            onDismiss = { showUpdateDialog = false },
            onDownload = vm::downloadUpdate,
            onInstall = vm::installDownloadedUpdate,
            onOpenSettings = vm::openInstallPermissionSettings,
            onOpenRelease = { url -> openExternal(url) }
        )
    }

    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onDismiss = {
                showCreatePlaylist = false
                pendingPlaylistTrack = null
            },
            onConfirm = { name ->
                val trackToAdd = pendingPlaylistTrack
                vm.createPlaylist(name) { id ->
                    if (trackToAdd != null) vm.addToPlaylist(id, trackToAdd)
                }
                showCreatePlaylist = false
                pendingPlaylistTrack = null
            }
        )
    }

    if (showDownload) {
        DownloadDialog(
            onDismiss = { showDownload = false },
            onDownload = { url, title, artist ->
                showDownload = false
                vm.startDownload(url, title, artist)
            }
        )
    }

    TransferDialog(
        transfer = transfer,
        onDismiss = vm::dismissTransfer
    )
}
