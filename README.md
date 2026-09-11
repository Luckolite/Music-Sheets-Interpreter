# Music Sheets Interpreter

Offline sheet-music recognition with downloadable model weights, a standalone Java decoder,
and a Python interface for images and PDFs.

**The code and model weights are Apache-2.0 licensed. You may use, modify and distribute
them in commercial and closed-source projects.** There is no requirement to publish your
application or contribute your changes. Preserve the license and applicable notices, and
mark modified files when distributing them. See [LICENSE](LICENSE), [NOTICE](NOTICE) and
[the licensing guide](docs/licensing.md).

This is an **experimental early release**. It can misread pitches, accidentals, rests, ties,
clefs and timing. Check its output against the printed score. It does not yet provide
exhaustive transcription accuracy, automatic text OCR, or MusicXML export.

## What is included

- A 988,676-byte float16 TFLite segmentation model, trained from our own synthetic exercises.
- A Java 17 recognition library for measures, staffs, noteheads, clefs, key signatures,
  accidentals, written durations, ties, articulations and selected triplets.
- Optional interpretation of measure numbers, tempos, dynamics and techniques from
  **caller-supplied OCR tokens**. Time-signature changes can be supplied explicitly.
- A Python image/PDF command and API producing inspectable JSON and a MIDI preview.
- Training, synthetic data generation and TFLite export code, plus the original checkpoint.
- Synthetic examples and geometry regression tests that can be redistributed.

No server, account, API key, Android SDK or Music Sheets installation is needed to run it.
After dependencies are installed, inference is local. The runtime does not download models
or upload score images. The model and its decoder are separate, so you can integrate the
weights directly into another runtime using the [tensor contract](models/MODEL_CARD.md).

## Quick start from a release

Install **Python 3.10 or 3.11** and **Java 17 or newer**. The release wheel includes the
compiled decoder and float16 weights. Download it from [Releases](https://github.com/Luckolite/Music-Sheets-Interpreter/releases).

```sh
python -m venv .venv
# Activate .venv for your shell, then:
python -m pip install "./music_sheets_interpreter-0.1.0-py3-none-any.whl[inference,pdf]"
sheet-interpreter score.pdf --output score.json --midi preview.mid --meter 4/4 --bpm 120
```

`--meter` and `--bpm` specify the initial time signature and **quarter-note tempo**;
they are not automatically read from the page. `--key-fifths` sets the fallback key
before a printed key signature is detected (two sharps: `2`; two flats: `-2`).
`--pages 1,2,3` selects PDF pages in reading order. PNG, JPEG and other Pillow-supported
single images work too. PDF input requires the `pdf` extra.

The Python frontend uses TensorFlow 2.15.1 for the reproducible TFLite runtime. TensorFlow
is substantially larger than the model. A mobile or embedded integration can use its own
compatible TFLite runtime instead. The Java decoder itself has no external runtime JARs.

## Build from source

Building requires **JDK 17+** with `java`, `javac` and `jar` on `PATH`, or `JAVA_HOME` set.

```sh
git clone https://github.com/Luckolite/Music-Sheets-Interpreter.git
cd Music-Sheets-Interpreter
python -m venv .venv
# Activate .venv for your shell.
python scripts/build_java.py
python -m pip install ".[inference,pdf]"
sheet-interpreter examples/scale.png --output build/scale.json --midi build/scale.mid --meter 4/4
```

On Windows, activate with `.venv\Scripts\Activate.ps1` in PowerShell. On macOS/Linux,
use `source .venv/bin/activate`. Runtime wheels contain generated assets; when building
your own wheel, run the Java build first, then `python -m pip wheel . --no-deps -w dist`.

## Python and Java APIs

```python
from PIL import Image
from sheet_interpreter import Interpreter

reader = Interpreter()  # Reuse this instance; one instance per worker.
with Image.open("score.png") as image:
    page = reader.interpret(image, meter=(4, 4), key_fifths=0)

for note in page["events"]:
    print(note["midi"], note["startBeat"], note["durationBeats"])
```

The returned `score` retains raw notation evidence; `events` adds pitches and estimated
timing for preview. Bounds, warnings and fallback flags remain visible so a host application
can offer correction instead of presenting uncertain output as authoritative.

Java hosts call `io.github.luckolite.interpreter.SheetInterpreter.analyze(labels, gray,
width, height)`. Both pixel arrays are unsigned bytes in row-major order. The overload
accepting `SheetInterpreter.Annotations` integrates an OCR engine of your choice.
See [integration and JSON format](docs/integration.md).

## Weights, training and limitations

The default model is [models/music_sheets_v3_float16.tflite](models/music_sheets_v3_float16.tflite).
Its SHA-256 is `1287f50f769e8f96164636941fa3c5f61ba9debe357130a5c45642dac207bd07`.
The package verifies this hash before loading the bundled model. PyTorch `best.pt`
and exporter `weights.npz` are also provided in `models/`.

The network was randomly initialized within our own training lineage; no HOMR, Andromr,
oemer or other pretrained OMR weights, pseudo-labels or distillation targets were used.
The current model continues earlier versions of our own synthetic experiment. It was
not trained on a downloaded music-score dataset or the user's library.

The decoder was developed for Music Sheets and extracted here with its printed-geometry
repairs. This is not a new accuracy benchmark. Recently reported real-score mistakes
motivated repairs to ledger pitches, repeated key signatures, false time-signature notes,
held ties and staff recovery. Those fixes do not establish general accuracy.

Read the [model card](models/MODEL_CARD.md), [training guide](training/README.md) and
[source records](docs/source-provenance.json). Training uses external Verovio and resvg tools;
font files retain their own OFL notices. Commercial score images and private phone logs
are not part of this repository.

## Tests and contributions

```sh
python scripts/build_java.py
python scripts/test_java.py
python -m unittest discover -s tests -v
python scripts/smoke_test.py
```

The Java tests use JUnit only for testing, downloaded with pinned checksums. Runtime
inference does not need JUnit. The smoke test reads the original synthetic scale through
the actual model, decoder and MIDI writer. See the
[v0.1.0 validation record](docs/validation-v0.1.0.json) for the checks and their limits.

Bug reports with a small original or redistributable score, expected notes and observed
JSON are welcome. Please do not upload copyrighted scores without permission, credentials,
or private device logs. See [CONTRIBUTING.md](CONTRIBUTING.md). Contributions are accepted
under Apache-2.0; contributing back is encouraged, not required.
