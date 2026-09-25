# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
import json
from pathlib import Path
import tempfile
import unittest

import cv2
import numpy as np

from hard_scan_data import STRATA, add_clutter, appearance, blank, corpus_records, sha, transform


class ScanDataTest(unittest.TestCase):
    def example(self):
        a = np.full((320, 320), 255, np.float32)
        b = np.zeros((320, 320), np.uint8)
        cv2.ellipse(a, (155, 156), (9, 6), 0, 0, 360, 0, -1)
        cv2.ellipse(b, (155, 156), (9, 6), 0, 0, 360, 2, -1)
        b[100:158, 164] = 1
        a[b > 0] = 0
        return a, b, np.where(b == 2, 3, 0).astype(np.uint8), [[155., 156., 2.]], [[146., 150., 165., 163.]]

    def test_clean_exact(self):
        a, b, c, centres, boxes = self.example()
        out = transform(a, b, c, centres, boxes, np.random.default_rng(12), 'clean')
        np.testing.assert_array_equal(a, out[0])
        np.testing.assert_array_equal(b, out[1])
        self.assertEqual(float(out[3].max()), 1.)

    def test_pairing_and_finite_all_strata(self):
        for stratum in STRATA:
            for seed in range(8):
                a, b, c, centres, boxes = self.example()
                image, labels, categories, heat, moved = transform(a, b, c, centres, boxes, np.random.default_rng(seed), stratum)
                self.assertEqual(image.shape, labels.shape)
                self.assertTrue(np.isfinite(image).all())
                self.assertTrue(0 <= image.min() <= image.max() <= 255)
                self.assertTrue(set(np.unique(labels)).issubset({0, 1, 2}))
                self.assertEqual(float(heat.max()), 1.)
                y, x = np.unravel_index(heat.argmax(), heat.shape)
                self.assertEqual(int(labels[y, x]), 2)
                self.assertEqual(int(categories[y, x]), 3)
                left, top, right, bottom = moved[0]
                self.assertTrue(left <= x < right and top <= y < bottom)

    def test_clutter_preserves_existing_labels(self):
        a, b, c, _, _ = self.example()
        for seed in range(20):
            aa, bb, cc = add_clutter(a, b, c, np.random.default_rng(seed))
            np.testing.assert_array_equal(bb[b > 0], b[b > 0])
            np.testing.assert_array_equal(aa[b > 0], a[b > 0])
            self.assertTrue(set(np.unique(bb)).issubset({0, 1, 2, 5}))
            self.assertEqual(int((bb == 2).sum()), int((b == 2).sum()))

    def test_faint_does_not_erase_core_head(self):
        a, b, _, _, _ = self.example()
        for seed in range(20):
            aa = appearance(a, np.random.default_rng(seed), 'combined')
            self.assertLess(float(aa[156, 155]), float(aa[180, 155])-25)

    def test_blank_has_no_note_targets(self):
        for seed in range(12):
            _, labels, _, heat, boxes = blank(np.random.default_rng(seed))
            self.assertFalse((labels == 2).any())
            self.assertFalse(heat.any())
            self.assertEqual(boxes, [])

    def test_appearance_deterministic(self):
        a, _, _, _, _ = self.example()
        np.testing.assert_array_equal(appearance(a, np.random.default_rng(4), 'combined'),
                                      appearance(a, np.random.default_rng(4), 'combined'))

    def test_sealed_test_pixel_files_not_read(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            cv2.imwrite(str(root/'image.png'), np.full((320, 320), 255, np.uint8))
            cv2.imwrite(str(root/'labels.png'), np.zeros((320, 320), np.uint8))
            page = dict(image='image.png', labels='labels.png', image_sha256=sha(root/'image.png'),
                        labels_sha256=sha(root/'labels.png'), shape=[320, 320], noteheads=[])
            manifest = dict(third_party_score_sources=[], pretrained_model_sources=[], exercises=[
                dict(seed=1, split='train', pretrained_labels=False, pages=[page]),
                dict(seed=2, split='test', pretrained_labels=False, pages=[dict(image='DOES_NOT_EXIST')])])
            (root/'manifest.json').write_text(json.dumps(manifest), encoding='utf-8')
            _, evidence = corpus_records([dict(name='original', root=str(root), manifest_sha256=sha(root/'manifest.json'))])
            self.assertFalse(evidence[0]['test_pixels_opened'])

    def test_reject_external_training_sources(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root/'manifest.json').write_text(json.dumps(dict(third_party_score_sources=['unreviewed'],
                pretrained_model_sources=[], exercises=[])), encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'Only original'):
                corpus_records([dict(name='bad', root=str(root), manifest_sha256=sha(root/'manifest.json'))])


if __name__ == '__main__':
    unittest.main()
