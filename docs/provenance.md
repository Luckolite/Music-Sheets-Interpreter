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
