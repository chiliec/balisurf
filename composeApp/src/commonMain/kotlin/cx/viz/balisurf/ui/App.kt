package cx.viz.balisurf.ui

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
 * MVP UI: one list screen. Each row is a spot + its star verdict + one-line call
 * and best window. Deliberately minimal — the product value is the verdict text,
 * not chrome. Detail screen / timeline come after the thesis is validated.
 */
@Composable
fun App(module: AppModule) = MaterialTheme {
    var state by remember { mutableStateOf<List<SpotForecast>?>(null) }

    LaunchedEffect(Unit) {
        state = module.loadAll()
    }

    Scaffold { padding ->
        val data = state
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
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "Bukit — today",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(data) { sf -> SpotCard(sf) }
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
private fun SpotCard(sf: SpotForecast) {
    Card(Modifier.fillMaxWidth()) {
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
        }
    }
}

/** "2026-08-15T07:00" -> "07:00". */
private fun hhmm(iso: String): String = iso.substringAfter('T').take(5)
