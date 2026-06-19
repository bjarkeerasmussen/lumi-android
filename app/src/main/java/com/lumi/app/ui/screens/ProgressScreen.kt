package com.lumi.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lumi.app.data.DayEntry
import com.lumi.app.data.LumiStore
import com.lumi.app.share.AiShare
import com.lumi.app.ui.components.InfoBanner
import com.lumi.app.ui.components.LumiCard
import com.lumi.app.ui.components.SectionTitle

@Composable
fun ProgressScreen(store: LumiStore) {
    val context = LocalContext.current
    val withPhotos = store.entries.filter { it.photoFile != null && store.photoFileFor(it.date).exists() }
    val first = withPhotos.minByOrNull { it.date }
    val latest = withPhotos.maxByOrNull { it.date }

    LazyColumn(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("${withPhotos.size}", "photos")
                Stat("${store.streak()}", "day streak")
                Stat("${store.entries.count { it.overall > 0 }}", "ratings")
            }
        }

        // --- Before / after cross-fade compare ---
        if (first != null && latest != null && first.date != latest.date) {
            item {
                LumiCard {
                    SectionTitle("Compare")
                    var f by remember { mutableFloatStateOf(1f) }
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = store.photoFileFor(first.date),
                            contentDescription = "First photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                        )
                        AsyncImage(
                            model = store.photoFileFor(latest.date),
                            contentDescription = "Latest photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).alpha(f)
                        )
                    }
                    Slider(value = f, onValueChange = { f = it }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(first.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(latest.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(
                        onClick = { AiShare.shareBeforeAfter(context, store, first, latest) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) { Text("Ask an AI to compare these") }
                }
            }
        }

        // --- Trend chart ---
        item {
            LumiCard {
                SectionTitle("Overall trend")
                val series = store.entries.sortedBy { it.date }.map { it.overall }.filter { it > 0 }
                if (series.size < 2) {
                    Text("Rate a few more days to see your trend line.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                } else {
                    LineChart(series, Modifier.fillMaxWidth().height(140.dp).padding(top = 10.dp))
                    Text("higher = better, over time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // --- Timeline thumbnails ---
        item { SectionTitle("Timeline", Modifier.padding(top = 4.dp)) }
        items(withPhotos.chunked(3)) { rowItems ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { e ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = store.photoFileFor(e.date),
                            contentDescription = e.date,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                        )
                        Text(e.date.substring(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                repeat(3 - rowItems.size) { Box(Modifier.weight(1f)) }
            }
        }

        if (withPhotos.isEmpty()) {
            item {
                InfoBanner("Your photo timeline and before/after will appear here once you start taking daily photos.")
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LineChart(values: List<Int>, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val maxV = 5f
        val minV = 1f
        val n = values.size
        if (n < 2) return@Canvas
        val w = size.width
        val h = size.height
        // baseline
        drawLine(grid, Offset(0f, h), Offset(w, h), strokeWidth = 2f)
        val stepX = w / (n - 1)
        fun y(v: Int) = h - ((v - minV) / (maxV - minV)) * h
        val pts = values.mapIndexed { i, v -> Offset(i * stepX, y(v).coerceIn(0f, h)) }
        for (i in 0 until pts.size - 1) {
            drawLine(color, pts[i], pts[i + 1], strokeWidth = 5f)
        }
        pts.forEach { drawCircle(color, radius = 6f, center = it) }
    }
}
