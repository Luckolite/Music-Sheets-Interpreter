#!/usr/bin/env python3
# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Export and verify the explicit dictionary from the pinned Latin v5 model.

Requires optional Python onnxruntime; no inference is performed.
"""
import argparse
import hashlib
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("model", type=Path)
    args = parser.parse_args()
    if hashlib.sha256(args.model.read_bytes()).hexdigest() != "b20bd37c168a570f583afbc8cd7925603890efbcdc000a59e22c269d160b5f5a":
        raise SystemExit("Unrecognized recognizer checksum")
    import onnxruntime as ort
    session = ort.InferenceSession(str(args.model), providers=["CPUExecutionProvider"])
    characters = session.get_modelmeta().custom_metadata_map["character"].splitlines()
    # Artifact format is explicitly CRLF on every OS, not platform write_text translation.
    payload = ("\r\n".join(["blank", *characters, " "]) + "\r\n").encode("utf-8")
    if hashlib.sha256(payload).hexdigest() != "1169fb297871f7a14d6a0f20c14af56de789c48b170e59dfb66950448e31c062":
        raise SystemExit("Dictionary export disagrees with validated UTF-8 artifact")
    target = Path(str(args.model) + ".dictionary")
    if target.exists() and target.read_bytes() != payload:
        raise SystemExit("Refusing to overwrite a different dictionary")
    target.write_bytes(payload)
    print(target)


if __name__ == "__main__":
    main()
