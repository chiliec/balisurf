# Satellite-Derived Bathymetry (SDB) — the moat

Open forecast data is a commodity; **reef bathymetry is not**. No open dataset
gives Bali spot bottom-shape at surf resolution — the commercial ones charge
thousands per site. This tool builds it ourselves, for **$0**, from free
Sentinel-2 imagery. The output is the defensible layer competitors can't pull
from an API.

## What it does

For a spot coordinate it produces a **relative bathymetry map** (reef shape:
shallow shelf → deep water) as a GeoTIFF + PNG heatmap.

Pipeline (all free, no account, no auth):
1. **Find imagery** — query the [Earth Search STAC](https://earth-search.aws.element84.com/v1)
   for the lowest-cloud Sentinel-2 L2A scene over the spot. Public Cloud-Optimized
   GeoTIFFs on AWS `us-west-2`.
2. **Windowed read** — GDAL `/vsicurl` reads only the ~1.8 km AOI window straight
   from the COG. Never downloads the 100×100 km tile.
3. **Stumpf log-ratio** ([Stumpf & Holderied 2003](https://doi.org/10.4319/lo.2003.48.1_part_2.0547)):
   `pSDB = ln(1000·blue) / ln(1000·green)`. Monotonic with depth in clear shallow
   water — exactly Bali's reefs. Higher = deeper. It's a **relative** index.
4. **Mask** — SCL scene-classification (class 6 = water), AND-gated with an
   NIR-dark test to drop foam/sunglint. Land/cliff → transparent.
5. **Denoise** — two passes of a NaN-aware 3×3 box filter; the reef gradient is a
   spatial signal, per-pixel Stumpf noise is not.
6. **Write** — float `pSDB` GeoTIFF (georeferenced, for the app) + PNG heatmap
   (shallow=warm, deep=cool) for eyeballing.

## Run

```bash
pip install -r requirements.txt
export GDAL_DISABLE_READDIR_ON_OPEN=EMPTY_DIR CPL_VSIL_CURL_ALLOWED_EXTENSIONS=.tif

python sdb_pipeline.py --lat -8.8153 --lon 115.0886 --name uluwatu --half-km 0.9
python sdb_pipeline.py --lat -8.8060 --lon 115.1120 --name bingin  --half-km 0.9
```

Outputs land in `out/<name>_psdb.tif` and `out/<name>_psdb.png`.

## Validated

Run live on two hero spots (scene `S2A_50LKR_20260607`, 0.09% cloud):
- **Uluwatu** — 63% water; coherent shallow reef shelf along the coast grading to
  deep water offshore, the point/cliff correctly masked. `out/uluwatu_psdb.png`.
- **Bingin** — 46% water; same clean shelf→deep gradient. `out/bingin_psdb.png`.

Both show real reef structure, not speckle.

## Honest limitations (what "relative" means)

- **Not metres yet.** `pSDB` is a depth *index*, not calibrated depth. To get
  metres, fit `depth = m·pSDB + c` against a handful of known soundings
  (nautical chart spot-depths, a dive computer, or a phone-depth log). That
  calibration is the next step and turns this into an absolute product.
- **Clear shallow water only** (~0–15 m). Deeper than that the signal saturates;
  whitewater and heavy sunglint are masked but reduce coverage on big-surf days.
- **Single scene.** Averaging several low-tide, low-cloud, low-swell scenes would
  cut noise further and fill masked gaps — a straightforward enhancement.
- Tide stage at image capture shifts the absolute datum; for *relative* reef
  shape it barely matters, for calibrated metres you'd record the capture tide.

## Roadmap into the app

1. Calibrate 1–2 hero spots to metres (known-depth points) → labelled depth.
2. Multi-scene median composite per spot for a clean master raster.
3. Ship the raster as an in-app reef overlay + use depth-at-spot to sharpen the
   tide rules in `SpotCatalog` (how much water actually covers the reef now).
4. Long game: crowdsource session depth/tide logs to refine — the compounding moat.
