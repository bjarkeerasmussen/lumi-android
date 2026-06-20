package com.lumi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lumi.app.analysis.PhotoAnalysis
import com.lumi.app.data.DayEntry
import com.lumi.app.data.LumiStore
import com.lumi.app.share.AiShare
import com.lumi.app.ui.components.InfoBanner
import com.lumi.app.ui.components.LumiCard
import com.lumi.app.ui.components.RatingSelector
import com.lumi.app.ui.components.SectionTitle

@Composable
fun TodayScreen(
    store: LumiStore,
    onOpenSkinCheck: () -> Unit,
    onTakePhoto: () -> Unit,
    requestNotifPermission: () -> Unit
) {
    val context = LocalContext.current
    val todayKey = store.todayKey()

    // Read today's entry reactively from the store.
    val entry: DayEntry = store.entries.firstOrNull { it.date == todayKey } ?: DayEntry(todayKey)
    val photoFile = store.photoFileFor(todayKey)
    val hasPhoto = entry.photoFile != null && photoFile.exists()

    // Instant, complimentary read of today's photo vs. previous photos.
    val insight = if (hasPhoto && entry.metrics != null) {
        val prevMetrics = store.entries.firstOrNull { it.date < todayKey && it.metrics != null }?.metrics
        PhotoAnalysis.buildInsight(entry.metrics!!, prevMetrics, store.streak())
    } else null

    // Open the face-alignment camera, prompting for the reminder permission too.
    fun launchCamera() {
        requestNotifPermission()
        onTakePhoto()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Today", style = MaterialTheme.typography.headlineMedium)
            Text("🔥 ${store.streak()}-day streak", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }

        // --- Photo card ---
        LumiCard {
            if (hasPhoto) {
                val cacheKey = "today-${photoFile.lastModified()}"
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoFile)
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .build(),
                    contentDescription = "Today's skin photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(16.dp))
                )
                OutlinedButton(onClick = { launchCamera() }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Icon(Icons.Filled.CameraAlt, null); Text("  Retake today's photo")
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                    Text("No photo yet today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Same spot, same light each day works best.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { launchCamera() }, modifier = Modifier.padding(top = 12.dp)) {
                        Icon(Icons.Filled.CameraAlt, null); Text("  Take today's photo")
                    }
                }
            }
        }

        // --- Instant analysis ---
        if (insight != null) {
            LumiCard {
                Text(
                    insight.headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                insight.lines.forEach { line ->
                    Text(
                        "•  $line",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Text(
                    "A friendly read of your photo — not a medical assessment.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // --- Quick ratings ---
        LumiCard {
            SectionTitle("How does it look today?")
            RatingSelector("Overall", entry.overall, { v -> store.upsert(entry.copy(overall = v)) }, "rough", "great")
            RatingSelector("Redness", entry.redness, { v -> store.upsert(entry.copy(redness = v)) }, "none", "a lot")
            RatingSelector("Breakouts", entry.breakouts, { v -> store.upsert(entry.copy(breakouts = v)) }, "clear", "many")
            OutlinedTextField(
                value = entry.note,
                onValueChange = { v -> store.upsert(entry.copy(note = v)) },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                minLines = 2
            )
        }

        // --- Actions ---
        LumiCard {
            SectionTitle("Tools")
            OutlinedButton(
                onClick = onOpenSkinCheck,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Filled.HealthAndSafety, null); Text("  Skin Check (common patterns)")
            }
            OutlinedButton(
                onClick = {
                    if (!AiShare.shareSingle(context, store, todayKey)) { /* no photo yet */ }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Filled.Share, null); Text("  Ask an AI about today's photo")
            }
            Text(
                "AI analysis opens your share menu so you can pick Claude, Gemini or ChatGPT. Your photo stays on your phone until you choose to send it.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        InfoBanner(
            "Lumi tracks your skin and offers general care tips. It can't diagnose. " +
                "For anything painful, spreading, or that won't heal, see a dermatologist."
        )
    }
}
