# Shared OCR boundary (migration foundation)

`OcrText` is immutable OCR evidence, independent of a platform SDK. It preserves
block, line, word and symbol order; exact text; duplicate tokens; input-pixel
bounding boxes; and missing geometry. It deliberately does not normalize text,
merge words, guess missing character boxes or clamp coordinates.

The Android app now adapts its existing ML Kit evidence into this representation
for header, meter, technique, ornament, measure-number and tablature consumers.
Its adapter copies all evidence levels without changing detection or correction
rules. The Android service, bitmap lifetime and ML Kit dependency stay outside
this standalone repository.

`OcrCtcDecoder` is a portable greedy decoder for probability tensors. A caller
supplies the exact model dictionary (including the blank slot) and blank index.
Repeated predictions collapse unless a blank separates them. Zero and letter o,
punctuation, whitespace and accented characters are preserved. Its time spans
are model steps, **not character bounding boxes**. Confidence matches the mean
probability at emitted token starts; it is not calibrated musical certainty.

These classes do not install or enable a new OCR model. Detection, shared raster
preprocessing, model/dictionary provenance, accurate symbol geometry, real ARM
validation and end-to-end interpretation comparison remain migration work.
Existing released interpretation caches and models are unchanged. No private
score images or device logs are included in these regressions.

Validation: original unit tests cover immutable nested collections, missing
geometry, duplicate evidence, repeated dynamics, exact numeric/letter handling,
all-blank output, vocabulary mismatch and non-finite probabilities.
