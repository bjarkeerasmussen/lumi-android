package com.lumi.app.data

import android.content.Context

/** Tiny wrapper over SharedPreferences for app settings (no extra deps). */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("lumi_prefs", Context.MODE_PRIVATE)

    var reminderEnabled: Boolean
        get() = sp.getBoolean("reminder_enabled", false)
        set(v) = sp.edit().putBoolean("reminder_enabled", v).apply()

    /** Minutes after midnight for the daily reminder. Default 20:00. */
    var reminderMinutes: Int
        get() = sp.getInt("reminder_minutes", 20 * 60)
        set(v) = sp.edit().putInt("reminder_minutes", v).apply()

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()
}
