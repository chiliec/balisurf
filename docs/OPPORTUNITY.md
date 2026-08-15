---
project: BaliSurf
status: research
tags: [project, kmp, surf, forecast, bali, opportunity]
---

**Summary**: Feasibility + tiny-MVP spec for a Bali surf-forecast app in Kotlin Multiplatform. Validated against live APIs 2026-08-15.
**Created**: 2026-08-15
**Status**: research → decide build/no-build

---

## The question

Vladimir (surfer + engineer) flagged that existing surf apps are weak:
- **Surfline** — paywalled; reports written by people who never surfed the spot.
- **Surf-Forecast** — swell power numbers drifted from reality.
- **Magicseaweed** — dead (absorbed into Surfline).
- **Windy** — built for wind sports; swell metrics unreliable.
- **Windguru** — the surfer's trusted source: swell **direction**, **period**, **power**.

Gaps he wants closed:
1. Tides wired in (many Bali spots only work on low; almost none on high).
2. **Bathymetry** of Bali spots — reef/bottom shape — which is nowhere in the open; owned by commercial orgs.

---

## Validated findings (live API tests, 2026-08-15)

### 1. Forecast data is a commodity — free, no key
**Open-Meteo Marine API** (`marine-api.open-meteo.com/v1/marine`) returns exactly Windguru's core:
- `swell_wave_height`, `swell_wave_direction`, `swell_wave_period`
- `wave_height/direction/period`, `wind_wave_height`
- Multiple wave models selectable (`ewam`, `gwam`, `gfs`) — can match/compare Windguru's inputs.
- Free tier: **<10,000 calls/day, non-commercial**. Commercial tier is paid (must revisit before monetizing).

**Open-Meteo Forecast API** — free wind (`wind_speed_10m`, `wind_direction_10m`).

Example (Uluwatu -8.815,115.089): swell 2.1m @ 13s from 200° — plausible SW groundswell. Data quality looks real, not stale.

### 2. Tides — free signal for MVP, paid for precision
- Open-Meteo returns `sea_level_height_msl` free; over 48h it oscillates −0.94→+1.59m with clean semidiurnal shape. **Enough to classify low/mid/high for the MVP.**
- For exact high/low **times + harmonic accuracy**: WorldTides API or StormGlass (both paid, small free tiers). Add later if MVP proves demand.

### 3. Bathymetry claim — CONFIRMED, this is the real moat
- Free global **GEBCO** (via opentopodata) returned `elevation 7.0m` at Uluwatu's reef point on a ~450m grid — worthless for spot-level reef shape.
- **Open bathymetry does not exist at surf resolution.** Vladimir is right.
- Implication: raw forecast is undifferentiated (anyone can pull Open-Meteo). **The defensible value is the spot-intelligence layer**, not the numbers.

---

## Where the real opportunity is

NOT "another forecast viewer" — that's commoditized free data with a nicer skin.

The moat is the **Spot Intelligence layer**: hand-curated, per-spot rules that turn raw swell/tide/wind into a plain-language "go / don't go / best window" call. This is exactly the human judgment Surfline fakes with non-surfers and that generic apps lack.

Each spot gets a small rule record:
- Works on tide: `low` / `mid` / `high` (+ optimal cm range once refined).
- Optimal swell direction window (e.g. 200–240°).
- Minimum useful period (e.g. ≥10s for it to break properly).
- Ideal wind (offshore direction) + kill-wind direction.
- Hazard/skill notes (reef, current, crowd, entry).
- **Bathymetry (future premium):** manually digitized reef contour / hand-drawn bottom profile — the proprietary asset nobody has openly. Start by digitizing 3–5 spots yourself from local knowledge + sat imagery at low tide; this is the thing that can't be copied by pulling an API.

The scoring engine cross-references live Open-Meteo swell/tide/wind against each spot's rule record and produces a 0–5 star (Windguru-style) + one-line verdict + best time-window today.

---

## Verdict

**Worth a tiny MVP.** Cost to validate is ~zero (free APIs, your own KMP toolchain). The differentiator is real and defensible (spot rules + eventual bathymetry), and it directly attacks the weaknesses of every incumbent you listed. Risk is low: if the spot-intelligence layer doesn't resonate, you've spent a weekend and lost nothing.

Do NOT try to out-forecast Windguru on raw numbers — lean on it/Open-Meteo as the data feed and win on **local judgment + tides + bathymetry**.

---

## Tiny MVP scope (v0.1)

Ship the smallest thing that proves the spot-intelligence thesis. **5 Bukit spots** hardcoded: Uluwatu, Padang Padang, Bingin, Impossibles, Dreamland.

**One screen, per spot:**
- Live swell height / direction / period (Open-Meteo Marine).
- Current tide state (low/mid/high from `sea_level_height_msl`) + next turn.
- Wind (speed + offshore/onshore relative to spot).
- **Verdict**: star rating + one line ("Firing — SW 12s, low tide, light offshore. Best 6–9am.") from the rule engine.
- Simple 12–24h timeline showing the best window.

Explicitly **out of scope for v0.1**: accounts, push notifications, webcams, paid tides, real bathymetry rendering, more than the 5 spots, Android/iOS store release.

---

## KMP architecture (mirrors your existing app conventions)

Compose Multiplatform, shared logic in `commonMain`, thin platform UI. Package-per-concern like TalkNative.

- `:shared` (commonMain)
  - `forecast/` — Ktor client + kotlinx-serialization models for Open-Meteo Marine + Forecast. `ForecastRepository` with a caching layer (respect free-tier call budget; cache per spot ~1h).
  - `spots/` — `SpotCatalog`: the 5 spots as data (lat/lng + `SpotRules`). This is the IP — keep it as structured data, not code.
  - `tide/` — `TideClassifier`: turn `sea_level_height_msl` series into low/mid/high + next-turn time.
  - `scoring/` — `SpotScorer`: pure function `(Forecast, Tide, Wind, SpotRules) -> Verdict(stars, headline, bestWindow)`. **Fully unit-testable in commonTest, no network** — this is where you write the tests first.
  - `ui/` — Compose screens (spot list, spot detail).
- `:composeApp` (androidMain / iosMain) — platform entry points, DI composition root.

Data-flow seam mirrors your `LanguageModelProvider` pattern: `ForecastSource` interface so Open-Meteo is one conformance; a paid tide/bathymetry source can be added later without touching the scorer or UI.

**Test-first target:** `SpotScorer` against fixed forecast/tide fixtures — encode "Uluwatu on high tide = low stars", "Bingin needs low tide", etc. That's the whole product thesis expressed as tests.

### Dependencies
- Ktor client (`ktor-client-core` + platform engines: `ktor-client-okhttp` android, `ktor-client-darwin` ios)
- `kotlinx-serialization-json`
- `kotlinx-datetime` (tide-window math across Asia/Makassar UTC+8)
- Compose Multiplatform
- (release path later) see skill `kmp-compose-play-release`

---

## API reference (copy-paste)

```
# Marine (swell/wave) — no key
https://marine-api.open-meteo.com/v1/marine?latitude=-8.815&longitude=115.089&hourly=swell_wave_height,swell_wave_direction,swell_wave_period,wave_height,wave_period,sea_level_height_msl&timezone=Asia/Makassar&forecast_days=2

# Wind — no key
https://api.open-meteo.com/v1/forecast?latitude=-8.815&longitude=115.089&hourly=wind_speed_10m,wind_direction_10m&timezone=Asia/Makassar&forecast_days=2
```

Spot coords (Bukit) to seed catalog:
- Uluwatu -8.8153, 115.0886
- Padang Padang -8.8107, 115.1035
- Bingin -8.8060, 115.1120
- Impossibles -8.8020, 115.1180
- Dreamland -8.7970, 115.1130
(refine to exact takeoff lat/lng before shipping)

---

## Decisions taken (2026-08-15)
1. Build **both** tides + bathymetry — not either/or.
2. Economics + full data-cost/acquisition analysis → see [[ECONOMICS]].
3. Bathymetry via DIY Satellite-Derived Bathymetry (Sentinel-2, $0); 2 hero spots in v0.1.

Related: [[TalkNative]] (KMP/Swift app conventions), skill `kmp-compose-play-release`.
