#!/usr/bin/env python3
"""
Multi-scene median composite for Satellite-Derived Bathymetry.

A single Sentinel-2 scene gives a usable but speckly pSDB (per-pixel noise,
plus foam/sunglint gaps that differ each day). Stacking several low-cloud scenes
and taking the PER-PIXEL MEDIAN of pSDB:
  - cancels random per-pixel noise (the reef gradient is stable, the noise isn't),
  - fills gaps (a pixel masked by glint in one scene is clear in another),
  - is robust to residual cloud/whitewater (median ignores the odd bright outlier).

Pipeline:
  1. STAC-search the N lowest-cloud scenes over the spot bbox.
  2. For each: windowed read of blue/green/NIR/SCL, water-mask (SCL class 6 AND
     NIR-dark), compute pSDB (Stumpf log-ratio). Masked pixels -> NaN.
  3. Stack and take np.nanmedian across scenes; require a minimum number of valid
     observations per pixel (else NaN) so thin coverage doesn't leak noise.
  4. Light NaN-aware smoothing, then write GeoTIFF + PNG.

Reuses the exact masking / Stumpf / smoothing logic from sdb_pipeline.py so the
composite is directly comparable to (and a drop-in upgrade for) the single-scene
raster that calibrate.py consumes.

Usage:
    python composite.py --lat -8.8153 --lon 115.0886 --name uluwatu \
        --half-km 0.9 --scenes 8 --min-obs 3

Requires: numpy, rasterio.
"""
from __future__ import annotations
import argparse, json, math, os, sys, urllib.request

import numpy as np
import rasterio
from rasterio.windows import from_bounds
from rasterio.warp import transform_bounds

# Reuse the validated single-scene helpers.
from sdb_pipeline import (
    stumpf_ratio, smooth, nearest_resize, save_heatmap, SCL_WATER,
)

STAC = "https://earth-search.aws.element84.com/v1/search"
COLLECTION = "sentinel-2-l2a"


def search_scenes(bbox, n, max_cloud, from_date=None):
    body = {
        "collections": [COLLECTION],
        "bbox": bbox,
        "query": {"eo:cloud_cover": {"lt": max_cloud}},
        "limit": n,
        "sortby": [{"field": "properties.eo:cloud_cover", "direction": "asc"}],
    }
    if from_date:
        body["datetime"] = f"{from_date}T00:00:00Z/2100-01-01T00:00:00Z"
    req = urllib.request.Request(
        STAC, data=json.dumps(body).encode(), headers={"Content-Type": "application/json"}
    )
    feats = json.load(urllib.request.urlopen(req))["features"]
    out = []
    for f in feats:
        a = f["assets"]
        out.append({
            "id": f["id"], "date": f["properties"]["datetime"][:10],
            "cloud": f["properties"]["eo:cloud_cover"],
            "blue": a["blue"]["href"], "green": a["green"]["href"],
            "nir": a["nir"]["href"], "scl": a["scl"]["href"],
        })
    return out


def read_window(href, bounds_ll):
    with rasterio.open("/vsicurl/" + href) as ds:
        l, b, r, t = transform_bounds("EPSG:4326", ds.crs, *bounds_ll)
        win = from_bounds(l, b, r, t, ds.transform)
        arr = ds.read(1, window=win, out_dtype="float32")
        return arr, ds.window_transform(win), ds.crs


def scene_psdb(scene, bbox, ref_shape=None):
    """Return a masked pSDB array for one scene, aligned to ref_shape if given."""
    blue, tr, crs = read_window(scene["blue"], bbox)
    green, _, _ = read_window(scene["green"], bbox)
    nir, _, _ = read_window(scene["nir"], bbox)
    scl_raw, _, _ = read_window(scene["scl"], bbox)

    h = min(blue.shape[0], green.shape[0], nir.shape[0])
    w = min(blue.shape[1], green.shape[1], nir.shape[1])
    if ref_shape:
        h, w = min(h, ref_shape[0]), min(w, ref_shape[1])
    blue, green, nir = blue[:h, :w], green[:h, :w], nir[:h, :w]
    scl = nearest_resize(scl_raw, h, w)

    psdb = stumpf_ratio(blue, green)
    water = np.isin(scl.astype(int), list(SCL_WATER)) & (nir < 500.0)
    psdb = np.where(water, psdb, np.nan)
    return psdb, tr, crs


def normalize(psdb):
    """Z-score a scene's pSDB over its own water pixels so scenes taken under
    different sun/tide/atmosphere share a common scale before stacking. Robust
    stats (median / IQR) resist outliers from residual glint. Returns NaN where
    the input was NaN. The composite is thus a RELATIVE index (mean 0, unit
    spread); calibrate.py re-anchors it to metres with control points exactly as
    for a single scene."""
    v = psdb[np.isfinite(psdb)]
    if v.size < 20:
        return psdb  # too few pixels to normalize meaningfully
    med = np.median(v)
    iqr = np.percentile(v, 75) - np.percentile(v, 25)
    if iqr < 1e-9:
        return psdb - med
    return (psdb - med) / iqr


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--lat", type=float, required=True)
    ap.add_argument("--lon", type=float, required=True)
    ap.add_argument("--name", required=True)
    ap.add_argument("--half-km", type=float, default=0.9)
    ap.add_argument("--scenes", type=int, default=8, help="max scenes to stack")
    ap.add_argument("--max-cloud", type=float, default=15.0)
    ap.add_argument("--from-date", default="2022-06-01",
                    help="only scenes on/after this date (YYYY-MM-DD). Default "
                         "2022-06 keeps a consistent S2 processing baseline — the "
                         "Jan-2022 baseline added a -1000 radiometric offset, so "
                         "mixing older scenes corrupts the pSDB stack.")
    ap.add_argument("--min-obs", type=int, default=3,
                    help="min valid scenes per pixel to keep it (else NaN)")
    ap.add_argument("--out-dir", default="out")
    args = ap.parse_args()

    dlat = args.half_km / 111.0
    dlon = args.half_km / (111.0 * math.cos(math.radians(args.lat)))
    bbox = [args.lon - dlon, args.lat - dlat, args.lon + dlon, args.lat + dlat]

    scenes = search_scenes(bbox, args.scenes, args.max_cloud, args.from_date)
    if not scenes:
        sys.exit("No scenes found; widen --max-cloud.")
    print(f"stacking up to {len(scenes)} scenes:")

    stack, tr, crs, ref = [], None, None, None
    for s in scenes:
        try:
            psdb, t, c = scene_psdb(s, bbox, ref_shape=ref)
        except Exception as e:
            print(f"  skip {s['id']} ({s['date']}): {e}")
            continue
        if ref is None:
            ref = psdb.shape; tr = t; crs = c
        # crop every scene to the common reference shape
        psdb = psdb[:ref[0], :ref[1]]
        if psdb.shape != ref:
            # pad short reads with NaN so the stack aligns
            pad = np.full(ref, np.nan, dtype="float32")
            pad[:psdb.shape[0], :psdb.shape[1]] = psdb
            psdb = pad
        # Normalize each scene to a common scale BEFORE stacking — scenes span
        # years with different sun/tide/atmosphere, so raw pSDB offsets differ and
        # medianing them straight adds variance instead of removing it.
        psdb = normalize(psdb)
        frac = np.isfinite(psdb).mean() * 100
        print(f"  + {s['id']}  {s['date']}  cloud {s['cloud']:.2f}%  water {frac:.0f}%")
        stack.append(psdb)

    if not stack:
        sys.exit("No usable scenes after masking.")

    cube = np.stack(stack, axis=0)                      # (scenes, h, w)
    obs = np.isfinite(cube).sum(axis=0)                 # valid count per pixel
    with np.errstate(all="ignore"):
        med = np.nanmedian(cube, axis=0)
    med = np.where(obs >= args.min_obs, med, np.nan)    # drop thin-coverage pixels
    med = smooth(med, radius=1)                         # gentle final denoise

    valid = med[np.isfinite(med)]
    if valid.size == 0:
        sys.exit("Composite empty — raise --scenes or lower --min-obs.")
    lo, hi = np.nanpercentile(valid, 2), np.nanpercentile(valid, 98)

    os.makedirs(args.out_dir, exist_ok=True)
    tif = os.path.join(args.out_dir, f"{args.name}_psdb_composite.tif")
    png = os.path.join(args.out_dir, f"{args.name}_psdb_composite.png")
    with rasterio.open(
        tif, "w", driver="GTiff", height=ref[0], width=ref[1], count=1,
        dtype="float32", crs=crs, transform=tr, nodata=np.nan,
    ) as dst:
        dst.write(med.astype("float32"), 1)
    save_heatmap(np.clip((med - lo) / (hi - lo + 1e-9), 0, 1), png)

    print(f"\nstacked {len(stack)} scenes")
    print(f"median coverage: {np.isfinite(med).mean()*100:.0f}% of {ref[0]}x{ref[1]} "
          f"(pixels with >= {args.min_obs} obs)")
    print(f"pSDB range (2-98%): {lo:.4f} .. {hi:.4f}")
    print(f"wrote {tif}")
    print(f"wrote {png}")


if __name__ == "__main__":
    main()
