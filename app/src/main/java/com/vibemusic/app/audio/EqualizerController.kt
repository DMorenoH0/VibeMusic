package com.vibemusic.app.audio

import android.media.audiofx.Equalizer
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Applies equalisation to the global output mix (audio session 0). Android
 * does not expose a public per-app effect API in Media3, so session 0 is the
 * only officially supported way to affect VibeMusic playback without
 * reimplementing the audio pipeline. Some OEMs expose no effects at all; in
 * that case [available] stays false and the UI explains the limitation instead
 * of pretending the sliders work.
 */
object EqualizerController {

    data class Band(
        val index: Short,
        val centerHz: Int,
        val minMb: Short,
        val maxMb: Short
    )

    private var equalizer: Equalizer? = null
    private var failed = false

    var available: Boolean = false
        private set
    var message: String? = null
        private set

    @Synchronized
    fun ensureReady(): Boolean {
        if (equalizer != null) return true
        if (failed) return false
        return try {
            val eq = Equalizer(0, 0)
            eq.enabled = false
            equalizer = eq
            available = true
            message = null
            true
        } catch (e: Throwable) {
            failed = true
            available = false
            message = "Este dispositivo no expone un ecualizador global (${e.message ?: "no compatible"}). " +
                "Puedes seguir usando los perfiles guardados; el efecto no se aplicará."
            false
        }
    }

    @Synchronized
    fun bands(): List<Band> {
        if (!ensureReady()) return emptyList()
        val eq = equalizer ?: return emptyList()
        return try {
            val range = eq.bandLevelRange
            (0 until eq.numberOfBands).map { index ->
                val hz = (eq.getCenterFreq(index.toShort()) / 1000).coerceAtLeast(1)
                Band(
                    index = index.toShort(),
                    centerHz = hz,
                    minMb = range[0],
                    maxMb = range[1]
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /** Applies the five conceptual gains, clamped to what the hardware allows. */
    @Synchronized
    fun apply(gainsDb: List<Float>): Boolean {
        if (!ensureReady()) return false
        val eq = equalizer ?: return false
        return try {
            val range = eq.bandLevelRange
            val anchors = EqPresets.anchorsHz
            for (index in 0 until eq.numberOfBands) {
                val centerHz = eq.getCenterFreq(index.toShort()) / 1000
                val anchorIndex = nearestAnchor(centerHz, anchors)
                val gain = gainsDb.getOrElse(anchorIndex) { 0f }
                val millibels = (gain * 100f).roundToInt()
                    .coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(index.toShort(), millibels.toShort())
            }
            eq.enabled = true
            true
        } catch (_: Throwable) {
            false
        }
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        if (!ensureReady()) return
        runCatching { equalizer?.enabled = enabled }
    }

    @Synchronized
    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        available = false
    }

    private fun nearestAnchor(hz: Int, anchors: List<Int>): Int {
        val logHz = ln(hz.toDouble().coerceAtLeast(1.0))
        return anchors.indices.minByOrNull { index ->
            abs(logHz - ln(anchors[index].toDouble()))
        } ?: 0
    }
}
