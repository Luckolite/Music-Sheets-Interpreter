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
