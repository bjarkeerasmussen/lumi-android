package com.lumi.app.analysis

import android.graphics.BitmapFactory
import com.lumi.app.data.PhotoMetrics
import java.io.File
import kotlin.math.sqrt

/**
 * On-device, non-medical photo analysis. Computes simple cosmetic/photographic
 * metrics from a daily photo and turns a comparison against the previous photo
 * into a warm, complimentary message. No network, no ML model — just pixels.
 */
object PhotoAnalysis {

    data class Insight(val headline: String, val lines: List<String>)

    /** Decode a downsampled copy of [file] and measure brightness/redness/unevenness over the central face area. */
    fun analyze(file: File): PhotoMetrics? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val srcW = bounds.outWidth
        if (srcW <= 0) return null
        var sample = 1
        while (srcW / sample > 320) sample *= 2
        val bmp = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null

        val bw = bmp.width
        val bh = bmp.height
        val x0 = (bw * 0.20f).toInt()
        val x1 = (bw * 0.80f).toInt()
        val y0 = (bh * 0.15f).toInt()
        val y1 = (bh * 0.85f).toInt()
        val stepX = maxOf(1, (x1 - x0) / 120)
        val stepY = maxOf(1, (y1 - y0) / 120)

        var n = 0L
        var sumR = 0.0; var sumG = 0.0; var sumB = 0.0
        var sumL = 0.0; var sumLL = 0.0
        var y = y0
        while (y < y1) {
            var x = x0
            while (x < x1) {
                val p = bmp.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val lum = 0.299 * r + 0.587 * g + 0.114 * b
                sumR += r; sumG += g; sumB += b
                sumL += lum; sumLL += lum * lum
                n++
                x += stepX
            }
            y += stepY
        }
        bmp.recycle()
        if (n == 0L) return null

        val mr = sumR / n
        val mg = sumG / n
        val mb = sumB / n
        val ml = sumL / n
        val variance = (sumLL / n - ml * ml).coerceAtLeast(0.0)
        val redness = (mr - (mg + mb) / 2.0).coerceAtLeast(0.0)
        return PhotoMetrics(
            brightness = ml.toFloat(),
            redness = redness.toFloat(),
            unevenness = sqrt(variance).toFloat()
        )
    }

    /** Build a complimentary insight comparing today's metrics to the previous photo. */
    fun buildInsight(today: PhotoMetrics, previous: PhotoMetrics?, streak: Int): Insight {
        val lines = mutableListOf<String>()
        if (previous == null) {
            lines += "This is your baseline — every future photo compares back to here. Lovely start. 💜"
        } else {
            val db = today.brightness - previous.brightness
            when {
                db > 6 -> lines += "A little brighter and more radiant than your last photo. ✨"
                db < -6 -> lines += "Lighting's a touch softer than last time — same spot and window each day gives the cleanest compare."
                else -> lines += "Beautifully consistent lighting with your last photo."
            }
            val dr = today.redness - previous.redness
            when {
                dr < -2.5 -> lines += "Looks a little calmer today — less redness than last time."
                dr > 2.5 -> lines += "A bit more warmth showing than last time (often just temperature or light)."
            }
            val du = today.unevenness - previous.unevenness
            if (du < -3) lines += "Your tone looks more even than before. 💫"
        }
        if (streak >= 2) lines += "Day $streak in a row — that consistency is what makes the before/after shine. 🔥"
        return Insight(headline = compliment(streak), lines = lines.take(4))
    }

    private fun compliment(streak: Int): String {
        val options = listOf(
            "You're glowing today ✨",
            "Looking lovely — thanks for checking in 💜",
            "Fresh photo, and you look radiant ✨",
            "Beautiful — that's today captured 💫"
        )
        return options[streak.coerceAtLeast(0) % options.size]
    }
}
