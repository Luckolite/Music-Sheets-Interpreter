# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Clean/degraded pairs with shared geometry and original TRAIN-only gap replay."""
import functools

import numpy as np

from book_geometry import centre_heatmap
from hard_scan_data import add_clutter, appearance, blank, crop, transform


@functools.lru_cache(maxsize=64)
def separated_neighbours(boxes):
    """Find nearby, genuinely separate head boxes without song-specific rules.

    True touching/overlapping chord glyphs are not split into invented targets.
    This only chooses TRAIN crop centres; it never changes labels or evaluation.
    """
    boxes = np.asarray(boxes, dtype=np.float32)
    if len(boxes) < 2:
        return ()
    left, top, right, bottom = boxes.T
    width, height = right-left, bottom-top
    cx, cy = (left+right)/2, (top+bottom)/2
    horizontal_gap = left[None, :] - right[:, None]
    vertical_gap = top[None, :] - bottom[:, None]
    horizontal = ((horizontal_gap >= 1) & (horizontal_gap <= np.minimum(width[:, None], width[None, :])*.5)
                  & (np.abs(cy[:, None]-cy[None, :]) <= np.minimum(height[:, None], height[None, :])*.5))
    vertical = ((vertical_gap >= 1) & (vertical_gap <= np.minimum(height[:, None], height[None, :])*.5)
                & (np.abs(cx[:, None]-cx[None, :]) <= np.minimum(width[:, None], width[None, :])*.5))
    rows, cols = np.nonzero(horizontal | vertical)
    return tuple((float((cx[i]+cx[j])/2), float((cy[i]+cy[j])/2)) for i, j in zip(rows, cols))


def paired_views(a, b, c, centres, boxes, rng, stratum, clutter=False):
    """Return two students, matching labels/heats, and aligned clear teachers.

    View0 is original clean notation. View1 can bend/tilt and degrade, but its
    teacher has the identical geometry and annotations before fading/shadow.
    Teacher predictions must never replace renderer targets in the loss.
    """
    clean = a.copy()
    original_labels = b.copy()
    clean_heat = centre_heatmap(centres, a.shape)
    if clutter:
        a, b, c = add_clutter(a, b, c, rng)
    geometry_stratum = 'warp' if stratum in ('warp', 'combined') else 'clean'
    reference, labels, _, heat, _ = transform(a, b, c, centres, boxes, rng, geometry_stratum)
    degraded = appearance(reference, rng, stratum)
    return (np.stack([clean, degraded])[:, None], np.stack([original_labels, labels]),
            np.stack([clean_heat, heat]), np.stack([clean, reference])[:, None])


def paired_training_sample(pools, specs, seed):
    rng = np.random.default_rng(seed)
    if rng.random() < .06:
        a, b, _, heat, _ = blank(rng)
        return (np.stack([a, a])[:, None], np.stack([b, b]), np.stack([heat, heat]),
                np.stack([a, a])[:, None])
    weights = np.asarray([spec['weight'] for spec in specs], dtype=float)
    spec = specs[int(rng.choice(len(specs), p=weights/weights.sum()))]
    pool = pools[spec['name']]['train']
    page = pool[int(rng.integers(len(pool)))]
    focus = None
    if rng.random() < .5:
        pairs = separated_neighbours(tuple(tuple(head['box']) for head in page['noteheads']))
        if pairs:
            focus = pairs[int(rng.integers(len(pairs)))]
    # Half the clean views keep the exact original320px raster, half add scale
    # diversity. No validation family participates in updates or crop mining.
    a, b, c, centres, boxes, _ = crop(page, rng, scale=rng.random() < .5, focus=focus)
    stratum = str(rng.choice(['warp', 'faint', 'shadow', 'thin', 'combined'], p=[.15, .25, .15, .20, .25]))
    return paired_views(a, b, c, centres, boxes, rng, stratum, clutter=rng.random() < .35)
