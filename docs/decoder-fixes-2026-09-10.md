# Decoder repairs, September 10, 2026

Touching piano heads could collapse a triad to two pitches or put a hollow dyad
on the wrong staff steps. The decoder now follows the broad oval lobes of regular
stacked chords. Local accidentals use the same sharp-before-flat ordering as key
signatures, require comparable sharp stems, and accept short natural extensions.

Dark dot cores can survive a light tie bridge or staff-line contact. The recovery
rejects tiny flag/stem fragments and excludes the bulbs of recognized rests.
Quarter-rest zigzags now contribute explicit silent beats. The triplet-numeral
window includes the normal offset toward the stems of a beamed group.

Small stemmed prefixes before larger heads are marked as grace notes. Preview
borrows their time from the principal note rather than adding beats or delaying
accompaniment. Full-bar sustained notes retain their barline onset despite large
engraving insets. See the grace timing convention in [integration.md](integration.md).

These are global geometry and rhythm rules, without song identifiers. Model
weights and their training lineage are unchanged. Original synthetic regressions
are in `EngravedSymbolRecoveryTest`; commercial pages and private diagnostics
remain outside this repository. The source now contains repairs newer than the
v0.1.0 packaged release; no existing release or weights were overwritten.

Validation: 195 standalone Java tests, 12 Python tests, and the PNG/PDF model-to-MIDI
smoke test. Private printed-score comparisons covered 13 selected passages; this
is not an exhaustive accuracy benchmark or a phone listening validation.

## Follow-up pitch and rest audit

A missing semantic staff rule could shift the pitch reference onto a ledger line.
The decoder now accepts the matching complete printed staff when the mask is
shifted by one rule, and uses that printed geometry when removing lines from rests.
Hollow triads with a weakened middle mask lobe are recovered only when every
slice contains a printed open oval.

Local accidental fragments can now join across the symbol and clef/key labels.
The original candidates remain available so a mixed-label union cannot replace
an already readable natural. Recognized accidental ink is excluded from rhythm
dots. A narrow slur terminal no longer adds another beam; single narrow flags
remain supported. Slight center uncertainty in stacked heads no longer loses
the neighboring augmentation dot.

Curled triplet numerals retain their value when their two lobes contain open
interiors; closed eights and fives with an upper-left stem remain negative controls.
A complete written accompaniment anchors beat zero beneath an incomplete melody
with a wide grace-note inset.

`StaffAndTupletRecoveryTest` adds ten original synthetic cases. Seven reproduce
failures in the preceding decoder; the three negative controls also pass there.
The updated decoder passes 205 Java tests and 12 Python tests, plus the PNG/PDF
inference-to-MIDI smoke tests. Nine private score pages were replayed, with 33
selected passage checks passing. These checks do not establish an overall note
accuracy rate or replace listening validation. The model and released weights
are unchanged; this source update does not overwrite the v0.1.0 release.

## Triplets and rests in overlapping voices

Small curled threes now retain openings that cross a pixel-row boundary. The
lower curve must turn inward before the baseline, so a two's flat foot cannot
supply that evidence. A private check of 210 digit, size and font combinations
found no newly accepted non-threes or lost previously recognized threes; this
check does not establish complete numeral recognition accuracy.

Explicit triplets use their own duration scale and dots even when the first
note in an attack group belongs to an ordinary held chord. Ordinary and triplet
voices preserve their separate written lengths. A positively recognized leading
rest can now time a moving voice above a sustained note, without delaying or
inserting silence into that held partner. Rest detection supports the raised
placement and requires vertical clearance from the held notehead.

`TripletGlyphBoundaryTest` and `PolyphonicLeadingRestTest` add 13 original
synthetic cases. Eight fail with the preceding published decoder. Validation
passed 285 standalone Java tests, 13 Python tests, the PNG/PDF model-to-MIDI
smoke tests, and 1,396 app tests plus app lint. A private 19-page comparison
preserved all pitches and note counts and passed 250 saved pitch expectations.
Changed timing passages were checked against their printed notation; the
overlapping-rest passage also has exact pitch, onset and duration assertions.

The broader library audit remains incomplete. Tuplets containing rests and
some dotted overlapping voices still need further work. Private scores remain
outside this repository. This is a source update with unchanged model weights;
it does not replace the existing packaged release or constitute phone listening
validation.

## Merged meter glyphs

The semantic mask could merge a stacked meter such as 6/8 into one tall head
region. Chord splitting then turned its rounded portions into three sounding
notes. The decoder now checks that region before splitting: it requires clef
context, the stacked numeral's upright enclosed counters, and no protective
stem extending beyond the stave. Broad, shallow counters in hollow chord heads
remain negative controls. This does not attempt to recognize every meter glyph.

`RoundedMeterGlyphTest` adds six original synthetic cases, including two full
extraction tests that fail in the preceding decoder. Validation passed 291 Java
tests, 13 Python tests, the PNG/PDF inference-to-MIDI smoke tests, and 1,402 app
tests plus lint. A 23-page targeted comparison and an hourly revisit of 22 earlier
pages passed 467 saved pitch/presence expectations. The only removed events were
three source-confirmed meter fragments; all real note pitches, onsets and
durations were unchanged. The 22 revisited page outputs were entirely unchanged.

The broader library audit remains ongoing; these checks do not imply every note
has been manually transcribed or listened to. Model weights and packaged releases
are unchanged, and the private score fixtures are not distributed here.

## Inline meter denominators

A rounded 8 in a meter such as 12/8 could be segmented independently of its
numerator and split into two sounding notes. Besides adding false notes, this
could attach an accidental to a false head and carry that alteration into a real
note. The pre-split filter now recognizes paired upright counters in a lower
denominator, with printed numerator evidence and either a clef or a complete
nearby barline. Actual upper noteheads and stems extending beyond the stave
protect real chords. This also covers signatures printed within a system.

Seven original synthetic cases were added; three fail with the preceding
published decoder. Validation passed 298 Java tests, 13 Python tests, PNG/PDF
inference-to-MIDI smoke tests, and 1,409 app tests plus lint. A private 38-page
comparison passed 305 saved pitch/presence expectations. Source inspection
confirmed 14 removed meter fragments and one corrected A-natural pitch. All
other matched pitches are unchanged; 34 page outputs are unchanged.

Removing false events also changes some neighboring onsets. Those fixtures do
not encode every internal meter change, and rest/voice timing errors remain.
This is a pitch and presence improvement, not full-performance validation.
The broader library review continues; private scans are not included. Model
weights, packaged releases, and the existing rollback checkpoint are unchanged.

## Dotted rests

The raw rest detector recognized quarter, eighth, and sixteenth rests but did
not include their augmentation dots. A dotted quarter therefore contributed
one beat instead of one and a half, which could shift surrounding chord attacks
when the timing resolver tried to fit the incomplete rhythm to a bar.

Recognized rests now inspect compact adjacent ink in the upper staff space for
one or two dots. Components crossing the search boundary, large noteheads,
distant marks, and marks aligned with another note are excluded. Raised voice
rests use their own placement. Eleven original synthetic regressions cover
ordinary, dotted and double-dotted rests, flagged rests, and negative controls;
five fail against the preceding published decoder.

Validation passed 309 Java tests, 13 Python tests, PNG/PDF inference-to-MIDI
smoke tests, and 1,420 app tests plus lint. Across 38 private pages, all note
pitches and counts remain unchanged, with 350 saved pitch/presence checks passing.
Source review confirmed the dots on all 26 changed rests across five pages.
Thirty-five selected note events pass exact source-derived pitch, onset and
duration expectations, including paired chord notes separated by dotted rests.
The other 33 page outputs are unchanged.

This does not establish complete timing or pitch accuracy for those songs or
the full library. Cut-time glyphs, some rest shapes and other previously recorded
issues still need work. Private source images are not included; weights and
packaged releases are unchanged.

## Meter counters at fractional staff boundaries

Some printed 8 denominators still became two sounding notes because the upper
pixel row fell just outside a fractional staff boundary, and the font used
wider counters than the filter accepted. The boundary now accounts for the
inclusive pixel extent. Independently segmented denominators permit wider
counters while still requiring printed numerator evidence and rejecting upper
semantic noteheads. The stricter full-stack counter rule is unchanged.

Four synthetic cases were added, including fractional boundaries, wide bowls,
missing numerators and shallow hollow-note counters. Two fail with the previous
decoder; the wide-bowl case also exercises complete note extraction. Validation
passed 313 Java tests, 13 Python tests, PNG/PDF inference-to-MIDI smoke tests,
and 1,424 app tests plus lint. A 48-page comparison passed 411 saved pitch and
presence expectations. Four source-confirmed meter fragments were removed;
all matched pitches remained unchanged. A separate remaining rest-bar artifact
changes onset after the preceding false events are removed.

The hourly revisit of 22 earlier pages also passed all 177 saved expectations
with identical sounding events. One previously missed dotted-rest duration was
confirmed against its printed source. The full library and performance audit
remains incomplete. Private scans, model weights and packaged releases are
unchanged by this source update.

## Header symbols and opening multimeasure rests

Small fragments of common-time and cut-time signs could become sounding notes.
Header notehead labels also prevented multimeasure-rest recognition or widened
the opening measure enough to select the wrong OCR crop. The decoder now checks
the printed open-right shape and clef context, protects real filled and hollow
heads and grace stems, and removes only confirmed non-note labels from a copied
mask. Rounded meter denominators use the same preparation before rest detection.

The app and standalone API refresh playable header edges after normalization.
They retain the original segmentation for staff and barline geometry, so a
cleared numeral bowl cannot turn its vertical stroke into a new barline.
Original caller masks and grayscale pixels remain unchanged.

Twelve original synthetic regressions cover header shapes, negative cases,
input preservation, repeated normalization, rest expansion and stable geometry.
Both API regressions fail with the preceding published decoder. A 48-page
comparison preserves all 421 saved pitch/presence expectations and 41 selected
pitch/onset/duration expectations; all matched pitches are unchanged. With
source-transcribed rest counts, two opening rests expand to eight and three
bars respectively, and 482 real-note pitches remain unchanged. The unannotated
comparison changes one page by removing a false time-signature note; the other
47 page outputs are identical.

Validation passed 325 standalone Java tests, 13 Python tests, PNG/PDF
inference-to-MIDI smoke checks, and 1,430 app tests plus Android lint. The app
check used the reviewed production core and its corresponding committed tests;
unpublished experiments were preserved separately.

These are selected source and regression checks, not a complete library accuracy
claim or live phone validation. OCR inputs remain the caller's responsibility in
the standalone API. Private scans and diagnostic fixtures are excluded, and
model weights, licensing and packaged releases are unchanged.

## Tempo beat units must not sound

A quarter-note symbol in a printed tempo equation could be interpreted as a
high melody note. Besides adding an unwanted pitch, it displaced the following
rest and distorted duration resolution in the opening bar. Header preparation
and direct note extraction now exclude a beat-unit head only when it sits above
the staff with an upward stem, two complete aligned equals-sign strokes, and
following text. This uses geometric text evidence; it does not OCR or change the
tempo value.

Twelve synthetic regressions cover quarter and hollow beat units, ordinary high
notes, ledger spacing, incomplete equations, clipped text, input preservation,
repeated preparation and the standalone API. Five fail on the preceding
published decoder. A 60-page comparison preserves all 460 saved pitch/presence
expectations and 45 selected pitch/onset/duration checks. Only the confirmed
tempo-note page changes: one false event is removed, all 220 real pitches remain
unchanged, and the first bar's printed rests and durations resolve correctly.
The other 59 complete page outputs and four annotated regression outputs are
unchanged.

Validation passed 337 Java tests, 13 Python tests, PNG/PDF inference-to-MIDI
smoke checks, and 1,441 app tests plus Android lint using the reviewed core.

This remains selected validation, not whole-library accuracy or live phone
verification. Source scans stay private. Model weights, dependencies, licensing
and packaged releases are unchanged.

## Rest dots and printed leading silence

A small notehead prediction could actually be an augmentation dot beside a
quarter rest, adding an unwanted pitch and shortening the rest. The decoder now
checks compact, stemless candidates against the dots of an independently
recognized rest. It removes a false head only when the raw dot geometry and the
same previously detected rest agree, then recomputes the rest's dotted value.
Real small notes with stems and marks without a proven rest remain eligible notes.

An incomplete optical bar now preserves a recognized leading rest when its
duration is consistent with the meter. A complete printed note/rest voice also
provides the onset for aligned cross-staff notes; conflicting complete voices
retain their independent clocks.

Twelve original synthetic regressions cover these cases; five fail on the
preceding decoder. Validation passed 349 Java tests, 13 Python tests, PNG/PDF
inference-to-MIDI smoke checks, and 1,453 app tests plus Android lint. A 60-page
comparison preserves 655 selected pitch/presence checks, 45 existing note timing
checks, and 19 newly source-checked onsets. One confirmed rest-dot false note is
removed, with no other pitch additions, removals or substitutions. The hourly
revisit of 22 earlier pages preserves all 177 saved pitch/presence checks and
their complete outputs, including a targeted follow-up after the cross-staff change.

This does not certify every note or rhythm on those pages. The wider short-staff
candidate and remaining recognition errors in damaged voices are still under
review. No model weights, dependencies, licenses or packaged releases change.

## Triplet numerals predicted as notes

A printed triplet numeral could be segmented as an extra long note. That false
pitch interrupted the three consecutive short notes needed to recognize the
triplet, so it also distorted their timing. The decoder now checks an unbeamed
candidate against a complete raw numeral 3 and a coherent adjacent triple on
the same staff. The candidate must overlap the numeral itself, outside the real
noteheads. Removal happens before rest ownership and rhythmic reconstruction.
The same glyph checks reject closed eights and solid-stem fives; intervening
ordinary notes, unequal beams and grace groups cannot manufacture a triplet.

Eleven original synthetic regressions cover the rule and full score extraction.
The full extraction regression fails on the preceding published decoder and
passes with this repair. Validation passed 360 Java tests, 13 Python tests,
PNG/PDF inference-to-MIDI smoke checks, and 1,464 app tests plus Android lint.
A targeted comparison of 14 pages from eight songs removes one confirmed false
pitch and corrects its neighboring triplet timing. The other 13 complete page
outputs are unchanged. Source inspection verifies 190 pitches across ten newly
reviewed pages, including all 83 pitches in one complete song, and ten note
timings in the repaired passage. This is selected source validation, not a
claim that all pitches and rhythms in the library are correct. One separate
false note on a multimeasure rest remains under investigation.

No song-specific rules, source scans, private logs, model weights, dependencies,
license changes or packaged release updates are included.

## Multimeasure rest bar fragments

Tiny notehead predictions inside a thick H-shaped rest bar could both sound as
notes and prevent the caller's correct rest count from being accepted. A compact
fragment is now removed only when the raw image proves a long, thick horizontal
band with a cap extending above and below the band at each end. The inspection
uses the fragment's rows so a crossing staff rule does not hide the end caps.
The cleaned mask is available before rest-count reconciliation; direct note
extraction applies the same rule. Source pixels and caller masks are preserved.

Twelve original synthetic tests cover extraction, rest-count acceptance, both
caps, narrow cap fragments, crossing staff rules, short beams, thin rules,
ordinary small notes, missing raw evidence and repeated preparation. Seven fail
on the preceding published decoder. Validation passed 372 Java tests, 13 Python
tests, PNG/PDF inference-to-MIDI smoke checks, and 1,476 app tests plus Android
lint. Six targeted pages with multimeasure rests were compared: five complete
outputs are unchanged. The remaining page loses three confirmed false events;
all 74 real pitches match a full visual transcription. Supplying its printed
seven- and five-bar rest counts now yields the correct 45 measures. The retained
notes keep their durations and local timing; their absolute starts shift only
by the restored silent measures.

This resolves the multimeasure-rest false-note case identified in the preceding
batch. Other library pages and remaining partial-measure issues still require
review. Model weights, dependencies, licenses and packaged releases are unchanged;
commercial source pages and private diagnostics are not included here.

## Pitch calibration from an imperfect staff seed

Incomplete semantic staff stripes could estimate the wrong spacing, then reject
the complete raw five-line group because it differed too much from that estimate.
The decoder fell back to incorrect pitch geometry across a sloping system. A
complete, well-supported printed staff can now recalibrate a moderately compressed
or expanded seed while remaining close to the same physical staff. Existing
checks for five complete rules, consistent spacing, broad page coverage and a
coherent trajectory remain in force; a distant staff cannot supply the wider
spacing correction.

Seven original synthetic tests cover compressed and expanded seeds, physical
staff separation, incomplete rules, sparse fragments, implausible scales and
input preservation. Two fail on the preceding published decoder. Validation
passed 379 Java tests, 13 Python tests, PNG/PDF inference-to-MIDI smoke checks,
and 1,483 app tests plus Android lint. In 71 page inputs, 70 complete outputs are
unchanged; only 17 independently verified pitch substitutions change on the
remaining page. Its 189 real pitches now match a full visual transcription.
A separate false event from a quarter-rest fragment remains under investigation.

The regression pass preserves 655 earlier pitch/presence expectations, 45 selected
note timing checks and 19 printed-rest onset checks, plus the recently reviewed
source pitches. The hourly revisit of 22 earlier pages preserves all 177 saved
pitch/presence checks and every complete output. Four OCR/meter-annotated page
outputs are also unchanged. These scoped checks do not certify the whole library.
No model, license, dependency or packaged release changes are included.


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
