package cx.viz.balisurf.data

import cx.viz.balisurf.domain.Spot
import cx.viz.balisurf.domain.SpotRules
import cx.viz.balisurf.domain.TideState

/**
 * Bali surf spots with hand-set local rules, grouped by region.
 *
 * WARNING: these rule values are a STARTING POINT set from general local
 * knowledge, not gospel. They are the product's IP and must be refined with a
 * real surfer (Vladimir) + the session-log data against observed sessions. Every
 * tweak should be pinned by a SpotScorer test so the judgement is versioned.
 *
 * Regional wind convention (offshore = clean):
 *   - SW/W-facing coasts (Bukit, Canggu, Medewi): offshore is the E/SE dry-season
 *     trade, ~90-135°. These fire in the dry season (Apr-Oct) morning glass.
 *   - E-facing coasts (Keramas, Nusa Dua, Serangan): offshore is from the W,
 *     ~250-290°. Best in the wet season (Nov-Mar) or on a glassy morning; the
 *     dry-season trade is onshore for them by afternoon.
 * Swell-direction windows are the LOCAL (refracted) direction the wave model
 * reports AT the spot: SW (~200-250°) on the Bukit, more S/SE (~150-200°) for the
 * east coast where the same groundswell wraps around the island.
 */
object SpotCatalog {
    val spots: List<Spot> = listOf(
        // ---- Bukit peninsula (SW-facing reefs, SE offshore) ----
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
            notes = "Bukit. Works across the tide but sections change; big-swell magnet. Reef, strong current at the cave exit.",
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
            notes = "Bukit. The barrel. Needs a solid long-period swell and mid/high water to break properly; small = closeout on dry reef.",
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
            notes = "Bukit. Low-tide reef. Shallow and sharp on dead low. Best on a pushing/dropping low to mid.",
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
            notes = "Bukit. Long walls between Padang and Bingin. Mid tide sweet spot; sections join up on the right swell.",
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
            notes = "Bukit. Beachy/reef mix, most tide-forgiving of the five. Good fallback when the reefs are too big or wrong tide.",
        ),
        Spot(
            id = "balangan",
            name = "Balangan",
            latitude = -8.7918, longitude = 115.1218,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 200, swellDirectionMax = 250,
                minPeriodSeconds = 9.0,
                minSwellHeightMeters = 1.0,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 28.0,
            ),
            notes = "Bukit. Fast left reef, best low-to-mid. Drains out and gets shallow on dead low.",
        ),
        Spot(
            id = "greenbowl",
            name = "Green Bowl",
            latitude = -8.8475, longitude = 115.1685,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 190, swellDirectionMax = 240,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.2,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 28.0,
            ),
            notes = "South Bukit. Low-tide reef down a long stairway; picks up plenty of swell. Rights and lefts.",
        ),

        // ---- West coast (SW/W-facing beach + reef, E offshore) ----
        Spot(
            id = "canggu",
            name = "Canggu (Echo Beach)",
            latitude = -8.6510, longitude = 115.1310,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 195, swellDirectionMax = 255,
                minPeriodSeconds = 8.0,
                minSwellHeightMeters = 0.9,
                offshoreWindMin = 45, offshoreWindMax = 110, // E/NE morning offshore
                maxWindSpeedKmh = 28.0,
            ),
            notes = "Canggu. Reef/beach peaks, punchy on a pushing low. Crowded; best early before the onshore fills in.",
        ),
        Spot(
            id = "batubolong",
            name = "Batu Bolong (Old Man's)",
            latitude = -8.6580, longitude = 115.1280,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 195, swellDirectionMax = 255,
                minPeriodSeconds = 7.0,
                minSwellHeightMeters = 0.6,
                offshoreWindMin = 45, offshoreWindMax = 110,
                maxWindSpeedKmh = 26.0,
            ),
            notes = "Canggu. Mellow longboard-friendly reef, best mid-to-high. The forgiving fallback of the west coast.",
        ),
        Spot(
            id = "medewi",
            name = "Medewi",
            latitude = -8.4265, longitude = 114.7930,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 200, swellDirectionMax = 250,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.2,
                offshoreWindMin = 45, offshoreWindMax = 110,
                maxWindSpeedKmh = 26.0,
            ),
            notes = "Far west. Long mellow left point over rock/cobble, best mid-to-high on a solid long-period swell. Slow, forgiving wall.",
        ),

        // ---- East coast (E-facing reefs, W offshore, wet-season / morning) ----
        Spot(
            id = "keramas",
            name = "Keramas",
            latitude = -8.5965, longitude = 115.3280,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 150, swellDirectionMax = 200,
                minPeriodSeconds = 11.0,
                minSwellHeightMeters = 1.2,
                offshoreWindMin = 250, offshoreWindMax = 290, // W offshore for the east coast
                maxWindSpeedKmh = 26.0,
            ),
            notes = "East coast. World-class right over black-sand reef; punchy and hollow. Best on a bigger long-period S/SE swell, morning offshore before the onshore trade. Wet-season favourite.",
        ),
        Spot(
            id = "nusadua",
            name = "Nusa Dua",
            latitude = -8.8010, longitude = 115.2320,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 150, swellDirectionMax = 200,
                minPeriodSeconds = 11.0,
                minSwellHeightMeters = 1.5,
                offshoreWindMin = 250, offshoreWindMax = 290,
                maxWindSpeedKmh = 26.0,
            ),
            notes = "East coast. Big-wave reef that needs size to turn on; rights and lefts well offshore. Wet season / morning offshore, mid-to-high water.",
        ),
        Spot(
            id = "serangan",
            name = "Serangan (Sri Lanka)",
            latitude = -8.7380, longitude = 115.2410,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 155, swellDirectionMax = 205,
                minPeriodSeconds = 9.0,
                minSwellHeightMeters = 1.0,
                offshoreWindMin = 250, offshoreWindMax = 290,
                maxWindSpeedKmh = 28.0,
            ),
            notes = "East coast (Serangan island). Consistent right reef, wet-season / morning offshore. More forgiving entry than Keramas.",
        ),
    )

    fun byId(id: String): Spot? = spots.firstOrNull { it.id == id }
}
