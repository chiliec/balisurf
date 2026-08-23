# Satellite-Derived Bathymetry (SDB) — the moat

> **Method background:** [`docs/SDB-METHODS.md`](../../docs/SDB-METHODS.md) — the
> primary-source grounding (algorithms, published accuracy numbers, ESA offset,
> failure modes) behind this pipeline, with a source ledger.

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

Outputs land in `out/<name>_psdb.tif`, `out/<name>_psdb.png`, and
`out/<name>_psdb.json` — the sidecar records the AOI args, bbox, scene id/href
and pSDB spread, so any map can be reproduced without reverse-engineering its
framing.

## AOI framing per spot

The default AOI centred on the catalog coordinate lands in the deep channel for
several spots. These are the framings that actually produced the shipped
overlays — a seaward shift (W on Bali's west coast, S into Lombok's bay mouths)
plus a wider window. **Re-running with the catalog coordinate reproduces the bad
map, not these.**

| Spot | Command (from `tools/sdb/`) | Shift vs catalog |
|---|---|---|
| serangan | `--lat -8.7380 --lon 115.2410 --half-km 0.9 --name serangan` | none |
| canggu | `--lat -8.6510 --lon 115.1240 --half-km 1.1 --name canggu_v2` | 0.0070° W |
| playgrounds | `--lat -8.6890 --lon 115.4470 --half-km 1.1 --name playgrounds_v2` | 0.0055° S, 0.0050° W |
| mawi | `--lat -8.8960 --lon 116.2050 --half-km 1.1 --name mawi_v2` | 0.0080° S |
| airportlefts | `--lat -8.7480 --lon 115.1550 --half-km 1.4 --name airportlefts_v2` | 0.0090° W |
| gerupuk | `--lat -8.9160 --lon 116.3360 --half-km 1.4 --name gerupuk_v3` | 0.0100° S |

*(These coordinates were recovered from the shipped rasters' georeferencing, not
from a build log — each matches its catalog coordinate exactly in the unshifted
axis, which is what confirms them. Scenes are not pinned, so a re-run may select
a newer low-cloud scene than the one that produced the committed raster; from
now on the JSON sidecar records the exact scene.)*

Superseded files kept in `out/` for the record: `canggu_psdb.*`,
`playgrounds_psdb.*`, `airportlefts_psdb.*` (valid rasters, wrong framing — no
`mawi_psdb.*` exists; its first run was never committed) and `gerupuk_psdb.*`
(**degenerate — 2-98% spread exactly 0.0000**, the
old-baseline scene that motivated the `--from-date` default and the degeneracy
check). None of these are wired into the app.

## Validated

Run live on two hero spots (scene `S2A_50LKR_20260607`, 0.09% cloud):
- **Uluwatu** — 63% water; coherent shallow reef shelf along the coast grading to
  deep water offshore, the point/cliff correctly masked. `out/uluwatu_psdb.png`.
- **Bingin** — 46% water; same clean shelf→deep gradient. `out/bingin_psdb.png`.

Both show real reef structure, not speckle.

## Multi-scene composite (`composite.py`)

Median-stacks N low-cloud scenes' pSDB (each robustly normalized, then per-pixel
`nanmedian` with a `--min-obs` floor) to suppress noise and fill glint/foam gaps.

**HONEST RESULT — a composite does NOT always beat a single scene.** Measured on
Uluwatu against GEBCO:
- best single scene (0.09% cloud): **corr 0.856**
- 10-scene composite (post-2022, <10% cloud): **corr 0.745**, and no smoother.

Why: when one near-perfect low-cloud scene exists, mixing in hazier scenes at
different tide stages / sun angles *dilutes* the best data and smears the shallow
reef edge (each scene images the reef at a different water level). Median stacking
wins when EVERY scene is mediocre — it averages out clouds/glint — not when you
already hold a flawless one.

**When to use which:**
- **Default: single best scene** (`sdb_pipeline.py`) — use its raster for
  `calibrate.py`. Better wherever a clean (<1% cloud) scene is available (most of
  Bali's dry season).
- **Composite fallback** (`composite.py`) — for spots/regions with NO clean single
  scene (persistently cloudy areas, e.g. wetter parts of Indonesia), where
  stacking many so-so scenes beats any one of them.

Two real pitfalls it handles:
- `--from-date 2022-06-01` default: Sentinel-2's Jan-2022 processing baseline added
  a −1000 radiometric offset; mixing pre/post-2022 scenes corrupts the stack.
- per-scene robust normalization (median/IQR) before stacking, so different
  sun/tide/atmosphere offsets don't add variance.

```bash
python composite.py --lat -8.8153 --lon 115.0886 --name uluwatu \
    --scenes 10 --min-obs 3            # writes out/<name>_psdb_composite.tif/.png
```

## Honest limitations (what "relative" means)

- **Calibration to metres — DONE (bootstrap) via `calibrate.py`.** It fits
  `depth = m·pSDB + c` against control points and writes a metre-depth GeoTIFF+PNG.
  - Validated on Uluwatu against a GEBCO grid: **correlation 0.84, RMSE ~3.4 m**
    over a 1–25 m range. The free satellite index genuinely tracks real depth.
  - The metre map is physically plausible: a shallow reef band (0–3 m) along the
    coast deepening to 20 m+ offshore, median ~10 m. See `out/uluwatu_depth_m.png`.
  - **GEBCO is only a bootstrap** (coarse ~450 m; it can't see the reef itself).
    It proves the model and gives a first metre map, but for a real product feed
    real soundings via `--csv` (see `control_points.example.csv`): chart spot
    depths, a dive-computer track, or crowdsourced phone-depth logs. Even 5–10
    good points beat a GEBCO grid because they sit ON the reef.
- **Clear shallow water only** (~0–15 m). Deeper than that the signal saturates
  (visible as residual speckle in the deep zone of the maps); whitewater and
  heavy sunglint are masked but reduce coverage on big-surf days.
- **Single scene.** Averaging several low-tide, low-cloud, low-swell scenes would
  cut noise further and fill masked gaps — a straightforward enhancement.
- Tide stage at capture shifts the absolute datum; for *relative* reef shape it
  barely matters, for calibrated metres record the capture tide (or calibrate
  with same-datum soundings).

## Calibrate to metres

```bash
# Bootstrap/demo (free, coarse — proves the model):
python calibrate.py --psdb out/uluwatu_psdb.tif --name uluwatu --gebco 12

# Production (real soundings — accurate on the reef):
python calibrate.py --psdb out/uluwatu_psdb.tif --name uluwatu \
    --csv control_points.example.csv
```
Outputs `out/<name>_depth_m.tif` (metres, georeferenced) + `out/<name>_depth_m.png`.
Prints correlation / slope / RMSE and warns if correlation < 0.6 (SDB unreliable
there — turbid, too deep, or glare).

## Roadmap into the app

1. Calibrate 1–2 hero spots to metres (known-depth points) → labelled depth.
2. Multi-scene median composite per spot for a clean master raster.
3. Ship the raster as an in-app reef overlay + use depth-at-spot to sharpen the
   tide rules in `SpotCatalog` (how much water actually covers the reef now).
4. Long game: crowdsource session depth/tide logs to refine — the compounding moat.
