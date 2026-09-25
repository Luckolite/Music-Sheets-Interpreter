# Generate data, train and export

Use Python 3.10 or 3.11. Keep PyTorch training and TensorFlow export in separate
environments if their dependencies conflict. Inference uses the TFLite model;
training tools and fonts are not needed in a deployed application.

## Load or fine-tune the current v4 checkpoint

`model_v4.py` defines the current architecture. `train_v4.py` loads the published
checkpoint, trains semantic and auxiliary centre outputs on original labelled pages,
and selects a checkpoint with validation data. It does not evaluate test pages.

From the repository root:

```sh
python -m pip install -r training/requirements-training.txt
python training/generate.py --out training/corpus-new --count 600 --workers 4 --start 5000000
python training/audit_corpus.py training/corpus-new --out training/corpus-new-audit.json
python training/train_v4.py --corpus training/corpus-new --checkpoint models/best.pt --out training/runs/new-v4 --steps 3000 --batch 8
```

Choose a new output directory for each run. The generator authors new MEI exercises
and derives labels from renderer geometry. It records seeds, splits, font identifiers
and hashes. Fine-tuning checks those hashes and starts a fresh optimizer.

To load tensors directly, put `training/` on Python's import path:

```python
import torch
from model_v4 import Segmenter

checkpoint = torch.load("models/best.pt", map_location="cpu", weights_only=True)
model = Segmenter()
model.load_state_dict(checkpoint["state_dict"])
model.eval()
```

The auxiliary centre output is available through `forward_with_centres`. The default
deployment model uses only semantic classes. Validate centre predictions separately
before using them to reject or create notes.

## Hard-scan candidate training

`train_hard_scan.py` provides an experimental, resumable fine-tuning recipe for
the existing six-class v4 architecture. It is not a newly released model and
does not deploy or export weights automatically. It uses original renderer
labels, clean retention examples, paired page curvature, faint/thin strokes,
paper shadows, and procedural marginal clutter. Geometric transforms move the
semantic masks, renderer-category masks, and note centres together. Thin ink is
attenuated rather than entirely erased to avoid teaching invisible-note targets.

Copy `hard-scan-config.example.json`, set your original corpus path and manifest
SHA-256, and confirm the parent checkpoint hash. Additional reviewed original
corpora can be listed with sampling weights. The loader checks image/label hashes,
path containment, and exercise-seed split isolation across corpora. It rejects
commercial/pretrained training provenance; test pixel files remain unopened.
Full-precision CUDA training is the default. PyTorch and OpenCV are required.

```sh
python training/train_hard_scan.py --config my-hard-scan-config.json --out training/runs/hard-scan
# After an interruption, use exactly the same config, code and output directory:
python training/train_hard_scan.py --config my-hard-scan-config.json --out training/runs/hard-scan --resume
python -m unittest discover -s training -p 'test_hard_scan_*.py'
```

The run freezes code/config/provenance and a parent-model development baseline.
It saves `last.pt` for resumption and saves `best-development.pt` only when the
candidate passes the development gates. No qualifying checkpoint is guaranteed.
The fixed comparison covers clean, warp, faint, shadow, thin, combined, and blank
strata. A 64-pixel crop border is excluded to avoid scoring warped edge fragments;
these are **crop development metrics, not whole-page or independent accuracy**.
Class-2 components use maximum-cardinality one-to-one renderer-box matching, with
IoU >= 0.5 and component area >= 3. Connected touching heads remain a limitation
of this proxy, as they are in the unchanged parent comparison.

Screening requires hard-stratum mean head F1 improvement of at least 0.003; no
extra blank heads; no increased false/missed-head counts or decreased true-head
counts in any clean crop; head precision/recall within 0.01 per stratum; semantic
precision/recall within 0.015; and renderer-category pixel recall within 0.02 for
categories with at least 100 pixels. Category recall is **not** rest/symbol
instance accuracy. The auxiliary centre head is trained but is not used to create
or suppress deployment notes.

Before promotion, separately require source-note retention with an identical
frozen decoder, a sealed original holdout, export/native parity, latency checks,
and reviewed model lineage/checksums. Keep private score evaluation separate from
training and public artifacts. A model experiment does not complete decoder,
tempo, repeat-navigation, or symbol-semantics work.

## Exporting qualified weights

```sh
python -m pip install -r training/requirements-export.txt
python training/export_v4.py models/weights.npz training/runs/v4-export
```

Use a fine-tuned run's `weights.npz` to export its model. The exporter checks both
outputs against stored PyTorch reference tensors, then verifies the TFLite tensor
contract. Its float16 semantic model is the default deployment format. Calibration
samples are original synthetic training crops; export parity is not a recognition
accuracy test.

The legacy `model.py`, `train.py` and `export_tflite.py` remain for the smaller v3
architecture. Use a v3 checkpoint from release v0.1.0 with those tools. The current
v4 checkpoint requires the corresponding v4 scripts above.

## Licenses and reproducibility

Code and weights are Apache-2.0. Verovio and resvg are external rendering tools;
Noto Serif, Bravura and Leland retain their SIL OFL notices. See
[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).

The current weights inherit several generations of our own synthetic experiment;
[lineage.json](../models/lineage.json) records their hashes and training sources.
These portable tools support model reuse and further training. They do not replay
every historical corpus generation or optimizer update, and bit-for-bit retraining
is not claimed. GPU kernels and dependency versions can also change numerical results.
