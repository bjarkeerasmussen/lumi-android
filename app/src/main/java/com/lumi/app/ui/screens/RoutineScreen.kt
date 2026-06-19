package com.lumi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.lumi.app.data.DayEntry
import com.lumi.app.data.LedSession
import com.lumi.app.data.LumiStore
import com.lumi.app.ui.components.InfoBanner
import com.lumi.app.ui.components.LumiCard
import com.lumi.app.ui.components.Pill
import com.lumi.app.ui.components.SectionTitle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutineScreen(store: LumiStore) {
    val todayKey = store.todayKey()
    val entry: DayEntry = store.entries.firstOrNull { it.date == todayKey } ?: DayEntry(todayKey)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Today's routine", style = MaterialTheme.typography.headlineMedium)

        ProductCard("Morning", entry.routineAm,
            onAdd = { p -> store.upsert(entry.copy(routineAm = (entry.routineAm + p).toMutableList())) },
            onRemove = { p -> store.upsert(entry.copy(routineAm = entry.routineAm.filterNot { it == p }.toMutableList())) }
        )
        ProductCard("Evening", entry.routinePm,
            onAdd = { p -> store.upsert(entry.copy(routinePm = (entry.routinePm + p).toMutableList())) },
            onRemove = { p -> store.upsert(entry.copy(routinePm = entry.routinePm.filterNot { it == p }.toMutableList())) }
        )

        // --- LED light therapy ---
        LumiCard {
            SectionTitle("LED light therapy")
            Text(
                "Log a mask session (Therabody, Omnilux, etc.). These use LED light — never UV.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            var type by remember { mutableStateOf("Red") }
            var minutes by remember { mutableIntStateOf(10) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Red", "Blue", "Near-infrared").forEach { t ->
                    Pill(t, selected = type == t, onClick = { type = t })
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = if (minutes == 0) "" else minutes.toString(),
                    onValueChange = { minutes = it.filter { c -> c.isDigit() }.take(3).toIntOrNull() ?: 0 },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (minutes > 0) {
                        store.upsert(entry.copy(ledSessions = (entry.ledSessions + LedSession(type, minutes)).toMutableList()))
                    }
                }) { Icon(Icons.Filled.Add, contentDescription = "Add session") }
            }
            entry.ledSessions.forEach { s ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${s.type} · ${s.minutes} min", style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = {
                        val idx = entry.ledSessions.indexOf(s)
                        val newList = entry.ledSessions.toMutableList().also { if (idx >= 0) it.removeAt(idx) }
                        store.upsert(entry.copy(ledSessions = newList))
                    }) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
                }
            }
        }

        InfoBanner("Logging what you do each day lets Progress line your routine up with how your skin looks over time.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductCard(
    title: String,
    items: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    LumiCard {
        SectionTitle(title)
        var text by remember { mutableStateOf("") }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Add a product") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { if (text.isNotBlank()) { onAdd(text.trim()); text = "" } }) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(p, selected = true, onClick = { onRemove(p) })
                }
            }
        }
        if (items.isEmpty()) {
            Text("Nothing logged yet.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        } else {
            Text("Tap a product to remove it.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
