package cx.viz.balisurf.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * MVP UI: a list screen of the 5 Bukit spots; tapping one opens a detail screen
 * with the 24h timeline, tides, and reef notes. The product value is the verdict
 * text + timeline, not chrome. Single-level nav via selection state — no nav lib
 * needed for one level.
 */
@Composable
fun App(module: AppModule) = MaterialTheme {
    var state by remember { mutableStateOf<List<SpotForecast>?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        state = module.loadAll()
    }

    val data = state
    val selected = data?.firstOrNull { it.spot.id == selectedId }

    if (selected != null) {
        SpotDetailScreen(
            sf = selected,
            logs = module.logs,
            onBack = { selectedId = null },
        )
        return@MaterialTheme
    }

    Scaffold { padding ->
        if (data == null) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text("Reading the ocean…", Modifier.padding(top = 12.dp))
            }
        } else {
            // Group by region, preserving catalog order. Best spot's stars per
            // region could sort later; for now keep the curated order.
            val grouped = data.groupBy { it.spot.region }
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "Bali surf — today",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                grouped.forEach { (region, spots) ->
                    item(key = "hdr-$region") {
                        Text(
                            region,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                    }
                    items(spots, key = { it.spot.id }) { sf ->
                        SpotCard(sf, onClick = { selectedId = sf.spot.id })
                    }
                }
                item {
                    Text(
                        "Forecast data © Open-Meteo (CC BY 4.0). Tides are relative bands, not chart datum.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotCard(sf: SpotForecast, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val stars = sf.verdict?.stars ?: 0
            Text(
                "${sf.spot.name}   ${"★".repeat(stars)}${"☆".repeat(5 - stars)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(sf.verdict?.headline ?: "No forecast available")
            sf.verdict?.bestWindow?.let { w ->
                Text(
                    "Best window: ${hhmm(w.startIso)}–${hhmm(w.endIso)} (${w.peakStars}★)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (sf.tides.isNotEmpty()) {
                Text(
                    "Tides: " + sf.tides.joinToString("  ") {
                        "${if (it.kind.name == "HIGH") "▲" else "▼"} ${hhmm(it.timeIso)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** "2026-08-15T07:00" -> "07:00". Shared with the detail screen. */
internal fun hhmm(iso: String): String = iso.substringAfter('T').take(5)
