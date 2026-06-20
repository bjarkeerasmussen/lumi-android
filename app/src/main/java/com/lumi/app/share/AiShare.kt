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
        appendLine("I'm tracking my skin over time and would like a friendly, non-medical read of these photos.")
        appendLine()
        if (hasBefore) {
            appendLine("There are two photos: the first is an EARLIER photo, the second is the MOST RECENT. Please compare them and tell me what visibly changed (tone, redness, breakouts, texture, glow).")
        } else {
            appendLine("Please look at this photo and describe what you notice.")
        }
        appendLine()
        appendLine("Please:")
        appendLine("• Point out common, everyday skin patterns you can see (e.g. dryness, oiliness, clogged pores, redness, breakouts, dullness, dark marks).")
        appendLine("• Suggest gentle, general skincare steps and ingredients that commonly help with those patterns.")
        appendLine("• Keep it encouraging and practical.")
        appendLine()
        appendLine("Important: do NOT attempt a medical diagnosis or name a medical condition. If you see anything that should be checked by a dermatologist or doctor (for example a changing mole, a spreading or painful rash, or a sore that won't heal), just tell me to see a professional rather than guessing.")
    }

    /** Share just today's (or a given day's) photo. Returns false if no photo. */
    fun shareSingle(context: Context, store: LumiStore, dateKey: String): Boolean {
        val file = store.photoFileFor(dateKey)
        if (!file.exists()) return false
        val text = prompt(hasBefore = false)
        copyPromptToClipboard(context, text)
        // Prefer the composite that bakes the prompt into the image; fall back
        // to the raw photo if composition fails for any reason.
        val toShare = PromptImage.compose(listOf("" to file), text, store.shareDirectory()) ?: file
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
