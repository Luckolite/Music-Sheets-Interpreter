# Music Sheets independent v3 float16

**License:** Apache-2.0, including the weights. **Status:** experimental.

| Property | Value |
|---|---|
| Architecture | `musicsheets-tiny-unet-v1`, six-class U-net |
| Parameters | 487,094 |
| TFLite file | `music_sheets_v3_float16.tflite` |
| File size | 988,676 bytes |
| SHA-256 | `1287f50f769e8f96164636941fa3c5f61ba9debe357130a5c45642dac207bd07` |
| Input | float32 `[1,3,320,320]`, raw grayscale 0..255 replicated into three channels |
| Output | int64 `[1,320,320]`, class IDs 0..5 |
| Full-page preprocessing | Analysis width 2048, preserve aspect ratio, grayscale on white |
| Tiling | 320-pixel square tiles, stride 192, tail anchored to the image edge |
| Merge | Largest distance from a tile edge wins; later tile wins equal distances |
| Default execution | CPU, two threads |

Class IDs: **0** background, **1** stems/rests/barlines, **2** noteheads,
**3** clefs/keys/accidentals, **4** staff, **5** other symbols.

Float16 describes stored model parameters. The input is still float32 and the output
is int64. Do not normalize the input to 0..1: normalization is inside the network.
The output is a segmentation map, not pitches, durations, confidence scores or MusicXML.
The Java decoder combines this map with the original grayscale image to interpret notation.

## Intended use

Research, prototyping and host applications that allow users to inspect or correct
recognized music. It is a starting point for permissively licensed local OMR, not
a claim of reliable automatic transcription for every score.

The current weights can be used without our Python frontend, for example through a
compatible TFLite runtime in an Android or desktop application. Use the precise tensor
contract and tile ownership above. Models are loaded locally; no server is required.

## Training data and lineage

The network is our own implementation with our own random initialization lineage.
No external pretrained OMR weights, distillation, pseudo-labels or downloaded score
datasets were used. Exercises are generated as MEI and rendered by Verovio, with labels
derived from their SVG elements. No user-library or commercial score is in training.

The current run uses 480 training pages, 60 validation pages and 60 test pages from
original generated exercises. Exercise seeds are disjoint across splits. Training and
validation use Bravura; Leland is held out for testing. Augmentation follows splitting.

This checkpoint continues our own v2 checkpoint, which continues our own v1 run.
The first generator had defective instance boxes; its synthetic accuracy figures were
withdrawn. The current generator validates the corrected boxes and expands rhythmic
and layout variation. The inherited v1 training history remains part of this model's
lineage; training a new model from scratch will not reproduce these exact weights.

The v3 run executed 3,000 optimization steps and selected the step-1,000 checkpoint
by validation foreground IoU. Training used PyTorch 2.1.2 on an RTX 2080 SUPER.
Full lineage and artifact hashes are in [lineage.json](lineage.json) and
[artifacts.json](artifacts.json); recorded run measurements are in [training.json](training.json).
The public `weights.npz` uses lossless ZIP compression. All arrays match the original
export input exactly; its original and compressed file hashes are both recorded.

## Measurements and their limits

The recorded v3 held-out **synthetic crop** foreground mean IoU is approximately 0.7403,
over 360 fixed foreground-biased crops from 60 Leland test pages. This is a pixel-class
segmentation measurement. It is not note accuracy, full-page accuracy or real-score
transcription accuracy. See the per-class results in `training.json`.

The float16 export was checked numerically against our own PyTorch graph and exercised
in an Android app. Targeted score tests found and repaired decoder bugs, including
ledger pitches, key-signature miscounts and ties. Users still reported substantial
real-score mistakes. No general accuracy percentage or state-of-the-art claim is made.

Public tests use generated geometry and an original eight-note scale. Commercial
score fixtures and private diagnostics are deliberately not distributed. Historical
`quality_approved` flags in the recorded training/export files describe experiment
gates; this public release remains experimental.

Known weak areas include complex polyphony, cue-sized parts, low-quality scans,
unusual engraving, cross-system ties, accurate voice separation, OCR-dependent
directions, ornaments, pickup timing and meter changes. MIDI is a preview with full
measure slots and fixed velocity, not a complete performance interpretation.

## Reuse and attribution

The code, checkpoint and exported weights are Apache-2.0. Include the license and
applicable notices when redistributing them. Training fonts and external libraries
retain their own licenses, described in [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).
This project is not affiliated with the authors of HOMR, Andromr or oemer.
