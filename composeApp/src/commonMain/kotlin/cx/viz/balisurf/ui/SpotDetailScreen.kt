package cx.viz.balisurf.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.viz.balisurf.data.SessionLogStore
import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.domain.TideEvent
import cx.viz.balisurf.platform.nowIso
import balisurf.composeapp.generated.resources.Res
import balisurf.composeapp.generated.resources.back_to_spots
import balisurf.composeapp.generated.resources.best_window
import balisurf.composeapp.generated.resources.dash
import balisurf.composeapp.generated.resources.log_didnt
import balisurf.composeapp.generated.resources.log_report_count
import balisurf.composeapp.generated.resources.log_thanks_no
import balisurf.composeapp.generated.resources.log_thanks_yes
import balisurf.composeapp.generated.resources.log_worked
import balisurf.composeapp.generated.resources.reef_airportlefts
import balisurf.composeapp.generated.resources.reef_balangan
import balisurf.composeapp.generated.resources.reef_batubolong
import balisurf.composeapp.generated.resources.reef_bingin
import balisurf.composeapp.generated.resources.reef_canggu
import balisurf.composeapp.generated.resources.reef_desertpoint
import balisurf.composeapp.generated.resources.reef_dreamland
import balisurf.composeapp.generated.resources.reef_gerupuk
import balisurf.composeapp.generated.resources.reef_greenbowl
import balisurf.composeapp.generated.resources.reef_impossibles
import balisurf.composeapp.generated.resources.reef_keramas
import balisurf.composeapp.generated.resources.reef_mawi
import balisurf.composeapp.generated.resources.reef_medewi
import balisurf.composeapp.generated.resources.reef_nusadua
import balisurf.composeapp.generated.resources.reef_nyangnyang
import balisurf.composeapp.generated.resources.reef_padang
import balisurf.composeapp.generated.resources.reef_playgrounds
import balisurf.composeapp.generated.resources.reef_serangan
import balisurf.composeapp.generated.resources.reef_shipwrecks
import balisurf.composeapp.generated.resources.reef_uluwatu
import balisurf.composeapp.generated.resources.reef_caption
import balisurf.composeapp.generated.resources.reef_content_description
import balisurf.composeapp.generated.resources.section_did_it_work
import balisurf.composeapp.generated.resources.section_next_24h
import balisurf.composeapp.generated.resources.section_peak
import balisurf.composeapp.generated.resources.section_reef
import balisurf.composeapp.generated.resources.section_tides
import balisurf.composeapp.generated.resources.tide_pill
import balisurf.composeapp.generated.resources.tile_swell
import balisurf.composeapp.generated.resources.tile_tide
import balisurf.composeapp.generated.resources.tile_wind
import balisurf.composeapp.generated.resources.tile_window
import balisurf.composeapp.generated.resources.swell_sub
import balisurf.composeapp.generated.resources.unit_kmh
import balisurf.composeapp.generated.resources.unit_m
import balisurf.composeapp.generated.resources.window_none
import balisurf.composeapp.generated.resources.window_peak
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Spot detail: teal hero (name/verdict/headline), then white section cards —
 * 24h bars, peak-conditions tile grid, tide pills, reef bathymetry, notes —
 * with the session-log card last. The tiles describe the verdict's own peak hour,
 * so the detail read always matches the list verdict.
 */
@Composable
fun SpotDetailScreen(sf: SpotForecast, logs: SessionLogStore, onBack: () -> Unit) {
    // DeepTeal fills the status-bar strip and the scrolling column is inset below
    // it, so cards never slide under the clock. It matches the top of the hero's
    // vertical gradient, which keeps the two reading as one surface at rest.
    Box(Modifier.fillMaxSize().background(BaliColors.DeepTeal)) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .background(BaliColors.Background)
                .verticalScroll(rememberScrollState()),
        ) {
            HeroHeader(sf, onBack)

            Column(
                Modifier.padding(16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sf.hours.isNotEmpty()) {
                    SectionCard(stringResource(Res.string.section_next_24h)) {
                        HourBars(sf.spot, sf.hours, height = 80.dp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val n = sf.hours.size
                            listOf(0, n / 4, n / 2, 3 * n / 4, n - 1).forEach { idx ->
                                sf.hours.getOrNull(idx)?.let {
                                    Text(hhmm(it.timeIso), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Peak hour of the day = a representative read for the tiles + log snapshot.
                val peak: Conditions? = sf.verdict?.peak
                peak?.let { c ->
                    SectionCard(stringResource(Res.string.section_peak)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConditionTile(
                                stringResource(Res.string.tile_swell),
                                stringResource(Res.string.unit_m, fmt1(c.swellHeightMeters)),
                                stringResource(
                                    Res.string.swell_sub,
                                    compassLabel(c.swellDirectionDeg),
                                    c.swellPeriodSeconds.toInt(),
                                ),
                                Modifier.weight(1f),
                            )
                            ConditionTile(
                                stringResource(Res.string.tile_wind),
                                stringResource(Res.string.unit_kmh, fmt1(c.windSpeedKmh)),
                                compassLabel(c.windDirectionDeg), Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConditionTile(
                                stringResource(Res.string.tile_tide),
                                tideLabel(c.tide),
                                stringResource(Res.string.unit_m, fmt1(c.tideHeightMeters)),
                                Modifier.weight(1f),
                            )
                            val w = sf.verdict.bestWindow
                            ConditionTile(
                                stringResource(Res.string.tile_window),
                                w?.let { "${hhmm(it.startIso)}–${hhmm(it.endIso)}" }
                                    ?: stringResource(Res.string.dash),
                                w?.let { stringResource(Res.string.window_peak, it.peakStars) }
                                    ?: stringResource(Res.string.window_none),
                                Modifier.weight(1f),
                            )
                        }
                    }
                }

                if (sf.tides.isNotEmpty()) {
                    SectionCard(stringResource(Res.string.section_tides)) {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            sf.tides.forEach { t ->
                                val arrow = if (t.kind == TideEvent.Kind.HIGH) "▲" else "▼"
                                Text(
                                    stringResource(
                                        Res.string.tide_pill,
                                        arrow, hhmm(t.timeIso), fmt1(t.heightMeters),
                                    ),
                                    Modifier.background(BaliColors.CardTint, RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    color = BaliColors.DeepTeal,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                reefDrawable(sf.spot.id)?.let { res ->
                    SectionCard(stringResource(Res.string.section_reef)) {
                        Image(
                            painter = painterResource(res),
                            contentDescription = stringResource(
                                Res.string.reef_content_description, sf.spot.name,
                            ),
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            contentScale = ContentScale.Fit,
                        )
                        Text(
                            stringResource(Res.string.reef_caption),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                    }
                }

                spotNotes(sf.spot.id)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }

                SessionLogCard(sf, logs, snapshot = peak)
            }
        }
    }
}

@Composable
private fun HeroHeader(sf: SpotForecast, onBack: () -> Unit) {
    val stars = sf.verdict?.stars ?: 0
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BaliColors.DeepTeal, BaliColors.Teal)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(Res.string.back_to_spots),
            Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            sf.spot.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${"★".repeat(stars)}${"☆".repeat(5 - stars)} · ${bucketLabel(stars).uppercase()}",
                Modifier.background(Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = BaliColors.DeepTeal,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            sf.verdict?.bestWindow?.let { w ->
                Text(
                    stringResource(Res.string.best_window, hhmm(w.startIso), hhmm(w.endIso)),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text(
            verdictHeadline(sf.verdict),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** White (or tinted) card with an uppercase micro-label title. */
@Composable
private fun SectionCard(title: String, tint: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = tint)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(title)
            content()
        }
    }
}

@Composable
private fun ConditionTile(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(BaliColors.TileTint, RoundedCornerShape(10.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = BaliColors.DeepTeal)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

/**
 * The crowdsource loop: let the user report whether the spot worked, capturing
 * the peak-conditions snapshot. Logs are on-device (SessionLogStore) and are
 * the ground-truth pipeline for calibrating spot rules / reef depth.
 */
@Composable
private fun SessionLogCard(sf: SpotForecast, logs: SessionLogStore, snapshot: Conditions?) {
    var count by remember(sf.spot.id) { mutableStateOf(logs.countForSpot(sf.spot.id)) }
    var justLogged by remember(sf.spot.id) { mutableStateOf<Boolean?>(null) }

    fun log(worked: Boolean) {
        val ts = nowIso()  // yyyy-MM-ddTHH:mm, platform clock
        count = logs.logSession(sf.spot.id, worked, ts, conditions = snapshot)
        justLogged = worked
    }

    SectionCard(stringResource(Res.string.section_did_it_work), tint = BaliColors.CardTint) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { log(true) }) { Text(stringResource(Res.string.log_worked)) }
            OutlinedButton(onClick = { log(false) }) { Text(stringResource(Res.string.log_didnt)) }
        }
        val msg = when (justLogged) {
            true -> stringResource(Res.string.log_thanks_yes)
            false -> stringResource(Res.string.log_thanks_no)
            null -> pluralStringResource(Res.plurals.log_report_count, count, count, sf.spot.name)
        }
        Text(msg, style = MaterialTheme.typography.bodySmall)
    }
}

/** Map a spot id to its bundled reef-bathymetry drawable, or null if none. All
 * 20 catalog spots now have a coherent SDB map; the null branch stays as a
 * graceful fallback for any future spot whose AOI can't be resolved to a
 * coherent reef (deep-channel, cloud-contaminated, or un-reframeable). */
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
    "keramas" -> Res.drawable.reef_keramas
    "medewi" -> Res.drawable.reef_medewi
    "canggu" -> Res.drawable.reef_canggu
    "serangan" -> Res.drawable.reef_serangan
    "playgrounds" -> Res.drawable.reef_playgrounds
    "mawi" -> Res.drawable.reef_mawi
    "airportlefts" -> Res.drawable.reef_airportlefts
    "gerupuk" -> Res.drawable.reef_gerupuk
    else -> null
}
