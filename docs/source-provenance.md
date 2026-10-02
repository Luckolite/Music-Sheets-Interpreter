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

Closed-head ownership reuses bounded per-thread flood-fill buffers. Every current
crop cell is overwritten, iteration uses current crop size, and paired growth
commits only after both allocations succeed. Oversized crops retain the original
uncached path. Original generated tone, edge, changing-crop and concurrent tests
preserve baseline decisions; the sorted median and recognition thresholds remain.
The helper has package-only app parity. No model, dependency, record layout or
private source material is included.

An in-staff triplet numeral may use a bounded secondary beam as context. Three
equal double-beamed attacks need distinct connected stems, two separated thick
rails, a secondary rail ending at both group edges and a continuing main rail.
The untouched raster supplies this evidence; numeral shape, independent contrast
levels, voice/system ownership and existing rest/fingering guards remain. The
helper and fourteen procedural tests are original, with package-only app parity.
No score images, song rules, weights, dependencies or record layout changed.

Engraved MusicXML tuplets recover exact written time only when an exporter
division grid cannot represent the cumulative type, dots and ratio and its
serialized duration is exactly the nearest export unit. Explicit intended
durations on exact grids are preserved. The pure-JDK helper has package-only app
parity and original procedural regressions; the app layout adapter still checks
full source identity, pitch, voice and timing coverage. No private score, external
dependency, model or wire-layout change is included.

Octave text and its dotted span use the same local paper contrast. Shallow
texture on shaded paper cannot supply numeral counters or a dotted-line chain;
genuine dark octave ink remains detectable. Original generated ring and stipple
geometry covers the negative case, dark marks on flat and gradient backgrounds,
and preservation of caller pixels. The standalone class has token parity with
the app; weights, dependencies and record layouts are unchanged.

Rest flags joined by a masked staff valley are separated only when visible
rounded peaks prove the printed flag spacing, a width dip and adequate unmasked
support. Ordinary unsplit bulb centers and existing rest outline guards remain.
Original generated width profiles cover merged flags, single rounded bulbs, flat
plateaus and small width noise. No model, dependency or record layout changes.

Overwide semantic staff bands no longer erase a curved raw tie crest. Actual
straight rows remain occluded, and every remaining thin stroke must pass the
existing continuity, returning shoulders and curvature checks. Original generated
curves cover mislabeled crests above and below; bare and partial rules, sloped
beams, one shoulder and broken middles remain negative controls. No inferred
ink, private source raster, model, dependency or record-layout change is added.

Antialiased returning ties crossing staff rules retain both shoulders with a
dark core and almost complete, independently curved stroke coverage. Strict
local contrast remains required. Original generated vectors test above and below
curves and reject bare rules, sloped beams, one-sided or broken curves and pale
ink without a dark core. The ultimate matcher has token parity with the app;
models, dependencies and record layouts are unchanged. Private evidence remains
outside this repository.

Written grace placement exposes original notation values and explicit principal
identity on a metrical clock that preserves the principal's full duration. The
performance clock retains its existing grace budget. Original generated tests
cover prefixes, terminal pairs, unknown ownership, ordinary fast notes, sessions
and distinct source identity. The shared timing API has exact package-only app
parity. The app MusicXML writer emits grace tags without metric duration, and its
layout reader validates grace type, anchor, pitch, voice and retained identities;
those adapters remain outside the standalone Python renderer boundary. No private
score, model, external dependency or wire-layout change is included.

Fermata notation planning selects one surviving outer chord member for a proved
printed symbol and preserves its orientation and final written release. The
shared planner has exact package-only parity with the app. Its original generated
regressions cover held notes/chords, filtered parts, ties and unknown ownership.
The app's MusicXML adapter emits the symbol on the fragment reaching that release
and omits the duplicate direction word; the adapter remains outside the standalone
renderer boundary. No private scan, model, dependency or record layout is included.

Printed fermatas reuse the independently proved compact roof/dot geometry. The
shared producer keeps a written attack descriptor and raw evidence; continuation
pages keep musical timing unresolved until inherited meter is supplied. Page
assembly rebases only detector-owned targets and identities, and rejects ambiguous
chord release times. Original generated ink tests cover orientation, distant
ledger ownership, false dots/slurs and cross-page targets. The standalone page
adapter and app/desktop producer and assembly changes were reviewed separately.
No model, external dependency or record layout changed. Exact hold duration and
performed-target realization remain explicit downstream responsibilities.

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

Shallow connecting strokes can split when staff rules are removed. The detector
rejoins only bounded straight fragments at a proved physical rule crossing,
restoring original dark pixels after combined residual, correlation and unique
immediate source checks. Existing detections are preserved and the staff-free
recheck cannot recurse. Original `StaffCrossingSlideTest` examples cover both
directions, curves, part/measure ownership, source ambiguity, intervening attacks
and genuinely absent ink. No private score or model/dependency/wire changes
are included.

The interpreter speedup review preserves tie candidate ordering and component connectivity. Per-call scratch buffers are reset for each curve, and component membership is captured before local-offset flood filling. The new isolation and membership tests use original generated geometry; no source scans, device data, models or dependencies were added.

Local pitch recovery accepts a beam-covered interior rule only when all four
remaining raw and semantic rules survive on both sides, a thick physical beam
covers the missing rule, and the five-rule extent is unambiguous. The existing
interior fixture has both outer rails and that complete proof; its phase
expectation was reviewed while retaining absent, one-sided and sixth-rule guards.

The original `ClosedStaffBarPhase` helper resolves a curved closing edge from
five thin raw rules joined to an isolated staff-height bar. Every rule must also
survive on both sides of the head. Raw line centers refine slope and spacing;
competing near-best fits must agree on pitch. Extended stems, missing rules,
weak contrast and extra joined rules cannot establish a phase. Original synthetic
controls cover direct geometry and the actual pitch-decoding path, including an
earlier-ending slur and absent semantic stripes. The helper uses the JDK only;
no private score, model, dependency or record-layout change is included.

The reviewed rest projection and sloped-rule cleanup optimizations preserve their detection thresholds and output order. Scratch state stays within one detection call; raw row statistics are reused only across placements of the same immutable input raster. Two original generated parity regressions cover complete rest records and cleaned mask bytes, including coverage boundaries. No private images, model changes, or additional dependencies are included.

The reviewed octave-word classifier packs the existing original template bits once. Exact intersection and union cardinalities replace per-pixel counts; bilinear and nearest sampling, thresholds, candidate order and float scores remain unchanged. Original scaled/shaded words and threshold-noise fixtures cover complete decision parity. No model, private scan or new dependency is included.

Tie candidate search retains all six original acceptance paths. A candidate is skipped only when the remaining samples cannot provide the required visible hits, total coverage, per-bin coverage or existing dark-core gate. Every sample of a completed candidate initializes its curve slots; abandoned tails are never evaluated. Original generated returning curves, staff occlusion, faint ink, straight rules and deterministic noise retain all 1,024 baseline decisions. No model, source image or new dependency is included.

Faint expressions require their original shape evidence. A long straight strong
hairpin arm can recover its pale partner only with opposing straight slopes,
a complete apex and unchanged staff ownership. Predominantly faded dynamic
components remain proposals for the existing music-font matcher; ordinary
dark components keep their bounds. Small accents round their existing core
fraction to pixels with one antialiased edge pixel tolerance while retaining
the absolute core, paired-chevron and physical-staff guards. Original generated
positive and rejection controls include the existing licensed Bravura templates.
The two new helpers use the JDK only; weights, dependencies and record fields
are unchanged, and no private source imagery is included.

Printed five-rule frames can replace compressed semantic aliases on tilted pages.
Measure geometry follows independently validated curved rules; its one-pixel
projection quantization tolerance does not alter strict pitch tracking. A
duplicate curve projection must cover most of the same horizontal staff span,
preserving short independent cue staffs. Existing extents remain in charge;
continuation extends the closing edge only when printed rules and a clipped
head independently prove it. A distant semantic head can veto a bar only with
a continuous locally contrasting printed stem. Ten original generated cases
cover compressed frames, genuine separate staffs, white/shaded semantic bridges,
real long stems and independent cues. Three pure-JDK classes have package-only
app behavior parity; no private source raster, new dependency, weight or record
field is included.

Reduced metrical heads keep their written timing when a continuous printed beam
connects them to an established ordinary attack. A complete pair of attached
shafts can recover a third beam only through the existing five-column proof of
three uninterrupted cores. Original generated mixed-size attacks and short or
ledger shafts preserve genuine isolated grace prefixes, two rails and broken
third-rail rejection.

Balanced thin curved brackets can surround an octave continuation. The inner
word still needs the existing numeral/suffix proof and a following printed dash
span. Original procedural brackets use the existing original rendered control
words; bare numerals and absent dashes cannot transpose notes.

Shaded-paper islands need a locally contrasting printed core before they become
augmentation dots. A long-rule veto beside a tenuto needs contrasting rule ink on
both flanks; broad shaded paper cannot supply it. Original generated uniform and
graded paper controls preserve real dots, double dots and pale rules. These
changes remain inside three mapped JDK classes, with package/diagnostic adapters
only; no private source raster, new dependency, weight or record field is included.

Literal OCR dynamics cannot claim a beamed head inside their own word box. A
printed opening-margin dynamic may use its center anchor within one staff gap
of the opening edge. Full-size ruled ovals can use existing closed raw-ink
topology; integer tie shafts round their existing minimum to raster pixels.
Reduced heads retain an established double beam only with complete attached
rail evidence and independent curved-flag rejection. Twenty-six original
generated controls cover positive geometry, boundaries and rejection cases; the ruled-head fallback requires enclosed pixels in the central oval to reject exterior pockets between filled heads.
The new ownership helper uses only the JDK. Full app token parity retains the
existing package and diagnostic adapters. No private score, new dependency,
weight or framed record field is included.

Complete full-size rail evidence can use a reduced neighboring head mask. A
written double beam can connect neighboring pitches within five staff gaps;
all five existing shaft-to-shaft probes must agree on straight separated cores.
The compact grace classifier retains its original three-gap bound. Thirteen
original generated controls cover mixed masks, broken third rails, wide written
rails, exact span bounds, single cores, bends and opposed shafts. No private
score, model change, dependency or framed record field is included.
