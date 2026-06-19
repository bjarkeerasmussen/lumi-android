package com.lumi.app.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lumi.app.MainActivity
import com.lumi.app.R
import com.lumi.app.data.LumiStore

/** Posts the daily reminder — but only if today's photo isn't already taken. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val store = LumiStore.get(context)
        val today = store.entryFor(store.todayKey())
        if (today?.photoFile != null) return // already done today

        Reminder.ensureChannel(context)

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, Reminder.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Today's skin photo")
            .setContentText("A quick photo keeps your progress streak going ✨")
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(4202, notif)
    }
}
