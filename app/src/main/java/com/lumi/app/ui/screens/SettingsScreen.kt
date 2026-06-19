package com.lumi.app.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.lumi.app.data.LumiStore
import com.lumi.app.data.Prefs
import com.lumi.app.reminder.Reminder
import com.lumi.app.ui.components.InfoBanner
import com.lumi.app.ui.components.LumiCard
import com.lumi.app.ui.components.SectionTitle
import java.io.File

@Composable
fun SettingsScreen(store: LumiStore, prefs: Prefs, onBack: () -> Unit) {
    val context = LocalContext.current
    var reminderOn by remember { mutableStateOf(prefs.reminderEnabled) }
    var minutes by remember { mutableIntStateOf(prefs.reminderMinutes) }

    fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Reminder ---
        LumiCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    SectionTitle("Daily photo reminder")
                    Text("A gentle nudge if you haven't taken today's photo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = reminderOn, onCheckedChange = {
                    reminderOn = it
                    prefs.reminderEnabled = it
                    if (it) Reminder.schedule(context, minutes) else Reminder.cancel(context)
                })
            }
            if (reminderOn) {
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            minutes = h * 60 + m
                            prefs.reminderMinutes = minutes
                            Reminder.schedule(context, minutes)
                        }, minutes / 60, minutes % 60, true).show()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) { Text("Reminder time: ${fmt(minutes)}") }
            }
        }

        // --- Privacy ---
        LumiCard {
            SectionTitle("Your data")
            Text(
                "Photos and your journal stay on this phone. Nothing is uploaded. " +
                    "AI analysis only sends a photo when you tap share and pick an app yourself.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
            OutlinedButton(
                onClick = { exportJournal(context, store) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) { Text("Export journal (backup)") }
        }

        // --- About ---
        LumiCard {
            SectionTitle("About Lumi")
            Text(
                "Lumi is a personal skin-progress journal. It helps you track and care for your skin — it is not a medical device and does not diagnose. For anything painful, spreading, changing, or that won't heal, please see a dermatologist.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        InfoBanner("Made for Jayu 💛")
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

/** Copy journal.json into the shareable cache dir and offer it via the share sheet. */
private fun exportJournal(context: android.content.Context, store: LumiStore) {
    store.save()
    val src = File(context.filesDir, "journal.json")
    if (!src.exists()) return
    val out = File(store.shareDirectory(), "lumi-journal-backup.json")
    src.copyTo(out, overwrite = true)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Export Lumi journal").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
