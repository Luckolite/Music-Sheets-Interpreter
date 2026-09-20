# Exact-raster OCR result reuse

`ExactRasterMemo<T>` is an optional utility for integrations supplying OCR annotations.
Create one instance per page and recognizer configuration. Pass the image dimensions,
ARGB pixels and a reader callback. Successful non-null results are reused only when
the dimensions and SHA-256 of all pixels match. Exceptions and null results are not
cached. Entries are bounded by the chosen capacity and evicted in least-recently-used
order. The caller must confine an instance to one thread and discard it after the page.

The standalone decoder consumes caller-supplied annotations; it does not run Android
OCR, so this utility does not change CLI recognition or exports. The companion app
uses a page-scoped adapter to reuse identical full-page and crop OCR requests across
its number, meter, ornament, technique and dynamics readers. It also reads meter-crop
pixels in bulk rather than invoking a native bitmap getter per pixel. These Android
adapters are intentionally not imported here. Their recognition thresholds, crop
geometry, retry variants and model are unchanged.

The synthetic regression verifies equal raster reuse, dimension separation, edited
pixels, exception retry and eviction. No private images or device diagnostics are
included. Existing source-drift reports outside this utility are not silenced by this
port; the provenance entry records the reviewed app base plus working-tree addition.
