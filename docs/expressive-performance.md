# Expressive previews

The written score and the performed clock remain separate. MusicXML retains the
original pitches, durations, ties and rest spans; MIDI and audio use the performed
clock. Detection keeps the printed word or symbol and its owning source column.
Unproved ownership remains unresolved rather than being turned into an optical
estimate of musical time.

The shared Java `ScoreExpressivePerformance` policy supplies Android, desktop
playback and the standalone export bridge. Its `expressive-preview-v1` defaults
are reproducible preview choices, not a claim that expressive notation specifies
one universal duration:

| Printed evidence | Preview behavior |
| --- | --- |
| `rit.`, `rall.` | Gradual slowing to 80% of the current quarter-note BPM, across four quarter beats unless a proved endpoint is supplied. |
| `poco`, `molto` | Slowing targets of 90% and 60%, respectively. |
| `rite`, `ritenuto` | Immediate tempo reduction at the proved onset. |
| `a tempo`, `tempo primo` | Restore the relevant earlier tempo. |
| Note equals note | Multiply BPM by the right pulse divided by the left pulse, including augmentation dots. Dotted eighth equals quarter therefore multiplies BPM by 4/3. |
| Fermata | Add the held span's active duration. An identified sounding note sustains through its hold; an identified rest stays silent. |
| Breath | Insert a pause lasting one quarter of a quarter-note beat at the active tempo, after the owning release. |
| `sf`, `sfz` | Attack gain 1.6, returning to normal gain across 0.12 seconds. |
| `sfp` | Attack gain 1.6, settling to gain 0.55 across 0.12 seconds. |

Tempo ramps integrate reciprocal BPM. Playback onsets, note releases, navigation
projection and inverse seeking use that same clock. Tremolo subdivisions are
created in musical beats before tempo integration, so slowing or a hold does not
create additional attacks. A tie keeps one sounding identity.

Rest-fermata timing requires an identified rest and an exactly accounted silent
slot between proved written note columns or bar boundaries. Overlapping sound,
ambiguous ownership or unaccounted silence prevents resolution. Continuation
pages are resolved again with inherited meter after page joining. A rest hold
does not invent a note.

MusicXML writes two metronome beat-unit groups for a pulse relation, standard
`sf`/`sfz`/`sfp` dynamics, fermatas on their owning note or rest, and breath marks
on the final written release fragment. Omitted source parts do not move these
marks onto another surviving part.

Guide 277 extends the stable expressive-kind table with `METRIC_MODULATION` at
ID 19. Its payload is the typed qualifier
`metric-pulse-v1:<left-quarter-beats>:<right-quarter-beats>`. Existing IDs and
length-framed records retain their meaning. Explicit guide-version readers and
writers reject this kind under a 263–276 header. App, Hub and worker compatibility
must be updated together before releasing a cache containing the new kind.

Raster templates are derived from the SIL Open Font License Bravura font. Their
manifest records the font checksum and SMuFL code points. Regressions use original
generated staff geometry and redistributable font-derived glyphs; no commercial
score scans or user-library data are included.
