package com.lumi.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lumi.app.data.LumiStore
import com.lumi.app.data.SkinCheckResult
import com.lumi.app.skincheck.SkinCheck
import com.lumi.app.ui.components.InfoBanner
import com.lumi.app.ui.components.LumiCard
import com.lumi.app.ui.components.SectionTitle

@Composable
fun SkinCheckScreen(store: LumiStore, onDone: () -> Unit) {
    val signSel: SnapshotStateMap<String, Boolean> = remember { mutableStateMapOf() }
    val flagSel: SnapshotStateMap<String, Boolean> = remember { mutableStateMapOf() }
    var result by remember { mutableStateOf<SkinCheckResult?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        InfoBanner(
            "A quick, non-medical self-check. It spots common, everyday skin patterns and suggests general care — it does not diagnose conditions."
        )

        if (result == null) {
            SkinCheck.signGroups.forEach { (group, signs) ->
                LumiCard {
                    SectionTitle(group)
                    signs.forEach { sign ->
                        CheckRow(
                            label = sign.label,
                            checked = signSel[sign.id] == true,
                            onToggle = { signSel[sign.id] = !(signSel[sign.id] ?: false) }
                        )
                    }
                }
            }

            LumiCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    SectionTitle("Anything like this?")
                }
                Text(
                    "Tick anything true — these belong with a professional, not an app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                SkinCheck.redFlags.forEach { flag ->
                    CheckRow(
                        label = flag.label,
                        checked = flagSel[flag.id] == true,
                        onToggle = { flagSel[flag.id] = !(flagSel[flag.id] ?: false) }
                    )
                }
            }

            Button(
                onClick = {
                    val signs = signSel.filterValues { it }.keys.toSet()
                    val flags = flagSel.filterValues { it }.keys.toSet()
                    val r = SkinCheck.evaluate(signs, flags)
                    result = r
                    // Save to today's entry.
                    val today = store.entryFor(store.todayKey()) ?: com.lumi.app.data.DayEntry(store.todayKey())
                    store.upsert(today.copy(skinCheck = r))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Show my skin check") }
        } else {
            ResultCard(result!!)
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            Text(
                "Saved to today's entry.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ResultCard(result: SkinCheckResult) {
    if (result.redFlag) {
        Column(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Please see a professional", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Text(result.noteForUser, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
        }
        return
    }

    LumiCard {
        SectionTitle("What your skin might be showing")
        if (result.patterns.isEmpty()) {
            Text(result.noteForUser, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        } else {
            val advice = SkinCheck.adviceFor(result.patterns)
            advice.forEach { p ->
                Column(Modifier.padding(top = 12.dp)) {
                    Text(p.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text(p.advice, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Text(
                result.noteForUser,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}
