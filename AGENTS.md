# AGENTS

Working notes for anyone (human or AI) picking up BaliSurf. See `README.md` for
the product pitch and `docs/` for the deeper design + rationale.

## Map

```
composeApp/src/commonMain/kotlin/cx/viz/balisurf/
  domain/     Pure types + logic, no Compose/network/platform. Fully unit-tested.
              Spot, SpotRules, Conditions, Verdict, TideState (Spot.kt/Conditions.kt),
              TideClassifier (LOW/MID/HIGH bands), TideEvents (high/low TIMES),
              SessionLog (@Serializable crowdsource record).
  data/       ForecastSource (the backend seam) + OpenMeteoForecastSource (free,
              no key), SpotCatalog (the 20 spots = the product IP, as data),
              SessionLogStore + LogFileIo (on-device log persistence, interface seam).
  scoring/    SpotScorer — pure (Spot, hourly Conditions) -> Verdict. THE product
              thesis. Hard gates + weighted blend + SDB water-over-reef multiplier.
  platform/   nowIso() — expect fun, java.time (android) / NSDateFormatter (ios).
  ui/         Compose: App (region-grouped list + nav), SpotDetailScreen (verdict,
              24h timeline, tides, reef overlay, session-log card), AppModule (DI root).
composeApp/src/commonTest/  36 tests. The scoring + catalog tests ARE the spec.
composeApp/src/androidMain/ MainActivity, AndroidLogFileIo, Clock, manifest, res.
composeApp/src/iosMain/     MainViewController, IosLogFileIo, Clock.
composeApp/src/commonMain/composeResources/drawable/  reef_<spot>.png overlays (18).
tools/sdb/    Python: satellite-derived-bathymetry pipeline (the moat). See its README.
iosApp/       Xcode project (ported from slovo). Thin: AppDelegate/SceneDelegate
              hand off to MainViewController(); a Run Script phase builds the
              KMP framework (:composeApp:embedAndSignAppleFrameworkForXcode).
docs/         OPPORTUNITY, ECONOMICS (rationale + costs), release-android, ARCHITECTURE.
store-assets/ Play listing metadata + placeholder images.
fastlane/     Android/Play + iOS/TestFlight release lanes (see docs/release-android.md
              and the Fastfile header for the iOS signing model).
```

## Conventions

- **Keep `domain/` and `scoring/` pure** — no Compose, no network, no platform.
  Time/IO goes behind a seam (`ForecastSource`, `LogFileIo`, `nowIso()`).
- **`SpotScorer` changes must be pinned by a test.** The tests encode the local
  surf judgement ("Ulu dead on low", "Bingin needs low"). Changing a rule without
  changing a test loses the spec. Same for `SpotCatalog` — the catalog tests guard
  structure (unique ids, Bali/Lombok bbox, east-coast west-offshore, north-crossing
  windows).
- **Spot rule values are ESTIMATES.** They came from general knowledge, not
  measured data. Calibrate against real sessions (the session-log CSV export feeds
  `tools/sdb/calibrate.py` and rule tuning). Every tuning change → a test.
- **Compose generated-resources package is `balisurf.composeapp.generated.resources`**
  (derived from `rootProject.name`, NOT the namespace). Bit me twice; don't guess.
- **Kotlin 2.4 gotcha:** `kotlinx.datetime.Clock.System` fails the Kotlin/Native
  compile (API moved). Use the `nowIso()` expect fun. expect *functions* need no
  compiler flag; expect *classes* need `-Xexpect-actual-classes`. Prefer a plain
  interface over an expect class for platform seams (see `LogFileIo`) — it lets
  commonTest fake it.
- Commit messages: conventional style, no Co-Authored-By trailer.
- No runtime secrets in the repo. Release signing/keys are gitignored (see
  `.gitignore`, `keystore.properties.example`, `docs/release-android.md`).

## Build / test / run

```bash
./gradlew :composeApp:allTests        # pure commonTest suite (36 tests)
./gradlew :composeApp:assembleDebug   # sideloadable debug APK
# iOS (needs Xcode + a JDK): open iosApp/iosApp.xcodeproj, or headless
# (ARCHS=arm64 is required — the generic sim destination otherwise adds x86_64,
# and the project has no iosX64 target; sim *tests* also need one simulator
# device to exist: xcrun simctl create ...):
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' ARCHS=arm64 \
  CODE_SIGNING_ALLOWED=NO build
```
No local build env? Every green CI run publishes the APK as the
`balisurf-debug-apk` artifact (Actions → CI) — download + sideload. See README.

## Add a spot

1. Add a `Spot(...)` to `SpotCatalog.kt` with a `region`, coords, and `SpotRules`.
   Mind the regional offshore-wind convention (Bukit/west = E/SE; east coast = W;
   south-Lombok bays = north-crossing 315..45). Comment block at the top of the
   file explains it.
2. The catalog tests (bbox, unique id, region) will guard it — run `allTests`.
3. Optional reef overlay: `cd tools/sdb && python sdb_pipeline.py --lat .. --lon ..
   --name <id>`, eyeball the PNG, and if coherent copy it to
   `composeResources/drawable/reef_<id>.png` + add a branch to `reefDrawable()` in
   `SpotDetailScreen.kt`. SDB fails on boat-access/deep spots — that's fine, the
   card just won't show. Low water%? Try shifting the AOI seaward + `composite.py`.

## Regenerate reef bathymetry

See `tools/sdb/README.md`. Pipeline: Sentinel-2 (free) → Stumpf log-ratio →
water mask → GeoTIFF + PNG. `calibrate.py` turns the relative index into metres
against control points. `composite.py` medians multiple scenes for cloudy spots.

## What's deliberately NOT done (next steps)

- **First TestFlight build** — the iOS pipeline (iosApp/, fastlane ios lanes,
  ios-testflight.yml) is wired but has never shipped: needs the App Store Connect
  app record, `fastlane ios signing_assets` run once locally, and the ASC_*/
  DIST_CERT_* CI secrets. No iOS store metadata/screenshots in store-assets/ yet
  (the `ios release` lane will error until they exist).
- **Real spot-rule calibration** — needs field session logs or your local knowledge.
- **Backend fan-out** — client-direct is fine < ~500 users; see docs/ECONOMICS.md.
- **2 spots lack reef overlays** (airportlefts, gerupuk — deep-channel / bay
  reef-flat where SDB has no usable gradient).
- **Placeholder store art + launcher icon** — replace before a public launch.
