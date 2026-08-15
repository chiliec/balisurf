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

        // ---- More Bukit / south Kuta ----
        Spot(
            id = "nyangnyang",
            name = "Nyang Nyang",
            latitude = -8.8360, longitude = 115.1010,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 195, swellDirectionMax = 250,
                minPeriodSeconds = 11.0,
                minSwellHeightMeters = 1.5,
                offshoreWindMin = 90, offshoreWindMax = 135,
                maxWindSpeedKmh = 28.0,
            ),
            notes = "South Bukit. Remote, big-swell reef down a long hike; needs size and mid/high water. Emptier than the main Bukit spots.",
        ),
        Spot(
            id = "airportlefts",
            name = "Airport Lefts",
            latitude = -8.7480, longitude = 115.1640,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 195, swellDirectionMax = 245,
                minPeriodSeconds = 11.0,
                minSwellHeightMeters = 1.3,
                offshoreWindMin = 90, offshoreWindMax = 140,
                maxWindSpeedKmh = 26.0,
            ),
            notes = "Kuta reef, off the airport runway (boat access). Long left, best mid/high on a solid long-period swell. SE offshore.",
        ),

        // ---- Nusa Lembongan (SW-facing reefs off SE Bali, SE offshore) ----
        Spot(
            id = "shipwrecks",
            name = "Shipwrecks (Lembongan)",
            latitude = -8.6790, longitude = 115.4470,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 195, swellDirectionMax = 245,
                minPeriodSeconds = 11.0,
                minSwellHeightMeters = 1.3,
                offshoreWindMin = 90, offshoreWindMax = 140,
                maxWindSpeedKmh = 28.0,
            ),
            notes = "Nusa Lembongan. Long right reef, best mid/high on a bigger long-period swell. SE offshore; boat/paddle access.",
        ),
        Spot(
            id = "playgrounds",
            name = "Playgrounds (Lembongan)",
            latitude = -8.6835, longitude = 115.4520,
            rules = SpotRules(
                worksOnTide = setOf(TideState.MID, TideState.HIGH),
                swellDirectionMin = 190, swellDirectionMax = 250,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.2,
                offshoreWindMin = 90, offshoreWindMax = 140,
                maxWindSpeedKmh = 28.0,
            ),
            notes = "Nusa Lembongan. Rights and lefts over reef, a touch more forgiving than Shipwrecks. Mid/high water, SE offshore.",
        ),

        // ---- Lombok (across the strait; south + SW coasts) ----
        Spot(
            id = "desertpoint",
            name = "Desert Point (Lombok)",
            latitude = -8.7620, longitude = 115.8180,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 200, swellDirectionMax = 250,
                minPeriodSeconds = 13.0,
                minSwellHeightMeters = 1.5,
                offshoreWindMin = 45, offshoreWindMax = 110, // E/NE offshore on SW Lombok
                maxWindSpeedKmh = 25.0,
            ),
            notes = "SW Lombok (Bangko Bangko). World-class barrelling left, fickle — needs a big long-period SW swell and low-to-mid water. Very shallow reef, serious wave.",
        ),
        Spot(
            id = "gerupuk",
            name = "Gerupuk (Lombok)",
            latitude = -8.9060, longitude = 116.3360,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID, TideState.HIGH),
                swellDirectionMin = 160, swellDirectionMax = 220,
                minPeriodSeconds = 9.0,
                minSwellHeightMeters = 0.9,
                offshoreWindMin = 315, offshoreWindMax = 45, // N-ish offshore in the bay
                maxWindSpeedKmh = 28.0,
            ),
            notes = "South Lombok. Several reefs in a bay (Insides/Outsides), boat access — a tide/wind option for most conditions. Takes S/SW swell.",
        ),
        Spot(
            id = "mawi",
            name = "Mawi (Lombok)",
            latitude = -8.8880, longitude = 116.2050,
            rules = SpotRules(
                worksOnTide = setOf(TideState.LOW, TideState.MID),
                swellDirectionMin = 170, swellDirectionMax = 230,
                minPeriodSeconds = 10.0,
                minSwellHeightMeters = 1.2,
                offshoreWindMin = 315, offshoreWindMax = 45,
                maxWindSpeedKmh = 26.0,
            ),
            notes = "South Lombok. Powerful, hollow reef peak in a scenic bay; strong currents. Best low-to-mid on a solid S/SW swell.",
        ),
    )

    fun byId(id: String): Spot? = spots.firstOrNull { it.id == id }
}
