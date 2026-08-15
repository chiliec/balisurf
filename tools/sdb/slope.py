#!/usr/bin/env python3
"""
Reef slope layer from an SDB depth (or pSDB) raster.

Surf relevance: how fast the bottom rises toward shore controls how a wave breaks.
  - steep slope  -> wave jacks up fast -> punchy, hollow, barrelling (reef-pass feel)
  - gentle slope -> wave stands tall slowly -> softer, more forgiving shoulder

Given a depth/pSDB GeoTIFF, compute the spatial gradient magnitude (per-pixel rise
over run) and write it as a GeoTIFF + PNG. If the input is calibrated metres, the
slope is real (m per 10 m pixel = %). If it's relative pSDB, the slope is a
relative steepness index — still shows WHERE the reef edge is steepest, which is
what matters for reading a break.

Usage:
    python slope.py --in out/uluwatu_depth_m.tif --name uluwatu
    python slope.py --in out/uluwatu_psdb.tif   --name uluwatu_rel

Requires: numpy, rasterio.
"""
from __future__ import annotations
import argparse, os
import numpy as np
import rasterio

# pixel size in metres (Sentinel-2 10 m grid; the SDB rasters inherit it)
PIXEL_M = 10.0


def gradient_magnitude(z, pixel_m=PIXEL_M):
    """NaN-aware central-difference gradient magnitude of a 2D field.
    Returns sqrt(dz/dx^2 + dz/dy^2). NaN neighbours propagate to NaN so the reef
    edge isn't contaminated by masked land."""
    gy, gx = np.gradient(z, pixel_m)          # np handles spacing; NaNs propagate
    mag = np.sqrt(gx * gx + gy * gy)
    return mag


def presmooth(z, radius=2):
    """NaN-aware box smoothing before differentiation. A gradient amplifies noise,
    so the raw pSDB/depth field MUST be smoothed first or the slope map is pure
    speckle. Larger radius than the pipeline's denoise since slope is 2nd-order."""
    filled = np.nan_to_num(z, nan=0.0)
    mask = np.isfinite(z).astype("float64")
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
    num, den = boxsum(filled), boxsum(mask)
    with np.errstate(invalid="ignore", divide="ignore"):
        res = num / den
    res[den == 0] = np.nan
    return res


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--in", dest="src", required=True, help="depth or pSDB GeoTIFF")
    ap.add_argument("--name", required=True)
    ap.add_argument("--out-dir", default="out")
    args = ap.parse_args()

    with rasterio.open(args.src) as ds:
        z = ds.read(1).astype("float64")
        transform = ds.transform
        crs = ds.crs
    z = np.where(np.isfinite(z), z, np.nan)

    z = presmooth(z, radius=3)     # denoise before differentiating (critical)
    slope = gradient_magnitude(z)
    slope = presmooth(slope, radius=2)   # post-smooth of the slope field

    valid = slope[np.isfinite(slope)]
    if valid.size == 0:
        raise SystemExit("No valid pixels — is the input all-NaN?")
    lo, hi = np.nanpercentile(valid, 2), np.nanpercentile(valid, 98)

    os.makedirs(args.out_dir, exist_ok=True)
    tif = os.path.join(args.out_dir, f"{args.name}_slope.tif")
    png = os.path.join(args.out_dir, f"{args.name}_slope.png")

    with rasterio.open(
        tif, "w", driver="GTiff", height=slope.shape[0], width=slope.shape[1],
        count=1, dtype="float32", crs=crs, transform=transform, nodata=np.nan,
    ) as dst:
        dst.write(slope.astype("float32"), 1)

    save_slope_png(np.clip((slope - lo) / (hi - lo + 1e-9), 0, 1), png)

    units = "m rise per 10m (=%)" if "depth" in os.path.basename(args.src) else "relative index"
    print(f"slope range (2-98%): {lo:.4f} .. {hi:.4f}  [{units}]")
    print(f"steepest reef pixels are the barrelling zone; flat = soft shoulder.")
    print(f"wrote {tif}")
    print(f"wrote {png}  (dark=flat, bright=steep)")


def save_slope_png(norm01, path):
    """Grayscale->hot ramp: flat=dark, steep=bright yellow/white. NaN=black."""
    from PIL import Image
    a = np.nan_to_num(norm01, nan=-1.0)
    m = a >= 0
    v = np.clip(a, 0, 1)
    # black -> red -> yellow -> white hot ramp
    r = np.clip(v * 3, 0, 1)
    g = np.clip(v * 3 - 1, 0, 1)
    b = np.clip(v * 3 - 2, 0, 1)
    rgb = np.zeros((*a.shape, 3), dtype=np.uint8)
    rgb[..., 0] = np.where(m, r * 255, 0)
    rgb[..., 1] = np.where(m, g * 255, 0)
    rgb[..., 2] = np.where(m, b * 255, 0)
    Image.fromarray(rgb, "RGB").save(path)


if __name__ == "__main__":
    main()
