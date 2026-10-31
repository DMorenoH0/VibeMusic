package com.vibemusic.app.ui

import com.vibemusic.app.data.model.Track
import com.vibemusic.app.ui.theme.AccentPalettes
import java.util.concurrent.TimeUnit

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatLongDuration(durationMs: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "$hours h $minutes min"
        totalMinutes > 0 -> "$minutes min"
        else -> "< 1 min"
    }
}

fun trackPalette(track: Track): List<androidx.compose.ui.graphics.Color> {
    val index = when {
        track.source == com.vibemusic.app.data.model.TrackSource.DEMO ->
            com.vibemusic.app.data.DemoCatalog.paletteFor(track)
        else -> (track.id.hashCode() and 0x7FFFFFFF) % AccentPalettes.size
    }
    return AccentPalettes[index % AccentPalettes.size]
}

fun paletteFor(seed: String): List<androidx.compose.ui.graphics.Color> =
    AccentPalettes[(seed.hashCode() and 0x7FFFFFFF) % AccentPalettes.size]
