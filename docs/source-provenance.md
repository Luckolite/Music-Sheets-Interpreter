# Source origins and standalone adaptations

Provenance records where distributed material came from. It is not an assistant
work log or a claim that recognition is perfect.

The original decoder and training code were selected from Music Sheets and
published with a separate Git history under Apache-2.0. Copyright, dependency
and font notices are retained in [NOTICE](../NOTICE),
[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) and the asset directories.
Model lineage, evaluation and checksums are documented separately in
[the model card](../models/MODEL_CARD.md) and `models/artifacts.json`.

## Source map

Printed note-to-note slides bind their source pitch only when the straight stroke
matches the unique immediate attack on the same staff and in the same measure.
An isolated approach, an intervening attack or an ambiguous source chord cannot
select an arbitrary previous pitch. The shared detector has package-only parity
with the app; existing note metadata and playback realization carry the binding.
The regression uses original synthetic line geometry, with no new assets,
dependencies, model weights or wire fields.

Lexical crescendo/diminuendo continuation preserves bounded OCR evidence in the
existing expressive frame and joins only contiguous pages with observed matching
staff ownership. Physical page edges, graphic hairpins, movement restarts, missing
arrivals and fixed targets remain distinct. The shared continuation helper and
detector have exact package-only parity with the app. The Android analyzer,
desktop page bridge, conversion/viewer assembly and desktop audio assembly use
those same helpers; their scoped adapter changes were reviewed separately.
Original synthetic ownership/geometry and semantic-frame regressions contain no
private score material. No model, external dependency or wire layout changed.

[source-provenance.json](source-provenance.json) records the upstream paths,
reviewed source hashes and base commits used for maintenance. The hashes describe
the reviewed upstream files, not the formatted standalone copies. A base commit
can include separately reviewed working-tree changes; the hash identifies the
exact source bytes. Historical changes and their tests remain in Git history.

An optional `source_normalized_sha256` records those same reviewed bytes with
CRLF converted to LF. It allows Git checkouts to normalize mixed line endings
without masking any other source change; the original raw hash is retained.

`scripts/check_app_drift.py --app PATH` checks those mapped source hashes. It does
not copy files, update hashes, prove semantic equivalence or discover new helpers.
New dependencies and adapters require separate review.

## Standalone boundary

- Java packages and logging are adapted to the standalone API. Android services,
  viewer selection, caches, bitmap ownership and audio rendering are not imported.
- Equivalent read-only collection operations may use Java 17 APIs instead of
  Android-compatible collectors. Source layout can differ without changing logic.
- `ScorePageTimeline` retains standalone page handling rather than the app's
  disabled-page viewer selection.
- Optional musical OCR accepts caller-owned inference and dictionaries. ONNX
  bindings are optional; they are not required by the default JDK-only decoder.
- Native decoder/OCR services use loopback transport and their own build
  fingerprints. App and Sync Hub lifecycle management remain outside this repo.
- Glyph loading uses checksum-verified classpath resources. Bravura and Leland
  templates retain their SIL OFL notices; no score pages are bundled with them.
- Octave direction templates are original font renders, including SMuFL music
  numerals with separately typeset small italic suffixes. The generator uses the
  bundled Bravura OFL font and locally licensed graphical text-font renders;
  Windows defaults use Times Italic, Cambria Italic and Georgia Italic for the
  suffixes. Other hosts can supply those fonts explicitly. No text-font files,
  private score pixels or extracted score glyphs are added to the distribution.
- Shared navigation records and projection are available to integrators. Python
  MIDI export uses the original standalone decoded-data `NavigationBridge` adapter
  to execute the actual shared Java traversal and arrangement rules. Its note
  projection clips partial endings and restores numeric tempo, but does not yet
  realize expressive curves, holds or pass-specific voice omissions. MusicXML
  remains in source reading order. The bridge and Python runtime helper add no
  third-party dependencies or private score fixtures.
- The bounded navigation traversal retains canonical segment anchors, repeat
  ownership, jump phase and diagnostics. Whole-bar projection deliberately
  rejects actual partial-bar jumps; complete segment playback remains separate.
- `ScoreNavigationPerformance` projects explicitly resolved numeric tempo curves
  through actual traversal segments, preserving curve phase at return destinations.
  Holds require explicit boundary ownership and a proved performed-target mapping.
  It does not infer the magnitude of printed ritardando or automatically wire
  application audio and Python exports to the resulting performance clock.
- Java `ScoreSemanticWire` and Python `sheet_interpreter.semantic_wire` encode
  and validate the two framed guide263 semantic sections (not a complete page
  guide). Both use fixed wire IDs and preserve source evidence, targets and
  cross-page endpoints. The Python helper uses only the standard library.

Public regressions use original procedural drawings and shareable examples.
Commercial scores, user libraries, device logs, internal checkpoints and private
source-review fixtures are not distributed. Passing tests does not certify every
pitch, rhythm, symbol or expressive playback behavior on arbitrary scores.

Long and shallow connecting slides retain straight-ink residual and correlation
checks and require a unique immediate printed source in the same staff and
measure. The original generated `WideConnectedSlideTest` covers ascending,
descending and shallow strokes plus absent, displaced, ambiguous and intervening
source attacks, curved ink and excessive length. No private page imagery or
score-specific rules are included. Model weights and record layouts are unchanged.

Short ledger rules can join a printed slide to its source head and stem. The
slide-only raster cleanup verifies a thin horizontal rule on the assigned
physical staff grid, outside its body and near the accepted head, while retaining
shaft and diagonal continuity. Original `LedgerCrossingSlideTest` examples
cover upper and lower rules and reject curved ink, thick shapes and unresolved
staff ownership. No private score imagery, weights, dependencies or wire changes
are included.
