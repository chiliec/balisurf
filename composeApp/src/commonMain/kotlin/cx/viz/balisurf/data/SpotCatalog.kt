package cx.viz.balisurf.data

import cx.viz.balisurf.domain.Spot
import cx.viz.balisurf.domain.SpotRules
import cx.viz.balisurf.domain.TideState

/**
 * Bali surf spots with hand-set local rules, grouped by region.
 *
 * Only language-free data lives here. The per-spot prose ("Bukit. Low-tide reef…")
 * and the region labels are in composeResources/values/strings.xml (plus each
 * values-<lang> sibling) as notes_<id> / region_<id>, resolved by ui/Strings.kt.
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
            region = "Bukit",
            latitude = -8.8153, longitude = 115.0886,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 200, swellDirectionMax = 250,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.0,
                offshoreWindMin = 90, offshoreWindMax = 135, // SE trade = offshore
                maxWindSpeedKmh = 30.0,
            ),
        ),
        Spot(
            id = "padang",
            name = "Padang Padang",
            region = "Bukit",
            latitude = -8.8107, longitude = 115.1035,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 205, swellDirectionMax = 245,
                minPeriodSeconds = 12.0,
                minSwellHeightMeters = 1.5,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 25.0,
            ),
        ),
        Spot(
            id = "bingin",
            name = "Bingin",
            region = "Bukit",
            latitude = -8.8060, longitude = 115.1120,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 200, swellDirectionMax = 250,
                minPeriodSeconds = 9.0,
                minSwellHeightMeters = 0.8,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 28.0,
            ),
        ),
        Spot(
            id = "impossibles",
            name = "Impossibles",
            region = "Bukit",
            latitude = -8.8020, longitude = 115.1180,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 200, swellDirectionMax = 245,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.0,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 28.0,
            ),
        ),
        Spot(
            id = "dreamland",
            name = "Dreamland",
            region = "Bukit",
            latitude = -8.7970, longitude = 115.1130,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID, TideState.HIGH),
                swellDirectionMin = 195, swellDirectionMax = 255,
                minPeriodSeconds = 8.0,
                minSwellHeightMeters = 0.8,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 30.0,
            ),
        ),
        Spot(
            id = "balangan",
            name = "Balangan",
            region = "Bukit",
            latitude = -8.7918, longitude = 115.1218,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 200, swellDirectionMax = 250,
                minPeriodSeconds = 9.0,
                minSwellHeightMeters = 1.0,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 28.0,
            ),
        ),
        Spot(
            id = "greenbowl",
            name = "Green Bowl",
            region = "Bukit",
            latitude = -8.8475, longitude = 115.1685,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 190, swellDirectionMax = 240,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.2,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 28.0,
            ),
        ),

        // ---- West coast (SW/W-facing beach + reef, E offshore) ----
        Spot(
            id = "canggu",
            name = "Canggu (Echo Beach)",
            region = "West Coast",
            latitude = -8.6510, longitude = 115.1310,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 195, swellDirectionMax = 255,
                minPeriodSeconds = 8.0,
                minSwellHeightMeters = 0.9,
                offshoreWindMin = 45, offshoreWindMax = 110, // E/NE morning offshore
                maxWindSpeedKmh = 28.0,
            ),
        ),
        Spot(
            id = "batubolong",
            name = "Batu Bolong (Old Man's)",
            region = "West Coast",
            latitude = -8.6580, longitude = 115.1280,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 195, swellDirectionMax = 255,
                minPeriodSeconds = 7.0,
                minSwellHeightMeters = 0.6,
                offshoreWindMin = 45, offshoreWindMax = 110,
                maxWindSpeedKmh = 26.0,
            ),
        ),
        Spot(
            id = "medewi",
            name = "Medewi",
            region = "West Coast",
            latitude = -8.4265, longitude = 114.7930,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 200, swellDirectionMax = 250,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.2,
                offshoreWindMin = 45, offshoreWindMax = 110,
                maxWindSpeedKmh = 26.0,
            ),
        ),

        // ---- East coast (E-facing reefs, W offshore, wet-season / morning) ----
        Spot(
            id = "keramas",
            name = "Keramas",
            region = "East Coast",
            latitude = -8.5965, longitude = 115.3280,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 150, swellDirectionMax = 200,
                minPeriodSeconds = 11.0,
                minSwellHeightMeters = 1.2,
                offshoreWindMin = 250, offshoreWindMax = 290, // W offshore for the east coast
                maxWindSpeedKmh = 26.0,
            ),
        ),
        Spot(
            id = "nusadua",
            name = "Nusa Dua",
            region = "East Coast",
            latitude = -8.8010, longitude = 115.2320,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 150, swellDirectionMax = 200,
                minPeriodSeconds = 11.0,
                minSwellHeightMeters = 1.5,
                offshoreWindMin = 250, offshoreWindMax = 290,
                maxWindSpeedKmh = 26.0,
            ),
        ),
        Spot(
            id = "serangan",
            name = "Serangan (Sri Lanka)",
            region = "East Coast",
            latitude = -8.7380, longitude = 115.2410,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 155, swellDirectionMax = 205,
                minPeriodSeconds = 9.0,
                minSwellHeightMeters = 1.0,
                offshoreWindMin = 250, offshoreWindMax = 290,
                maxWindSpeedKmh = 28.0,
            ),
        ),

        // ---- More Bukit / south Kuta ----
        Spot(
            id = "nyangnyang",
            name = "Nyang Nyang",
            region = "Bukit",
            latitude = -8.8360, longitude = 115.1010,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 195, swellDirectionMax = 250,
                minPeriodSeconds = 11.0,
                minSwellHeightMeters = 1.5,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 28.0,
            ),
        ),
        Spot(
            id = "airportlefts",
            name = "Airport Lefts",
            region = "Kuta",
            latitude = -8.7480, longitude = 115.1640,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 195, swellDirectionMax = 245,
                minPeriodSeconds = 11.0,
                minSwellHeightMeters = 1.3,
                offshoreWindMin = 90, offshoreWindMax = 140,
                maxWindSpeedKmh = 26.0,
            ),
        ),

        // ---- Nusa Lembongan (SW-facing reefs off SE Bali, SE offshore) ----
        Spot(
            id = "shipwrecks",
            name = "Shipwrecks (Lembongan)",
            region = "Nusa Lembongan",
            latitude = -8.6790, longitude = 115.4470,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 195, swellDirectionMax = 245,
                minPeriodSeconds = 11.0,
                minSwellHeightMeters = 1.3,
                offshoreWindMin = 90, offshoreWindMax = 140,
                maxWindSpeedKmh = 28.0,
            ),
        ),
        Spot(
            id = "playgrounds",
            name = "Playgrounds (Lembongan)",
            region = "Nusa Lembongan",
            latitude = -8.6835, longitude = 115.4520,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 190, swellDirectionMax = 250,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.2,
                offshoreWindMin = 90, offshoreWindMax = 140,
                maxWindSpeedKmh = 28.0,
            ),
        ),

        // ---- Lombok (across the strait; south + SW coasts) ----
        Spot(
            id = "desertpoint",
            name = "Desert Point (Lombok)",
            region = "Lombok",
            latitude = -8.7620, longitude = 115.8180,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 200, swellDirectionMax = 250,
                minPeriodSeconds = 13.0,
                minSwellHeightMeters = 1.5,
                offshoreWindMin = 45, offshoreWindMax = 110, // E/NE offshore on SW Lombok
                maxWindSpeedKmh = 25.0,
            ),
        ),
        Spot(
            id = "gerupuk",
            name = "Gerupuk (Lombok)",
            region = "Lombok",
            latitude = -8.9060, longitude = 116.3360,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID, TideState.HIGH),
                swellDirectionMin = 160, swellDirectionMax = 220,
                minPeriodSeconds = 9.0,
                minSwellHeightMeters = 0.9,
                offshoreWindMin = 315, offshoreWindMax = 45, // N-ish offshore in the bay
                maxWindSpeedKmh = 28.0,
            ),
        ),
        Spot(
            id = "mawi",
            name = "Mawi (Lombok)",
            region = "Lombok",
            latitude = -8.8880, longitude = 116.2050,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 170, swellDirectionMax = 230,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.2,
                offshoreWindMin = 315, offshoreWindMax = 45,
                maxWindSpeedKmh = 26.0,
            ),
        ),
    )

    fun byId(id: String): Spot? = spots.firstOrNull { it.id == id }
}
