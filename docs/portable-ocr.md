# Shared page OCR pipeline (experimental)

`PortableOcr` is a Java-only detector/recognizer pipeline accepting ARGB pixels
and an `Inference` implementation. Windows and Android execute the same resize,
normalization, connected-component extraction, stacked-row splitting, CTC decoding,
and word construction. The default standalone reader is unchanged; this class
does not automatically download or activate an OCR engine.

The caller supplies detector probabilities, recognizer probabilities and a pinned
dictionary (including blank index zero). It must manage runtime/session ownership,
timeouts, and model verification. The recognizer expects 48-pixel BGR input,
zero padding to at least 320 pixels, and CTC probability output. Detector inputs
use BGR ImageNet normalization with maximum side 2048 and multiples of 32.

## Evidence and limits

- Original text-only fixtures at 18, 28 and 42 pixels: 123/123 tokens matched
  for both tested detector candidates with the Latin v5 recognizer.
- Isolated dynamics, fret numbers and stacked meter: mobile v4 detector missed
  `f` and `1`, and recognized `0` as `o`. Mobile v5 plus stacked-row separation
  matched 21/21 dark-print tokens. Faint-print v5 matched 19/21 exactly (one
  missing zero and a capitalization mismatch). No global digit substitution.
- Complete output (including all estimated symbol boxes) matched between Windows
  Java and an ARM64 Android phone on an original isolated-markings page and one
  private real-score raster. Private evidence is not included in this repository.
- These checks establish limited parity, not general OCR or musical accuracy.
  A full interpretation migration still requires downstream regression review.
- Boxes are axis-aligned. Rotated text and skew are not yet validated. Symbol
  boxes are estimated from CTC alignment, **not measured character contours**.

Candidate artifacts were evaluated from RapidOCR's versioned `v3.9.2` model
registry, without modifying weights:

| Artifact | SHA-256 |
| --- | --- |
| ch_PP-OCRv4_det_mobile.onnx | d2a7720d45a54257208b1e13e36a8479894cb74155a5efe29462512d42f49da9 |
| ch_PP-OCRv5_det_mobile.onnx | 4d97c44a20d30a81aad087d6a396b08f786c4635742afc391f6621f5c6ae78ae |
| latin_PP-OCRv5_rec_mobile.onnx | b20bd37c168a570f583afbc8cd7925603890efbcdc000a59e22c269d160b5f5a |
| Explicit UTF-8 Latin dictionary | 1169fb297871f7a14d6a0f20c14af56de789c48b170e59dfb66950448e31c062 |

The explicit dictionary avoids relying on runtime-specific metadata string
decoding. Validate its length against the output vocabulary. Android ONNX Runtime
1.23.2 crashed on a tested Snapdragon/Android 16 device; 1.25.1 passed these
checks. Windows Java also used 1.25.1 with a compatible C++ runtime. These are
evaluation dependencies, not additions to the JDK-only default package.

## Optional desktop evaluation

Build the normal Java core first. Obtain the desktop `onnxruntime-1.25.1.jar`
from the official Maven coordinate `com.microsoft.onnxruntime:onnxruntime:1.25.1`.
Download the pinned models from the registry paths
`onnx/PP-OCRv5/det/ch_PP-OCRv5_det_mobile.onnx` and
`onnx/PP-OCRv5/rec/latin_PP-OCRv5_rec_mobile.onnx` under
`https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.9.2/`.
The binding verifies all model/dictionary hashes before opening model sessions.

With the optional Python `onnxruntime` package installed, export the explicit
dictionary beside the recognizer:

```sh
python scripts/export_ocr_dictionary.py /path/to/latin_PP-OCRv5_rec_mobile.onnx
python scripts/build_portable_ocr.py --onnx-jar /path/to/onnxruntime-1.25.1.jar
```

Use the printed classpath with Java class
`io.github.luckolite.interpreter.PortableOcrProbe`, followed by detector path,
recognizer path and image path. It prints two timed passes and TSV rows containing
base64 UTF-8 text plus original-pixel left/top/right/bottom coordinates.
`-Docr.symbols=true` prints estimated symbols instead of words.
This evaluation command does not create interpretation caches or sync anything.

Windows must have a compatible Visual C++ runtime. Some JDK installations carry
old private runtime DLLs that shadow a newer system installation; an ONNX library
load failure is not evidence of model failure. Use a compatible supported JDK/runtime
installation rather than replacing DLLs inside another application's installation.

Model lineage and redistribution terms:
[RapidOCR registry](https://github.com/RapidAI/RapidOCR/blob/main/python/rapidocr/default_models.yaml),
[RapidOCR model licensing](https://github.com/RapidAI/RapidOCR/blob/main/README.md),
[PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR),
[ONNX Runtime](https://github.com/microsoft/onnxruntime).
RapidOCR documents converted artifacts under the upstream Apache-2.0 terms.
No third-party model artifacts, fonts, runtime binaries, or private scans are
included by this change.
