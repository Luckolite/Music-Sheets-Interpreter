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
