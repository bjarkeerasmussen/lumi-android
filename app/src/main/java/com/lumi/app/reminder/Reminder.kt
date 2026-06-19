package com.lumi.app.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/** Schedules / cancels the daily "take today's photo" reminder via AlarmManager. */
object Reminder {

    const val CHANNEL_ID = "lumi_daily_reminder"
    private const val REQUEST_CODE = 4201

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            val existing = mgr.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily skin photo reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "A gentle nudge to take today's skin photo." }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Schedule a daily inexact alarm at [minutesAfterMidnight]. */
    fun schedule(context: Context, minutesAfterMidnight: Int) {
        ensureChannel(context)
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val first = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutesAfterMidnight / 60)
            set(Calendar.MINUTE, minutesAfterMidnight % 60)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        mgr.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            first.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mgr.cancel(pendingIntent(context))
    }
}
