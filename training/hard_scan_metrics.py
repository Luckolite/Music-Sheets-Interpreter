# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""CPU-only synthetic notehead detection metrics.

Renderer and component boxes use (left, top, right, bottom), with exclusive
right/bottom coordinates. Predictions are six-class semantic masks; class 2
is the notehead class. Each retained 8-connected component is one prediction,
so split and merged heads are not silently repaired by this evaluator.

These are box-detection metrics, not pitch, duration, rest-instance, or symbol
recognition metrics. Evaluate the truth semantic mask with the same boxes as
an additional component reference: even true masks can contain touching heads.
"""
from collections import deque

import cv2
import numpy as np


def _boxes(value, name):
    result = np.asarray(value, dtype=np.float64)
    if result.size == 0:
        if result.shape not in ((0,), (0, 4)):
            raise ValueError(name + ' must have shape (N, 4)')
        return result.reshape(0, 4)
    if result.ndim != 2 or result.shape[1] != 4:
        raise ValueError(name + ' must have shape (N, 4)')
    if not np.isfinite(result).all() or np.any(result[:, 2:] <= result[:, :2]):
        raise ValueError(name + ' must contain finite, positive-area boxes')
    return result


def _threshold(value):
    if not np.isscalar(value) or not np.isfinite(value) or not 0 < value <= 1:
        raise ValueError('iou_threshold must be finite and in (0, 1]')
    return float(value)


def _overlap(a, b):
    return np.maximum(0., np.minimum(a[:, None, 2:], b[None, :, 2:]) -
                      np.maximum(a[:, None, :2], b[None, :, :2])).prod(axis=2)


def _ious(a, b):
    intersection = _overlap(a, b)
    return intersection / ((a[:, 2:] - a[:, :2]).prod(axis=1)[:, None] +
                           (b[:, 2:] - b[:, :2]).prod(axis=1)[None, :] - intersection)


def match_boxes(truth_boxes, predicted_boxes, *, iou_threshold=.5):
    """Return maximum-cardinality (truth index, prediction index) matches.

    Every pair meets the IoU threshold and every index appears at most once.
    Higher IoU edges are visited first for deterministic tie breaking, but the
    objective is maximum match count, not maximum sum of IoUs. Augmenting paths
    prevent a locally best pair from blocking two valid one-to-one matches.
    The iterative search does not depend on Python's recursion limit.
    """
    threshold = _threshold(iou_threshold)
    a, b = _boxes(truth_boxes, 'truth_boxes'), _boxes(predicted_boxes, 'predicted_boxes')
    ious = _ious(a, b)
    adjacency = []
    for row in ious:
        eligible = np.flatnonzero(row >= threshold)
        adjacency.append(sorted(eligible.tolist(), key=lambda j: (-row[j], j)))
    truth_match = [-1] * len(a)
    prediction_match = [-1] * len(b)
    for root in range(len(a)):
        queue = deque([root])
        seen_truth = {root}
        parent_prediction = {}
        free_prediction = -1
        while queue and free_prediction < 0:
            truth = queue.popleft()
            for prediction in adjacency[truth]:
                if prediction in parent_prediction:
                    continue
                parent_prediction[prediction] = truth
                owner = prediction_match[prediction]
                if owner < 0:
                    free_prediction = prediction
                    break
                if owner not in seen_truth:
                    seen_truth.add(owner)
                    queue.append(owner)
        while free_prediction >= 0:
            truth = parent_prediction[free_prediction]
            previous_prediction = truth_match[truth]
            truth_match[truth] = free_prediction
            prediction_match[free_prediction] = truth
            free_prediction = previous_prediction
    return [(i, j) for i, j in enumerate(truth_match) if j >= 0]


def head_counts(prediction2d, boxes, *, min_area=3, iou_threshold=.5,
                boundary_margin=None):
    """Return integer ``tp``, ``fp``, ``fn`` for class-2 notehead components.

    By default all renderer boxes and predictions are scored. Set
    ``boundary_margin`` to a nonnegative pixel distance to ignore boxes touching
    or crossing that distance from the image edge (0 means the image edge).
    A predicted fragment at least half inside an ignored truth box is also
    ignored unless it can match a retained truth box. This accommodates an
    eroded clipped head without letting it claim an interior target.

    Boundary exclusion must be fixed identically for parent and candidate and
    must not be used for full-page retention claims. Inputs are not modified.
    """
    prediction = np.asarray(prediction2d)
    if prediction.ndim != 2 or not all(prediction.shape):
        raise ValueError('prediction2d must be a nonempty two-dimensional mask')
    if not isinstance(min_area, (int, np.integer)) or isinstance(min_area, bool) or min_area < 1:
        raise ValueError('min_area must be a positive integer')
    threshold = _threshold(iou_threshold)
    truth = _boxes(boxes, 'boxes')
    _, _, stats, _ = cv2.connectedComponentsWithStats(
        (prediction == 2).astype(np.uint8), connectivity=8)
    components = stats[1:]
    components = components[components[:, cv2.CC_STAT_AREA] >= min_area]
    predicted = np.column_stack((components[:, 0], components[:, 1],
                                 components[:, 0] + components[:, 2],
                                 components[:, 1] + components[:, 3])).astype(np.float64)
    if boundary_margin is not None:
        if (not np.isscalar(boundary_margin) or not np.isfinite(boundary_margin)
                or boundary_margin < 0):
            raise ValueError('boundary_margin must be None or finite and nonnegative')
        height, width = prediction.shape
        if 2 * boundary_margin >= min(height, width):
            raise ValueError('boundary_margin must leave a nonempty image interior')

        def interior(candidate):
            return ((candidate[:, 0] > boundary_margin) &
                    (candidate[:, 1] > boundary_margin) &
                    (candidate[:, 2] < width - boundary_margin) &
                    (candidate[:, 3] < height - boundary_margin))

        keep_truth = interior(truth)
        ignored_truth, truth = truth[~keep_truth], truth[keep_truth]
        predicted = predicted[interior(predicted)]
        if len(ignored_truth) and len(predicted):
            area = (predicted[:, 2:] - predicted[:, :2]).prod(axis=1)
            in_ignored = (_overlap(ignored_truth, predicted) / area[None, :] >= .5).any(axis=0)
            matches_interior = (_ious(truth, predicted) >= threshold).any(axis=0)
            predicted = predicted[~in_ignored | matches_interior]
    matched = len(match_boxes(truth, predicted, iou_threshold=threshold))
    return {'tp': matched, 'fp': len(predicted) - matched, 'fn': len(truth) - matched}
