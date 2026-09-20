# Exact raster preparation helpers

Two optional Java utilities support integrations that provide OCR evidence:

- `ColumnInkBounds` computes the red-channel ink bounds of both halves at every
  vertical cut. One column scan and prefix/suffix unions replace a complete scan
  per cut; empty halves remain null and the threshold is supplied by the caller.
- `MeterCropRaster.prepare` performs in-place ARGB meter-crop cleanup using the
  same ink mask, staff bridges and edge-rule removal as the companion Android
  app. The crop's source top, first staff line and staff gap retain source pixel
  coordinates. Supply the same valid crop/staff geometry as the Android adapter.

The app reads/writes the bitmap in bulk, reuses the exact split bounds, and caches
shape matches only for identical rectangles within one immutable glyph crop.
Android bitmap scaling and text-recognition variants are unchanged. The CLI does
not run Android OCR, so these optional helpers do not change CLI decoding.

Generated tests compare every split against the original full-raster scan,
including empty rasters and threshold edges, and compare meter cleanup across
200 colored synthetic crops against the original operation order. No private
scores, device logs or app services are included. No model weights changed.

The barline detector also defers pure note-ownership veto scans until a column
passes its existing semantic/raw barline checks. These scans cannot create a bar,
so running them on rejected columns supplied no output. A 96-case seeded golden
regression captured before the optimization checks sparse/dense, sloped, pale
and label-only inputs, with unchanged input masks and exact boundary lists.

The app now also precomputes tile-edge ownership scores instead of repeating the
same arithmetic for each overlapping tile. This matches the standalone reader's
existing precomputed `edge` array. Its dedicated PC-owned Android runtime uses up
to four CPU inference threads when available; phones/tablets retain two. The
standalone CLI already exposes `--threads` (default two), so no CLI default is
changed. A repeated two/four-thread Android benchmark verified identical class
arrays across eight generated tiles; this is scheduling optimization, not a new
model or an accuracy claim.
