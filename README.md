# BaliSurf

A Kotlin Multiplatform (Compose) surf-forecast app for Bali's Bukit peninsula.

It does **not** try to out-forecast Windguru on raw numbers. It pulls the same
commodity swell/tide/wind data everyone has (free, from Open-Meteo) and wins on
the thing incumbents lack: **per-spot local judgement** — a rule engine that
turns raw conditions into a plain "go / don't go / best window today" call, plus
(roadmap) hand-built **bathymetry** nobody has in the open.

See `docs/OPPORTUNITY.md` and `docs/ECONOMICS.md` for the full rationale, data-cost
model, and bathymetry acquisition plan.

## Status: v0.1 scaffold

- 5 Bukit spots (Uluwatu, Padang Padang, Bingin, Impossibles, Dreamland).
- One list screen: each spot → star rating + one-line verdict + best window today.
- Live data via Open-Meteo Marine + Forecast APIs (free tier, no key).
- Tides classified from `sea_level_height_msl` (relative LOW/MID/HIGH bands).

## Architecture

Single-module Compose Multiplatform, mirroring the sibling apps (`slovo`,
`indonesian-app`). All logic in `commonMain`, thin platform entry points.

```
composeApp/src/commonMain/kotlin/cx/viz/balisurf/
  domain/     Spot, SpotRules, Conditions, Verdict, TideClassifier   (pure types)
  data/       ForecastSource (seam), OpenMeteoForecastSource, SpotCatalog
  scoring/    SpotScorer — pure, deterministic; THE product thesis
  ui/         App (Compose), AppModule (composition root)
composeApp/src/commonTest/  SpotScorer + TideClassifier tests = the spec, pinned
composeApp/src/androidMain/ MainActivity, AndroidManifest, res
composeApp/src/iosMain/     MainViewController
```

### Key seams
- **`ForecastSource`** is the single boundary to any backend. `OpenMeteoForecastSource`
  is today's conformance; a shared caching backend or a paid tide source drops in
  as another conformance without touching the scorer or UI. (See ECONOMICS:
  client-direct is fine below ~500 users; flip to a backend before monetizing.)
- **`SpotScorer`** is a pure function `(Spot, hourly Conditions) -> Verdict`. No
  network, no platform APIs — fully testable. Every rule in `SpotCatalog` is
  pinned by a test in `SpotScorerTest`, so the local judgement is versioned, not
  vibes. **The rule values are a starting point and must be refined with a real
  surfer against observed sessions.**

## Build & test

Requires JDK 21 + Android SDK (compileSdk 37). No build environment is bundled.

```bash
./gradlew :composeApp:allTests        # run the pure commonTest suite
./gradlew :composeApp:assembleDebug   # unsigned debug APK
```

iOS: open `iosApp` in Xcode once the Xcode project is added (not in v0.1 scaffold).

## Data attribution

Forecast data © [Open-Meteo](https://open-meteo.com/) under CC BY 4.0. Attribution
is required and shown in-app. Open-Meteo's free tier is **non-commercial**; a
commercial licence is required before monetizing (see `docs/ECONOMICS.md`).

## Roadmap (post-v0.1)

1. Spot detail screen + 24h timeline with the best-window highlighted.
2. Precise high/low tide **times** — DONE, and **free**: `TideEvents` derives them
   from the same Open-Meteo `sea_level_height_msl` series by extrema detection +
   parabolic interpolation (sub-hour precision). No paid tide API needed. A paid
   harmonic source (WorldTides) stays an optional future accuracy upgrade only.
3. **Bathymetry**: DIY Satellite-Derived Bathymetry (Sentinel-2, free) for hero
   spots — the defensible moat. Then crowdsourced session logs.
4. Shared caching backend once past ~500 users; Play/App Store release
   (see the `kmp-compose-play-release` workflow).

## Placeholders to replace before release

- Launcher icons under `androidMain/res/mipmap-*` are copied from a sibling app.
- Spot rule values in `SpotCatalog` need real-surfer calibration.
