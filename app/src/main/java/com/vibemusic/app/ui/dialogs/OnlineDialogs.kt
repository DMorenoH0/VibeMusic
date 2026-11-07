package com.vibemusic.app.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.components.CoverArt
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurfaceHigh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineTrackActionSheet(
    track: Track,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onOpenAlbum: (() -> Unit)?,
    onOpenArtist: (() -> Unit)?
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VibeSurfaceHigh,
        contentColor = VibeCream,
        dragHandle = { BottomSheetDefaults.DragHandle(color = VibeLine) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(track = track, modifier = Modifier.size(58.dp), corner = 12.dp, showBrand = false)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Vista previa oficial de 30 s · Deezer",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeRed
                    )
                }
            }
            HorizontalDivider(color = VibeLine.copy(alpha = 0.6f))
            OnlineSheetAction(Icons.Rounded.PlayArrow, "Reproducir vista previa", onClick = onPlay)
            OnlineSheetAction(Icons.Rounded.SkipNext, "Reproducir a continuación", onClick = onPlayNext)
            if (onOpenAlbum != null) {
                OnlineSheetAction(Icons.Rounded.Album, "Ver álbum", onClick = onOpenAlbum)
            }
            if (onOpenArtist != null) {
                OnlineSheetAction(Icons.Rounded.Person, "Ver artista", onClick = onOpenArtist)
            }
            HorizontalDivider(color = VibeLine.copy(alpha = 0.6f))
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = VibeMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "El catálogo solo ofrece clips promocionales. VibeMusic no descarga canciones completas ni omite la protección de las plataformas.",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeMuted
                )
            }
        }
    }
}

@Composable
private fun OnlineSheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
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
