package cx.viz.balisurf.data

import cx.viz.balisurf.domain.Spot
import cx.viz.balisurf.domain.SpotRules
import cx.viz.balisurf.domain.TideState

/**
 * The 5 Bukit hero spots for v0.1, with hand-set local rules.
 *
 * WARNING: these rule values are a STARTING POINT set from general local
 * knowledge, not gospel. They are the product's IP and must be refined with a
 * real surfer (Vladimir) against observed sessions. Every tweak should be pinned
 * by a SpotScorer test so the judgement is versioned, not vibes.
 *
 * All spots face the same SW groundswell window; they differ mostly on tide.
 */
object SpotCatalog {
    val spots: List<Spot> = listOf(
        Spot(
            id = "uluwatu",
            name = "Uluwatu",
            latitude = -8.8153, longitude = 115.0886,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 200, swellDirectionMax = 250,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.0,
                offshoreWindMin = 90, offshoreWindMax = 135, // SE trade = offshore
                maxWindSpeedKmh = 30.0,
            ),
            notes = "Works across the tide but sections change; big-swell magnet. Reef, strong current at the cave exit.",
        ),
        Spot(
            id = "padang",
            name = "Padang Padang",
            latitude = -8.8107, longitude = 115.1035,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 205, swellDirectionMax = 245,
                minPeriodSeconds = 12.0,
                minSwellHeightMeters = 1.5,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 25.0,
            ),
            notes = "The barrel. Needs a solid long-period swell and mid/high water to break properly; small = closeout on dry reef.",
        ),
        Spot(
            id = "bingin",
            name = "Bingin",
            latitude = -8.8060, longitude = 115.1120,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 200, swellDirectionMax = 250,
                minPeriodSeconds = 9.0,
                minSwellHeightMeters = 0.8,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 28.0,
            ),
            notes = "Low-tide reef. Shallow and sharp on dead low. Best on a pushing/dropping low to mid.",
        ),
        Spot(
            id = "impossibles",
            name = "Impossibles",
            latitude = -8.8020, longitude = 115.1180,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 200, swellDirectionMax = 245,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.0,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 28.0,
            ),
            notes = "Long walls between Padang and Bingin. Mid tide sweet spot; sections join up on the right swell.",
        ),
        Spot(
            id = "dreamland",
            name = "Dreamland",
            latitude = -8.7970, longitude = 115.1130,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID, TideState.HIGH),
                swellDirectionMin = 195, swellDirectionMax = 255,
                minPeriodSeconds = 8.0,
                minSwellHeightMeters = 0.8,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 30.0,
            ),
            notes = "Beachy/reef mix, most tide-forgiving of the five. Good fallback when the reefs are too big or wrong tide.",
        ),
    )

    fun byId(id: String): Spot? = spots.firstOrNull { it.id == id }
}
