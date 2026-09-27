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
- Shared navigation records and projection are available to integrators. Python
  MIDI and MusicXML exports remain linear; they do not automatically execute
  document-level navigation.

Public regressions use original procedural drawings and shareable examples.
Commercial scores, user libraries, device logs, internal checkpoints and private
source-review fixtures are not distributed. Passing tests does not certify every
pitch, rhythm, symbol or expressive playback behavior on arbitrary scores.
