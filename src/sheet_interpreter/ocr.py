# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Bundled, offline OCR for raster score text and scanned tablature frets."""

import numpy as np
import re


def tempo_numbers(words):
    """Bound BPM digits within an OCR line; the Java decoder verifies the equals glyph."""
    result = []
    for word in words:
        value = str(word.get("text", ""))
        match = re.search(r"=\s*(\d{2,3})\s*[)\]]?\s*$", value)
        if not match:
            continue
        bpm = int(match.group(1))
        if bpm < 30 or bpm > 400:
            continue
        width = word["right"] - word["left"]
        if width <= 0 or not value:
            continue
        left = word["left"] + width * match.start(1) / len(value)
        result.append(dict(value=bpm, left=left, top=word["top"],
                           right=word["left"] + width * match.end(1) / len(value), bottom=word["bottom"],
                           annotationLeft=word["left"]))
    return result


def rest_count_numbers(words):
    """Offer isolated numerals to the decoder's multi-measure-rest geometry check."""
    result = []
    for word in words:
        value = str(word.get("text", "")).strip()
        if not value.isascii() or not value.isdigit():
            continue
        count = int(value)
        if 2 <= count <= 32:
            result.append(dict(value=count, left=word["left"], top=word["top"],
                               right=word["right"], bottom=word["bottom"]))
    return result


class LocalOcr:
    def __init__(self):
        try:
            from rapidocr import RapidOCR
        except ImportError as error:
            raise RuntimeError(
                "Install the inference extra for built-in OCR: "
                "pip install 'music-sheets-interpreter[inference]'"
            ) from error
        self.engine = RapidOCR()

    def words(self, gray):
        """Return decoder words in normalized page coordinates."""
        if not isinstance(gray, np.ndarray) or gray.ndim != 2 or gray.dtype != np.uint8:
            raise ValueError("OCR expects a 2D uint8 grayscale page")
        height, width = gray.shape
        result = self.engine(gray)
        if result.boxes is None or result.txts is None:
            return []
        words = []
        scores = result.scores if result.scores is not None else [1.0] * len(result.txts)
        for box, value, score in zip(result.boxes, result.txts, scores):
            value = value.strip()
            # A tab string crossing a fret can be read as a leading minus sign.
            # Negative frets do not exist, so retain the printed digit evidence.
            numeric = value.strip("-–—")
            if numeric.isdigit():
                value = numeric
            if score < .5 or not value or len(value.encode("utf-8")) > 10000:
                continue
            left = max(0.0, min(1.0, float(np.min(box[:, 0])) / width))
            right = max(0.0, min(1.0, float(np.max(box[:, 0])) / width))
            top = max(0.0, min(1.0, float(np.min(box[:, 1])) / height))
            bottom = max(0.0, min(1.0, float(np.max(box[:, 1])) / height))
            if left < right and top < bottom:
                words.append(dict(text=value, left=left, top=top, right=right, bottom=bottom))
        return words[:10000]
