# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Synthetic scanned-tab regression for the installed OCR and Java decoder."""

import json
import subprocess
import tempfile
import unittest
from pathlib import Path

import numpy as np
from PIL import Image

from sheet_interpreter.ocr import LocalOcr
from sheet_interpreter.reader import java_executable, write_page


ROOT = Path(__file__).resolve().parents[1]


class BuiltinOcrTests(unittest.TestCase):
    def test_scanned_frets_reach_the_decoder_without_supplied_annotations(self):
        # Newly drawn tab rules and frets, saved as pixels so rendering does not
        # vary across operating systems and font rasterizers.
        with Image.open(ROOT / "tests/fixtures/synthetic-scanned-tab.png") as image:
            gray = np.asarray(image.convert("L"), dtype=np.uint8)
        words = LocalOcr().words(gray)
        self.assertEqual({"0", "12", "7", "3"}, {word["text"] for word in words})
        for fret, x in (("0", 240), ("12", 420), ("7", 590), ("3", 760)):
            word = next(word for word in words if word["text"] == fret)
            self.assertLess(abs((word["left"] + word["right"]) * 600 - x), 20)

        with tempfile.TemporaryDirectory() as folder:
            page = Path(folder) / "scan.page.gz"
            output = Path(folder) / "score.json"
            write_page(page, np.zeros_like(gray), gray, {"words": words})
            run = subprocess.run([java_executable(), "-jar", str(ROOT / "src/sheet_interpreter/interpreter.jar"),
                                  str(page), str(output), "4", "4", "0"],
                                 capture_output=True, text=True)
            self.assertEqual(0, run.returncode, run.stderr)
            score = json.loads(output.read_text(encoding="utf-8"))
        self.assertEqual([64, 67, 52, 62], [event["midi"] for event in score["events"]])


if __name__ == "__main__":
    unittest.main()
