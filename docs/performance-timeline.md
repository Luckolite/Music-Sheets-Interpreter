# Performance timeline

`ScorePerformanceTimeline` maps resolved quarter-note beats to seconds. It is
shared by numeric-tempo playback and navigation's dynamic projection. It does
not interpret engraving coordinates or recognize expressive marks.

Tempo segments start at beat zero, are contiguous, and contain a linear change
in quarter-note BPM over musical beats. Time integrates the reciprocal BPM;
the final endpoint tempo continues after the last segment. Holds are explicit
performance occurrences at resolved release boundaries, with extra seconds and
an immutable set of sustained target identifiers.

Use `secondsAtBeat(beat, BEFORE)` to exclude a pause at that exact beat and
`AFTER` to include it. Inverse `positionAtSeconds` returns a frozen beat, hold
identifier and progress during a pause. At its exact end it returns the held
beat without an active hold. `activeSecondsAtSeconds` removes inserted pause
time so compatible dynamic envelopes can freeze rather than creep during holds.

Repeated depictions with one occurrence ID merge sustain targets, not durations.
Conflicting durations or distinct unreconciled occurrences at one beat reject
explicitly. Distinct source events and repeat visits need distinct occurrence
IDs. Resolve notation ownership before constructing the timeline; do not dedup
all pauses in a measure or attach rest fermatas to sounding notes.

The numeric factory preserves existing tempo-record coordinates. It deliberately
does not silently convert legacy geometric positions to rhythmic anchors. Ramp
and hold APIs are building blocks, not a claim that recognition, persisted
expressive fields, audio release ownership or MIDI expressive export are already
wired end to end. Source note/rest durations must remain unchanged when those
consumers are connected.
