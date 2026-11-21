package com.vibemusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.provider.ProviderArtist
import com.vibemusic.app.data.provider.ProviderCollection
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed

/**
 * Cards shared by the Audius search, artist and collection screens. They take
 * provider-neutral models, so a future provider can reuse them unchanged.
 */
@Composable
fun ProviderArtistCard(
    artist: ProviderArtist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(124.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlbumArt(
            title = artist.name,
            artworkUri = artist.artworkUrl,
            seed = artist.seed,
            modifier = Modifier
                .width(112.dp)
                .aspectRatio(1f)
                .clip(CircleShape),
            corner = 56.dp,
            showBrand = false
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (artist.verified) {
                Spacer(Modifier.width(3.dp))
                Icon(
                    Icons.Rounded.Verified,
                    contentDescription = "Artista verificado",
                    tint = VibeRed,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Text(
            text = if (artist.followerCount > 0) {
                "${formatCompactCount(artist.followerCount)} seguidores"
            } else {
                "${artist.trackCount} pistas"
            },
            style = MaterialTheme.typography.labelSmall,
            color = VibeMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ProviderCollectionCard(
    collection: ProviderCollection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(148.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        AlbumArt(
            title = collection.name,
            artworkUri = collection.artworkUrl,
            seed = collection.seed,
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(1f),
            corner = 14.dp,
            showBrand = false
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = collection.typeLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = VibeRed
        )
        Text(
            text = collection.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${collection.creator} · ${collection.trackCount} " +
                if (collection.trackCount == 1) "pista" else "pistas",
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Horizontal rail used by Inicio, Explorar and Para ti. Audius is a public
 * chart provider: opening it sends no search text or listening history.
 */
@Composable
fun AudiusTrackRail(
    title: String,
    tracks: List<Track>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onPlay: (Track) -> Unit,
    emptyMessage: String = "Audius no está disponible en este momento."
) {
    if (tracks.isEmpty() && !loading && error == null) return
    SectionHeader(
        title = title,
        action = when {
            loading -> null
            error != null -> "Reintentar"
            else -> "Actualizar"
        },
        onAction = onRetry
    )
    when {
        loading && tracks.isEmpty() -> Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = VibeRed,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Cargando canciones completas de Audius…",
                style = MaterialTheme.typography.bodySmall,
                color = VibeMuted
            )
        }

        tracks.isEmpty() -> Text(
            text = error ?: emptyMessage,
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        else -> LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                CompactTrackCard(track = track, onClick = { onPlay(track) })
            }
        }
    }
}

/** Source pill used above provider sections so the origin is never ambiguous. */
@Composable
fun ProviderPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(VibeRed.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = VibeRed
        )
    }
}

/** 1.2 mil / 34,5 mil / 2,1 M, with Spanish decimal comma. */
fun formatCompactCount(count: Long): String = when {
    count < 1_000L -> count.toString()
    count < 1_000_000L -> {
        val value = count / 1_000.0
        if (value < 10.0) {
            String.format(java.util.Locale.US, "%.1f mil", value).replace('.', ',')
        } else {
            "${Math.round(value)} mil"
        }
    }
    else -> {
        val value = count / 1_000_000.0
        String.format(java.util.Locale.US, "%.1f M", value).replace('.', ',')
    }
}
