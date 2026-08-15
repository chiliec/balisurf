package cx.viz.balisurf.domain

/**
 * A surf spot and the local rules that turn raw swell/tide/wind into a verdict.
 *
 * This record IS the product's intellectual property: the hand-curated local
 * knowledge that generic forecast apps lack. Keep it as data, not code, so it
 * can eventually move to a remote config / be crowd-sourced without a release.
 *
 * Angles are compass degrees the swell/wind is coming FROM (0=N, 90=E, 180=S,
 * 270=W), matching Open-Meteo's convention.
 */
data class Spot(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val rules: SpotRules,
    val notes: String = "",
)

data class SpotRules(
    /** Tide states the spot works on. Many Bukit reefs only fire on LOW. */
    val worksOnTide: Set<TideState>,
    /** Optimal swell-direction window (degrees FROM), inclusive. */
    val swellDirectionMin: Int,
    val swellDirectionMax: Int,
    /** Below this period (seconds) the swell is too weak/short to break well. */
    val minPeriodSeconds: Double,
    /** Below this significant swell height (metres) the spot is flat/soft. */
    val minSwellHeightMeters: Double,
    /** Ideal (offshore) wind direction window, degrees FROM. Light wind here = clean. */
    val offshoreWindMin: Int,
    val offshoreWindMax: Int,
    /** Above this wind speed (km/h) the spot gets blown out regardless of direction. */
    val maxWindSpeedKmh: Double,
)

enum class TideState { LOW, MID, HIGH }
