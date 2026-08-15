package cx.viz.balisurf.ui

import cx.viz.balisurf.data.ForecastSource
import cx.viz.balisurf.data.SessionLogStore
import cx.viz.balisurf.data.SpotCatalog
import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.domain.Spot
import cx.viz.balisurf.domain.TideEvent
import cx.viz.balisurf.domain.TideEvents
import cx.viz.balisurf.domain.Verdict
import cx.viz.balisurf.scoring.SpotScorer

/** Composition root: the app's dependencies, mirroring the sibling apps' AppModule. */
class AppModule(
    val forecast: ForecastSource,
    val logs: SessionLogStore,
)

/**
 * A spot paired with its freshly computed verdict, today's tide turning points,
 * and the raw hourly series (kept so the detail screen can draw the 24h timeline
 * without re-fetching).
 */
data class SpotForecast(
    val spot: Spot,
    val verdict: Verdict?,
    val tides: List<TideEvent> = emptyList(),
    val hours: List<Conditions> = emptyList(),
)

/** Load + score every catalog spot. Pure orchestration over the seam + scorer. */
suspend fun AppModule.loadAll(): List<SpotForecast> =
    SpotCatalog.spots.map { spot ->
        val hours = forecast.conditions(spot).getOrNull().orEmpty()
        val verdict = if (hours.isNotEmpty()) SpotScorer.verdict(spot, hours) else null
        val tides = if (hours.isNotEmpty()) {
            TideEvents.detect(hours.map { it.timeIso }, hours.map { it.tideHeightMeters })
        } else emptyList()
        SpotForecast(spot, verdict, tides, hours)
    }
