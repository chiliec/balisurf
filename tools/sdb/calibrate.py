#!/usr/bin/env python3
"""
Calibrate a relative pSDB raster (from sdb_pipeline.py) to absolute depth in
METRES by fitting a linear model  depth = m * pSDB + c  against control points.

Two control-point sources:
  --csv FILE      real soundings: columns lat,lon,depth_m  (positive = metres
                  deep). This is the production path — nautical-chart spot depths,
                  a dive-computer track, or crowdsourced phone-depth logs.
  --gebco N       bootstrap/demo: auto-sample an NxN grid of GEBCO depths over
                  the raster footprint (free, coarse ~450m). Good enough to prove
                  the model and get a first metre map; replace with real soundings
                  for a real product.

Outputs:
  <name>_depth_m.tif   calibrated depth in metres (GeoTIFF, georeferenced)
  <name>_depth_m.png   depth heatmap with a printed scale
  fit stats to stdout (correlation, slope/intercept, RMSE, n)

Requires: numpy, rasterio, pillow.
"""
from __future__ import annotations
import argparse, csv, json, sys, urllib.request

import numpy as np
import rasterio
from rasterio.transform import rowcol
from rasterio.warp import transform as warp_tf


def sample_raster(psdb, transform, crs, lats, lons):
    """Return pSDB values at each lon/lat (NaN if off-grid or masked)."""
    xs, ys = warp_tf("EPSG:4326", crs, list(lons), list(lats))
    out = []
    for x, y in zip(xs, ys):
        r, c = rowcol(transform, x, y)
        if 0 <= r < psdb.shape[0] and 0 <= c < psdb.shape[1]:
            out.append(float(psdb[r, c]))
        else:
            out.append(np.nan)
    return np.array(out)


def gebco_grid(bounds_ll, n):
    """Sample an NxN grid of GEBCO depths (positive metres) over lon/lat bounds.
    opentopodata's free API caps at 100 locations per request, so batch."""
    left, bottom, right, top = bounds_ll
    lats = np.linspace(bottom, top, n)
    lons = np.linspace(left, right, n)
    pts = [(la, lo) for la in lats for lo in lons]
    results = []
    for i in range(0, len(pts), 100):
        chunk = pts[i:i + 100]
        locs = "|".join(f"{la},{lo}" for la, lo in chunk)
        url = "https://api.opentopodata.org/v1/gebco2020?locations=" + locs
        results.extend(json.load(urllib.request.urlopen(url))["results"])
        if i + 100 < len(pts):
            import time; time.sleep(1.1)   # free tier: 1 req/sec
    rows = []
    for (la, lo), r in zip(pts, results):
        el = r["elevation"]
        if el is not None and el < 0:            # underwater only
            rows.append((la, lo, -float(el)))    # depth = -elevation
    return rows


def read_csv(path):
    rows = []
    with open(path, newline="") as f:
        for d in csv.DictReader(f):
            rows.append((float(d["lat"]), float(d["lon"]), float(d["depth_m"])))
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--psdb", required=True, help="pSDB GeoTIFF from sdb_pipeline.py")
    ap.add_argument("--name", required=True)
    ap.add_argument("--csv", help="control points CSV: lat,lon,depth_m (positive=deep)")
    ap.add_argument("--gebco", type=int, metavar="N",
                    help="bootstrap: sample NxN GEBCO grid instead of a CSV")
    ap.add_argument("--out-dir", default="out")
    ap.add_argument("--max-depth", type=float, default=20.0,
                    help="clip calibrated output to this depth (m) for display")
    args = ap.parse_args()

    if not args.csv and not args.gebco:
        sys.exit("Provide --csv FILE (real soundings) or --gebco N (demo bootstrap).")

    with rasterio.open(args.psdb) as ds:
        psdb = ds.read(1)
        transform = ds.transform
        crs = ds.crs
        b = ds.bounds
    # raster bounds back to lon/lat for GEBCO sampling
    xs = [b.left, b.right]; ys = [b.bottom, b.top]
    lons_b, lats_b = warp_tf(crs, "EPSG:4326", xs, ys)
    bounds_ll = (min(lons_b), min(lats_b), max(lons_b), max(lats_b))

    if args.csv:
        cps = read_csv(args.csv)
        src = f"CSV {args.csv}"
    else:
        cps = gebco_grid(bounds_ll, args.gebco)
        src = f"GEBCO {args.gebco}x{args.gebco} (bootstrap)"

    if len(cps) < 3:
        sys.exit(f"Only {len(cps)} usable control points from {src}; need >=3.")

    lats = [p[0] for p in cps]; lons = [p[1] for p in cps]; depths = np.array([p[2] for p in cps])
    pv = sample_raster(psdb, transform, crs, lats, lons)

    ok = np.isfinite(pv) & np.isfinite(depths) & (depths > 0)
    pv, dv = pv[ok], depths[ok]
    if pv.size < 3:
        sys.exit(f"Only {pv.size} control points fell on wet, unmasked pixels; need >=3.")

    corr = float(np.corrcoef(pv, dv)[0, 1])
    m, c = np.polyfit(pv, dv, 1)
    rmse = float(np.sqrt(np.mean((m * pv + c - dv) ** 2)))
    print(f"control points: {pv.size} usable from {src}")
    print(f"corr(pSDB, depth) = {corr:.3f}")
    print(f"fit: depth_m = {m:.1f} * pSDB + {c:.1f}")
    print(f"RMSE = {rmse:.2f} m over observed {dv.min():.0f}..{dv.max():.0f} m")
    if corr < 0.6:
        print("WARNING: weak correlation — SDB unreliable here (turbid/deep/glare). "
              "Treat the metre map as indicative only.")

    depth_map = m * psdb + c
    depth_map = np.where(np.isfinite(psdb), depth_map, np.nan)
    depth_map = np.clip(depth_map, 0, None)   # no negative depths

    import os
    os.makedirs(args.out_dir, exist_ok=True)
    tif = os.path.join(args.out_dir, f"{args.name}_depth_m.tif")
    png = os.path.join(args.out_dir, f"{args.name}_depth_m.png")

    with rasterio.open(
        tif, "w", driver="GTiff", height=depth_map.shape[0], width=depth_map.shape[1],
        count=1, dtype="float32", crs=crs, transform=transform, nodata=np.nan,
    ) as dst:
        dst.write(depth_map.astype("float32"), 1)

    save_depth_png(depth_map, png, args.max_depth)
    print(f"wrote {tif}")
    print(f"wrote {png}  (scale 0 m = warm/shallow -> {args.max_depth:.0f} m = deep/blue)")


def save_depth_png(depth_m, path, max_depth):
    from PIL import Image
    d = np.clip(depth_m / max_depth, 0, 1)      # 0 shallow .. 1 deep
    a = np.where(np.isfinite(depth_m), d, -1.0)
    m = a >= 0
    shallow = 1.0 - np.clip(a, 0, 1)            # 1 = shallow
    r = np.clip(1.5 * shallow, 0, 1)
    g = np.clip(1.0 - np.abs(shallow - 0.5) * 2, 0, 1)
    bl = np.clip(1.5 * (1 - shallow), 0, 1)
    rgb = np.zeros((*a.shape, 3), dtype=np.uint8)
    rgb[..., 0] = np.where(m, r * 255, 0)
    rgb[..., 1] = np.where(m, g * 255, 0)
    rgb[..., 2] = np.where(m, bl * 255, 0)
    Image.fromarray(rgb, "RGB").save(path)


if __name__ == "__main__":
    main()
