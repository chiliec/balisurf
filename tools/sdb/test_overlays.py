#!/usr/bin/env python3
"""Guard the committed pSDB rasters against the one failure that already shipped:
a DEGENERATE map — every pixel the same value, no depth signal at all.

It happened once (the old gerupuk raster, 2-98% spread exactly 0.0000, from an
old-baseline scene whose L2A returns 0 over dark water: both bands pin to
stumpf_ratio's clip floor and the ratio collapses to 1.0000). Nothing but an
eyeball caught it. sdb_pipeline.py now refuses to write one; this pins the same
threshold over what's already in out/, so a bad raster can't be committed by
hand or carried in from an older run.

Stdlib only, on purpose: the pipeline writes uncompressed float32 GeoTIFFs, so
the samples are readable with `struct` alone. That keeps this runnable in CI
(and on a laptop) without numpy/rasterio/GDAL.

Run: python3 tools/sdb/test_overlays.py
"""
import glob
import os
import struct
import sys

# Same threshold as sdb_pipeline.py's gate. Measured over every committed
# raster, real maps span 0.013 (canggu) to 0.084 (serangan); the known-bad
# gerupuk sat at exactly 0.0000. 1e-3 separates them with >13x margin.
MIN_SPREAD = 1e-3

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "out")

# TIFF tag ids we need. Anything else in the header is ignored.
WIDTH, HEIGHT, COMPRESSION, STRIP_OFFSETS, STRIP_BYTES = 256, 257, 259, 273, 279


def read_tags(path):
    """Minimal TIFF IFD parse — {tag: (values,)} for the first directory."""
    data = open(path, "rb").read()
    endian = "<" if data[:2] == b"II" else ">"
    ifd = struct.unpack(endian + "I", data[4:8])[0]
    count = struct.unpack(endian + "H", data[ifd:ifd + 2])[0]
    sizes = {1: ("B", 1), 3: ("H", 2), 4: ("I", 4), 11: ("f", 4), 12: ("d", 8)}
    tags = {}
    for i in range(count):
        entry = ifd + 2 + i * 12
        tag, typ, n = struct.unpack(endian + "HHI", data[entry:entry + 8])
        if typ not in sizes:
            continue
        fmt, size = sizes[typ]
        total = size * n
        # Values <= 4 bytes are inlined in the entry, otherwise it holds an offset.
        pos = struct.unpack(endian + "I", data[entry + 8:entry + 12])[0] if total > 4 else entry + 8
        tags[tag] = struct.unpack(endian + fmt * n, data[pos:pos + total])
    return data, endian, tags


def psdb_spread(path):
    """2-98% spread of the finite samples — the pipeline's own coherence metric."""
    data, endian, tags = read_tags(path)
    if tags[COMPRESSION][0] != 1:
        raise SystemExit(f"{path}: compressed TIFF, this reader only handles raw strips")
    vals = []
    for off, nbytes in zip(tags[STRIP_OFFSETS], tags[STRIP_BYTES]):
        vals.extend(struct.unpack(endian + "%df" % (nbytes // 4), data[off:off + nbytes]))
    finite = sorted(v for v in vals if v == v)  # NaN != NaN drops the masked land
    if not finite:
        raise SystemExit(f"{path}: no finite pixels — fully masked raster")
    n = len(finite)
    return finite[int(n * 0.02)], finite[int(n * 0.98)]


def main():
    # Single-scene rasters only: *_psdb_composite / *_depth_m / *_slope are
    # different products on different scales and don't share this threshold.
    rasters = sorted(glob.glob(os.path.join(OUT_DIR, "*_psdb.tif")))
    assert rasters, f"no rasters found in {OUT_DIR}"

    failures = []
    for path in rasters:
        lo, hi = psdb_spread(path)
        name = os.path.basename(path)
        if hi - lo < MIN_SPREAD:
            failures.append(f"  {name}: spread {hi - lo:.4f} ({lo:.4f}..{hi:.4f})")
        else:
            print(f"ok   {name:28s} spread {hi - lo:.4f}")

    if failures:
        print(f"\nDEGENERATE rasters (spread < {MIN_SPREAD}) — no depth signal:")
        print("\n".join(failures))
        print("Re-run sdb_pipeline.py with a different scene or AOI; see tools/sdb/README.md.")
        sys.exit(1)

    print(f"\n{len(rasters)} rasters, all carry a depth signal.")


if __name__ == "__main__":
    main()
