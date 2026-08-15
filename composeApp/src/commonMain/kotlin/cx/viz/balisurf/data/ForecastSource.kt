package cx.viz.balisurf.data

import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.domain.Spot

/**
 * The single seam between the app and any weather backend. Open-Meteo is one
 * conformance today; a shared caching backend, or a paid tide source, can be
 * added later as another conformance without touching SpotScorer or the UI.
 *
 * Mirrors the LanguageModelProvider pattern from the sibling apps.
 */
interface ForecastSource {
    /**
     * Return the day's hourly conditions for a spot, tide already classified.
     * Implementations are responsible for caching to respect API call budgets.
     */
    suspend fun conditions(spot: Spot): Result<List<Conditions>>
}
