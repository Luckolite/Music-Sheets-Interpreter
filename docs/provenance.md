# Source and artifact provenance

The standalone decoder was extracted from Music Sheets revision
`eb6e9ed8a607225aea0f448e870ae039e72314b1`, after its printed-geometry repairs.
The selected Java source history identifies Luckolite as its author, apart from
a local checkpoint author label. The app's source notice identifies its measure
postprocessing as original Music Sheets code, not a copy of HOMR's Python pipeline.

The selected dependency closure comprises recognition and musical-data classes.
The extraction changes the package to `io.github.luckolite.interpreter`, replaces
Android logging with optional standard-error diagnostics, and adds a public API,
file bridge, build scripts and Python frontend. Original source hashes are recorded
in [source-provenance.json](source-provenance.json). The generic six-class label
vocabulary is an interface; the historical third-party segmentation model is absent.

The training network, exercise generator and export code were authored for the
independent Music Sheets experiment. The provided model is trained within that
experiment's own checkpoint lineage, documented in the model card and manifests.
Recorded checkpoints contain tensors and ordinary training metadata, not user scores.

This is a reviewed extraction from the available source history, not a claim that
the project was developed through a formal clean-room process. All third-party
material retained in the repository has its own identified notice. The Apache
license covers the original material the project author is licensing.

## Public repository boundary

The repository starts with a fresh history. It excludes the Android application,
Sync Hub, update/signing machinery, phone logs, local backups, commercial score
fixtures, recorded instrument banks, Google OCR assets, and earlier HOMR/Andromr
weights. Only explicitly selected source, the independent weights, original examples
and license material are staged for publication.

The source Music Sheets working directory and rollback snapshots are preserved.
Changes to this repository do not alter or install an application on the user's phone.

## September 10 staff and voice repair

The decoder now corroborates local pitch offsets across the printed staff rules,
uses ledger chains to resolve ambiguous grand-staff ownership, recognizes sharp
glyphs with a small staff fringe, and aligns separated seconds only when a shared
printed stem proves their common attack. Mixed-duration chords preserve the
quarter voice while another voice plays eighths. `StablePitchGeometryTest` uses
original generated geometry and note events. No score scans were added.

The app's synthesizer also carries independent sustain through tied barlines.
That Android playback class remains outside this package; the standalone MIDI
writer already retains tied durations across intervening pitches and has an
additional original regression for a short attack continuing into a held voice.
Weights and training lineage are unchanged.

## September 10 archived-result pitch audit

Small bass-clef changes are now confirmed from printed dots and tails when the
semantic mask splits their parts. Staff calibration rejects thick beam rows,
checks local spacing, and requires several corroborating rules on faded scans.
Flat recognition requires a sustained spine; short bowl edges no longer inflate
key-signature counts. A filled unison touching two hollow chord heads is separated
using the printed open centres, preserving the held voices and their dots.

`IndependentPitchAuditTest` contains nine original synthetic regressions. Private
evaluation checked all 210 matched pitch disagreements on 22 archived score pages:
130 supported the newer reading, 77 supported the old reading, and three supported
neither. All 210 adjudicated expectations passed after the repair. Another pitch
change introduced by the repair was checked against the score and confirmed.
These are disagreement checks, not a full transcription accuracy percentage or
proof of parity on arbitrary scores. Commercial images and legacy model artifacts
remain outside this repository. Weights and dependencies are unchanged.

## September 10 accidental font repair

Narrow, tall sharp glyphs now retain their two-spine and crossbar evidence even
when neighboring ink expands the detected bounds. Fragmented natural signs are
reconstructed from their printed spines and offset endpoints, preserving those
endpoints where a thick staff rule crosses the glyph. Accidentals use the locally
measured staff spacing. These are general geometry repairs with no song rules.

`AccidentalFontRegressionTest` adds five original synthetic drawings. The selected
publication passed 230 Java tests, 13 Python tests, and PNG/PDF smoke checks.
Private score review passed 186 pitch assertions across six pages, including 95
newly adjudicated musical pitches and 11 repeated-key regression checks. These
checks exclude a separately tracked metronome-symbol false detection and do not
establish full-score accuracy. Commercial score images remain private. Weights,
dependencies, and the Apache-2.0 license are unchanged.

The recorded source commit identifies the reviewed app snapshot. Additional
staff, key-change, and symbol-filter experiments in the development worktree are
still under evaluation and are excluded from this publication.

The source hashes for `OmrMeasurePostProcessor`, `RawStaffLineDetector`, and
`ScoreNoteTiming` were reconciled with the committed app snapshot after checking
their complete source. Their published implementations already match that snapshot
apart from the documented package, attribution, and terminology adaptations.

## September 10 curved-staff pitch repair

The decoder follows complete five-line groups across curved paper and uses local
line contrast on shaded scans. A sloped local check requires wider evidence of
curvature, so dense beams on a flat staff do not move the pitch reference. Broad
evidence from all five straight rules also rejects false curves made by darker
beams beside faded outer rules, without requiring complete segmentation labels.
`StaffPitchTrack` is original code extracted from the reviewed app snapshot;
`StaffPitchTrackTest` supplies eight original synthetic regressions. Collection
operations preserve compatibility with the app's Android 8 minimum version.

Validation passed 259 standalone Java tests, 13 Python tests, and the model-to-MIDI
PNG/PDF smoke checks. Private source review verified 216 printed pitches, including
all 109 detected notes on one previously troublesome page. Targeted comparisons
covered 14 pages. A wider check of 25 earlier pages passed 344 archived assertions;
an earlier eight-page regression rotation preserved all notes and pitches.
These are selected checks, not whole-library accuracy results.
Artwork false detections, some remaining pitch errors, and duplicated staff groups
are still under investigation. Commercial fixtures remain private; model weights
and dependencies are unchanged.

## September 10 compact-row and ledger ownership repair

Distinct staves with aligned bars now require a visible system connection when
source pixels are available. Overlapping raw and semantic estimates of the same
staff keep their existing merge behavior. Without source pixels, the previous
alignment fallback remains available. This prevents consecutive solo rows from
being interpreted as simultaneous parts.

Ambiguous ledger heads between adjacent solo rows use the same ledger-chain and
stem evidence as connected parts. Heads inside another staff retain their owner.
Short ledger rules use pixel-rounded margins, including one-pixel margins on small
staves; long ending brackets and beams do not count as ledger lines. These are
original geometry repairs with no song-specific rules or model-weight changes.

Eight new synthetic regressions cover separate compact rows, connected pairs,
overlapping staff estimates, ledger ownership beside an ending bracket, opposing
stems, middle-staff ownership, and small ledger margins. An existing skew fixture
now explicitly verifies left-to-right order within each of its three separate rows.
Validation passed 267 standalone Java tests, 13 Python tests, and PNG/PDF
model-to-MIDI smoke checks. Private review checked all nine changed or recovered
pitches across two affected pages and retained 241 saved pitch expectations.
Eleven additional control pages produced identical complete JSON outputs,
including note timings and measure regions. These selected checks do not establish
whole-library accuracy; remaining duplicated staff estimates and other recognition
errors are still under investigation. Commercial score fixtures remain private.

## September 10 touching filled voices

Two touching filled noteheads can form one wide segmentation component and be
misclassified as a half note. The decoder now splits that component when the
printed image contains two filled lobes separated by a narrow neck and supported
by opposing stems. Each head then follows normal pitch, position, beam, and
duration decoding. Existing hollow-voice handling remains separate.

Aligning a displaced chord second now moves other tones in its original chord
columns with it, retaining the earliest original attack and each tone's duration.
Five original synthetic regressions cover separate filled voices, ambiguous ink,
missing stem evidence, complete chord alignment, and an earlier chord anchor.

Validation passed 272 standalone Java tests, 13 Python tests, PNG/PDF model-to-MIDI
smoke checks, and the app's 1,383 tests and Android lint. Comparisons covered
19 focused pages and a rotation of 22 different earlier pages; all 427 retained
pitch/absence expectations passed. No existing pitches or note counts regressed,
and one previously merged note was recovered. Printed-score review confirmed the
new head and affected chord attacks. A saved absence expectation was correctly
interpreted as no note, rather than a missing pitch value.

These are selected comparisons, not whole-library accuracy results. The recovered
passage still needs its printed triplet/rest timing repaired; some other shared-stem
chords have inconsistent duration classifications. A later-page audit now uses the
printed 12/8 meter instead of an old 4/4 fallback. Private scans and score fixtures
remain excluded, and model weights, dependencies, and licensing are unchanged.


## Header normalization and rest preparation

The shared header geometry and measure postprocessor were reviewed from app
commit `2b1179f16232ec149957255f780f2e06248634e0`. The Android `OmrMeasureAnalyzer` adapter
and standalone `SheetInterpreter` API both prepare header labels before rest
recognition and refresh the playable edge using the original staff/barline
segmentation. The standalone adapter continues to accept caller-supplied OCR;
no Android OCR service, private page fixture or device log was copied. Ten
shared synthetic cases and two standalone API cases cover the change.


## Tempo beat-unit recognition

The shared decoder was reviewed from app commit `8ec875f80d8632942135a40541ad4353daf35d08`.
Only printed geometry and copied-mask preparation changed. Eleven original
shared tests and one standalone API test cover tempo symbols and negative
cases; no OCR service, private scan or model asset was imported.
