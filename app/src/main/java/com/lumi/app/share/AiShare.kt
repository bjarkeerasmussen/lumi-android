package com.lumi.app.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.lumi.app.data.DayEntry
import com.lumi.app.data.LumiStore
import java.io.File

/**
 * Hands a skin photo (optionally a before/after pair) plus a carefully framed
 * prompt to the Android share sheet, so the user can pick whichever AI app
 * they already have — Claude, Gemini, ChatGPT, etc. No backend, no API keys;
 * the photo only leaves the device if the user actively shares it.
 */
object AiShare {

    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, authority(context), file)

    /**
     * The prompt is explicit that we want NON-medical, common-pattern feedback
     * and a recommendation to see a professional for anything medical.
     */
    private fun prompt(hasBefore: Boolean): String = buildString {
        appendLine("Please give me a friendly, NON-medical read of my skin from this image. It was shared from my skin-tracking app, Lumi.")
        appendLine()
        if (hasBefore) {
            appendLine("The image contains two photos: one labelled EARLIER and one labelled LATEST (the most recent). Compare them.")
        } else {
            appendLine("The image contains one recent photo of my skin.")
        }
        appendLine()
        appendLine("Reply in exactly this format:")
        appendLine("• 3 to 5 short bullet points on what you notice — tone, brightness, redness, breakouts, texture, glow — plus gentle, general skincare suggestions.")
        if (hasBefore) {
            appendLine("• A final line written exactly as: \"Today vs last time: X/10\" — where X (1-10) rates how the LATEST photo compares to the EARLIER one (higher = improved), followed by one short sentence on why.")
        } else {
            appendLine("• A final line written exactly as: \"Today: X/10\" — an overall score (1-10) for how the skin looks, followed by one short sentence on why.")
        }
        appendLine()
        appendLine("Keep it encouraging and practical. Do NOT give a medical diagnosis or name a condition. If something looks like it needs a professional (a changing mole, a spreading or painful rash, a sore that won't heal), just say to see a dermatologist instead of guessing.")
    }

    /**
     * Share a day's photo. If an earlier photo exists, include it (labelled)
     * so the AI can score "Today vs last time"; otherwise share just this one.
     */
    fun shareSingle(context: Context, store: LumiStore, dateKey: String): Boolean {
        val file = store.photoFileFor(dateKey)
        if (!file.exists()) return false

        val previous = store.entries.firstOrNull {
            it.date < dateKey && it.photoFile != null && store.photoFileFor(it.date).exists()
        }
        val hasBefore = previous != null
        val text = prompt(hasBefore = hasBefore)
        copyPromptToClipboard(context, text)

        val items = if (previous != null) {
            listOf(
                "Earlier — ${previous.date}" to store.photoFileFor(previous.date),
                "Latest — $dateKey" to file
            )
        } else {
            listOf("" to file)
        }
        // Prefer the composite that bakes the prompt into the image; fall back
        // to the raw photo if composition fails for any reason.
        val toShare = PromptImage.compose(items, text, store.shareDirectory()) ?: file
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uriFor(context, toShare))
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launch(context, intent)
        return true
    }

    /** Share an earlier photo + the latest photo as a before/after pair. */
    fun shareBeforeAfter(context: Context, store: LumiStore, before: DayEntry, after: DayEntry): Boolean {
        val beforeFile = store.photoFileFor(before.date)
        val afterFile = store.photoFileFor(after.date)
        if (!beforeFile.exists() || !afterFile.exists()) return false
        val text = prompt(hasBefore = true)
        copyPromptToClipboard(context, text)
        // Single composite image: earlier photo + latest photo + prompt panel.
        val composite = PromptImage.compose(
            listOf("Earlier — ${before.date}" to beforeFile, "Latest — ${after.date}" to afterFile),
            text,
            store.shareDirectory()
        )
        val intent = if (composite != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uriFor(context, composite))
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    arrayListOf(uriFor(context, beforeFile), uriFor(context, afterFile))
                )
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        launch(context, intent)
        return true
    }

    /**
     * Copies the prompt to the clipboard and tells the user, because most AI
     * apps (ChatGPT, Gemini, Claude) accept a shared image but ignore the
     * shared caption text — so the reliable way to include the prompt is to
     * paste it into the chat. The EXTRA_TEXT above still covers apps that do
     * read it.
     */
    private fun copyPromptToClipboard(context: Context, text: String) {
        runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Lumi skin prompt", text))
            Toast.makeText(
                context,
                "Prompt copied — paste it into the chat with your photo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun launch(context: Context, intent: Intent) {
        val chooser = Intent.createChooser(intent, "Ask Claude, Gemini or ChatGPT")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
