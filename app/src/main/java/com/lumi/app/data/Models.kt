package com.lumi.app.data

import org.json.JSONArray
import org.json.JSONObject

/** One logged LED light-therapy session. Type is Red / Blue / Near-infrared. */
data class LedSession(
    val type: String,
    val minutes: Int
) {
    fun toJson(): JSONObject = JSONObject()
        .put("type", type)
        .put("minutes", minutes)

    companion object {
        fun fromJson(o: JSONObject) = LedSession(
            type = o.optString("type", "Red"),
            minutes = o.optInt("minutes", 10)
        )
    }
}

/**
 * The result of a Skin Check self-assessment.
 *
 * IMPORTANT: this is a NON-MEDICAL, cosmetic self-assessment. [patterns] are
 * everyday skin patterns (e.g. "Dryness / dehydration"), never medical
 * diagnoses. [redFlag] is true when the user reported something that should be
 * looked at by a dermatologist.
 */
data class SkinCheckResult(
    val patterns: List<String>,
    val redFlag: Boolean,
    val noteForUser: String
) {
    fun toJson(): JSONObject = JSONObject()
        .put("patterns", JSONArray(patterns))
        .put("redFlag", redFlag)
        .put("noteForUser", noteForUser)

    companion object {
        fun fromJson(o: JSONObject): SkinCheckResult {
            val pats = mutableListOf<String>()
            val arr = o.optJSONArray("patterns") ?: JSONArray()
            for (i in 0 until arr.length()) pats.add(arr.getString(i))
            return SkinCheckResult(
                patterns = pats,
                redFlag = o.optBoolean("redFlag", false),
                noteForUser = o.optString("noteForUser", "")
            )
        }
    }
}

/**
 * A single day's journal entry. [date] is the local date key "yyyy-MM-dd".
 * Ratings are 0 (not set) or 1..5. [photoFile] is a bare filename inside the
 * app-private photos dir, or null if no photo yet.
 */
data class DayEntry(
    val date: String,
    var photoFile: String? = null,
    var overall: Int = 0,
    var redness: Int = 0,
    var breakouts: Int = 0,
    var note: String = "",
    var routineAm: MutableList<String> = mutableListOf(),
    var routinePm: MutableList<String> = mutableListOf(),
    var ledSessions: MutableList<LedSession> = mutableListOf(),
    var skinCheck: SkinCheckResult? = null
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("date", date)
        o.put("photoFile", photoFile ?: JSONObject.NULL)
        o.put("overall", overall)
        o.put("redness", redness)
        o.put("breakouts", breakouts)
        o.put("note", note)
        o.put("routineAm", JSONArray(routineAm as List<*>))
        o.put("routinePm", JSONArray(routinePm as List<*>))
        val led = JSONArray()
        ledSessions.forEach { led.put(it.toJson()) }
        o.put("ledSessions", led)
        o.put("skinCheck", skinCheck?.toJson() ?: JSONObject.NULL)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): DayEntry {
            fun strList(key: String): MutableList<String> {
                val out = mutableListOf<String>()
                val arr = o.optJSONArray(key) ?: JSONArray()
                for (i in 0 until arr.length()) out.add(arr.getString(i))
                return out
            }
            val led = mutableListOf<LedSession>()
            val ledArr = o.optJSONArray("ledSessions") ?: JSONArray()
            for (i in 0 until ledArr.length()) led.add(LedSession.fromJson(ledArr.getJSONObject(i)))
            val scObj = o.opt("skinCheck")
            return DayEntry(
                date = o.getString("date"),
                photoFile = if (o.isNull("photoFile")) null else o.optString("photoFile", null),
                overall = o.optInt("overall", 0),
                redness = o.optInt("redness", 0),
                breakouts = o.optInt("breakouts", 0),
                note = o.optString("note", ""),
                routineAm = strList("routineAm"),
                routinePm = strList("routinePm"),
                ledSessions = led,
                skinCheck = if (scObj is JSONObject) SkinCheckResult.fromJson(scObj) else null
            )
        }
    }
}
