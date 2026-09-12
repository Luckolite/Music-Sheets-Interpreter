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

## Export

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
