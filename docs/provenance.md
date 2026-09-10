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


## Curved staff quarter rests

Rest detection now follows the accepted printed staff trajectory in a small image
band. Column translation preserves glyph height, note ownership and source-page
coordinates. A compact, stemless segmentation prediction inside an independently
recognized quarter-rest body is removed before playback timing. No production
rule refers to a song, title or fixture.

Thirteen original synthetic regressions cover both slope directions, raised rests,
held-note ownership, augmentation dots, coordinate mapping, input preservation,
false rest-body heads and real-head negatives. Validation passed 392 standalone
Java tests, 13 Python tests, PNG/PDF model-to-MIDI smoke checks, and 1,496 app tests
with Android lint. Nine targeted private pages were compared: eight complete
outputs stayed identical; one false sounding note was removed from the affected
page, whose 189 printed pitches and final sounding-bar durations were checked.
These are selected checks, not a whole-library accuracy claim. Model weights,
licenses and dependencies are unchanged; commercial images remain private.


## Reduced ledger rules on small stemmed notes

Small stemmed heads can have shorter ledger rules than full-size notes. The
ledger validator now uses a head-sized minimum for that geometry while retaining
normal staff spacing between ledger lines. Reduced rules must extend on both
sides of the head, and the separate inner-ledger requirement remains in force.
Existing grace-prefix recognition then supplies ornament timing.

Ten original synthetic tests cover low and high grace pitches, principal-note
pitch and duration, borrowed grace time, missing ledgers, one-sided rules,
unstemmed blobs and nearby slur fragments. All 402 standalone Java tests, 13 Python
tests, PNG/PDF model-to-MIDI smoke checks, and 1,506 app tests with Android lint
passed. Thirteen targeted private pages were compared: twelve complete outputs
stayed identical. The affected page recovered one printed A3 grace; its D4
principal retained its pitch and gave the grace a quarter-beat from its half-note
duration. No existing pitches changed. All 135 currently reviewed present pitches
across three recent arrangements still agree with the source. A separate false
grace-beam event remains under investigation; this is not a whole-library accuracy
claim. Model weights, licenses and dependencies are unchanged. No commercial
images or device logs were included.


## Grace prefixes and chord triplets

Small grace heads now retain short raw stems even when semantic stem labels are
missing. Pixel-rounded size bounds accommodate fractional staff spacing, and
stemmed ornamental prefixes are not limited to four notes. A printed triplet
applies to three consecutive attack columns, including every compatible chord
member while preserving independent held voices. Complete raw numeral geometry
also removes false note events on the numeral when beam predictions are noisy.

A complete written voice spanning the measure can anchor aligned accompaniment
onsets. Conflicting complete voices keep independent clocks, and an ordering
conflict in a third staff does not disable a consistent piano alignment.

Twenty-eight original synthetic tests cover these repairs and their negative
cases. Validation passed 430 standalone Java tests, 13 Python tests, PNG/PDF
model-to-MIDI smoke checks, and 1,534 app tests with Android lint. Fifteen targeted
private pages were compared: eleven complete outputs stayed identical, no
existing pitches were substituted, three true grace heads were recovered, and
three false numeral events were removed. All 88 changed existing events were
reviewed against printed rhythm; an existing hollow-chord duration error remains
separately tracked. A false grace-beam event also remains under investigation.
These selected checks do not establish whole-library accuracy. Model weights,
licenses and dependencies are unchanged. No commercial images or private logs
are included in this repository.


## Rest bodies, beam separation and rest-containing triplets

Stemless predictions over a complete quarter-rest body are now removed using
independent raw rest geometry, including when an augmentation dot was also
mistaken for a notehead. A separate bound keeps this body check from broadening
the ordinary augmentation-dot rule. Attached stems and nearby real noteheads
remain protected. A complete deskewed five-line staff can also replace a nested
half-spacing alias when all five rules are strong and no halfway rules exist.

Ordinary half rests are recognized from a filled rectangle resting on the middle
staff rule. Explicit leading and interior rests retain their written silence in
incomplete optical measures. Beam reading separates two similarly thick dark
cores when lighter ink has fused the beams; thin slur terminals, noteheads and
single beams remain protected. A printed triplet may contain notes, chords and
rests. Its silence and note durations scale together, while each independent
voice retains ownership of its own leading or following rests. Tuplet processing
runs once at the page-interpretation boundary, after raw note/rest extraction.

The 55 original synthetic regressions cover these cases, including silent-only
triplets, rest positions, unsupported-value barriers, repeated application,
independent sustains and unchanged input pixels. Of the 41 new tests that use the
previous decoder API, 21 fail on the preceding published implementation. Private
source review covers 17 eligible pages: six false quarter-rest notes disappear,
seven ordinary half rests are recovered, and 38 changed existing note events
match the printed timing. No existing MIDI pitch substitutions occur in this
batch. Nine complete page outputs remain identical. Generated audio-to-sheet
sources are excluded from song-accuracy validation and are not accepted as
unverified training labels. These results do not certify the whole library.

No weights, model lineage, license, or third-party dependencies changed. The
public implementation and regressions remain Apache-2.0; private scans and
library metadata are not included.

Validation passed 485 standalone Java tests, 13 Python tests, PNG/PDF-to-MIDI
smoke checks, and 1,584 app tests in each Android variant with debug/release lint.
The corresponding Music Sheets 1.18.109 phone update preserves all 396 recorded
song identities. Whole-library listening validation remains in progress.


## Split flat spines, staff-crossing ties and rounded grace groups

A flat can retain its labelled bowl while its tall spine is labelled as a stem.
The decoder now reconstructs that narrow accidental from the source ink, removes
crossing staff rules and requires flat geometry and a matching bowl pitch. It
rejects sharps, naturals and stems without bowls. The existing measure accidental
state then carries the recovered flat to subsequent matching notes.

Short ties whose two ends overlap one staff rule now require a nearly complete
returning curve with a visible middle and both shoulders. Straight rules,
sloping/stepped beams and incomplete arcs remain excluded. Beamed grace groups
with rounded raster masks can be slightly larger when they have shortened stems,
at least two notes and a substantially larger principal. They share the existing
grace-time budget instead of displacing the written rhythm.

Twenty original synthetic regressions cover these repairs. The exact private
Android capture reproduces three B-natural errors in place of B-flats, an omitted
C tie and two grace groups treated as metrical notes. The candidate corrects
those readings. Fifteen of eighteen targeted page outputs remain identical;
548 separately transcribed pitch checks pass. Another changed page retains a
previously identified false beam-fragment note in a grace group; that unresolved
presence defect is not counted as a correct note or whole-page accuracy success.
Private score scans and phone fixtures are not distributed.

The Android adapter also repairs a cache validation restriction that rejected
quarter, half, dotted and triplet rests. This is app-only serialization code; the
standalone interpreter has no Android guide-cache reader. No weights, dependencies,
license terms or model lineage changed. Whole-library evaluation remains ongoing.

Validation passed 505 standalone Java tests, 13 Python tests, PNG/PDF-to-MIDI smoke
checks, 1,606 unit tests in each Android variant and both lint checks. The corresponding
1.18.110 phone update preserves 396 song identities. All three reported-song pages
were regenerated on the phone; refreshed guides contain the corrected flats, tie
and grace groups and survive cache round trips. These are targeted device checks,
not a claim that every library page or recorded performance is accurate.

## Dotted rests touching thick staff rules

A rest augmentation dot can merge with the antialiased edge of a staff line.
The dot component then spans the search window and was rejected as unrelated ink,
shortening a dotted quarter rest from 1.5 beats to 1 beat. Dot tracing now excludes
long horizontal rows at the expected staff-rule height, retaining the existing
size, position, distance and note-ownership checks.

Six original synthetic regressions cover dots touching either rule, two dots,
plain thick rules, crossing stems and large noteheads. All 511 standalone Java
tests, 13 Python tests and PNG/PDF-to-MIDI smoke checks pass. The Android detector's
17 dotted-rest tests also pass. No weights or licensing changed.

A fresh private phone capture recovers five dotted rests and the printed onset
sequence in two affected measures. An hourly revisit of 22 earlier pages preserves
all pitches and note counts, with 188 prior expectations passing; 21 complete
outputs are unchanged. The remaining older raster recovers three rest dots but
still misses other rest bodies, so its complete timing remains unresolved. One
plain quarter rest also remains missing in the fresh capture. Private source
images are excluded from this repository.

The app invalidates guide and generated-audio caches for the next build. This is
a source update; the installed phone remains on 1.18.110. Whole-library review is
still in progress.

## Mild staff tilt and gradual curvature

The staff tracker previously discarded displacement smaller than 0.8 staff spaces.
A smaller tilt can still push the local pitch search onto an adjacent rule near
the page edge. Mild tracks now require at least six complete five-line samples
spanning 60 percent of the page, consistent spacing, bounded adjacent movement,
and little backtracking. Straight staffs, sparse ledger groups and alternating
noise remain excluded.

Seven original geometry tests reproduce uphill and downhill failures, edge-space
pitch assignment, slight curvature, and misleading sparse or alternating samples.
All 518 standalone Java tests, 13 Python tests, PNG/PDF-to-MIDI smoke checks and
15 targeted Android staff tests pass. No weights or license terms changed.

A complete private page pitch/presence review covers 230 real notes and one false
text component. The candidate corrects 29 source-confirmed pitches. Three other
pitches at curled edges, the false text component and a missed final barline remain
open; this page is not certified fully accurate. Two targeted earlier pages retain
all pitches and note counts. One output is identical; the other recovers a printed
quarter rest and the following three quarter-note onsets. This focused geometry
check does not replace the separate hourly library regression rotation.

Guide/audio cache revisions are advanced for the next Android build. This source
repair has not been installed on the phone, which remains on 1.18.110. Private
scans, library metadata and diagnostic captures are not distributed.

## Complete local staff evidence on white curled paper

A page-wide staff reference can miss a curl near the right edge. On white paper,
the decoder previously used a partial local match that could select the adjacent
staff line. It now checks a complete five-line group in a small inclined window,
requiring substantial raw ink and staff labels on both sides of the note before
replacing the reference. The existing shaded-paper path is preserved.

Twelve original synthetic regressions cover shifted and inclined references,
flat staffs, beams, one-sided support, missing outer rules, short ledger fragments
and inconsistent spacing. All 530 standalone Java tests, 13 Python tests,
PNG/PDF-to-MIDI smoke checks and 27 targeted Android staff tests pass.

The private page reviewed in the preceding batch now matches all 230 real-note
pitches. Exactly three pitches change; note timing and presence are unchanged.
One false text-derived note and a missed final barline remain unresolved, so this
is not a whole-page playback accuracy claim. Five targeted earlier pages have
identical outputs, with all 548 saved pitch expectations passing. Private scores
and captures remain outside the public repository.

No weights, license terms or dependencies changed. Guide/audio revisions advance
for the next Android build; the phone remains on 1.18.110 until a new APK is
installed. Whole-library review continues on non-generated score sources.
