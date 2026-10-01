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
