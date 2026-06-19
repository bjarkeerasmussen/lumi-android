package com.lumi.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * On-device journal store. Everything lives in the app's private storage:
 * photos as JPEG files under filesDir/photos, and the journal as a single
 * journal.json. No network, no cloud, no analytics.
 *
 * Held as a process singleton and observed directly by Compose via the
 * [entries] snapshot list.
 */
class LumiStore private constructor(appContext: Context) {

    private val filesDir = appContext.filesDir
    private val photosDir = File(filesDir, "photos").apply { mkdirs() }
    private val shareDir = File(appContext.cacheDir, "share").apply { mkdirs() }
    private val journalFile = File(filesDir, "journal.json")

    /** Newest-first list of day entries, observable by Compose. */
    val entries: SnapshotStateList<DayEntry> = mutableStateListOf()

    init {
        load()
    }

    // ---- date helpers ----------------------------------------------------

    private val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayKey(): String = keyFmt.format(Date())

    fun photoFileFor(dateKey: String): File = File(photosDir, "$dateKey.jpg")

    fun photosDirectory(): File = photosDir

    fun shareDirectory(): File = shareDir

    // ---- read ------------------------------------------------------------

    fun entryFor(dateKey: String): DayEntry? = entries.firstOrNull { it.date == dateKey }

    fun today(): DayEntry = entryFor(todayKey()) ?: DayEntry(todayKey())

    /** Most recent entry that actually has a saved photo, excluding [exceptKey]. */
    fun latestWithPhoto(exceptKey: String? = null): DayEntry? =
        entries.firstOrNull { it.photoFile != null && it.date != exceptKey }

    /** Current consecutive-day streak counting back from today. */
    fun streak(): Int {
        if (entries.isEmpty()) return 0
        val have = entries.map { it.date }.toHashSet()
        var count = 0
        val cal = Calendar.getInstance()
        while (true) {
            val key = keyFmt.format(cal.time)
            if (have.contains(key)) {
                count++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        return count
    }

    // ---- write -----------------------------------------------------------

    /** Insert or replace an entry, keeping the list sorted newest-first, then persist. */
    fun upsert(entry: DayEntry) {
        val idx = entries.indexOfFirst { it.date == entry.date }
        if (idx >= 0) entries[idx] = entry else entries.add(entry)
        entries.sortByDescending { it.date }
        save()
    }

    fun deleteEntry(dateKey: String) {
        entries.removeAll { it.date == dateKey }
        photoFileFor(dateKey).delete()
        save()
    }

    private fun load() {
        entries.clear()
        if (!journalFile.exists()) return
        runCatching {
            val arr = JSONArray(journalFile.readText())
            val parsed = ArrayList<DayEntry>(arr.length())
            for (i in 0 until arr.length()) parsed.add(DayEntry.fromJson(arr.getJSONObject(i)))
            parsed.sortByDescending { it.date }
            entries.addAll(parsed)
        }
    }

    fun save() {
        runCatching {
            val arr = JSONArray()
            entries.forEach { arr.put(it.toJson()) }
            journalFile.writeText(arr.toString())
        }
    }

    companion object {
        @Volatile private var instance: LumiStore? = null
        fun get(context: Context): LumiStore =
            instance ?: synchronized(this) {
                instance ?: LumiStore(context.applicationContext).also { instance = it }
            }
    }
}
