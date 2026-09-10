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
