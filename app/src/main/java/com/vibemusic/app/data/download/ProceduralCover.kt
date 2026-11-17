package com.vibemusic.app.data.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import kotlin.math.min
import kotlin.random.Random

/**
 * Creates the same procedural cover the app draws in Compose, so a downloaded
 * sample keeps a real image file as artwork instead of losing its identity when
 * it leaves the UI.
 */
object ProceduralCover {

    private val palettes = listOf(
        intArrayOf(0xFFFF3B30.toInt(), 0xFF8E0E00.toInt(), 0xFF2A0508.toInt()),
        intArrayOf(0xFFFF7A45.toInt(), 0xFFB31217.toInt(), 0xFF1A0710.toInt()),
        intArrayOf(0xFFF2C14E.toInt(), 0xFFB3541E.toInt(), 0xFF140A08.toInt()),
        intArrayOf(0xFFFF5E8A.toInt(), 0xFF7A1030.toInt(), 0xFF12070D.toInt()),
        intArrayOf(0xFF6EE7D8.toInt(), 0xFF0E6B66.toInt(), 0xFF04100F.toInt()),
        intArrayOf(0xFFB58CFF.toInt(), 0xFF4B2A8A.toInt(), 0xFF0A0614.toInt()),
        intArrayOf(0xFF4C8DFF.toInt(), 0xFF1F4FA8.toInt(), 0xFF060A14.toInt()),
        intArrayOf(0xFF35D07F.toInt(), 0xFF146B3A.toInt(), 0xFF04100A.toInt())
    )

    fun paletteIndex(seed: String): Int =
        (seed.hashCode() and 0x7FFFFFFF) % palettes.size

    /** Returns a `file://` URI to a PNG cover, reusing it on later downloads. */
    fun ensure(context: Context, seed: String, title: String): String? = runCatching {
        val directory = File(context.filesDir, "artwork").apply { mkdirs() }
        val target = File(directory, "cover-${hash("$seed|$title")}.png")
        if (!target.exists() || target.length() <= 0L) {
            val bitmap = render(seed, title)
            target.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 92, output)
            }
            bitmap.recycle()
        }
        if (target.length() > 0L) Uri.fromFile(target).toString() else null
    }.getOrNull()

    private fun render(seed: String, title: String): Bitmap {
        val size = 640
        val palette = palettes[paletteIndex(seed)]
        val random = Random(("$seed|$title").hashCode())
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(palette[1], palette[2], palette[0]),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        paint.shader = RadialGradient(
            size * 0.18f, size * 0.2f, size * 0.8f,
            intArrayOf(withAlpha(palette[0], 0x8C), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(size * 0.18f, size * 0.2f, size * 0.8f, paint)

        paint.shader = null
        paint.color = Color.argb(14, 255, 255, 255)
        canvas.drawCircle(
            size * 0.82f,
            size * 0.22f,
            size * (0.18f + random.nextFloat() * 0.18f),
            paint
        )
        paint.color = withAlpha(palette[0], 0x29)
        canvas.drawCircle(
            size * 0.72f,
            size * 0.68f,
            size * (0.26f + random.nextFloat() * 0.12f),
            paint
        )

        paint.color = Color.argb(26, 0, 0, 0)
        paint.strokeWidth = size * 0.012f
        canvas.save()
        canvas.rotate(-18f, size / 2f, size / 2f)
        val step = size / 7f
        repeat(9) { index ->
            val start = -size + index * step * 1.5f
            canvas.drawLine(start, 0f, start + size * 1.3f, size * 1.3f, paint)
        }
        canvas.restore()

        val bars = 9
        val barWidth = size * 0.035f
        val gap = size * 0.018f
        val startX = size * 0.13f
        val baseY = size * 0.8f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.save()
        canvas.rotate(-6f, size / 2f, baseY)
        repeat(bars) { index ->
            val height = size * (0.08f + random.nextFloat() * 0.24f)
            paint.color = if (index == 4) palette[0] else Color.argb(217, 255, 255, 255)
            val left = startX + index * (barWidth + gap)
            canvas.drawRoundRect(
                left,
                baseY - height,
                left + barWidth,
                baseY,
                barWidth / 2f,
                barWidth / 2f,
                paint
            )
        }
        canvas.restore()

        paint.color = Color.argb(26, 255, 255, 255)
        canvas.drawCircle(
            size * 0.86f,
            size * 0.13f,
            min(size * 0.03f, 30f),
            paint
        )
        return bitmap
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
