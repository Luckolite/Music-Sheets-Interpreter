# Third-party notices

The runtime wheel and Java JAR contain our original code and our independent weights.
Dependencies are installed separately, not copied into the wheel or JAR. Preserve their
complete upstream notices if you redistribute them as part of a larger application.

| Component | Role | Upstream license information |
|---|---|---|
| TensorFlow 2.15.1 / LiteRT-compatible inference | Python inference | [Apache-2.0](https://github.com/tensorflow/tensorflow/blob/v2.15.1/LICENSE); its distributions carry dependency notices |
| NumPy 1.26.x | Pixel arrays | [BSD-3-Clause and bundled-component notices](https://github.com/numpy/numpy/blob/v1.26.4/LICENSE.txt); binary wheels include additional library licenses |
| Pillow | Image I/O | [MIT-CMU](https://github.com/python-pillow/Pillow/blob/main/LICENSE) for the verified 12.2.0 build, plus codec/dependency notices in binary distributions |
| pypdfium2 4.x / PDFium | Optional PDF rasterization | [Apache-2.0 OR BSD-3-Clause, plus PDFium third-party notices](https://github.com/pypdfium2-team/pypdfium2/tree/main/licenses) |
| Java runtime | Runs the decoder | Supplied by the user; licensing depends on the JDK/JRE distribution |
| PyTorch 2.1.2 | Optional training | [BSD-style license and third-party notices](https://github.com/pytorch/pytorch/blob/v2.1.2/LICENSE) |
| OpenCV Python 4.10.0.84 | Optional training data and augmentation | [Apache-2.0 and bundled-component notices](https://github.com/opencv/opencv-python/blob/4.10.0.84/LICENSE.txt) |
| Verovio 6.3.0 | Optional synthetic-score renderer | [LGPL-3.0](https://github.com/rism-digital/verovio/tree/version-6.3.0); external training tool, not a runtime dependency |
| resvg-py 0.3.0 / resvg | Optional SVG rasterizer for training | [MIT wrapper](https://github.com/baseplate-admin/resvg-py/blob/main/LICENSE), with resvg and its dependency licenses |
| JUnit 4.13.2 / Hamcrest 1.3 | Java tests only | [EPL-1.0](https://github.com/junit-team/junit4/blob/r4.13.2/LICENSE-junit.txt) / [BSD-3-Clause](https://github.com/hamcrest/JavaHamcrest/blob/v1.3/LICENSE.txt) |

Dependency licenses are not replaced by this project's Apache license. In particular,
an upstream package's headline license is not a substitute for its complete binary
distribution notices.

## Training fonts

The bundled, unmodified `training/fonts/NotoSerif.ttf` is copyright 2022 The Noto
Project Authors and is distributed under SIL OFL 1.1. Its complete notice is
[NOTO-SERIF-OFL.txt](training/fonts/NOTO-SERIF-OFL.txt).

The Bravura and Leland music glyphs are supplied by the pinned Verovio installation.
Their upstream notices accompany the training tooling as
[BRAVURA-OFL.txt](training/fonts/BRAVURA-OFL.txt) and
[LELAND-OFL.txt](training/fonts/LELAND-OFL.txt). They are not bundled in the runtime wheel.

Generated score images in `examples/` are newly authored exercises. No commercial
sheet-music pages, recordings, pretrained third-party OMR models or Google OCR assets
are included.
