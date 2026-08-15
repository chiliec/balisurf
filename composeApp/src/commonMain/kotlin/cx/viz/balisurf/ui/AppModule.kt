package cx.viz.balisurf.ui

import cx.viz.balisurf.data.ForecastSource
import cx.viz.balisurf.data.SpotCatalog
import cx.viz.balisurf.domain.Spot
import cx.viz.balisurf.domain.TideEvent
import cx.viz.balisurf.domain.TideEvents
import cx.viz.balisurf.domain.Verdict
import cx.viz.balisurf.scoring.SpotScorer

/** Composition root: the app's dependencies, mirroring the sibling apps' AppModule. */
class AppModule(
    val forecast: ForecastSource,
)

/** A spot paired with its freshly computed verdict + today's tide turning points. */
data class SpotForecast(
    val spot: Spot,
    val verdict: Verdict?,
    val tides: List<TideEvent> = emptyList(),
)

/** Load + score every catalog spot. Pure orchestration over the seam + scorer. */
suspend fun AppModule.loadAll(): List<SpotForecast> =
    SpotCatalog.spots.map { spot ->
        val hours = forecast.conditions(spot).getOrNull()
        val verdict = hours?.let { SpotScorer.verdict(spot, it) }
        val tides = hours?.let {
            TideEvents.detect(it.map { h -> h.timeIso }, it.map { h -> h.tideHeightMeters })
        } ?: emptyList()
        SpotForecast(spot, verdict, tides)
    }
