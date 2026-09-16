package cx.viz.balisurf.ui

import androidx.compose.runtime.Composable
import balisurf.composeapp.generated.resources.Res
import balisurf.composeapp.generated.resources.bucket_fair
import balisurf.composeapp.generated.resources.bucket_flat
import balisurf.composeapp.generated.resources.bucket_go
import balisurf.composeapp.generated.resources.bucket_poor
import balisurf.composeapp.generated.resources.compass
import balisurf.composeapp.generated.resources.header_date
import balisurf.composeapp.generated.resources.headline_firing
import balisurf.composeapp.generated.resources.headline_flat
import balisurf.composeapp.generated.resources.headline_fun
import balisurf.composeapp.generated.resources.headline_good
import balisurf.composeapp.generated.resources.headline_marginal
import balisurf.composeapp.generated.resources.months_short
import balisurf.composeapp.generated.resources.no_forecast
import balisurf.composeapp.generated.resources.notes_airportlefts
import balisurf.composeapp.generated.resources.notes_balangan
import balisurf.composeapp.generated.resources.notes_batubolong
import balisurf.composeapp.generated.resources.notes_bingin
import balisurf.composeapp.generated.resources.notes_canggu
import balisurf.composeapp.generated.resources.notes_desertpoint
import balisurf.composeapp.generated.resources.notes_dreamland
import balisurf.composeapp.generated.resources.notes_gerupuk
import balisurf.composeapp.generated.resources.notes_greenbowl
import balisurf.composeapp.generated.resources.notes_impossibles
import balisurf.composeapp.generated.resources.notes_keramas
import balisurf.composeapp.generated.resources.notes_mawi
import balisurf.composeapp.generated.resources.notes_medewi
import balisurf.composeapp.generated.resources.notes_nusadua
import balisurf.composeapp.generated.resources.notes_nyangnyang
import balisurf.composeapp.generated.resources.notes_padang
import balisurf.composeapp.generated.resources.notes_playgrounds
import balisurf.composeapp.generated.resources.notes_serangan
import balisurf.composeapp.generated.resources.notes_shipwrecks
import balisurf.composeapp.generated.resources.notes_uluwatu
import balisurf.composeapp.generated.resources.region_bukit
import balisurf.composeapp.generated.resources.region_east_coast
import balisurf.composeapp.generated.resources.region_kuta
import balisurf.composeapp.generated.resources.region_lombok
import balisurf.composeapp.generated.resources.region_nusa_lembongan
import balisurf.composeapp.generated.resources.region_west_coast
import balisurf.composeapp.generated.resources.swell_summary
import balisurf.composeapp.generated.resources.tide_high
import balisurf.composeapp.generated.resources.tide_low
import balisurf.composeapp.generated.resources.tide_mid
import balisurf.composeapp.generated.resources.weekdays_short
import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.domain.TideState
import cx.viz.balisurf.domain.Verdict
import cx.viz.balisurf.scoring.SpotScorer
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/**
 * Display-text layer. Everything below turns a stable id (spot id, region id,
 * TideState, star count, compass degrees) into text in the user's language;
 * the domain and scoring layers stay language-free so they remain testable.
 */

/** Localized 16-point compass label for a degrees-FROM bearing. */
@Composable
fun compassLabel(deg: Int): String = stringArrayResource(Res.array.compass)[SpotScorer.compassIndex(deg)]

@Composable
fun tideLabel(tide: TideState): String = stringResource(
    when (tide) {
        TideState.LOW -> Res.string.tide_low
        TideState.MID -> Res.string.tide_mid
        TideState.HIGH -> Res.string.tide_high
    },
)

@Composable
fun regionLabel(region: String): String = stringResource(
    when (region) {
        "Bukit" -> Res.string.region_bukit
        "Kuta" -> Res.string.region_kuta
        "West Coast" -> Res.string.region_west_coast
        "East Coast" -> Res.string.region_east_coast
        "Nusa Lembongan" -> Res.string.region_nusa_lembongan
        "Lombok" -> Res.string.region_lombok
        else -> return region
    },
)

/** Hand-written local knowledge per spot; null for a spot with no notes yet. */
@Composable
fun spotNotes(spotId: String): String? = spotNotesResource(spotId)?.let { stringResource(it) }

private fun spotNotesResource(spotId: String): StringResource? = when (spotId) {
    "uluwatu" -> Res.string.notes_uluwatu
    "padang" -> Res.string.notes_padang
    "bingin" -> Res.string.notes_bingin
    "impossibles" -> Res.string.notes_impossibles
    "dreamland" -> Res.string.notes_dreamland
    "balangan" -> Res.string.notes_balangan
    "greenbowl" -> Res.string.notes_greenbowl
    "nyangnyang" -> Res.string.notes_nyangnyang
    "canggu" -> Res.string.notes_canggu
    "batubolong" -> Res.string.notes_batubolong
    "medewi" -> Res.string.notes_medewi
    "keramas" -> Res.string.notes_keramas
    "nusadua" -> Res.string.notes_nusadua
    "serangan" -> Res.string.notes_serangan
    "airportlefts" -> Res.string.notes_airportlefts
    "shipwrecks" -> Res.string.notes_shipwrecks
    "playgrounds" -> Res.string.notes_playgrounds
    "desertpoint" -> Res.string.notes_desertpoint
    "gerupuk" -> Res.string.notes_gerupuk
    "mawi" -> Res.string.notes_mawi
    else -> null
}

/** Quality-bucket label ("GO", "Едем"). Uppercased by the chip/section styling. */
@Composable
fun bucketLabel(stars: Int): String = stringResource(
    when {
        stars <= 0 -> Res.string.bucket_flat
        stars <= 2 -> Res.string.bucket_poor
        stars == 3 -> Res.string.bucket_fair
        else -> Res.string.bucket_go
    },
)

/**
 * The one-line call. Built here rather than in SpotScorer so the scorer stays a
 * pure, language-free function; the verdict carries its peak hour as data.
 */
@Composable
fun verdictHeadline(verdict: Verdict?): String {
    val peak = verdict?.peak ?: return stringResource(Res.string.no_forecast)
    if (verdict.stars <= 0) return stringResource(Res.string.headline_flat)
    val swell = swellSummary(peak)
    val tide = tideLabel(peak.tide).lowercase()
    return stringResource(
        when (verdict.stars) {
            1, 2 -> Res.string.headline_marginal
            3 -> Res.string.headline_fun
            4 -> Res.string.headline_good
            else -> Res.string.headline_firing
        },
        swell, tide,
    )
}

/** "1.5m SW 14s" / "1.5 м ЮЗ 14 с". */
@Composable
fun swellSummary(c: Conditions): String = stringResource(
    Res.string.swell_summary,
    fmt1(c.swellHeightMeters),
    compassLabel(c.swellDirectionDeg),
    c.swellPeriodSeconds.toInt(),
)

/** "2026-08-20T07:00" -> "Wed 20 Aug" — header date from forecast data, no platform clock. */
@Composable
fun headerDate(iso: String): String {
    val (weekday, day, month) = dateParts(iso) ?: return ""
    return stringResource(
        Res.string.header_date,
        stringArrayResource(Res.array.weekdays_short)[weekday],
        day,
        stringArrayResource(Res.array.months_short)[month],
    )
}

/**
 * "2026-08-20T07:00" -> (weekday ordinal Mon=0, day of month, month ordinal Jan=0),
 * or null if unparseable. Pure, so the date maths stays testable off the composer.
 */
internal fun dateParts(iso: String): Triple<Int, Int, Int>? = runCatching {
    val d = LocalDate.parse(iso.substringBefore('T'))
    // dayOfMonth, not the newer `day`: on Android LocalDate is java.time's, which
    // has no `day`. The Native-only deprecation warning is the cheaper trade.
    Triple(d.dayOfWeek.ordinal, d.dayOfMonth, d.month.ordinal)
}.getOrNull()

/** One decimal place, trailing ".0" dropped. Shared by every numeric readout. */
internal fun fmt1(v: Double): String {
    val r = (v * 10).toInt() / 10.0
    return if (r == r.toInt().toDouble()) r.toInt().toString() else r.toString()
}
