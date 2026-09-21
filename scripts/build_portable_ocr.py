#!/usr/bin/env python3
# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Compile the optional shared OCR binding against a caller-supplied ONNX Runtime jar."""
import argparse
import os
import subprocess
from pathlib import Path
from build_java import ROOT, jdk_tool


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--onnx-jar", type=Path, required=True)
    args = parser.parse_args()
    if not args.onnx_jar.is_file():
        parser.error("ONNX Runtime 1.25.1 desktop jar is required")
    sources = sorted((ROOT / "optional/ocr/java").rglob("*.java"))
    classes = ROOT / "build/ocr-classes"
    classes.mkdir(parents=True, exist_ok=True)
    cp = os.pathsep.join(map(str, [ROOT / "build/classes", args.onnx_jar.resolve()]))
    subprocess.run([jdk_tool("javac"), "--release", "17", "-encoding", "UTF-8",
                    "-cp", cp, "-d", str(classes), *map(str, sources)], check=True)
    print("OCR classpath: " + os.pathsep.join([str(classes), cp]))


if __name__ == "__main__":
    main()
