# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Original procedural fixtures; no score images, pretrained outputs, or GPU."""
import itertools
import unittest

import numpy as np

from hard_scan_metrics import head_counts, match_boxes


class HeadCountsTest(unittest.TestCase):
    def mask(self, boxes, shape=(24, 32)):
        result = np.zeros(shape, dtype=np.uint8)
        for left, top, right, bottom in boxes:
            result[top:bottom, left:right] = 2
        return result

    def test_empty(self):
        self.assertEqual(head_counts(self.mask([]), []), {'tp': 0, 'fp': 0, 'fn': 0})

    def test_exact_distinct_heads(self):
        boxes = [[2, 2, 7, 7], [12, 3, 18, 8]]
        self.assertEqual(head_counts(self.mask(boxes), boxes), {'tp': 2, 'fp': 0, 'fn': 0})

    def test_false_head_and_missing_head(self):
        boxes = [[2, 2, 7, 7], [12, 3, 18, 8]]
        prediction = self.mask([boxes[0], [22, 12, 27, 17]])
        self.assertEqual(head_counts(prediction, boxes), {'tp': 1, 'fp': 1, 'fn': 1})

    def test_other_classes_do_not_count(self):
        prediction = self.mask([])
        for label in (1, 3, 4, 5):
            prediction[label * 3:label * 3 + 2, 2:6] = label
        self.assertEqual(head_counts(prediction, []), {'tp': 0, 'fp': 0, 'fn': 0})

    def test_component_area_not_box_area(self):
        prediction = self.mask([[2, 2, 3, 3], [8, 2, 10, 3], [15, 2, 18, 3]])
        # A diagonal pair has a 2x2 box but only two component pixels.
        prediction[9, 2] = prediction[10, 3] = 2
        self.assertEqual(head_counts(prediction, []), {'tp': 0, 'fp': 1, 'fn': 0})

    def test_eight_connected_diagonal_pixels_are_one_component(self):
        prediction = self.mask([])
        prediction[2, 2] = prediction[3, 3] = prediction[4, 4] = 2
        self.assertEqual(head_counts(prediction, [[2, 2, 5, 5]]), {'tp': 1, 'fp': 0, 'fn': 0})

    def test_merged_heads_fail_both_at_strict_half_iou(self):
        boxes = [[2, 2, 6, 6], [7, 2, 11, 6]]
        prediction = self.mask([[2, 2, 11, 6]])
        self.assertEqual(head_counts(prediction, boxes), {'tp': 0, 'fp': 1, 'fn': 2})

    def test_merged_component_cannot_match_two_truths(self):
        boxes = [[2, 2, 7, 6], [7, 2, 12, 6]]
        prediction = self.mask([[2, 2, 12, 6]])
        self.assertEqual(head_counts(prediction, boxes), {'tp': 1, 'fp': 0, 'fn': 1})

    def test_split_head_has_extra_prediction(self):
        prediction = self.mask([[2, 2, 9, 8], [10, 2, 14, 8]])
        self.assertEqual(head_counts(prediction, [[2, 2, 14, 8]]), {'tp': 1, 'fp': 1, 'fn': 0})

    def test_duplicate_truth_instances_stay_separate(self):
        box = [2, 2, 7, 7]
        self.assertEqual(head_counts(self.mask([box]), [box, box]), {'tp': 1, 'fp': 0, 'fn': 1})

    def test_boundary_exclusion_is_explicit(self):
        boxes = [[-3, 3, 4, 8], [12, 3, 18, 8]]
        prediction = self.mask([[0, 3, 4, 8], boxes[1], [24, 12, 29, 17]])
        self.assertEqual(head_counts(prediction, boxes), {'tp': 2, 'fp': 1, 'fn': 0})
        self.assertEqual(head_counts(prediction, boxes, boundary_margin=0),
                         {'tp': 1, 'fp': 1, 'fn': 0})

    def test_eroded_clipped_fragment_can_be_ignored(self):
        boxes = [[-3, 3, 4, 8], [12, 3, 18, 8]]
        prediction = self.mask([[1, 3, 4, 8], boxes[1]])
        self.assertEqual(head_counts(prediction, boxes, boundary_margin=0),
                         {'tp': 1, 'fp': 0, 'fn': 0})

    def test_ignored_overlap_cannot_steal_interior_match(self):
        # Independent instances may overlap geometrically. The interior one
        # must remain scoreable even if its box is within an excluded box.
        boxes = [[-1, 2, 12, 10], [2, 3, 8, 8]]
        self.assertEqual(head_counts(self.mask([boxes[1]]), boxes, boundary_margin=0),
                         {'tp': 1, 'fp': 0, 'fn': 0})

    def test_positive_margin(self):
        boxes = [[1, 3, 5, 8], [12, 3, 18, 8]]
        self.assertEqual(head_counts(self.mask(boxes), boxes, boundary_margin=2),
                         {'tp': 1, 'fp': 0, 'fn': 0})

    def test_inputs_are_unchanged(self):
        boxes = np.asarray([[2., 2., 7., 7.]])
        prediction = self.mask(boxes.astype(int))
        original = prediction.copy()
        head_counts(prediction, boxes, boundary_margin=0)
        np.testing.assert_array_equal(prediction, original)
        np.testing.assert_array_equal(boxes, [[2., 2., 7., 7.]])

    def test_matching_prediction_may_cross_artificial_region_boundary(self):
        # Original synthetic note: GT ends just inside the scored region;
        # one extra predicted row must not turn a .944-IoU match into a miss.
        prediction = self.mask([[227, 238, 248, 256]], shape=(320, 320))
        self.assertEqual(head_counts(prediction, [[227, 238, 248, 255]], boundary_margin=64),
                         {'tp': 1, 'fp': 0, 'fn': 0})

    def test_unmatched_boundary_noise_is_still_excluded(self):
        boxes = [[100, 100, 116, 112]]
        prediction = self.mask(boxes + [[52, 90, 67, 100], [150, 250, 165, 265]],
                               shape=(320, 320))
        self.assertEqual(head_counts(prediction, boxes, boundary_margin=64),
                         {'tp': 1, 'fp': 0, 'fn': 0})

    def test_invalid_inputs(self):
        for prediction in (np.zeros((2, 3, 1)), np.zeros((0, 3))):
            with self.assertRaises(ValueError):
                head_counts(prediction, [])
        for boxes in ([2, 2, 7, 7], [[2, 2, 2, 4]], [[0, 0, np.nan, 3]]):
            with self.assertRaises(ValueError):
                head_counts(self.mask([]), boxes)
        for threshold in (0, -1, 1.01, np.nan):
            with self.assertRaises(ValueError):
                head_counts(self.mask([]), [], iou_threshold=threshold)
        for area in (0, -1, 2.5, True):
            with self.assertRaises(ValueError):
                head_counts(self.mask([]), [], min_area=area)
        for margin in (-1, np.nan, 12):
            with self.assertRaises(ValueError):
                head_counts(self.mask([]), [], boundary_margin=margin)


class MaximumMatchingTest(unittest.TestCase):
    def test_greedy_trap(self):
        truth = [[0, 0, 10, 10], [3, 0, 13, 10]]
        predicted = [[1, 0, 11, 10], [-2, 0, 8, 10]]
        # Greedy takes (0,0), leaving truth 1 unmatched. Augmentation replaces
        # it with (0,1) and (1,0), the two legal matches.
        self.assertEqual(match_boxes(truth, predicted), [(0, 1), (1, 0)])

    def test_count_invariant_under_input_order(self):
        truth = [[0, 0, 10, 10], [3, 0, 13, 10], [30, 0, 40, 10]]
        predicted = [[1, 0, 11, 10], [-2, 0, 8, 10], [30, 0, 40, 10]]
        for a in itertools.permutations(truth):
            for b in itertools.permutations(predicted):
                self.assertEqual(len(match_boxes(a, b)), 3)

    def test_inclusive_threshold_and_exclusive_box_coordinates(self):
        self.assertEqual(match_boxes([[0, 0, 4, 4]], [[0, 0, 8, 4]]), [(0, 0)])
        self.assertEqual(match_boxes([[0, 0, 4, 4]], [[0, 0, 8.01, 4]]), [])
        self.assertEqual(match_boxes([[0, 0, 4, 4]], [[4, 0, 8, 4]]), [])

    def test_empty_sides(self):
        self.assertEqual(match_boxes([], [[0, 0, 4, 4]]), [])
        self.assertEqual(match_boxes([[0, 0, 4, 4]], []), [])

    def test_seeded_small_cases_against_exhaustive_search(self):
        rng = np.random.default_rng(240926)
        for _ in range(200):
            boxes = []
            for count in (int(rng.integers(1, 6)), int(rng.integers(1, 6))):
                origin = rng.integers(0, 8, size=(count, 2))
                end = origin + rng.integers(4, 12, size=(count, 2))
                boxes.append(np.column_stack((origin, end)))
            truth, predicted = boxes
            eligible = []
            # Independent scalar IoU and exhaustive assignment oracle.
            for a in truth:
                row = []
                for j, b in enumerate(predicted):
                    intersection = max(0, min(a[2], b[2]) - max(a[0], b[0])) * max(
                        0, min(a[3], b[3]) - max(a[1], b[1]))
                    union = ((a[2]-a[0]) * (a[3]-a[1]) +
                             (b[2]-b[0]) * (b[3]-b[1]) - intersection)
                    if intersection / union >= .5:
                        row.append(j)
                eligible.append(row)

            def exhaustive(i, used):
                if i == len(truth):
                    return 0
                best = exhaustive(i + 1, used)
                for j in eligible[i]:
                    if j not in used:
                        best = max(best, 1 + exhaustive(i + 1, used | {j}))
                return best

            pairs = match_boxes(truth, predicted)
            self.assertEqual(len(pairs), exhaustive(0, set()))
            self.assertEqual(len({i for i, _ in pairs}), len(pairs))
            self.assertEqual(len({j for _, j in pairs}), len(pairs))
            self.assertTrue(all(j in eligible[i] for i, j in pairs))


if __name__ == '__main__':
    unittest.main()
