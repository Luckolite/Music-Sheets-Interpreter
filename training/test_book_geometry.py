# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
import unittest

import cv2
import numpy as np

from book_geometry import BookGeometry, centre_heatmap, warp_book


class BookGeometryTest(unittest.TestCase):
    def fixture(self):
        image = np.full((320, 320), 255, np.uint8)
        labels = np.zeros_like(image)
        centres = [[64., 80., 2., 'ordinary'], [156., 165., 2., 'ordinary'],
                   [260., 235., 2., 'ordinary']]
        for x, y, _, _ in centres:
            cv2.ellipse(image, (int(x), int(y)), (6, 4), 0, 0, 360, 0, -1)
            cv2.ellipse(labels, (int(x), int(y)), (6, 4), 0, 0, 360, 2, -1)
        return image, labels, centres

    def test_identity_preserves_pixels_labels_and_centres(self):
        image, labels, centres = self.fixture()
        a, masks, moved, heat = warp_book(image, [labels], centres, BookGeometry())
        np.testing.assert_array_equal(a, image)
        np.testing.assert_array_equal(masks[0], labels)
        self.assertEqual(moved, centres)
        np.testing.assert_array_equal(heat, centre_heatmap(centres, image.shape))

    def test_both_book_edges_and_tilt_directions_keep_truth_on_ink(self):
        image, labels, centres = self.fixture()
        for sign in [-1, 1]:
            g = BookGeometry(.12 * sign, 12 * sign, .12 * sign, 10 * sign, sign, .9)
            a, masks, moved, heat = warp_book(image, [labels, labels.copy()], centres, g)
            self.assertEqual(len(moved), len(centres))
            self.assertEqual(set(np.unique(masks[0])), {0, 2})
            np.testing.assert_array_equal(masks[0], masks[1])
            for x, y, *_ in moved:
                ix, iy = round(x), round(y)
                self.assertLess(a[iy, ix], 32)
                self.assertEqual(masks[0][iy, ix], 2)
                self.assertEqual(heat[iy, ix], 1.)

    def test_forward_inverse_agree_under_curvature_and_taper(self):
        for seed in range(20):
            g = BookGeometry.sample(np.random.default_rng(seed))
            sx, sy = g.inverse_maps((320, 320))
            x, y = g.forward(sx, sy, (320, 320))
            yy, xx = np.mgrid[:320, :320]
            np.testing.assert_allclose(x, xx, atol=5e-5)
            np.testing.assert_allclose(y, yy, atol=5e-5)

    def test_outside_centres_do_not_leave_heat_ghosts(self):
        image, labels, _ = self.fixture()
        _, _, moved, heat = warp_book(image, [labels], [[1., 1., 2.]], BookGeometry(slope=.2))
        self.assertEqual(moved, [])
        self.assertEqual(float(heat.max()), 0.)

    def test_empty_page_stays_empty(self):
        image = np.full((320, 320), 255, np.uint8)
        a, masks, moved, heat = warp_book(image, [np.zeros_like(image)], [], BookGeometry(bend=12))
        self.assertTrue(np.all(a == 255))
        self.assertFalse(masks[0].any())
        self.assertFalse(heat.any())
        self.assertEqual(moved, [])

    def test_sample_reproducible(self):
        self.assertEqual(BookGeometry.sample(np.random.default_rng(21)),
                         BookGeometry.sample(np.random.default_rng(21)))

    def test_folded_page_is_rejected(self):
        with self.assertRaises(ValueError):
            BookGeometry(taper=1.).inverse_maps((320, 320))


if __name__ == '__main__':
    unittest.main()
