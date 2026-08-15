package cx.viz.balisurf.domain

/**
 * Classifies a raw MSL sea-level series into LOW/MID/HIGH per hour.
 *
 * MVP approach: relative banding against the day's own min/max range, since the
 * absolute datum from Open-Meteo's `sea_level_height_msl` is not a chart datum.
 * Lower third = LOW, middle third = MID, upper third = HIGH. Good enough to drive
 * spot selection; a paid harmonic API (WorldTides) can replace this later behind
 * the same output type without touching the scorer.
 */
object TideClassifier {

    fun classify(heights: List<Double>): List<TideState> {
        if (heights.isEmpty()) return emptyList()
        val lo = heights.min()
        val hi = heights.max()
        val range = (hi - lo).takeIf { it > 1e-6 } ?: return heights.map { TideState.MID }
        val third = range / 3.0
        return heights.map { h ->
            when {
                h < lo + third -> TideState.LOW
                h < lo + 2 * third -> TideState.MID
                else -> TideState.HIGH
            }
        }
    }

    /** Classify a single height given the day's known range. */
    fun classifyOne(height: Double, dayMin: Double, dayMax: Double): TideState {
        val range = (dayMax - dayMin).takeIf { it > 1e-6 } ?: return TideState.MID
        val third = range / 3.0
        return when {
            height < dayMin + third -> TideState.LOW
            height < dayMin + 2 * third -> TideState.MID
            else -> TideState.HIGH
        }
    }
}
