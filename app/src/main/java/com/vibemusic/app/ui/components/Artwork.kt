package com.vibemusic.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.paletteFor
import com.vibemusic.app.ui.trackPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

private object ArtworkCache {
    private val cache = object : LruCache<String, Bitmap>(24) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

private suspend fun loadArtwork(context: Context, uriString: String): Bitmap? =
    withContext(Dispatchers.IO) {
        ArtworkCache.get(uriString)?.let { return@withContext it }
        runCatching {
            val stream = if (uriString.startsWith("https://", ignoreCase = true)) {
                val connection = java.net.URL(uriString).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 12_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "VibeMusic/1.1 (Android)")
                connection.setRequestProperty("Accept", "image/*")
                if (connection.responseCode !in 200..299) {
                    connection.disconnect()
                    return@runCatching null
                }
                connection.inputStream
            } else {
                context.contentResolver.openInputStream(Uri.parse(uriString))
            }
            stream?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return@use null
                val scaled = if (bitmap.width > 720 || bitmap.height > 720) {
                    val ratio = min(720f / bitmap.width, 720f / bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt().coerceAtLeast(1),
                        (bitmap.height * ratio).toInt().coerceAtLeast(1),
                        true
                    ).also { if (it !== bitmap) bitmap.recycle() }
                } else {
                    bitmap
                }
                ArtworkCache.put(uriString, scaled)
                scaled
            }
        }.getOrNull()
    }

@Composable
fun CoverArt(
    track: Track,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
    showBrand: Boolean = true
) {
    Artwork(
        title = track.title,
        artworkUri = track.artworkUri,
        seed = track.id,
        palette = trackPalette(track),
        modifier = modifier,
        corner = corner,
        showBrand = showBrand
    )
}

@Composable
fun AlbumArt(
    title: String,
    artworkUri: String?,
    seed: String,
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    showBrand: Boolean = true,
    palette: List<Color>? = null
) {
    Artwork(
        title = title,
        artworkUri = artworkUri,
        seed = seed,
        palette = palette ?: paletteFor(seed),
        modifier = modifier,
        corner = corner,
        showBrand = showBrand
    )
}

@Composable
fun Artwork(
    title: String,
    artworkUri: String?,
    seed: String,
    palette: List<Color>,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
    showBrand: Boolean = true
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(
        initialValue = artworkUri?.let { ArtworkCache.get(it) },
        key1 = artworkUri
    ) {
        value = artworkUri?.let { loadArtwork(context, it) }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(palette))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(corner))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Portada de $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                        )
                    )
            )
        } else {
            ProceduralArtwork(seed = seed, palette = palette)
        }
        if (showBrand) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(if (corner > 10.dp) 12.dp else 7.dp)
            ) {
                Text(
                    text = "VIBE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProceduralArtwork(seed: String, palette: List<Color>) {
    val random = Random(seed.hashCode())
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(palette[1], palette[2], palette[0]),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )
        // Soft radial glows
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette[0].copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.2f),
                radius = size.minDimension * 0.8f
            ),
            radius = size.minDimension * 0.8f,
            center = Offset(size.width * 0.18f, size.height * 0.2f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.055f),
            radius = size.minDimension * (0.18f + random.nextFloat() * 0.18f),
            center = Offset(size.width * 0.82f, size.height * 0.22f)
        )
        drawCircle(
            color = palette[0].copy(alpha = 0.16f),
            radius = size.minDimension * (0.26f + random.nextFloat() * 0.12f),
            center = Offset(size.width * 0.72f, size.height * 0.68f)
        )
        // Diagonal grooves
        rotate(degrees = -18f, pivot = center) {
            val step = size.width / 7f
            repeat(9) { i ->
                drawLine(
                    color = Color.Black.copy(alpha = 0.10f),
                    start = Offset(-size.width + i * step * 1.5f, 0f),
                    end = Offset(i * step * 1.5f, size.height * 1.3f),
                    strokeWidth = size.width * 0.012f
                )
            }
        }
        // Waveform bars
        val bars = 9
        val barWidth = size.width * 0.035f
        val gap = size.width * 0.018f
        val startX = size.width * 0.13f
        val baseY = size.height * 0.8f
        rotate(degrees = -6f, pivot = Offset(size.width / 2, baseY)) {
            repeat(bars) { i ->
                val h = size.height * (0.08f + random.nextFloat() * 0.24f)
                drawRoundRect(
                    color = if (i == 4) palette[0] else Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(startX + i * (barWidth + gap), baseY - h),
                    size = Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
                )
            }
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.10f),
            radius = size.minDimension * 0.03f,
            center = Offset(size.width * 0.86f, size.height * 0.13f)
        )
    }
}
