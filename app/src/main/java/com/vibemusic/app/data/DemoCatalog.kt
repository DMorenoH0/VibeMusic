package com.vibemusic.app.data

import android.content.Context
import android.net.Uri
import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Six original, procedurally generated instrumental pieces shipped with the
 * app. They are our own recordings (synthesized from code), so they can be
 * bundled, copied and played offline without any licensing restrictions.
 */
object DemoCatalog {

    data class Spec(
        val asset: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val genre: String,
        val palette: Int
    )

    val specs = listOf(
        Spec("neon_skyline.wav", "Neon Skyline", "Midnight Pulse", "Neon Skyline", 42_000L, "Synthwave", 0),
        Spec("afterglow.wav", "Afterglow", "Luna Vale", "Afterglow", 40_000L, "Chill", 1),
        Spec("crimson_drive.wav", "Crimson Drive", "Vibe Machine", "Crimson Drive", 44_000L, "Electronic", 2),
        Spec("velvet_rain.wav", "Velvet Rain", "Nocturne Club", "Velvet Rain", 40_000L, "Lo-fi", 3),
        Spec("solar_flare.wav", "Solar Flare", "Atlas Bloom", "Solar Flare", 44_000L, "Ambient", 4),
        Spec("midnight_bloom.wav", "Midnight Bloom", "Echo District", "Midnight Bloom", 41_000L, "Downtempo", 5)
    )

    fun paletteFor(track: Track): Int {
        specs.firstOrNull { it.asset.substringBeforeLast('.') == track.id.removePrefix("demo:") }
            ?.let { return it.palette }
        return (track.id.hashCode() and 0x7FFFFFFF) % 6
    }

    fun coverSeed(track: Track): Int = (track.id.hashCode() * 31 + track.title.hashCode())
}

class DemoInstaller(private val context: Context) {

    suspend fun install(): List<Track> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "demo").apply { mkdirs() }
        DemoCatalog.specs.mapNotNull { spec ->
            try {
                val target = File(dir, spec.asset)
                val assetSize = runCatching {
                    context.assets.openFd("demo/${spec.asset}").use { it.length }
                }.getOrNull()
                if (!target.exists() || assetSize == null || target.length() != assetSize) {
                    context.assets.open("demo/${spec.asset}").use { input ->
                        target.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
                    }
                }
                if (target.length() <= 0) return@mapNotNull null
                Track(
                    id = "demo:${spec.asset.substringBeforeLast('.')}",
                    uri = Uri.fromFile(target).toString(),
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    durationMs = spec.durationMs,
                    source = TrackSource.DEMO,
                    fileName = target.absolutePath,
                    genre = spec.genre
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
