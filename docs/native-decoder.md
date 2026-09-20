# Optional native Java decoding service

The Java core can run directly on a desktop JVM. `NativeDecoderStages` preserves
the existing geometry preparation sequence, and `NativeDecoderWire` transports
its geometry and the complete analysis records without Java object serialization.
This does not replace image rasterization, segmentation or a caller's OCR engine.

`scripts/build_java.py` generates a fingerprint of the standalone Java sources.
The optional `io.github.luckolite.interpreter.NativeDecoderServer` entry point
requires a 64-hex-character credential as its first standard-input line. It binds
only to loopback (default port 45924, or a port supplied as its first argument).
Keep standard input open for the service lifetime; closing it stops the process.
Do not put credentials in command-line arguments or logs.

Clients must present the same credential and exact source fingerprint. The
standalone fingerprint is deliberately different from an application build:
matching class names do not establish matching interpretation behavior. A caller
must retain local processing when the service is unavailable, mismatched or busy.
Three requests can execute and three more can wait; further connections close.
Packet sizes, dimensions, record counts and decompressed input are bounded.

## Review and verification boundary

The app-side client, managed-worker selection, Hub process ownership and Android
OCR prefetch are platform adapters and are not copied into this standalone
package. OCR prefetch uses an owned raster and retains the normal fallback; it
does not replace OCR or alter detection thresholds. The native core model weights
are unchanged. The standalone build fingerprint helper is original integration
code, not an Android service.

An experiment with a bounded `ExactRasterMemo` for small OCR crops found only
one identical crop among approximately 200 OCR calls on the measured page, with
no end-to-end benefit. That adapter was not retained. Existing standalone
exact-raster regressions remain available to integrations with substantial reuse.

The app worktree has pre-existing differences in `MeterChangeDetector`,
`OmrScoreInterpreter` and `ScorePageTimeline`. This service addition does not
overwrite those classes or update their provenance hashes. Each build hashes
its own sources so the two revisions cannot be silently interchanged.

Shareable tests cover complete note/rest/key and geometry record round-trips,
malformed records, authentication, fingerprint mismatch, concurrent requests and
parent lifetime. Private-page comparisons remain outside this repository. Passing
synthetic transport tests is not a claim of whole-library recognition accuracy or
a guaranteed end-to-end speedup.
