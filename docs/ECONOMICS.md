---
project: BaliSurf
status: research
tags: [project, kmp, surf, economics, data-acquisition]
---

**Economics + Data Acquisition** — verified against live pricing pages 2026-08-15.
Companion to [[OPPORTUNITY]]. Decision: build **both** tides + bathymetry.

---

## 1. Data sources — what they cost (verified)

### Forecast (swell/wave/wind) — Open-Meteo
- **Free tier**: 10,000 calls/day, 300,000/month, 600/min, 5,000/hour. **Non-commercial only.**
- **Commercial** (needed the moment you monetize) — `customer-api.open-meteo.com`, dedicated key:
  - **Standard**: 1M calls/month
  - **Professional**: 5M calls/month (+ historical/climate/ensemble)
  - **Enterprise**: >50M calls/month
  - Exact $ is behind a Stripe iframe; published band is roughly €29 / €99 / custom per month (Standard/Pro/Enterprise). Confirm at checkout before relying on it.
  - Pricing is a **fixed monthly call budget**, no per-call overage, no hard cutoff yet (email alerts at 80/90/100%).
- **Attribution required** (CC BY 4.0) — must credit Open-Meteo in-app.

### Tides — WorldTides (verified from pricing page)
- **1 credit = one 7-day high/low prediction for one location.**
- Monthly subscriptions: **$4.99 → 20,000 credits** · $9.99 → 50k · $17.99 → 100k · $29.99 → 200k · $59.99 → 500k · $99.99 → 1M.
- Prepaid (no expiry-pressure): $9.99 → 20k · $19.99 → 50k · $34.99 → 100k · $59.99 → 200k · $124.99 → 500k · $199 → 1M.
- Commercial use allowed.
- Alt: StormGlass (also paid, small free tier) — WorldTides is cheaper/simpler for tides-only.

### Bathymetry — see §3 (the moat; $0 cash, labor-bound)

---

## 2. The economic model — architecture decides everything

The single biggest cost lever is **backend vs client-direct**.

### Option B — Shared backend (RECOMMENDED)
Backend fetches each spot once, caches JSON (~1h forecast, ~1/day tide), fans out to all users.
- Forecast: 5 spots × 24 refresh = **120 calls/day = 3,600/month** → free tier, trivial.
- Tide: Bukit spots share one regional station = **~30 credits/month** → covered by the $4.99 tier with 19,900 to spare.
- **Result: unlimited users at ~$0 data cost.** Cost is only a small VPS/serverless.
- Downside: you run infra (but it's a cron + static JSON; can be a $0–6/mo serverless function or even a GitHub Action writing to a CDN).

### Option A — Client-direct (no backend)
Every app calls the APIs itself → cost scales with **users**:
| Users | Forecast calls/mo | Forecast tier | Tide credits/mo | Tide tier |
|------:|------------------:|---------------|----------------:|-----------|
| 100 | 45,000 | Free (300k) | 9,000 | Free $4.99 |
| 1,000 | 450,000 | **Paid commercial** | 90,000 | $17.99/mo |
| 10,000 | 4,500,000 | **Paid commercial** | 900,000 | $99.99+/mo |

Client-direct blows past the free forecast tier at ~700 users and makes tides a real line item. **Don't do it if you expect growth.**

### Recommendation
Ship v0.1 client-direct (fastest, free, validates thesis at <700 users), but **design behind a `ForecastSource` seam** so flipping to a backend is a one-conformance change. Move to backend before monetizing or crossing ~500 users.

### Total run cost
- **MVP (backend or small userbase): ~$5–11/month** (WorldTides $4.99 + optional $0–6 hosting). Open-Meteo free until monetization.
- **At monetization**: add Open-Meteo commercial licence (~€29/mo Standard covers 1M calls = plenty with a backend).

---

## 3. Bathymetry acquisition — how and how much

This is the defensible asset. No open surf-resolution source exists (confirmed: GEBCO ~450m grid is useless). Four paths, cheapest first:

### Path 1 — DIY Satellite-Derived Bathymetry (SDB) ★ recommended
- **Data**: Sentinel-2, 10m pixels, 5-day revisit, via Copernicus Data Space / Google Earth Engine — **$0**.
- **Method**: Stumpf log-ratio (blue/green band ratio) → relative depth, calibrated with a handful of known depths. Valid to ~15–20m in **clear water**. Bali reefs are shallow (<10m) and clear → ideal fit.
- **Compute**: GEE free tier or local Python — **$0**.
- **Cost = your time**: ~1–2 days for the first spot (build the pipeline), then ~2–4h/spot once scripted.
- **Output**: a 10m depth raster per spot — the proprietary layer nobody has in the open.

### Path 2 — Allen Coral Atlas (free, instant)
- Free benthic + geomorphic + some bathymetry for tropical reefs incl. Indonesia. Coarser and not surf-tuned, but a **free base layer and SDB calibration aid**. Grab it day one.

### Path 3 — Commercial SDB / survey (EOMAP, TCarta, hydrographic vessel)
- $ thousands per area of interest to tens of thousands for a boat survey. **Skip for MVP.** Only if a spot really needs it and revenue justifies.

### Path 4 — Crowdsourced soundings (long-game moat)
- Users log "worked at X tide/swell" + optional GPS/depth pings → a proprietary dataset that **compounds and can't be copied**. $0, post-MVP. This is the real durable moat once you have users.

### Bathymetry plan for v0.1
- Build the SDB pipeline once, produce depth rasters for **2 hero spots (Uluwatu + Bingin)** to prove the concept. Layer Allen Coral Atlas underneath for free coverage on the other 3. Cash cost: **$0**.

---

## 4. Bottom line

- **Cash to run the MVP: ~$5/month.** Cash to build bathymetry: **$0** (labor only).
- The forecast+tide numbers are a commodity you get for free at MVP scale; the money question only appears at monetization (add ~€29/mo Open-Meteo commercial) and at scale (backend keeps it flat).
- **The whole economic case rests on the backend fan-out + DIY SDB**: both push marginal cost per user to ≈$0 while the spot-intelligence + bathymetry layer is what you actually charge for.
- Decision confirmed: **build both** — tides now (free, high impact on spot selection), bathymetry as the SDB pipeline that becomes the moat.

Related: [[OPPORTUNITY]] · skill `kmp-compose-play-release`.
