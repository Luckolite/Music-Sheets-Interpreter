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
