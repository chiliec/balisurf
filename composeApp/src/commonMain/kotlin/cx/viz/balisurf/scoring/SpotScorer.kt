package cx.viz.balisurf.scoring

import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.domain.Factor
import cx.viz.balisurf.domain.Spot
import cx.viz.balisurf.domain.SpotRules
import cx.viz.balisurf.domain.TimeWindow
import cx.viz.balisurf.domain.Verdict
import kotlin.math.abs
import kotlin.math.min

/**
 * Pure scoring engine. NO network, NO platform APIs — fully unit-testable in
 * commonTest. This function IS the product thesis: it encodes local surf
 * judgement ("Uluwatu on high tide = poor", "Bingin needs low + long period")
 * as a deterministic score. Every rule change should be pinned by a test.
 */
object SpotScorer {

    /** Score a single hour 0.0..1.0 against a spot's rules. */
    fun scoreHour(spot: Spot, c: Conditions): Double {
        val r = spot.rules

        // Hard gates — any one violated makes the hour unsurfable regardless of
        // how good the other factors are. Wrong tide, too small, blown-out wind,
        // and fully-wrong swell direction each zero the hour on their own; an
        // additive blend must never let a great period mask a fatal factor.
        val tideOk = c.tide in r.worksOnTide
        val heightOk = c.swellHeightMeters >= r.minSwellHeightMeters
        val windOk = c.windSpeedKmh <= r.maxWindSpeedKmh
        if (!tideOk || !heightOk || !windOk) return 0.0

        // Swell direction: full credit inside the window, linear falloff to 0
        // at 45° outside either edge. Zero direction credit = wrong-facing swell = gate.
        val dirScore = windowScore(
            value = c.swellDirectionDeg.toDouble(),
            min = r.swellDirectionMin.toDouble(),
            max = r.swellDirectionMax.toDouble(),
            falloff = 45.0,
            circular = true,
        )
        if (dirScore <= 0.0) return 0.0

        // Period: below min = 0 (gated softly), climbs to full credit ~4s above min.
        val periodScore = when {
            c.swellPeriodSeconds < r.minPeriodSeconds -> 0.0
            else -> min(1.0, (c.swellPeriodSeconds - r.minPeriodSeconds) / 4.0 + 0.5)
        }

        // Wind quality (already past the blown-out gate): reward light + offshore.
        val offshore = windowScore(
            value = c.windDirectionDeg.toDouble(),
            min = r.offshoreWindMin.toDouble(),
            max = r.offshoreWindMax.toDouble(),
            falloff = 60.0,
            circular = true,
        )
        val calmness = 1.0 - min(1.0, c.windSpeedKmh / r.maxWindSpeedKmh)
        val windScore = 0.5 * offshore + 0.5 * calmness

        // Weighted blend. Direction + period carry the swell quality; wind gates cleanliness.
        val base = (0.35 * dirScore + 0.30 * periodScore + 0.35 * windScore)
            .coerceIn(0.0, 1.0)

        // SDB refinement: if we know the reef-crest depth, estimate actual
        // water-over-reef at this hour's tide and modulate the score by how close
        // it is to the spot's ideal breaking band. This turns the coarse
        // LOW/MID/HIGH gate into a continuous, bathymetry-aware signal. When no
        // depth is known (reefCrestDepthM == null) the multiplier is 1.0 and
        // behaviour is unchanged.
        return base * waterOverReefFactor(r, c)
    }

    /**
     * Multiplier in [0.6, 1.0] from water-over-reef vs the spot's ideal band.
     * water = crest depth (below MSL) + tide height (relative to MSL). Inside the
     * ideal band -> 1.0; outside -> tapers, but never fully zeroes (the tide-state
     * gate already handled the hard cases). Returns 1.0 if no crest depth known.
     */
    internal fun waterOverReefFactor(r: SpotRules, c: Conditions): Double {
        val crest = r.reefCrestDepthM ?: return 1.0
        val water = crest + c.tideHeightMeters
        return when {
            water < 0.0 -> 0.6                       // reef dry / barely covered
            water in r.idealWaterMinM..r.idealWaterMaxM -> 1.0
            water < r.idealWaterMinM ->
                (0.6 + 0.4 * (water / r.idealWaterMinM)).coerceIn(0.6, 1.0)
            else -> {                                // too deep = fat/soft
                val over = water - r.idealWaterMaxM
                (1.0 - 0.4 * min(1.0, over / 2.0)).coerceIn(0.6, 1.0)
            }
        }
    }

    /** Produce the full verdict for a spot across a day's hourly series. */
    fun verdict(spot: Spot, hours: List<Conditions>): Verdict {
        if (hours.isEmpty()) {
            return Verdict(0, "No data", null, emptyList())
        }
        val scored = hours.map { it to scoreHour(spot, it) }
        // `hours` is non-empty (guarded above), so `scored` is too — use the
        // structurally-safe destructuring rather than maxByOrNull()!!, so a
        // later refactor that filters `scored` can't turn this into a crash.
        val best = scored.reduce { a, b -> if (b.second > a.second) b else a }
        val stars = toStars(best.second)

        val window = bestWindow(scored)
        val headline = headline(spot, best.first, stars)
        val factors = factorsFor(spot, best.first)
        return Verdict(stars, headline, window, factors)
    }

    // --- helpers ---

    /** 1.0 inside [min,max]; linear falloff to 0 across `falloff` degrees outside. */
    internal fun windowScore(
        value: Double,
        min: Double,
        max: Double,
        falloff: Double,
        circular: Boolean,
    ): Double {
        val dist = if (circular) distanceToArc(value, min, max) else {
            when {
                value in min..max -> 0.0
                value < min -> min - value
                else -> value - max
            }
        }
        return (1.0 - dist / falloff).coerceIn(0.0, 1.0)
    }

    /** Angular distance (deg) from `value` to the nearest edge of arc [min,max]. 0 if inside. */
    internal fun distanceToArc(value: Double, min: Double, max: Double): Double {
        // Normalise everything to 0..360.
        fun norm(a: Double) = ((a % 360) + 360) % 360
        val v = norm(value); val lo = norm(min); val hi = norm(max)
        val inside = if (lo <= hi) v in lo..hi else (v >= lo || v <= hi)
        if (inside) return 0.0
        val dLo = angularDelta(v, lo)
        val dHi = angularDelta(v, hi)
        return min(dLo, dHi)
    }

    internal fun angularDelta(a: Double, b: Double): Double {
        val d = abs(a - b) % 360
        return if (d > 180) 360 - d else d
    }

    internal fun toStars(score: Double): Int = when {
        score <= 0.0 -> 0
        score < 0.20 -> 1
        score < 0.40 -> 2
        score < 0.60 -> 3
        score < 0.80 -> 4
        else -> 5
    }

    private fun bestWindow(scored: List<Pair<Conditions, Double>>): TimeWindow? {
        // Longest run of hours scoring >= 3 stars; report its span + peak.
        var bestStart = -1; var bestLen = 0; var bestPeak = 0.0
        var curStart = -1; var curLen = 0; var curPeak = 0.0
        scored.forEachIndexed { i, (_, s) ->
            if (toStars(s) >= 3) {
                if (curStart < 0) { curStart = i; curLen = 0; curPeak = 0.0 }
                curLen++; curPeak = maxOf(curPeak, s)
                if (curLen > bestLen) { bestLen = curLen; bestStart = curStart; bestPeak = curPeak }
            } else {
                curStart = -1; curLen = 0; curPeak = 0.0
            }
        }
        if (bestStart < 0) return null
        val startIso = scored[bestStart].first.timeIso
        val endIso = scored[bestStart + bestLen - 1].first.timeIso
        return TimeWindow(startIso, endIso, toStars(bestPeak))
    }

    private fun headline(spot: Spot, c: Conditions, stars: Int): String {
        val dir = compass(c.swellDirectionDeg)
        val swell = "${fmt(c.swellHeightMeters)}m $dir ${c.swellPeriodSeconds.toInt()}s"
        val tide = c.tide.name.lowercase()
        return when (stars) {
            0 -> "Flat / off — wrong tide or too small."
            1, 2 -> "Marginal: $swell, $tide tide."
            3 -> "Fun: $swell, $tide tide."
            4 -> "Good: $swell, $tide tide — worth the paddle."
            else -> "Firing: $swell, $tide tide."
        }
    }

    private fun factorsFor(spot: Spot, c: Conditions): List<Factor> {
        val r = spot.rules
        return listOf(
            Factor("Swell dir", windowScore(c.swellDirectionDeg.toDouble(), r.swellDirectionMin.toDouble(), r.swellDirectionMax.toDouble(), 45.0, true),
                "${c.swellDirectionDeg}° (${compass(c.swellDirectionDeg)}), want ${r.swellDirectionMin}–${r.swellDirectionMax}°"),
            Factor("Period", if (c.swellPeriodSeconds < r.minPeriodSeconds) 0.0 else min(1.0, (c.swellPeriodSeconds - r.minPeriodSeconds) / 4.0 + 0.5),
                "${fmt(c.swellPeriodSeconds)}s, need ≥${fmt(r.minPeriodSeconds)}s"),
            Factor("Tide", if (c.tide in r.worksOnTide) 1.0 else 0.0,
                "${c.tide.name.lowercase()} (${fmt(c.tideHeightMeters)}m), works on ${r.worksOnTide.joinToString { it.name.lowercase() }}"),
            Factor("Wind", if (c.windSpeedKmh > r.maxWindSpeedKmh) 0.0 else 1.0 - min(1.0, c.windSpeedKmh / r.maxWindSpeedKmh),
                "${fmt(c.windSpeedKmh)}km/h ${compass(c.windDirectionDeg)}"),
        )
    }

    internal fun compass(deg: Int): String {
        val dirs = listOf("N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW")
        val i = (((deg % 360) + 360) % 360 + 11) / 22 % 16
        return dirs[i]
    }

    private fun fmt(v: Double): String {
        val r = (v * 10).toInt() / 10.0
        return if (r == r.toInt().toDouble()) r.toInt().toString() else r.toString()
    }
}
