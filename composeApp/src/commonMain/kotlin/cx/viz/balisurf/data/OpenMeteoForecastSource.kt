package cx.viz.balisurf.data

import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.domain.Spot
import cx.viz.balisurf.domain.TideClassifier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * Open-Meteo conformance of [ForecastSource]. Free tier, no API key. Two calls:
 *   - Marine API: swell height/direction/period + sea_level_height_msl (tide).
 *   - Forecast API: wind speed/direction.
 * Rows are zipped by hour index (both APIs return the same hourly grid for the
 * same coords/timezone) and the tide series is banded by TideClassifier.
 *
 * MVP caching is naive (per-instance memo, keyed by spot id). A backend
 * conformance would replace this with a shared server-side cache — see ECONOMICS.
 * Attribution: Open-Meteo data is CC BY 4.0 — credit required in the app.
 */
class OpenMeteoForecastSource(
    engineClient: HttpClient? = null,
) : ForecastSource {

    private val client: HttpClient = engineClient ?: HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val memo = mutableMapOf<String, List<Conditions>>()

    override suspend fun conditions(spot: Spot): Result<List<Conditions>> = runCatching {
        memo[spot.id]?.let { return@runCatching it }

        val marine: MarineResponse = client.get(MARINE_URL) {
            parameter("latitude", spot.latitude)
            parameter("longitude", spot.longitude)
            parameter("hourly", "swell_wave_height,swell_wave_direction,swell_wave_period,sea_level_height_msl")
            parameter("timezone", TZ)
            parameter("forecast_days", 1)
        }.body()

        val wind: WindResponse = client.get(FORECAST_URL) {
            parameter("latitude", spot.latitude)
            parameter("longitude", spot.longitude)
            parameter("hourly", "wind_speed_10m,wind_direction_10m")
            parameter("timezone", TZ)
            parameter("forecast_days", 1)
        }.body()

        val h = marine.hourly
        val w = wind.hourly
        val tides = TideClassifier.classify(h.seaLevelHeightMsl)

        val n = minOf(h.time.size, w.time.size)
        val rows = (0 until n).map { i ->
            Conditions(
                timeIso = h.time[i],
                swellHeightMeters = h.swellWaveHeight.getOrElse(i) { 0.0 },
                swellDirectionDeg = h.swellWaveDirection.getOrElse(i) { 0.0 }.toInt(),
                swellPeriodSeconds = h.swellWavePeriod.getOrElse(i) { 0.0 },
                windSpeedKmh = w.windSpeed10m.getOrElse(i) { 0.0 },
                windDirectionDeg = w.windDirection10m.getOrElse(i) { 0.0 }.toInt(),
                tide = tides.getOrElse(i) { cx.viz.balisurf.domain.TideState.MID },
                tideHeightMeters = h.seaLevelHeightMsl.getOrElse(i) { 0.0 },
            )
        }
        memo[spot.id] = rows
        rows
    }

    companion object {
        const val MARINE_URL = "https://marine-api.open-meteo.com/v1/marine"
        const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        const val TZ = "Asia/Makassar"
    }
}

// --- wire models (snake_case from the API) ---

@Serializable
private data class MarineResponse(val hourly: MarineHourly)

@Serializable
private data class MarineHourly(
    val time: List<String> = emptyList(),
    val swell_wave_height: List<Double> = emptyList(),
    val swell_wave_direction: List<Double> = emptyList(),
    val swell_wave_period: List<Double> = emptyList(),
    val sea_level_height_msl: List<Double> = emptyList(),
) {
    val swellWaveHeight get() = swell_wave_height
    val swellWaveDirection get() = swell_wave_direction
    val swellWavePeriod get() = swell_wave_period
    val seaLevelHeightMsl get() = sea_level_height_msl
}

@Serializable
private data class WindResponse(val hourly: WindHourly)

@Serializable
private data class WindHourly(
    val time: List<String> = emptyList(),
    val wind_speed_10m: List<Double> = emptyList(),
    val wind_direction_10m: List<Double> = emptyList(),
) {
    val windSpeed10m get() = wind_speed_10m
    val windDirection10m get() = wind_direction_10m
}
