package com.lumi.app.share

import android.content.Context
import android.content.Intent
import android.net.Uri
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
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            putExtra(Intent.EXTRA_TEXT, prompt(hasBefore = false))
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
        val uris = arrayListOf(uriFor(context, beforeFile), uriFor(context, afterFile))
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_TEXT, prompt(hasBefore = true))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launch(context, intent)
        return true
    }

    private fun launch(context: Context, intent: Intent) {
        val chooser = Intent.createChooser(intent, "Ask Claude, Gemini or ChatGPT")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
