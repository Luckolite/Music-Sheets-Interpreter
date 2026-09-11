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

## Additional ledger evidence for remote heads

Instruction text and nearby underlines could pass the first two ledger checks,
producing a false low note. A head more than three and a half staff gaps outside
the staff now needs another inward ledger. Nearer notes keep their current
allowance, including shorter rules for reduced grace heads. The condition uses
geometry and applies to every score.

Twelve original synthetic tests cover high and low lines/spaces, stemless whole
notes, small heads, insufficient horizontal strokes and adjacent ordinary notes.
Four assertions fail on the previous decoder. All 542 Java tests, 13 Python
tests, PNG/PDF-to-MIDI smoke checks and 27 targeted Android tests pass.

On a privately reviewed page, the one false instruction event is removed and all
230 real-note pitches remain correct. Eight targeted earlier pages retain
identical complete decoder outputs. A missed barline and separate timing issues
remain open; this does not certify whole-page or whole-library playback.

The app port and standalone source were compared directly. Pre-existing mixed
experiments remain outside this commit. No weights, dependencies or license terms
change, and no private scores or phone logs are included. Cache revisions advance
for the next app build; this source-only change has not been installed on a phone.

## Slur islands, masked ties and rounded ledger graces

A small semantic notehead can actually be the rounded part of a longer slur.
The decoder now inspects the full raw component after removing thin continuous
staff rules, requiring a wider thin returning curve. Real stems, intact ovals
and straight ledger extensions are protected. Rejected slur labels are cleared
only in a private tie-analysis copy, preserving the caller's mask and retained
heads so the false islands no longer hide printed ties.

Rounded grace heads could qualify for ornament grouping but fail the earlier
ledger check. The shorter-ledger allowance now also covers a short beamed prefix
followed by a substantially larger principal. Adjacent grace stems must share
actual beam ink; a staff rule or unconnected short stems does not qualify.

Twenty-one original synthetic regressions cover the new cases and safeguards;
seven fail on the prior decoder. All 563 Java tests, 13 Python tests,
PNG/PDF-to-MIDI smoke checks and 47 focused Android tests pass. All 1,270 saved
private pitch/presence checks pass. Eleven of 12 targeted pages have identical
complete outputs. In the changed page, two false slur notes disappear, a missing
grace is recovered, and a printed tie between repeated F notes is restored.

The two-page private score now matches all 489 printed pitches. A missed barline
and separate rhythm/tie issues remain open, so this is not a full playback
certification. No private scans, transcriptions or logs are distributed. App and
standalone code were compared directly; pre-existing mixed experiments were
excluded. Weights, dependencies and Apache-2.0 terms are unchanged. This source
batch has not been installed on a phone; cache revisions advance for the next
Android build.

## Printed staff centers and nearby barlines

Slight errors in semantic staff centers could leave a real staff rule in the
note-to-bar connection check. That horizontal rule connected an unrelated nearby
head to a valid barline, causing two printed bars to merge. The check now uses
five thin, bilaterally supported raw rules when they agree with the existing
staff geometry. Incomplete, thick or one-sided evidence retains the original
behavior. Actual attached heads continue to veto note stems as barlines.

Ten original generated-geometry regressions cover both displacement directions,
two resolutions, already accurate staff centers, true attached heads, incomplete
ledger evidence and one-sided ink. Four fail on the previous decoder. All 573
Java tests, 13 Python tests, PNG/PDF/MIDI smoke checks and 52 focused Android tests
pass. All 1,270 saved private pitch/presence assertions pass.

In the private two-page source, page 1 now has all 27 printed bars instead of 26;
its 220 pitches are unchanged. Nineteen source-transcribed timing assertions in
the two formerly merged bars now pass, including the final triplet. Page 2's
event output is identical; one boundary moves by a single pixel. Ten other
targeted pages have completely identical output. Other known rhythm/tie issues
remain open; this is not exhaustive listening certification.

Only reviewed shared code and original synthetic tests are published. Private
scores and logs, mixed experiments, weights, dependencies and license terms are
unchanged. Guide104/audio20 invalidate derived caches in the next Android build.
No APK, phone, Sync Hub or EXE update is included in this source batch.

## Partial staccato masks

A round printed staccato dot can have a flattened semantic notehead island when
the model labels only its upper or lower half. The old aspect-ratio check let
that island become a second played pitch. A bounded raw-component check now
allows an incomplete island to qualify as a dot only when the entire printed
component is small, round, filled and isolated. Actual stems, non-round ovals,
connected ink and missing raw evidence retain the previous safeguards.

Six original generated regressions cover those cases; two fail on the preceding
decoder. All 579 Java tests, 13 Python tests, PNG/PDF/MIDI smoke checks and 20
focused Android tests pass. Thirteen of 14 targeted pages have completely
identical outputs. The sole change is removal of one false E5 staccato event on
Arcade page 3; all 146 real notes on that page retain the same pitches and timing.

All 1,270 previously saved pitch/presence assertions pass. Including the newly
reviewed Arcade pages gives 1,581 assertions: 1,579 pass and two known page-2
defects remain open (one missing B5 grace and one false short grace slur).
Those failures are retained as audit work rather than declared fixed. The last
hourly rotation remains separate: 22 earlier pages, 188 checks, no changes.

Only reviewed shared code and original synthetic tests are published; private
scans, transcriptions and logs remain private. Mixed experiments and rollback
snapshots are preserved. Weights, dependencies and Apache-2.0 terms are unchanged.
Guide105/audio21 refresh derived caches in the next Android build. This source
batch does not update the phone, APK, Sync Hub or Desktop executable.

## Faded inner ledger support

The recent extra-ledger safeguard rejected a real high G when part of an inner
ledger faded below the strong-ink threshold. A continuous weak-ink run may now
complete that ledger only when strong pixels still supply at least 60 percent
of the required run. Entirely pale strokes and disconnected fragments remain
insufficient; the required number and geometry of ledgers do not change.

Six original synthetic cases cover upper/lower faded ledgers, a white break,
entirely or mostly pale unsupported strokes and ordinary solid rules. Two fail
on the preceding decoder. All 585 Java tests, 13 Python tests, PNG/PDF/MIDI smoke
checks and 28 focused Android tests pass. Fourteen of 15 targeted pages have
completely identical outputs. The sole changed page recovers its high G and
corrects four following note timings in that measure; no existing pitch changes.

Private source review confirms all 249 real pitches on Dearly Beloved now match.
The dropped G was present through the 530-test build and lost in the 542-test
ledger update; this repairs that regression without removing its safeguards.
Five source-transcribed onset/duration checks in the repaired measure pass. A
missed tie and a false short grace-slur note remain separate open issues.

Across 1,831 saved private pitch/presence assertions, 1,828 pass; the three known
remaining failures are one missing grace and two short grace slurs on the newly
reviewed pages. No private scores, transcriptions or logs are published. Original
synthetic fixtures and reviewed code are mirrored to the public interpreter;
mixed experiments, rollback snapshots, weights and license terms are preserved.
Guide106/audio22 invalidate caches in the next Android build. No phone, APK,
Sync Hub or Desktop executable update is included in this source batch.

## Tie endpoint clearance

An engraved tie can leave a small blank clearance beside either notehead. The
existing continuous-curve check expected ink across almost the entire space,
so some clearly printed ties became separate attacks. If the original check
fails, a bounded inset search now allows those clearances while retaining the
same-pitch, minimum-span, complete returning-curve and staff/beam safeguards.

Nine original synthetic tests cover upper, lower and asymmetric clearances,
already attached ties, straight/sloped/stepped strokes, a half curve and an arc
too far from the heads. Three fail with the preceding algorithm. All 594 Java
tests, 13 Python tests, PNG/PDF/MIDI smoke checks and 23 focused Android tests pass.

Every changed real-score result was inspected against private printed source:
16 recovered ties across seven of 15 targeted pages are genuine ties. These
include A Beautiful Distraction, Run, I Saw Three Ships, Boulevard of Broken
Dreams, Arcade and Dearly Beloved. The other eight complete page outputs are
identical. Only tie-continuation flags change; pitches, attacks' nominal onsets
and written durations do not change. The held-note behavior is not phone-tested.

All 1,828 currently passing pitch/presence assertions remain green. Three known
open errors among 1,831 assertions remain: one missing grace and two false short
grace slurs. Sixteen new private tie expectations are saved for later checks.
Hourly rotation remains separate, without repeating it after every patch.

Only reviewed shared code and original synthetic tests are published. Private
scans, crops and transcriptions stay private; mixed experiments and rollback
snapshots remain intact. Weights, dependencies and license terms are unchanged.
Guide107/audio23 refresh derived caches in the next Android build. No APK,
phone, Sync Hub or Desktop executable update is included in this source batch.

## Deep grace slur fragments

A partial semantic head mask can cover one side of a compact, deep grace slur.
The raw curve was previously rejected by the shallow tie-like aspect and height
limits, leaving a spurious note in the score. The complete raw-component check
now admits deeper returning curves only with a stronger bend and closer endpoint
heights. It retains overlap, minimum span, crop-edge, staff-rule and attached-stem
safeguards. Production behavior uses geometry only, with no song-specific rules.

Ten original synthetic tests cover lower, upper and mirrored curves, absent raw
ink, unequal endpoints, attached stems, filled and hollow ovals, ledger lines and
incomplete curves. Three fail with the previous algorithm. All 604 Java tests,
13 Python tests, PNG/PDF/MIDI smoke checks and 45 focused Android tests pass.

On 15 targeted non-YouTube pages, 14 full outputs are unchanged. Dearly Beloved
loses only the source-confirmed false C4 under a grace slur; all 249 real printed
pitches remain present and correct. One neighboring grace F4 duration changes
from 0.1875 to 0.375 when the false chord member disappears. The two small heads
are genuinely a grace group in the source, but their duration/grouping is not
certified by this fix and remains a separate rhythm investigation.

1,829 of 1,831 source-reviewed pitch/presence assertions pass; the two remaining
known failures are the missing high grace and staff-obscured short slur in Arcade.
All 16 source-verified tie expectations from the preceding repair still pass.
These counts cover the selected review set, not the complete library.

Only reviewed shared source and original synthetic tests are published. Private
score images, diagnostic masks and transcriptions remain private. Model weights,
dependencies and Apache-2.0 terms are unchanged. Mixed experiments and rollback
snapshots are preserved. Guide108/audio24 refresh derived caches in the next app
build; this source batch does not update the phone, APK, Hub or Desktop executable.

## Key-signature flat bowls mistaken for notes

A flat can be segmented into an accidental-labelled spine and a note-labelled
part of its bowl. The false head then produces an extra pitch and steals time
from the first bar. A new shared check requires a nearby printed bass or treble
clef, the surviving semantic accidental spine, and a complete raw flat profile.
It excludes natural and sharp profiles and verifies the bowl's vertical position.
Both direct interpretation and header normalization reject the proven false head
without changing the caller's labels or grayscale pixels.

Ten original synthetic tests cover the split flat, normalization, missing clef
or spine evidence, distant ink, real up/down stems, hollow heads, idempotence and
source preservation. Two fail before the repair. All 614 Java tests, 13 Python
tests, PNG/PDF/MIDI smoke checks and 40 focused Android tests pass.

On 17 targeted non-YouTube pages, 16 full outputs are identical. Caro Mio Ben
loses only the source-confirmed false B-flat2 inside a key signature. All 135
real pitches remain correct. Removing that false event corrects six onsets in
the opening bar of its seventh system; the printed half note again starts on
the barline, followed by its grace group, quarter, dotted eighth and sixteenth.
Six source-reviewed timing assertions pass. The page's printed bass clefs are
respected regardless of its instrument label. No title exception is used.

The expanded source-review set has 2,048 passing pitch/presence checks out of
2,050, retaining the two known Arcade grace-related errors for further work.
All 16 saved tie checks pass. Hoist the Colours was newly reviewed in full too:
all 83 printed pitches are present and correct. These are selected source checks,
not a claim that the complete library is correct.

Only reviewed shared code and original synthetic tests are public. Source scans,
label maps and private transcriptions remain private. Weights, dependencies and
Apache-2.0 terms are unchanged. Mixed experiments and original rollback snapshots
remain intact. Guide109/audio25 refresh derived caches in the next app build;
the phone remains on the 1.18.111 release installed before this source repair.

## Sharp alignment at adjacent staff positions

A sharp close to a note could affect the neighboring staff position because its
vertical acceptance range exceeded half a staff gap. The crossbar centre now has
to be within 0.45 gaps of the head. The raw flat fallback also excludes complete
semantic sharps, so rejecting a misaligned sharp cannot reinterpret its cropped
lower strokes as a flat. Natural and flat alignment rules are unchanged.

Eight original synthetic tests cover matching and adjacent pitches, small raster
offsets, semantic-only input and source preservation. Three fail on the previous
decoder. All 622 Java tests, 13 Python tests, PNG/PDF/MIDI smoke checks and 69
focused Android tests pass.

On 22 targeted eligible pages, 21 complete outputs are unchanged. The affected
page changes exactly four pitches from G-sharp5 to the source-verified G5, with
no event-count, onset, duration or tie changes. A fresh phone-rendered capture
reproduces the same defect and all 122 manually reviewed pitches from the reported
bar to the end now pass with the candidate. The expanded private source set has
2,849 passing pitch/presence assertions out of 2,863; the 14 known failures are
separate grace, curved-symbol and parenthesized-note issues still under review.
This does not certify the whole library or every rhythm/rest.

The repair is global. No title-specific code, private score pixels, transcriptions,
phone logs, training data or model changes enter the public repository. Apache-2.0
terms and model lineage remain unchanged. Guide110/audio26 invalidate derived
caches in the next app build. The phone remains on 1.18.111; this source repair is
not yet installed. Original rollback snapshots and mixed experiments are preserved.

## Preserve displaced ledger chords

Two adjacent chord tones can form one wide semantic component with their shared
stem between the ovals. Ledger validation treated that component as a single
stemless note and required a ledger to extend beyond its combined width. This
discarded complete runs of real notes. Recognized displaced seconds now validate
the ledger evidence around each constituent head; both tones must pass the same
ledger and inner-ledger checks. Single-head behavior is unchanged.

Six original synthetic tests cover both recovered tones, their pitches and shared
attack, missing-ledger rejection, an ordinary staff note and input preservation.
Three fail on the previous decoder. All 628 Java tests, 13 Python tests,
PNG/PDF/MIDI smoke checks and 62 focused Android tests pass.

Across 23 targeted pages plus three later pages of the reported sheet, 25 full
outputs are unchanged. The affected page gains exactly 48 source-confirmed chord
tones without losing any notes: 32 in one bar and 16 in another. A fresh phone
capture reproduces both omissions and both recoveries. All 165 source-reviewed
pitches in the reported run and affected chord passages pass on desktop and
phone captures. The 64 note-timing assertions for the first restored chord bar
also pass. The second bar still has a separate false-triplet timing issue: 50
of its 64 timing assertions remain open. Recovery is not a complete rhythm audit.

The expanded source set has 3,014 passing pitch/presence assertions out of 3,028;
the 14 known failures concern separate grace, curved-symbol and parenthesized-note
issues. Only the global repair and original synthetic tests are published. Private
score pixels, phone captures and transcriptions remain private. No weights or
licensing changed. Guide111/audio27 refresh derived caches in the next app build;
the phone still runs 1.18.111 without this repair. Mixed experiments and original
rollback snapshots are preserved.

## Preserve chord rhythms around stacked fingerings

Vertically stacked fingering numbers above repeated chords included a printed 3.
The tuplet detector treated this as a rhythmic marking and shortened three chord
attacks, shifting the remainder of the bar. It now checks for a separate, similarly
sized upright glyph immediately above or below the 3 when all three candidate
attacks contain multiple chord tones. That stacked arrangement preserves the
written rhythm. Ordinary isolated triplet numerals retain their prior behavior.

Eight original synthetic tests cover upper and lower stacked fingers, genuine
chord triplets, horizontal and distant neighbors, single-voice behavior, ordinary
chords and input preservation. Two fail on the previous decoder. All 636 Java
tests, 13 Python tests, PNG/PDF/MIDI smoke checks and 94 focused Android tests pass.

Across 26 targeted pages, 25 full outputs are identical. The affected page changes
only onset or duration for 84 notes in two bars; all pitches, note membership,
ties and geometry remain unchanged. Raw source review confirms ordinary
thirty-second-note chords and stacked fingerings in both bars. On desktop and
fresh phone captures, all 229 reviewed pitches and 192 timing assertions now pass,
including the previously restored displaced chord runs. The expanded source set
has 3,078 passing pitch/presence checks out of 3,092, with 14 previously documented
unrelated failures. Later-page output stability does not prove those pages fully
correct; the wider library audit remains ongoing.

Only the global geometry repair and original synthetic tests are published.
Private score pixels, phone captures and transcriptions remain private. Model
weights and licensing are unchanged. Guide112/audio28 refresh derived caches in
the next app build. The installed phone app remains 1.18.111 without these fixes;
this is source and captured-input validation, not an installed listening test.
Original rollback snapshots and separate mixed experiments are preserved.

## Recover tightly spaced repeated noteheads

A thin semantic bridge along a staff line could join an entire tightly engraved
run into a single long notehead component. The oversized component then failed
notehead validation, dropping every attack it contained. The decoder now separates
three or more repeated heads only when raw ink shows distinct oval lobes and
every resulting head has an attached printed stem. Each part must also satisfy
head size, area, vertical alignment and filled-center checks. Solid blobs,
stemless rows and ordinary single heads retain their previous treatment.

Seven original synthetic tests cover recovered attacks, pitch, beam counts,
solid-blob and stemless rejection, ordinary heads and input preservation. Three
fail on the previous decoder. All 643 Java tests, 13 Python tests, PNG/PDF/MIDI
smoke checks and 62 focused Android tests pass.

Across 26 targeted score pages, 25 full outputs are identical. The affected bar
gains exactly 14 source-confirmed G4 notes, with no removed events or changed
pitches. Seven existing onsets or durations change as the recovered attacks
replace the previously stretched timing. All 214 newly reviewed pitches across
three preceding chord bars and the affected run match the printed source.
The expanded set has 3,292 passing pitch/presence assertions out of 3,306; the
14 previously documented unrelated failures remain. One separate partial-beam
error remains in the recovered bar: its second note is a thirty-second but is
still decoded as a sixteenth, shifting subsequent onsets by one eighth beat.
This recovery does not claim complete rhythm accuracy.

The later page was checked from saved source data. A new phone capture could not
run because wireless debugging disconnected; no phone update is claimed. Only
the general repair and original synthetic tests are public. Private score pixels,
transcriptions and device data remain private. No model or license changes.
Guide113/audio29 invalidate derived caches in the next app build. Existing mixed
experiments and rollback snapshots remain preserved.

## Keep rejected head fragments out of key-signature boundaries

The key reader used every plausible head-shaped component to locate the first
note, including fragments rejected later as non-notes. A rejected fragment in a
header could cut the reading window short before the last flat, creating a false
key change and altering an entire following passage. The boundary now uses only
heads belonging to emitted notes. Genuine first notes still end the signature.

Five original synthetic tests cover rejected high and low fragments, intact key
signatures, a real first-note boundary and input preservation. Two fail on the
previous decoder. All 648 Java tests, 13 Python tests, PNG/PDF/MIDI smoke checks
and 55 focused Android tests pass.

Across 26 targeted pages, 24 full outputs are identical. The reported page changes
exactly 15 pitches from D-natural5 to the printed D-flat5, removing the false
four-flat-to-three-flat key change. All 344 pitches on that page now match the
manual source review; event count, rhythm, positions and ties are unchanged by
this repair. One other page now recognizes its printed single-flat signature
from its opening bar instead of the seventh bar; its supplied key already matched,
so every playback event remains identical. Both changes were checked against the
raw printed score. The expanded source set has 3,422 passing pitch/presence checks
out of 3,436, with 14 previously documented unrelated failures. The scheduled
hourly revisit checked 22 additional earlier pages: all complete outputs are
identical to the prior hourly decoder and all 188 saved assertions pass.

The separate connected-bowing-mark beam endpoint issue remains open: one note in
a later dense run is still twice its printed duration. Pitch correctness does not
imply complete rhythm correctness. Phone wireless debugging remains disconnected;
this repair has not been installed or listening-tested there. Guide114/audio30
invalidate derived caches in the next app build. Only general code and original
synthetic tests are public; private scores and captures remain private. No weights,
license changes or song-specific rules. Rollback snapshots and mixed experiments
remain preserved.

## Android collection compatibility

The emitted-head key-boundary fix now uses the Java 8 collection operation instead
of Stream.toList, which Android lint correctly rejected below API 34. The app
supports API 26 onward. The public port uses the same equivalent operation; all 648
Java tests, 13 Python tests and smoke checks pass. Complete outputs for both
affected pages and the opening single-flat signature example are identical.
No model, licensing or recognition behavior changed.

## Trace beams below detached bowing marks

A stem trace tolerates tiny paper gaps so raster seams do not break real stems.
That tolerance could connect a note to a detached down-bow mark immediately above
its beam. Using the mark's cap as the stem endpoint then placed the beam search
too high, missing a secondary beam and lengthening the note. The beam reader now
recognizes the detached square's complete cap, two legs and open interior, plus
a real paper gap and a thick beam attached below it. It uses that beam endpoint
for rhythm analysis. Other uses of the raw stem trace remain unchanged.

Seven original synthetic tests cover one, two and three beams, an ordinary beam
run, a farther bow mark, unchanged pitches and positions, and input preservation.
Two fail on the previous decoder. All 655 Java tests, 13 Python tests, PNG/PDF/MIDI
smoke checks, 59 focused Android tests and Android debug lint pass.

Across 26 targeted pages, 25 complete outputs are identical. The affected page
changes only one duration from a sixteenth to the printed thirty-second and the
following 20 onsets. All 22 timing assertions for the affected bar now pass.
Note membership, pitches, ties and positions are unchanged. All 3,706 previously
passing source pitch/presence checks remain passing out of 3,720; the 14 separate
known failures are unchanged. The hourly review was not repeated early.

The repair is global and uses no song identifiers. Only original synthetic ink
and general code are public; private source scores and captures remain private.
No model or license changes. Guide115/audio31 invalidate derived caches in the
next app build. The published APK remains 1.18.112 without this additional repair;
the phone is disconnected and no new device validation is claimed. Existing
rollback snapshots and separate mixed experiments are preserved.

## Keep unpitched cross heads out of the pitched melody

A semantic notehead mask can retain only a corner of an X-shaped count-in head.
Treating that corner as an oval creates an audible pitch. The decoder now checks
the raw ink for two opposing diagonals that converge toward their intersection.
The check excludes complete crosses and partial corner detections from pitched
events. It measures actual long staff ink rather than relying on a rounded staff
seed. Hollow oval sides have the opposite convergence and remain pitched notes.
This does not add percussion playback or change the public event schema.

Nine original synthetic tests cover partial and complete crosses, crosses on
staff lines and spaces, silent cross-only bars, filled and hollow ovals, a small
grace head, unaffected pitch/position, and unchanged input arrays. Five tests
fail on the previous decoder. All 664 Java tests, 13 Python tests, PNG/PDF/MIDI
smoke checks, 68 focused Android tests and Android debug lint pass.

Across 28 targeted pages, 27 complete outputs are identical. The affected page
loses exactly two false pitched events. Every remaining event, measure region
and total page duration is unchanged. Both newly reviewed pages retain all 622
manually checked pitched notes, including chords, grace notes and high ledger
notes. All 4,330 previously passing or newly corrected pitch/presence checks pass
out of 4,344; 14 separate known failures remain unchanged. This is a selected
source audit, not a claim that all library pitches or timing are correct. The
hourly revisit was not repeated early.

The repair is global and uses no song identifiers. Only original synthetic ink
and general code are public; private scores and source transcriptions remain
private. Model weights and license are unchanged. Guide116/audio32 invalidate
derived caches in the next app build. The phone is disconnected; no new device
validation is claimed. Existing rollback snapshots and mixed experiments remain
preserved.

## Keep short tie bowls out of the pitched melody

A short tie crossing a staff rule can leave a compact, rounded semantic island.
The existing slur filter required a longer complete raw curve, so that island
could become a false note, interrupt the actual tie and delay following notes.
The decoder now requires two larger stemmed endpoints at the same printed
height and a continuous raw arc whose bend passes through the candidate island.
Only then is the island excluded from notes and retained as arc ink for tie
analysis. Inclusive pixel width is used for the shortest accepted tie span.

Nine original synthetic tests cover lower and upper ties, corrupted and clean
tie masks, independent filled and hollow notes between the same endpoints, an
attached small note, a separate arc away from a sustained note, and input-array
preservation. Three fail on the previous decoder. All 673 Java tests, 13 Python
tests, PNG/PDF/MIDI smoke checks, 93 focused Android tests and debug lint pass.

Across 30 targeted pages, 29 complete outputs are identical. The affected page
loses four false pitched events; all real note pitches and positions remain
unchanged. The affected four bars pass all 24 source timing/duration/tie checks.
There are 13 updated real events as ties, one recovered quarter-note duration
and subsequent onsets are corrected.
All 4,719 passing or newly repaired source pitch/presence checks pass out of
4,734; 15 separate known failures remain. This is selected validation, not an
exhaustive library claim. The next hourly revisit was not run early.

No song-specific production logic, model or license changes. Only original
synthetic fixtures and general source are public. Commercial scores, private
captures and source transcriptions remain private. Guide117/audio33 invalidate
derived caches in the next app build. The published APK remains 1.18.113 without
this additional repair. No new phone install or live validation is claimed;
rollback snapshots and separate mixed experiments remain preserved.
