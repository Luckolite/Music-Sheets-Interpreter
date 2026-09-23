# Music Sheets Interpreter

Offline sheet-music recognition for images and PDFs, with downloadable model weights,
a Java decoder and a Python interface. Exports JSON, MIDI and MusicXML.

**Code and weights are Apache-2.0 licensed, including commercial and closed-source use.**
You do not need to publish your own code. Follow the [license and notice requirements](docs/licensing.md).

This is experimental software. Always compare recognized notes, octaves, measure boundaries
and timing with the original score before relying on playback or exported notation.

## Install and use

Requires **Python 3.10 or 3.11** and **JDK 17+**. Build from source for the latest features;
[release downloads](https://github.com/Luckolite/Music-Sheets-Interpreter/releases) may be older.

```sh
git clone https://github.com/Luckolite/Music-Sheets-Interpreter.git
cd Music-Sheets-Interpreter
python -m venv .venv
```

Activate with `.venv\Scripts\Activate.ps1` on Windows or `source .venv/bin/activate`
on macOS/Linux, then run:

```sh
python scripts/build_java.py
python -m pip install ".[inference,pdf]"
sheet-interpreter score.pdf --output score.json --midi preview.mid --musicxml score.musicxml
```

Images work too. `--midi` and `--musicxml` are optional. Use `--meter 3/4`, `--bpm 90`,
`--key-fifths 2` or `--pages 1,2,3` when needed. Default meter and tempo are 4/4 and 120 BPM.

Detected tempo changes report `bpm` in quarter notes per minute, including fractional values,
and `beatUnit` as the printed pulse length in quarter notes. For example, eighth note = 163
reports `bpm: 81.5, beatUnit: 0.5`. MIDI and MusicXML use the quarter-note tempo directly.
Everything runs locally after installation; no account or server is required.

## What it supports

- Standard notation: pitches, accidentals, chords, rests, ties and written timing.
- Multi-staff piano, violin and ensemble pages, including independently barred staves.
- Printed meter and numeric tempo changes when the symbols can be read confidently.
- Six- and seven-string guitar tabs, using embedded PDF text or built-in offline OCR.
- Printed tuning headers, including alternate tunings carried across PDF pages.
- Detached tab stems, partial beams, rests, dots, triplets, grace frets and visible tied continuations.
- Explicit hammer-on, pull-off, tapping, slide, bend, vibrato and harmonic symbols.
- JSON for integration, MIDI for preview, and MusicXML for editing in notation software.

MusicXML reconstructs a concert-pitch score, not the original layout or tab placement.
Guitar effects are text annotations in MusicXML. Recognition can miss symbols, and
missing tab rhythm remains estimated. Graphical bends, quarter-tone bends, whammy-bar
directions and strum direction are not reconstructed. Scanned tabs are read with the
installed local OCR engine; small or faint fret numbers can still be missed.

The decoder uses conservative visual checks to keep arpeggio marks from becoming
barlines, ordinary hollow chord heads from becoming artificial harmonics, and tiny
notation fragments from becoming implausible whole-note-denominator meters. It also
preserves barlines drawn separately through multiple staves and beams briefly crossed
by articulation marks. These safeguards reduce known false readings, but they are not
a guarantee that every note or rhythm in a score is correct.

Note-equals-number tempo marks are supported. Note-equals-note metric modulations are
not yet interpreted, so passages using them can play at the wrong relative tempo.

## Integration and model

Use `Interpreter` and `write_musicxml` from Python, or `SheetInterpreter.analyze()`
from Java. See [API examples and output format](docs/integration.md).
An optional [native Java decoding service](docs/native-decoder.md) supports
bounded, source-matched geometry and analysis requests from local workers.
The Python reader runs bundled-model OCR automatically on images and scanned PDFs.
The separate [shared Java OCR pipeline](docs/portable-ocr.md) and optional ONNX
binding remain available for cross-platform evaluation.

The bundled v4 model comes from our own synthetic training lineage, without pretrained
HOMR/oemer weights or commercial score scans. Tab and export improvements do not change
the weights. See the [model card](models/MODEL_CARD.md), [evaluation](models/evaluation.json),
[training guide](training/README.md) and [source provenance](docs/source-provenance.json).

## Development

```sh
python scripts/build_java.py
python scripts/test_java.py
python -m unittest discover -s tests
python scripts/smoke_test.py
```

Bug reports and contributions are welcome. Use shareable examples and keep private logs
and commercial scores out of public posts. See [CONTRIBUTING.md](CONTRIBUTING.md).
