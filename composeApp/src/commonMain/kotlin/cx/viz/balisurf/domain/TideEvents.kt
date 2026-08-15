package cx.viz.balisurf.domain

/** A tide turning point: a high or low, with its clock time and height. */
data class TideEvent(
    val kind: Kind,
    val timeIso: String,
    val heightMeters: Double,
    /** Fractional-hour offset from timeIso for sub-hour precision, e.g. +0.3 = +18min. */
    val refinedOffsetHours: Double,
) {
    enum class Kind { HIGH, LOW }
}

/**
 * Extracts high/low tide TIMES from the free Open-Meteo `sea_level_height_msl`
 * hourly series — no paid tide API. A turning point is a local extremum of the
 * hourly heights; parabolic interpolation over the three surrounding samples
 * refines the peak to sub-hour precision.
 *
 * Accuracy is model-grade, not harmonic-station-grade, but for spot selection
 * ("is it low around dawn?") it is more than enough and costs $0.
 */
object TideEvents {

    fun detect(times: List<String>, heights: List<Double>): List<TideEvent> {
        if (times.size != heights.size || heights.size < 3) return emptyList()
        val out = ArrayList<TideEvent>()
        for (i in 1 until heights.size - 1) {
            val a = heights[i - 1]; val b = heights[i]; val c = heights[i + 1]
            val isHigh = b >= a && b > c
            val isLow = b <= a && b < c
            if (!isHigh && !isLow) continue
            out.add(
                TideEvent(
                    kind = if (isHigh) TideEvent.Kind.HIGH else TideEvent.Kind.LOW,
                    timeIso = times[i],
                    heightMeters = b,
                    refinedOffsetHours = vertexOffset(a, b, c),
                )
            )
        }
        return out
    }

    /**
     * Offset (in hours, roughly [-0.5, 0.5]) of a parabola's vertex from the
     * middle sample, given three equally-spaced samples. 0 if the points are
     * collinear/flat.
     */
    internal fun vertexOffset(y0: Double, y1: Double, y2: Double): Double {
        val denom = y0 - 2 * y1 + y2
        if (kotlin.math.abs(denom) < 1e-9) return 0.0
        return 0.5 * (y0 - y2) / denom
    }
}
