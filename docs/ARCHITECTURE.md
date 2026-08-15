# Architecture

How BaliSurf is put together and why. For the map + conventions see `AGENTS.md`;
for the business rationale see `docs/OPPORTUNITY.md` and `docs/ECONOMICS.md`.

## The thesis in one line

Forecast data is a free commodity; **local judgement and reef bathymetry are not**.
BaliSurf pulls the same free swell/tide/wind everyone has and wins on the layer
incumbents lack: per-spot rules that turn raw numbers into a plain "go / best
window" call, plus satellite-derived reef maps.

## Layers (dependency direction points inward)

```
ui  ─────────────►  scoring  ─────────►  domain
 │                     ▲                    ▲
 └──►  data  ──────────┘────────────────────┘
        │
        └──►  platform (nowIso)
```

- **domain** depends on nothing. Pure Kotlin types + math. This is where the
  product's testable truth lives.
- **scoring** depends only on domain. `SpotScorer` is a pure function; you can run
  the entire product logic in a unit test with no Android, no network.
- **data** implements the seams: `ForecastSource` (where forecasts come from) and
  `SessionLogStore`/`LogFileIo` (where logs go). `SpotCatalog` is data, not code.
- **ui** is Compose Multiplatform, one screen list + one detail screen, manual DI
  through `AppModule`. No business logic here — it calls `SpotScorer`.
- **platform** is the thin expect/actual (just `nowIso()` today).

## Key design decisions

### 1. `SpotScorer` is a pure function, and the tests are the spec
`scoreHour(spot, conditions) -> Double` and `verdict(spot, hours) -> Verdict`
have zero dependencies. The commonTest suite encodes real surf judgement as
assertions ("Uluwatu scores 0 on low tide", "a gale blows it out"). This is the
product; the UI is a thin viewer. Any rule change must move a test.

Scoring shape: hard gates first (wrong tide / too small / blown-out wind /
fully-wrong swell direction each zero the hour), then a weighted blend of swell
direction + period + wind quality, then an optional **SDB water-over-reef
multiplier** when a spot has a known reef-crest depth.

### 2. `ForecastSource` is the one seam to any backend
`OpenMeteoForecastSource` (free, no key, two calls: marine + wind) is today's
conformance. A shared caching backend, or a paid tide source, drops in as another
conformance without touching the scorer or UI. This is what makes the
client-direct → backend migration a one-line change when scale demands it (see
docs/ECONOMICS.md: client-direct is fine below ~500 users).

### 3. Tides are free, twice
- `TideClassifier` bands Open-Meteo's `sea_level_height_msl` into LOW/MID/HIGH.
- `TideEvents` finds high/low **times** from the same series by local-extrema
  detection + parabolic interpolation (sub-hour precision). No paid tide API —
  WorldTides stays an optional future accuracy upgrade only.

### 4. Bathymetry is the moat, and it's built not bought
`tools/sdb/` derives reef bathymetry from free Sentinel-2 imagery (Stumpf
log-ratio). No open dataset provides Bali reef shape at surf resolution; the
commercial ones charge thousands. 14 of 20 spots have overlays; the rest are
SDB failures (boat-access / deep water). The relative index calibrates to metres
against control points (`calibrate.py`), and the app's session logs are the
compounding ground-truth source.

### 5. The crowdsource loop closes the ground-truth gap
`SessionLog` captures worked/didn't + the conditions snapshot. `SessionLogStore`
persists on-device (JSON via the `LogFileIo` interface — a plain interface, not
expect/actual, so tests fake it) and `exportCsv()` emits exactly the shape
`tools/sdb/calibrate.py --csv` consumes. User tap → ground-truth data → sharper
rules. It's both the growth feature and the data pipeline.

## Data flow (one app open)

1. `App` calls `AppModule.loadAll()`.
2. For each spot: `ForecastSource.conditions(spot)` → hourly `Conditions`
   (marine + wind, tide already classified).
3. `SpotScorer.verdict(spot, hours)` → stars + headline + best window.
4. `TideEvents.detect(...)` → high/low times.
5. UI renders the region-grouped list; tapping a spot opens the detail screen
   (timeline re-scores each hour, reef overlay if bundled, session-log buttons).

## Tech stack

Kotlin Multiplatform 2.4.10, Compose Multiplatform 1.11.1, AGP 9.3.1, Ktor 3.2
(client), kotlinx-serialization, kotlinx-datetime. Android minSdk 24 / compileSdk
37. Mirrors the sibling apps `chiliec/slovo` and `chiliec/indonesian-app`.

## Testing

36 commonTest cases, all platform-independent (run on the JVM). The scoring and
catalog tests are the meaningful ones — they pin the surf judgement and catch
data-entry mistakes. CI (`.github/workflows/ci.yml`) runs them + assembles a
debug APK on every push; `android-play.yml` builds the release AAB.

Verification note: this project was largely built without a local JVM/Android
build environment — logic was validated by porting to Python and running against
live Open-Meteo data, then confirmed by CI on the real toolchain. If you extend
it on a machine with the SDK, prefer `./gradlew :composeApp:allTests`.
