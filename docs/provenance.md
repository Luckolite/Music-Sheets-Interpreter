# Source and artifact provenance

The standalone decoder was extracted from Music Sheets revision
`eb6e9ed8a607225aea0f448e870ae039e72314b1`, after its printed-geometry repairs.
The selected Java source history identifies Luckolite as its author, apart from
a local checkpoint author label. The app's source notice identifies its measure
postprocessing as original Music Sheets code, not a copy of HOMR's Python pipeline.

The selected dependency closure comprises recognition and musical-data classes.
The extraction changes the package to `io.github.luckolite.interpreter`, replaces
Android logging with optional standard-error diagnostics, and adds a public API,
file bridge, build scripts and Python frontend. Original source hashes are recorded
in [source-provenance.json](source-provenance.json). The generic six-class label
vocabulary is an interface; the historical third-party segmentation model is absent.

The training network, exercise generator and export code were authored for the
independent Music Sheets experiment. The provided model is trained within that
experiment's own checkpoint lineage, documented in the model card and manifests.
Recorded checkpoints contain tensors and ordinary training metadata, not user scores.

This is a reviewed extraction from the available source history, not a claim that
the project was developed through a formal clean-room process. All third-party
material retained in the repository has its own identified notice. The Apache
license covers the original material the project author is licensing.

## Public repository boundary

The repository starts with a fresh history. It excludes the Android application,
Sync Hub, update/signing machinery, phone logs, local backups, commercial score
fixtures, recorded instrument banks, Google OCR assets, and earlier HOMR/Andromr
weights. Only explicitly selected source, the independent weights, original examples
and license material are staged for publication.

The source Music Sheets working directory and rollback snapshots are preserved.
Changes to this repository do not alter or install an application on the user's phone.
