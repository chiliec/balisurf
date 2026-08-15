package cx.viz.balisurf.domain

/**
 * A single hour of conditions at a spot, already reduced from the raw API rows.
 * Directions are degrees FROM (Open-Meteo convention).
 */
data class Conditions(
    val timeIso: String,
    val swellHeightMeters: Double,
    val swellDirectionDeg: Int,
    val swellPeriodSeconds: Double,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val tide: TideState,
    /** Raw tide height (m, MSL) — kept for display of the actual level. */
    val tideHeightMeters: Double,
)

/** The scored output for one spot: the one-line call the app exists to make. */
data class Verdict(
    /** 0..5, Windguru-style. */
    val stars: Int,
    val headline: String,
    /** Best contiguous window today, or null if nothing scores. */
    val bestWindow: TimeWindow?,
    /** Per-factor breakdown, for the detail screen + debugging. */
    val factors: List<Factor>,
)

data class TimeWindow(val startIso: String, val endIso: String, val peakStars: Int)

data class Factor(val label: String, val score: Double, val detail: String)
