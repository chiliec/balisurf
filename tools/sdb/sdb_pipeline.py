#!/usr/bin/env python3
"""
Satellite-Derived Bathymetry (SDB) for a surf spot — the proprietary reef-shape
layer that no open dataset provides at surf resolution.

Pipeline (all free, no auth):
  1. Query Earth Search STAC for the lowest-cloud Sentinel-2 L2A scene over the
     spot's bbox (public COGs on AWS us-west-2).
  2. Windowed read of just the bbox (blue B02, green B03, NIR B08, SCL) — never
     downloads the whole 100x100km tile.
  3. Stumpf & Holderied (2003) log-ratio relative bathymetry:
         pSDB = ln(n * blue) / ln(n * green)
     Monotonic with depth in clear shallow water; higher pSDB = deeper. It's a
     RELATIVE index — calibrate to metres later with a handful of known depths.
  4. Mask land and non-water (SCL classes + a NIR-brightness fallback) and clip
     the deep-water tail so the reef gradient uses the full colour range.
  5. Write a GeoTIFF (float pSDB) + a PNG heatmap for eyeballing.

Usage:
    python sdb_pipeline.py --lat -8.8153 --lon 115.0886 --name uluwatu \
        --half-km 0.8 --out-dir out

Requires: numpy, rasterio (GDAL). Reads COGs over HTTP via GDAL /vsicurl.
"""
from __future__ import annotations
import argparse, json, math, os, sys, urllib.request

import numpy as np
import rasterio
from rasterio.windows import from_bounds
from rasterio.warp import transform_bounds

STAC = "https://earth-search.aws.element84.com/v1/search"
COLLECTION = "sentinel-2-l2a"
# SCL classes to KEEP as candidate water: 6=water. Everything else is masked.
SCL_WATER = {6}


def find_scene(bbox, max_cloud=10.0, from_date="2022-06-01"):
    body = {
        "collections": [COLLECTION],
        "bbox": bbox,
        "query": {"eo:cloud_cover": {"lt": max_cloud}},
        "limit": 1,
        "sortby": [{"field": "properties.eo:cloud_cover", "direction": "asc"}],
    }
    if from_date:
        # Old-baseline (pre-2022) scenes have produced degenerate flat pSDB here:
        # their L2A returns 0 over dark water, both bands pin to stumpf_ratio's
        # clip floor, and the ratio collapses to exactly 1.0000. Earth Search
        # reports the baseline state per item (`earthsearch:boa_offset_applied`
        # is False on pre-2022 products, True after), so this date default is a
        # blunt convenience, not the real guard — the degeneracy check at the
        # end of main() is. Pass an older date to search the full archive.
        body["datetime"] = f"{from_date}T00:00:00Z/2100-01-01T00:00:00Z"
    req = urllib.request.Request(
        STAC, data=json.dumps(body).encode(), headers={"Content-Type": "application/json"}
    )
    feats = json.load(urllib.request.urlopen(req))["features"]
    if not feats:
        sys.exit(f"No scene under {max_cloud}% cloud for bbox {bbox}. Widen the search.")
    f = feats[0]
    a = f["assets"]
    return {
        "id": f["id"],
        "date": f["properties"]["datetime"][:10],
        "cloud": f["properties"]["eo:cloud_cover"],
        "blue": a["blue"]["href"],
        "green": a["green"]["href"],
        "nir": a["nir"]["href"],
        "scl": a["scl"]["href"],
    }


def read_window(href, dst_bounds_ll):
    """Read only the lon/lat bbox from a COG, reprojecting bounds to its CRS."""
    url = "/vsicurl/" + href
    with rasterio.open(url) as ds:
        left, bottom, right, top = transform_bounds("EPSG:4326", ds.crs, *dst_bounds_ll)
        win = from_bounds(left, bottom, right, top, ds.transform)
        arr = ds.read(1, window=win, out_dtype="float32")
        win_transform = ds.window_transform(win)
        return arr, win_transform, ds.crs


def nearest_resize(a, h, w):
    """Nearest-neighbour resize of a 2D array to (h, w). For integer class maps
    (e.g. SCL) that must not be interpolated."""
    src_h, src_w = a.shape
    ri = (np.arange(h) * src_h / h).astype(int).clip(0, src_h - 1)
    rj = (np.arange(w) * src_w / w).astype(int).clip(0, src_w - 1)
    return a[np.ix_(ri, rj)]


def stumpf_ratio(blue, green, n=1000.0):
    """Relative depth index. Guard against non-positive reflectance."""
    b = np.clip(blue, 1.0, None)
    g = np.clip(green, 1.0, None)
    return np.log(n * b) / np.log(n * g)


def smooth(a, radius=1):
    """NaN-aware box smoothing to suppress single-pixel speckle. Pure numpy:
    average each pixel over a (2r+1)^2 window, ignoring NaNs."""
    filled = np.nan_to_num(a, nan=0.0)
    mask = np.isfinite(a).astype("float32")
    k = 2 * radius + 1
    def boxsum(x):
        c = np.cumsum(np.cumsum(x, axis=0), axis=1)
        c = np.pad(c, ((1, 0), (1, 0)), mode="constant")
        h, w = x.shape
        out = np.zeros_like(x)
        for i in range(h):
            i0, i1 = max(0, i - radius), min(h, i + radius + 1)
            for j in range(w):
                j0, j1 = max(0, j - radius), min(w, j + radius + 1)
                out[i, j] = c[i1, j1] - c[i0, j1] - c[i1, j0] + c[i0, j0]
        return out
    num = boxsum(filled)
    den = boxsum(mask)
    with np.errstate(invalid="ignore", divide="ignore"):
        res = num / den
    res[den == 0] = np.nan
    return res


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--lat", type=float, required=True)
    ap.add_argument("--lon", type=float, required=True)
    ap.add_argument("--name", required=True)
    ap.add_argument("--half-km", type=float, default=0.8,
                    help="half-width of the square AOI in km")
    ap.add_argument("--max-cloud", type=float, default=10.0)
    ap.add_argument("--from-date", default="2022-06-01",
                    help="earliest scene date (YYYY-MM-DD). Defaults to post the "
                         "Jan-2022 processing baseline to avoid the -1000 offset "
                         "trap; pass an older date to widen the search.")
    ap.add_argument("--out-dir", default="out")
    args = ap.parse_args()

    # Build a small square bbox around the spot (deg per km varies with latitude).
    dlat = args.half_km / 111.0
    dlon = args.half_km / (111.0 * math.cos(math.radians(args.lat)))
    bbox = [args.lon - dlon, args.lat - dlat, args.lon + dlon, args.lat + dlat]

    scene = find_scene(bbox, args.max_cloud, args.from_date)
    print(f"scene {scene['id']}  {scene['date']}  cloud {scene['cloud']:.2f}%")

    blue, tr, crs = read_window(scene["blue"], bbox)
    green, _, _ = read_window(scene["green"], bbox)
    nir, _, _ = read_window(scene["nir"], bbox)
    scl_raw, _, _ = read_window(scene["scl"], bbox)

    # blue/green/NIR share the 10m grid; SCL is 20m so it comes back ~half-size.
    # Upsample SCL to the 10m shape by nearest-neighbour (integer classes must
    # not be interpolated) before any per-pixel combination.
    h = min(blue.shape[0], green.shape[0], nir.shape[0])
    w = min(blue.shape[1], green.shape[1], nir.shape[1])
    blue, green, nir = blue[:h, :w], green[:h, :w], nir[:h, :w]
    scl = nearest_resize(scl_raw, h, w)

    pSDB = stumpf_ratio(blue, green)

    # Water mask. The SCL scene-classification band (class 6 = water) is the
    # reliable primary mask — on this AOI it cleanly separates the ~62% ocean
    # from the Bukit cliff (veg/bare/shadow). We AND it with an NIR-dark test as
    # a belt-and-braces drop of any foam/sunglint the classifier tagged as water
    # (water is strongly NIR-absorbing: median ~67 DN here vs >3000 on land).
    scl_water = np.isin(scl.astype(int), list(SCL_WATER))
    nir_water = nir < 500.0
    water = scl_water & nir_water
    pSDB = np.where(water, pSDB, np.nan)

    # Suppress single-pixel speckle: the reef gradient is a spatial signal, the
    # per-pixel Stumpf noise is not. Two passes of a 3x3 NaN-aware box filter.
    pSDB = smooth(smooth(pSDB, radius=1), radius=1)

    # Clip the deep tail so the reef gradient uses the colour range.
    valid = pSDB[np.isfinite(pSDB)]
    if valid.size == 0:
        sys.exit("No water pixels in AOI — check coordinates/cloud.")
    lo, hi = np.nanpercentile(valid, 2), np.nanpercentile(valid, 98)

    # A flat pSDB carries no reef signal — it means the bands pinned to the clip
    # floor in stumpf_ratio (old-baseline scene, all-cloud AOI, fully-masked
    # water). Fail loudly instead of writing a raster that only an eyeball
    # catches. Measured over every committed map, the 2-98% spread runs
    # 0.013 (canggu) to 0.084 (serangan), and the one known-degenerate raster
    # (the old gerupuk) sits at exactly 0.0000 — so 1e-3 separates them with
    # >13x margin.
    if hi - lo < 1e-3:
        sys.exit(
            f"Degenerate pSDB ({lo:.4f}..{hi:.4f}) — no depth signal. Bands likely "
            f"hit the clip floor. Scene {scene['id']} ({scene['date']}); try "
            f"another scene (--from-date/--max-cloud) or re-frame the AOI."
        )

    os.makedirs(args.out_dir, exist_ok=True)
    tif = os.path.join(args.out_dir, f"{args.name}_psdb.tif")
    png = os.path.join(args.out_dir, f"{args.name}_psdb.png")

    with rasterio.open(
        tif, "w", driver="GTiff", height=h, width=w, count=1,
        dtype="float32", crs=crs, transform=tr, nodata=np.nan,
    ) as dst:
        dst.write(pSDB.astype("float32"), 1)

    # PNG heatmap (shallow=warm, deep=cool) without matplotlib.
    norm = np.clip((pSDB - lo) / (hi - lo + 1e-9), 0, 1)
    save_heatmap(norm, png)

    frac = np.isfinite(pSDB).mean() * 100

    # Provenance sidecar. The AOI that makes a map coherent is hand-tuned per
    # spot (seaward shift + wider --half-km), and none of that survives in the
    # raster — write it down so the map can be reproduced without re-deriving
    # the framing from the GeoTIFF's corner coordinates.
    meta = os.path.join(args.out_dir, f"{args.name}_psdb.json")
    with open(meta, "w") as fh:
        json.dump({"args": vars(args), "bbox": bbox, "scene": scene,
                   "psdb_2_98": [float(lo), float(hi)],
                   "water_pct": round(float(frac), 1)}, fh, indent=2)

    print(f"water pixels: {frac:.0f}% of {h}x{w} AOI")
    print(f"pSDB range (2-98%): {lo:.4f} .. {hi:.4f}  (higher = deeper)")
    print(f"wrote {tif}")
    print(f"wrote {png}")
    print(f"wrote {meta}")


def save_heatmap(norm01, path):
    """Blue(deep)->green->yellow(shallow) ramp; NaN -> black. Uses Pillow."""
    from PIL import Image
    a = np.nan_to_num(norm01, nan=-1.0)
    rgb = np.zeros((*a.shape, 3), dtype=np.uint8)
    m = a >= 0
    # depth = 1-norm so shallow (low pSDB) is warm; invert for intuitive reef map
    d = 1.0 - a  # 1 = shallow, 0 = deep
    r = np.clip(1.5 * d, 0, 1)
    g = np.clip(1.0 - np.abs(d - 0.5) * 2, 0, 1)
    b = np.clip(1.5 * (1 - d), 0, 1)
    rgb[..., 0] = np.where(m, (r * 255), 0)
    rgb[..., 1] = np.where(m, (g * 255), 0)
    rgb[..., 2] = np.where(m, (b * 255), 0)
    Image.fromarray(rgb, "RGB").save(path)


if __name__ == "__main__":
    main()
