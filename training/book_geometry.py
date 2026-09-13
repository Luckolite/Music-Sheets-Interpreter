# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Paired book-scan geometry for original training images and their labels."""
from dataclasses import dataclass

import cv2
import numpy as np


@dataclass(frozen=True)
class BookGeometry:
    slope: float = 0.
    bend: float = 0.
    taper: float = 0.
    curl: float = 0.
    gutter: float = -1.
    horizontal_scale: float = 1.

    @classmethod
    def sample(cls, rng):
        return cls(slope=float(np.tan(np.deg2rad(rng.uniform(-7, 7)))),
                   bend=float(rng.uniform(-12, 12)), taper=float(rng.uniform(-.12, .12)),
                   curl=float(rng.uniform(-10, 10)), gutter=float(rng.choice([-1., 1.])),
                   horizontal_scale=float(rng.uniform(.9, 1.05)))

    def _vertical(self, x, shape):
        height, width = shape
        u = (np.asarray(x) - (width - 1) / 2) / ((width - 1) / 2)
        scale = 1 + self.taper * u
        offset = (self.slope * (np.asarray(x) - (width - 1) / 2)
                  + self.bend * (u * u - 1 / 3)
                  + self.curl * np.exp(-((u - self.gutter) / .45) ** 2))
        return scale, offset

    def forward(self, x, y, shape):
        height, width = shape
        scale, offset = self._vertical(x, shape)
        return ((np.asarray(x) - (width - 1) / 2) * self.horizontal_scale + (width - 1) / 2,
                (np.asarray(y) - (height - 1) / 2) * scale + (height - 1) / 2 + offset)

    def inverse_maps(self, shape):
        height, width = shape
        if min(shape) < 2 or self.horizontal_scale <= 0 or abs(self.taper) >= .5:
            raise ValueError('Book geometry must preserve page orientation and positive staff spacing')
        yy, xx = np.mgrid[:height, :width].astype(np.float32)
        source_x = (xx - (width - 1) / 2) / self.horizontal_scale + (width - 1) / 2
        scale, offset = self._vertical(source_x, shape)
        source_y = (yy - (height - 1) / 2 - offset) / scale + (height - 1) / 2
        return source_x.astype(np.float32), source_y.astype(np.float32)


def centre_heatmap(centres, shape):
    """Rebuild exact unit peaks; interpolating a heatmap loses focal-loss positives."""
    height, width = shape
    heat = np.zeros(shape, np.float32)
    for cx, cy, sigma, *_ in centres:
        ix, iy = int(round(cx)), int(round(cy))
        if not (0 <= ix < width and 0 <= iy < height):
            continue
        radius = int(np.ceil(3 * sigma))
        left, right = max(0, ix - radius), min(width, ix + radius + 1)
        top, bottom = max(0, iy - radius), min(height, iy + radius + 1)
        yy, xx = np.mgrid[top:bottom, left:right]
        heat[top:bottom, left:right] = np.maximum(heat[top:bottom, left:right],
            np.exp(-((xx - ix) ** 2 + (yy - iy) ** 2) / (2 * sigma ** 2)))
    return heat


def warp_book(image, masks, centres, geometry):
    """Warp pixels, categorical masks and note centres using the same invertible map.

    Centres are [x, y, sigma, ...metadata]. Masks retain their discrete classes.
    The analytic bend and gutter curl approximate local book curvature; they are
    training augmentation, not a claim to physically reconstruct a book surface.
    """
    shape = image.shape
    if any(mask.shape != shape for mask in masks):
        raise ValueError('Image and label geometry must match')
    maps = geometry.inverse_maps(shape)
    warped = cv2.remap(image.astype(np.float32), *maps, cv2.INTER_LINEAR,
                       borderMode=cv2.BORDER_CONSTANT, borderValue=255)
    labels = [cv2.remap(mask, *maps, cv2.INTER_NEAREST,
                       borderMode=cv2.BORDER_CONSTANT, borderValue=0) for mask in masks]
    moved = []
    for cx, cy, sigma, *metadata in centres:
        x, y = geometry.forward(cx, cy, shape)
        if 0 <= x < shape[1] and 0 <= y < shape[0]:
            vertical_scale, _ = geometry._vertical(cx, shape)
            moved.append([float(x), float(y), max(1., sigma * min(geometry.horizontal_scale,
                                                                 float(vertical_scale))), *metadata])
    return warped, labels, moved, centre_heatmap(moved, shape)
