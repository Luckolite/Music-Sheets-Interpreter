# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Original procedural CPU fixtures for paired hard-scan augmentation."""
import unittest
from unittest.mock import patch

import numpy as np

from book_geometry import BookGeometry, centre_heatmap, warp_book
from hard_scan_data import crop
from hard_scan_pairs import paired_training_sample, paired_views, separated_neighbours


def notation():
    image = np.full((320, 320), 255, np.float32)
    labels = np.zeros((320, 320), np.uint8)
    boxes = [[120, 140, 136, 152], [140, 140, 156, 152], [190, 200, 208, 216]]
    centres = []
    for left, top, right, bottom in boxes:
        image[top:bottom, left:right] = 0
        labels[top:bottom, left:right] = 2
        centres.append([(left+right)/2, (top+bottom)/2, 2.])
    return image, labels, np.zeros_like(labels), centres, boxes


class SeparatedNeighboursTest(unittest.TestCase):
    def test_empty_or_single_head(self):
        self.assertEqual(separated_neighbours(()), ())
        self.assertEqual(separated_neighbours(((10, 10, 20, 20),)), ())

    def test_small_horizontal_positive_gap(self):
        boxes = ((10, 10, 20, 20), (22, 10, 32, 20))
        self.assertEqual(separated_neighbours(boxes), ((21., 15.),))
        self.assertEqual(separated_neighbours(tuple(reversed(boxes))), ((21., 15.),))

    def test_small_vertical_positive_gap(self):
        self.assertEqual(separated_neighbours(((10, 10, 20, 20), (10, 21, 20, 31))),
                         ((15., 20.5),))

    def test_touching_and_overlapping_heads_are_not_split_targets(self):
        for second in ((20, 10, 30, 20), (19, 10, 29, 20), (10, 20, 20, 30),
                       (10, 19, 20, 29), (10, 10, 20, 20)):
            with self.subTest(second=second):
                self.assertEqual(separated_neighbours(((10, 10, 20, 20), second)), ())

    def test_distant_and_diagonally_unaligned_heads_are_not_selected(self):
        for second in ((26, 10, 36, 20), (22, 30, 32, 40), (40, 21, 50, 31)):
            self.assertEqual(separated_neighbours(((10, 10, 20, 20), second)), ())


class PairedViewsTest(unittest.TestCase):
    def test_clean_view_is_exact_and_inputs_are_unchanged(self):
        a, b, c, centres, boxes = notation()
        originals = a.copy(), b.copy(), c.copy()
        student, labels, heat, teacher = paired_views(
            a, b, c, centres, boxes, np.random.default_rng(12), 'combined', clutter=True)
        np.testing.assert_array_equal(student[0, 0], a)
        np.testing.assert_array_equal(teacher[0, 0], a)
        np.testing.assert_array_equal(labels[0], b)
        np.testing.assert_array_equal(heat[0], centre_heatmap(centres, a.shape))
        for value, original in zip((a, b, c), originals):
            np.testing.assert_array_equal(value, original)
        self.assertEqual(student.shape, (2, 1, 320, 320))
        self.assertEqual(labels.shape, (2, 320, 320))
        self.assertEqual(heat.shape, (2, 320, 320))
        self.assertEqual(teacher.shape, student.shape)

    def test_warped_teacher_and_student_targets_share_exact_geometry(self):
        a, b, c, centres, boxes = notation()
        geometry = BookGeometry(slope=.035, bend=5., taper=.03, curl=2., gutter=1.,
                                horizontal_scale=.97)
        expected_image, expected_masks, moved, expected_heat = warp_book(a, [b, c], centres, geometry)
        with patch('hard_scan_data.BookGeometry.sample', return_value=geometry):
            student, labels, heat, teacher = paired_views(
                a, b, c, centres, boxes, np.random.default_rng(4), 'combined')
        np.testing.assert_array_equal(teacher[1, 0], expected_image)
        np.testing.assert_array_equal(labels[1], expected_masks[0])
        np.testing.assert_array_equal(heat[1], expected_heat)
        self.assertFalse(np.array_equal(student[1], teacher[1]))
        self.assertEqual(int((heat[1] == 1).sum()), len(centres))
        for x, y, *_ in moved:
            ix, iy = round(x), round(y)
            self.assertEqual(labels[1, iy, ix], 2)
            self.assertEqual(heat[1, iy, ix], 1.)
            self.assertLess(teacher[1, 0, iy, ix], 128)

    def test_appearance_only_pair_does_not_move_heads(self):
        a, b, c, centres, boxes = notation()
        for stratum in ('faint', 'shadow', 'thin'):
            student, labels, heat, teacher = paired_views(
                a, b, c, centres, boxes, np.random.default_rng(13), stratum)
            np.testing.assert_array_equal(labels[0], labels[1])
            np.testing.assert_array_equal(heat[0], heat[1])
            np.testing.assert_array_equal(teacher[1, 0], a)
            self.assertFalse(np.array_equal(student[1], teacher[1]))

    def test_annotations_do_not_invent_or_replace_heads(self):
        a, b, c, centres, boxes = notation()
        student, labels, heat, teacher = paired_views(
            a, b, c, centres, boxes, np.random.default_rng(3), 'faint', clutter=True)
        np.testing.assert_array_equal(labels[1] == 2, b == 2)
        np.testing.assert_array_equal(heat[0], heat[1])
        self.assertEqual(int((heat[1] == 1).sum()), 3)
        self.assertGreater(int((labels[1] == 5).sum()), 0)
        self.assertFalse((labels[0] == 5).any())
        self.assertTrue((teacher[1, 0][labels[1] == 5] < 255).all())

    def test_views_are_deterministic_for_the_same_seed(self):
        args = notation()
        first = paired_views(*args, np.random.default_rng(7), 'combined', clutter=True)
        second = paired_views(*args, np.random.default_rng(7), 'combined', clutter=True)
        for left, right in zip(first, second):
            np.testing.assert_array_equal(left, right)


class PairedSamplingTest(unittest.TestCase):
    def test_blank_shortcut_has_no_heads_or_heat(self):
        seed = next(i for i in range(1000) if np.random.default_rng(i).random() < .06)
        student, labels, heat, teacher = paired_training_sample({}, [], seed)
        self.assertFalse((labels == 2).any())
        self.assertFalse(heat.any())
        np.testing.assert_array_equal(student, teacher)
        np.testing.assert_array_equal(student[0], student[1])

    def test_sample_is_deterministic_and_only_selects_train_pages(self):
        a, b, c, centres, boxes = notation()
        train_page = {'noteheads': [{'box': q} for q in boxes]}
        pools = {'original': {'train': [train_page], 'validation': [object()], 'test': [object()]}}
        specs = [{'name': 'original', 'weight': 1.}]

        def fixed_crop(page, rng, scale=False, focus=None):
            self.assertIs(page, train_page)
            return a, b, c, centres, boxes, {'crop': [0, 0, 320]}

        with patch('hard_scan_pairs.crop', side_effect=fixed_crop) as crop_mock:
            first = paired_training_sample(pools, specs, 123)
            second = paired_training_sample(pools, specs, 123)
        self.assertEqual(crop_mock.call_count, 2)
        for left, right in zip(first, second):
            np.testing.assert_array_equal(left, right)

    def test_focus_crop_preserves_raster_and_maps_centres(self):
        image = np.arange(512*512, dtype=np.float32).reshape(512, 512) % 256
        labels = np.zeros((512, 512), np.uint8)
        labels[300:312, 290:306] = 2
        page = dict(root='synthetic', image='none', labels='none', seed=1, corpus='original',
                    noteheads=[dict(box=[290, 300, 306, 312])])
        with patch('hard_scan_data.page_arrays', return_value=(image, labels, np.zeros_like(labels))):
            a, b, _, centres, boxes, origin = crop(page, np.random.default_rng(3), focus=(300, 310))
        self.assertEqual(origin['crop'], [140, 150, 320])
        np.testing.assert_array_equal(a, image[150:470, 140:460])
        np.testing.assert_array_equal(b, labels[150:470, 140:460])
        self.assertEqual(boxes, [[150., 150., 166., 162.]])
        self.assertEqual(centres[0][:2], [158., 156.])


if __name__ == '__main__':
    unittest.main()
