First experimental standalone release of Music Sheets Interpreter.

The original code and independent model weights are licensed under Apache-2.0,
permitting commercial and closed-source use subject to the license and notices.

Included: a Python image/PDF command and API, dependency-free Java recognition core,
JSON and MIDI preview output, float16 TFLite weights, a PyTorch checkpoint, exporter
arrays, original examples, training scripts and geometry regression tests.

Install Python 3.10/3.11 and Java 17+, then install the wheel with its `inference`
and optional `pdf` extras. The wheel includes the core JAR and model. Individual
model/JAR downloads and a model archive are provided for other integrations.

This is experimental OMR. Review recognized notes and timing. Text OCR is supplied
by the caller; initial meter and tempo are explicit arguments. MIDI is a preview,
not a complete expressive performance. Read the model card and licensing guide.

Validation includes 180 Java tests, 9 Python tests, exact eight-note PNG and PDF
model-to-MIDI smoke checks, a synthetic generator audit, a two-step training smoke
run and a fresh TFLite re-export. These are not a general transcription-accuracy score.
The re-export reproduces the bundled float16 model byte for byte. The installed
release wheel passed the same image/PDF checks. `SHA256SUMS` covers all binary assets.
