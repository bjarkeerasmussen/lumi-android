package com.lumi.app.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream

/**
 * Builds a single shareable JPEG that stacks the skin photo(s) with the AI
 * prompt rendered as a readable text panel underneath. This bakes the prompt
 * *into the image*, so any AI that reads the picture also reads the
 * instructions — independent of whether the receiving app keeps the shared
 * caption text.
 */
object PromptImage {

    private const val W = 1080
    private const val MARGIN = 40f
    private const val LABEL_SIZE = 32f
    private const val TEXT_SIZE = 34f
    private const val BG = "#0E0B1E"        // near-black indigo behind photos
    private const val PANEL = "#1C1736"     // Nightfall bg for the text panel

    /**
     * @param items label (may be empty) -> photo file, drawn top to bottom
     * @param prompt instruction text rendered in the bottom panel
     * @param outDir cache/share directory to write into
     */
    fun compose(items: List<Pair<String, File>>, prompt: String, outDir: File): File? {
        val photos = items.mapNotNull { (label, f) ->
            val bmp = decodeScaled(f, W) ?: return@mapNotNull null
            label to bmp
        }
        if (photos.isEmpty()) return null

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = TEXT_SIZE
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B6A6F2")
            textSize = LABEL_SIZE
            isFakeBoldText = true
        }
        val panelW = (W - MARGIN * 2).toInt()
        val layout = StaticLayout.Builder
            .obtain(prompt, 0, prompt.length, textPaint, panelW)
            .setLineSpacing(8f, 1f)
            .build()
        val panelH = layout.height + MARGIN * 2

        var total = 0f
        photos.forEach { (label, bmp) ->
            if (label.isNotEmpty()) total += LABEL_SIZE + MARGIN
            total += bmp.height + 8f
        }
        total += panelH

        val result = Bitmap.createBitmap(W, total.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(result)
        c.drawColor(Color.parseColor(BG))

        var y = 0f
        photos.forEach { (label, bmp) ->
            if (label.isNotEmpty()) {
                c.drawText(label.uppercase(), MARGIN, y + MARGIN * 0.5f + LABEL_SIZE, labelPaint)
                y += LABEL_SIZE + MARGIN
            }
            c.drawBitmap(bmp, (W - bmp.width) / 2f, y, null)
            y += bmp.height + 8f
            bmp.recycle()
        }

        val panelPaint = Paint().apply { color = Color.parseColor(PANEL) }
        c.drawRect(0f, y, W.toFloat(), total, panelPaint)
        c.save()
        c.translate(MARGIN, y + MARGIN)
        layout.draw(c)
        c.restore()

        return runCatching {
            val out = File(outDir, "lumi-ai-share.jpg")
            FileOutputStream(out).use { result.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            result.recycle()
            out
        }.getOrNull()
    }

    /** Decode [f] downsampled, then scale exactly to [targetW] preserving aspect. */
    private fun decodeScaled(f: File, targetW: Int): Bitmap? {
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, bounds)
        val srcW = bounds.outWidth
        if (srcW <= 0) return null
        var sample = 1
        while (srcW / sample > targetW * 2) sample *= 2
        val decoded = BitmapFactory.decodeFile(
            f.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null
        val scale = targetW.toFloat() / decoded.width
        val h = (decoded.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(decoded, targetW, h, true)
        if (scaled != decoded) decoded.recycle()
        return scaled
    }
}
