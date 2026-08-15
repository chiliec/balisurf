# BaliSurf

A Kotlin Multiplatform (Compose) surf-forecast app for Bali, the Nusa islands,
and Lombok.

It does **not** try to out-forecast Windguru on raw numbers. It pulls the same
commodity swell/tide/wind data everyone has (free, from Open-Meteo) and wins on
the thing incumbents lack: **per-spot local judgement** — a rule engine that
turns raw conditions into a plain "go / don't go / best window today" call — plus
hand-built **reef bathymetry** derived from free satellite imagery, which nobody
has in the open at surf resolution.

## Docs

- `AGENTS.md` — dev map, conventions, how to resume, how to add a spot.
- `docs/ARCHITECTURE.md` — how it's built and why.
- `docs/OPPORTUNITY.md` — the market gap + why it's worth building.
- `docs/ECONOMICS.md` — data sources, costs, and the scaling model.
- `docs/release-android.md` — Google Play release runbook.
- `tools/sdb/README.md` — the satellite-bathymetry pipeline (the moat).

## Status

Working v0.1 — builds for Android + iOS, CI green, sideloadable APK on every run.

- **20 spots** across Bukit, Kuta, west coast, east coast, Nusa Lembongan, Lombok,
  grouped by region in the list.
- **Verdict per spot**: star rating + plain-language call + best window today,
  from the pure `SpotScorer` engine (36 tests pin the judgement).
- **24-hour timeline** on the detail screen.
- **Free tide times** (high/low) derived from `sea_level_height_msl` — no paid API.
- **Reef bathymetry overlays** for 14 of 20 spots (satellite-derived, relative
  depth).
- **Session logging** — tap 👍/👎; logs persist on-device and export to the
  calibration CSV. This is the crowdsource ground-truth loop.
- Live data via Open-Meteo Marine + Forecast APIs (free tier, no key).

**Honest caveats:** spot rules are estimates pending real-session calibration;
the launcher icon + store art are placeholders; iOS has no Xcode project yet
(the KMP framework builds, but there's no `iosApp/`).

## Architecture

Single-module Compose Multiplatform, mirroring the sibling apps (`slovo`,
`indonesian-app`). All logic in `commonMain`, thin platform entry points.
See `docs/ARCHITECTURE.md` for the full design and `AGENTS.md` for the file map.

```
composeApp/src/commonMain/kotlin/cx/viz/balisurf/
  domain/     Pure types + logic (Spot, Conditions, Verdict, TideClassifier,
              TideEvents, SessionLog). No Compose/network/platform.
  data/       ForecastSource (seam) + OpenMeteoForecastSource, SpotCatalog (20
              spots), SessionLogStore + LogFileIo (on-device logs).
  scoring/    SpotScorer — pure (Spot, hourly Conditions) -> Verdict. THE thesis.
  platform/   nowIso() expect fun.
  ui/         App (region-grouped list + nav), SpotDetailScreen, AppModule (DI).
composeApp/src/commonTest/  36 tests — the scoring + catalog tests are the spec.
composeApp/src/{androidMain,iosMain}/  platform entry points + LogFileIo/Clock.
tools/sdb/    Satellite-derived-bathymetry pipeline (Python). The moat.
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

### Install on your Android phone (no Play account needed)

Every green CI run publishes a sideloadable debug APK:

1. Open the repo's **Actions → CI** → the latest green run.
2. Download the **`balisurf-debug-apk`** artifact (a zip) and unzip it.
3. Copy `composeApp-debug.apk` to your phone and tap it to install (allow
   "install from unknown sources" for your file manager / browser once).

This is a debug build — unsigned for Play, but perfect for field-testing the
verdicts against real surf. The Play release pipeline (`docs/release-android.md`)
is for public distribution.

iOS: no Xcode project yet. The KMP framework builds (`ComposeApp`), but an
`iosApp/` Xcode project still needs adding before it runs on a device — see
`AGENTS.md` "What's deliberately NOT done".

## Data attribution

Forecast data © [Open-Meteo](https://open-meteo.com/) under CC BY 4.0. Attribution
is required and shown in-app. Open-Meteo's free tier is **non-commercial**; a
commercial licence is required before monetizing (see `docs/ECONOMICS.md`).
Reef bathymetry is derived from Copernicus Sentinel-2 imagery (see
`tools/sdb/README.md`).

## Roadmap

Done: verdict engine, region-grouped 20-spot catalog, 24h timeline, free tide
times, satellite reef overlays (14/20), session-logging crowdsource loop,
Android/Play release pipeline. Next, in rough order:

1. **Calibrate spot rules** against real session logs / local knowledge (the
   rules are estimates). Every change pinned by a `SpotScorerTest`.
2. **iOS**: add the `iosApp/` Xcode project + port slovo's `platform :ios`
   fastlane block.
3. **Replace placeholders**: launcher icon, Play store art.
4. **Reef overlays for the last 6 spots** (boat-access/deep — harder; try AOI
   shifts + `composite.py`).
5. **Backend fan-out** once past ~500 users (see `docs/ECONOMICS.md`).

## Placeholders to replace before a public launch

- Launcher icons under `androidMain/res/mipmap-*` (copied from a sibling app).
- Play store art under `store-assets/` (generated placeholders).
- Spot rule values in `SpotCatalog` need real-surfer calibration.

## Licence

No licence chosen yet — that's the owner's call. Until a `LICENSE` file is added,
this is "all rights reserved" by default. Forecast/imagery data carry their own
upstream licences (see Data attribution).
