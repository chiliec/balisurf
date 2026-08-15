package cx.viz.balisurf.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cx.viz.balisurf.data.SessionLogStore
import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.platform.nowIso
import cx.viz.balisurf.scoring.SpotScorer
import balisurf.composeapp.generated.resources.Res
import balisurf.composeapp.generated.resources.reef_balangan
import balisurf.composeapp.generated.resources.reef_batubolong
import balisurf.composeapp.generated.resources.reef_bingin
import balisurf.composeapp.generated.resources.reef_desertpoint
import balisurf.composeapp.generated.resources.reef_dreamland
import balisurf.composeapp.generated.resources.reef_greenbowl
import balisurf.composeapp.generated.resources.reef_impossibles
import balisurf.composeapp.generated.resources.reef_nusadua
import balisurf.composeapp.generated.resources.reef_nyangnyang
import balisurf.composeapp.generated.resources.reef_padang
import balisurf.composeapp.generated.resources.reef_shipwrecks
import balisurf.composeapp.generated.resources.reef_uluwatu
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Spot detail screen: the full read on one break — verdict, a 24h star timeline,
 * tide turning points, reef notes, and the session-log widget (the crowdsource
 * loop). Reuses the pure SpotScorer so the timeline matches the list verdict.
 */
@Composable
fun SpotDetailScreen(sf: SpotForecast, logs: SessionLogStore, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("‹ Back") }

        val stars = sf.verdict?.stars ?: 0
        Text(
            sf.spot.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${"★".repeat(stars)}${"☆".repeat(5 - stars)}",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(sf.verdict?.headline ?: "No forecast available")

        SessionLogCard(sf, logs)

        if (sf.hours.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Next 24h", fontWeight = FontWeight.Bold)
                    TimelineChart(sf)
                }
            }
        }

        reefDrawable(sf.spot.id)?.let { res ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reef (satellite bathymetry)", fontWeight = FontWeight.Bold)
                    Image(
                        painter = painterResource(res),
                        contentDescription = "${sf.spot.name} reef bathymetry",
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        "Warm = shallow reef, cool = deep water. Derived from free Sentinel-2 " +
                            "imagery (relative depth). Experimental.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        sf.verdict?.bestWindow?.let { w ->
            Text("Best window: ${hhmm(w.startIso)}–${hhmm(w.endIso)} (${w.peakStars}★)")
        }

        if (sf.tides.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tides", fontWeight = FontWeight.Bold)
                    sf.tides.forEach { t ->
                        val arrow = if (t.kind.name == "HIGH") "▲ High" else "▼ Low"
                        Text("$arrow  ${hhmm(t.timeIso)}   ${fmt1(t.heightMeters)} m")
                    }
                }
            }
        }

        // Current conditions (peak hour of the day for a representative read).
        sf.hours.maxByOrNull { SpotScorer.scoreHour(sf.spot, it) }?.let { c ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Peak conditions", fontWeight = FontWeight.Bold)
                    Text("Swell: ${fmt1(c.swellHeightMeters)} m ${SpotScorer.compass(c.swellDirectionDeg)} @ ${c.swellPeriodSeconds.toInt()}s")
                    Text("Wind: ${fmt1(c.windSpeedKmh)} km/h ${SpotScorer.compass(c.windDirectionDeg)}")
                    Text("Tide: ${c.tide.name.lowercase()} (${fmt1(c.tideHeightMeters)} m)")
                }
            }
        }

        if (sf.spot.notes.isNotBlank()) {
            Text(sf.spot.notes, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * 24h star timeline as vertical bars — one per hour, height = stars/5, coloured
 * by quality. Pure Canvas so it works identically on Android + iOS.
 */
@Composable
private fun TimelineChart(sf: SpotForecast) {
    val scores = sf.hours.map { SpotScorer.scoreHour(sf.spot, it) }
    if (scores.isEmpty()) return

    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        val n = scores.size
        val gap = 2f
        val barW = (size.width - gap * (n - 1)) / n
        scores.forEachIndexed { i, s ->
            val h = (s.toFloat().coerceIn(0f, 1f)) * size.height
            val x = i * (barW + gap)
            drawRect(
                color = barColor(s),
                topLeft = Offset(x, size.height - h),
                size = androidx.compose.ui.geometry.Size(barW, h),
            )
        }
    }
    // Hour labels at 0 / 6 / 12 / 18 for orientation.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val n = sf.hours.size
        listOf(0, n / 4, n / 2, 3 * n / 4, n - 1).forEach { idx ->
            sf.hours.getOrNull(idx)?.let {
                Text(hhmm(it.timeIso), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * The crowdsource loop: let the user report whether the spot worked, capturing
 * the current conditions snapshot. Logs are on-device (SessionLogStore) and are
 * the ground-truth pipeline for calibrating spot rules / reef depth. Local count
 * shown; export happens via the store's CSV (surfaced app-wide, see AppModule).
 */
@Composable
private fun SessionLogCard(sf: SpotForecast, logs: SessionLogStore) {
    var count by remember(sf.spot.id) { mutableStateOf(logs.countForSpot(sf.spot.id)) }
    var justLogged by remember(sf.spot.id) { mutableStateOf<Boolean?>(null) }

    // Snapshot to attach: the peak-scoring hour = a representative "now" for MVP.
    val snapshot: Conditions? = sf.hours.maxByOrNull { SpotScorer.scoreHour(sf.spot, it) }

    fun log(worked: Boolean) {
        val ts = nowIso()  // yyyy-MM-ddTHH:mm, platform clock
        count = logs.logSession(sf.spot.id, worked, ts, conditions = snapshot)
        justLogged = worked
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Did it work?", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { log(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                ) { Text("👍 Worked") }
                OutlinedButton(onClick = { log(false) }) { Text("👎 Didn't") }
            }
            val msg = when (justLogged) {
                true -> "Logged — thanks. Your reports calibrate this spot."
                false -> "Logged. Even a 'no' sharpens the forecast."
                null -> "$count report${if (count == 1) "" else "s"} for ${sf.spot.name} so far."
            }
            Text(msg, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun barColor(score: Double): Color = when (SpotScorer.toStars(score)) {
    0 -> Color(0xFFBDBDBD)
    1, 2 -> Color(0xFFEF9A9A)
    3 -> Color(0xFFFFD54F)
    4 -> Color(0xFF81C784)
    else -> Color(0xFF2E7D32)
}

/** Map a spot id to its bundled reef-bathymetry drawable, or null if none. Only
 * spots whose SDB produced a coherent map are included; the rest (boat-access,
 * deep-water, or cloud-contaminated AOIs) show no reef card. */
private fun reefDrawable(spotId: String): DrawableResource? = when (spotId) {
    "uluwatu" -> Res.drawable.reef_uluwatu
    "padang" -> Res.drawable.reef_padang
    "bingin" -> Res.drawable.reef_bingin
    "impossibles" -> Res.drawable.reef_impossibles
    "dreamland" -> Res.drawable.reef_dreamland
    "balangan" -> Res.drawable.reef_balangan
    "greenbowl" -> Res.drawable.reef_greenbowl
    "batubolong" -> Res.drawable.reef_batubolong
    "nusadua" -> Res.drawable.reef_nusadua
    "desertpoint" -> Res.drawable.reef_desertpoint
    "nyangnyang" -> Res.drawable.reef_nyangnyang
    "shipwrecks" -> Res.drawable.reef_shipwrecks
    else -> null
}

private fun fmt1(v: Double): String {
    val r = (v * 10).toInt() / 10.0
    return if (r == r.toInt().toDouble()) r.toInt().toString() else r.toString()
}
