# Generate data, train and export

The generator authors new MEI exercises; it does not download scores. Labels come
from renderer geometry. Seeds are assigned to a split before augmentation, with
Leland held out from the Bravura training/validation sets.

Use Python 3.10 or 3.11. Keep training and TensorFlow export in separate virtual
environments if their numerical dependencies conflict. The recorded training used
PyTorch 2.1.2+cu121; ordinary CPU PyTorch works too, with lower throughput.

From the repository root:

```sh
python -m pip install -r training/requirements-training.txt
python training/generate.py --out training/corpus-new --count 600 --workers 4 --start 5000000
python training/audit_corpus.py training/corpus-new --out training/corpus-new-audit.json
python training/train.py --corpus training/corpus-new --out training/runs/new --steps 3000 --batch 8
python training/evaluate_pages.py --corpus training/corpus-new --checkpoint training/runs/new/best.pt --out training/runs/new-pages
```

Choose a new output directory per experiment. `--resume models/best.pt` continues
the published checkpoint's weights, but restarts the optimizer; it is not an exact
training-state resume. Without `--resume`, a fresh model is randomly initialized.
The generator checks bounding boxes and writes seeds, renderer/font identifiers,
hashes and split metadata into its manifest. The trainer verifies the corpus hashes.

Export in a TensorFlow environment:

```sh
python -m pip install -r training/requirements-export.txt
python training/export_tflite.py training/runs/new/weights.npz training/runs/new-export
```

The exporter mirrors the original graph, checks logits against stored PyTorch
reference samples, then exercises the actual exported TFLite interpreter. The
published `models/weights.npz` contains our numeric weights plus original synthetic
parity inputs/logits and can be passed to this exporter directly.

`--quantize` additionally exports calibrated int8. Int8 was not selected for the
current model because it failed musical regression checks. Export parity on training
samples is not an independent accuracy evaluation. Treat every new model as an
experimental candidate and evaluate it on representative scores you have rights to use.

To recreate the small public demonstration:

```sh
python -m pip install -r training/requirements-generator.txt
python scripts/generate_example.py
```

## Licenses and reproducibility

Verovio 6.3.0 (LGPL-3.0) and resvg-py 0.3.0 are external rendering tools. Noto Serif,
Bravura and Leland retain SIL OFL notices. None of these font files or rendering
libraries are required by the inference JAR or Python wheel.

The current weights were trained through three generations of our own experiment;
see [the model card](../models/MODEL_CARD.md). Running this current generator and
training from scratch produces a new model, not the exact published checkpoint.
GPU drivers, hardware, dependency builds and floating-point kernels can affect
results even with fixed seeds. The provided hashes identify the released artifacts.
