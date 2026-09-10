# Integration and output format

## Inference contract

The bundled model takes float32 `[1,3,320,320]` pixels in 0..255 and returns int64
`[1,320,320]` class IDs. The three channels contain the same grayscale plane.
Normalization is part of the graph. Preserve the model card's analysis scale,
tile stride and overlap ownership when implementing another runtime.

The Java API accepts flat `byte[]` label and grayscale arrays, with unsigned pixel
values, and explicit dimensions. Invalid shapes, unknown class IDs and pages over
20 million analysis pixels are rejected. The decoder has no Android dependencies.
`ScorePageInterpretation` and the `Score*` records retain musical evidence without
forcing a MIDI representation.

## JSON

The CLI writes a document with `schemaVersion: 1`, `inputName`, `initialBpm` and
`pages`. Each page includes:

| Field | Meaning |
|---|---|
| `width`, `height` | Dimensions after analysis resizing |
| `sourcePage` | One-based page in the input PDF |
| `modelSha256` | Exact model artifact used |
| `score.measures` | Normalized 0..1 measure rectangles |
| `score.notes` | Raw `ScoreNoteEvent` fields: staff steps, beams, dots, accidentals, ties and other notation |
| `score.keyChanges` | Zero-based measure index and number of sharps/negative flats |
| `score.tempoChanges` | Quarter-note tempos supported by supplied OCR and raw printed geometry |
| `score.meterChanges` | Caller-supplied validated meter changes |
| `score.rests`, `techniqueChanges`, `dynamicChanges` | Available rest/direction evidence |
| `events` | MIDI pitch, start/duration in quarter-note beats, staff identity, analysis-pixel x/y, tie and fallback flags |
| `measureBeats`, `totalBeats` | Measure durations and total, in quarter-note beats |
| `warnings` | Recognition and metadata limitations |

Measure indices and event times are local to each page. The MIDI writer concatenates
selected pages in reading order. A fallback key and meter carry between CLI pages;
detected key signatures and supplied meter changes update that state.

`clefInferred` means a conventional treble/bass fallback was used because the printed
clef was not established. `durationFallback` means the duration was unresolved and
the preview used half a quarter-note beat. These are not calibrated confidence scores.
Absent written accidentals are resolved using the detected or fallback key.

Recognized small grace heads carry `NoteOrnament.GRACE` (`32768`) in the raw
note's `articulations` mask. Preview timing omits them from the metrical clock,
then plays the grace group on the principal beat, borrowing at most 0.25 quarter
beats or one quarter of the principal's duration, whichever is smaller. The
principal chord moves together; accompaniment and later beats keep their timing.
This is a preview convention, not a claim that every ornament style is recognized.

## Optional OCR annotations

The CLI does not bundle an OCR engine. `--annotations file.json` accepts a list with
one object per selected page. Java callers pass equivalent `SheetInterpreter.Annotations`.
All OCR boxes are normalized 0..1 coordinates on the source page, not crop-relative
coordinates. A page object can contain these optional lists:

```json
{
  "measureNumbers": [],
  "tempoNumbers": [],
  "restCounts": [],
  "words": [],
  "meters": [{"measureIndex": 0, "numerator": 4, "denominator": 4}]
}
```

Numbers have `value`, `left`, `top`, `right`, `bottom`, and optional `annotationLeft`.
For a tempo, `annotationLeft` identifies the beginning of its printed direction line
while the other coordinates bound the digits. A number alone does not establish
tempo: the detector also requires supporting printed symbol/equals-sign geometry.
Put verified measure-number tokens and multi-rest counts in their respective lists.

Words have `text`, `left`, `top`, `right`, `bottom`. They can carry recognized dynamics
and techniques such as `p`, `mf`, `pizz.` or `arco`; the decoder places supported tokens
using the staff and note geometry. This interface does not reproduce the Android app's
whole-page and repeated crop OCR strategy.

## MIDI preview limits

MIDI uses 480 ticks per quarter note, a fixed velocity and standard program 0. Recognized
ties join compatible same-pitch events when timing agrees. Simultaneous same-pitch voices
use separate non-percussion channels. The preview is not an expressive instrument engine.

Measure slots retain their full meter duration, including the first measure; pickup,
cadenza and unusual engraving timing can need correction. Cross-page ties, repeats,
ornament realization and exact polyphonic voice separation are not fully handled.
Tempo/meter arguments are explicit fallbacks, not claims of automatic recognition.
Use the retained geometry and raw score events to implement editing and richer playback.
