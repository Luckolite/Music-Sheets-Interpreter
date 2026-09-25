# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Original-only, paired scan augmentation. No score-specific geometry or pixels."""
import functools
import hashlib
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

from book_geometry import BookGeometry, centre_heatmap, warp_book

STRATA = ('clean', 'warp', 'faint', 'shadow', 'thin', 'combined')


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def corpus_records(specs):
    """Verify train/development payloads; never open sealed-test pixel files.

    Splits are by complete exercise seed, across corpora as well as within one.
    A reused seed must always remain in the same split. Provenance assertions
    are explicit exceptions, so python -O cannot disable them.
    """
    pools, manifests, seen = {}, [], {}
    for spec in specs:
        root = Path(spec['root']).resolve()
        digest = sha(root / 'manifest.json')
        if digest != spec['manifest_sha256']:
            raise ValueError('Corpus manifest changed: ' + spec['name'])
        manifest = json.loads((root / 'manifest.json').read_text(encoding='utf-8'))
        if manifest.get('third_party_score_sources') != [] or manifest.get('pretrained_model_sources') != []:
            raise ValueError('Only original renderer-labelled exercises are allowed')
        local_seen, splits = set(), {'train': [], 'validation': []}
        counts = {'train': 0, 'validation': 0, 'test': 0}
        for exercise in manifest['exercises']:
            seed, split = exercise['seed'], exercise['split']
            if split not in counts or seed in local_seen or exercise.get('pretrained_labels') is not False:
                raise ValueError('Invalid exercise provenance/split')
            local_seen.add(seed)
            if seed in seen and seen[seed] != split:
                raise ValueError('Exercise family crosses splits')
            seen[seed] = split
            counts[split] += len(exercise['pages'])
            if split == 'test':
                continue
            for page in exercise['pages']:
                for field in ('image', 'labels', 'categories'):
                    if field not in page:
                        if field == 'categories':
                            continue
                        raise ValueError('Missing renderer payload')
                    item = (root / page[field]).resolve()
                    if not item.is_relative_to(root) or sha(item) != page[field + '_sha256']:
                        raise ValueError('Corpus payload failed containment/hash check')
                height, width = page['shape']
                for head in page['noteheads']:
                    left, top, right, bottom = head['box']
                    if not (0 <= left < right <= width and 0 <= top < bottom <= height
                            and right-left <= 80 and bottom-top <= 60):
                        raise ValueError('Invalid renderer head box')
                splits[split].append(dict(page, root=str(root), seed=seed, corpus=spec['name']))
        if not splits['train']:
            raise ValueError('Empty training corpus')
        pools[spec['name']] = splits
        manifests.append(dict(name=spec['name'], manifest_sha256=digest, pages=counts,
                              test_pixels_opened=False, third_party_score_sources=[]))
    return pools, manifests


@functools.lru_cache(maxsize=12)
def page_arrays(root, image, labels, categories):
    a = np.asarray(Image.open(Path(root) / image).convert('L'))
    b = np.asarray(Image.open(Path(root) / labels))
    c = np.asarray(Image.open(Path(root) / categories)) if categories else np.zeros(b.shape, np.uint8)
    if a.shape != b.shape or b.shape != c.shape or not np.isin(b, range(6)).all():
        raise ValueError('Invalid semantic mask')
    return a, b, c


def crop(page, rng, scale=False):
    a, b, c = page_arrays(page['root'], page['image'], page['labels'], page.get('categories', ''))
    side = min(int(rng.uniform(250, 430)) if scale else 320, *a.shape)
    categories = [v for v in range(3, 17) if page.get('category_pixels', [0]*17)[v] > 0]
    chosen = int(rng.choice(categories)) if categories and rng.random() < .75 else None
    points = np.column_stack(np.nonzero(c == chosen if chosen else ((b > 0) & (b != 4))))
    if len(points) and rng.random() < .9:
        cy, cx = points[int(rng.integers(len(points)))]
        x, y = int(np.clip(cx-side//2, 0, a.shape[1]-side)), int(np.clip(cy-side//2, 0, a.shape[0]-side))
    else:
        x, y = int(rng.integers(a.shape[1]-side+1)), int(rng.integers(a.shape[0]-side+1))
    aa = cv2.resize(a[y:y+side, x:x+side], (320, 320), interpolation=cv2.INTER_AREA).astype(np.float32)
    bb = cv2.resize(b[y:y+side, x:x+side], (320, 320), interpolation=cv2.INTER_NEAREST)
    cc = cv2.resize(c[y:y+side, x:x+side], (320, 320), interpolation=cv2.INTER_NEAREST)
    centres, boxes = [], []
    for head in page['noteheads']:
        left, top, right, bottom = head['box']
        left, right = (left-x)*320/side, (right-x)*320/side
        top, bottom = (top-y)*320/side, (bottom-y)*320/side
        cx, cy = (left+right)/2, (top+bottom)/2
        if 0 <= cx < 320 and 0 <= cy < 320:
            centres.append([cx, cy, max(1., min(right-left, bottom-top)/5)])
            boxes.append([left, top, right, bottom])
    origin = dict(corpus=page['corpus'], exercise_seed=page['seed'], image=page['image'],
                  crop=[x, y, side], category=chosen)
    return aa, bb, cc, centres, boxes, origin


def add_clutter(a, b, c, rng):
    """Procedural marginal marks; never overwrite the original supervised ink."""
    a, b, c = a.copy(), b.copy(), c.copy()
    ink = np.zeros(a.shape, np.uint8)
    for _ in range(int(rng.integers(1, 5))):
        x, y = int(rng.integers(5, 280)), int(rng.integers(25, 300))
        if rng.random() < .6:
            cv2.putText(ink, str(int(rng.integers(1, 100))), (x, y), cv2.FONT_HERSHEY_SCRIPT_SIMPLEX,
                        float(rng.uniform(.35, .65)), 255, 1, cv2.LINE_AA)
        else:
            cv2.ellipse(ink, (x, y), (int(rng.integers(12, 45)), int(rng.integers(3, 10))),
                        float(rng.uniform(-20, 20)), 185, 350, 255, 1, cv2.LINE_AA)
    # Keep a two-pixel buffer around real heads/stems/symbols: no destructive
    # label conflicts, no hidden targets, and no invented note instances.
    protected = cv2.dilate((b > 0).astype(np.uint8), np.ones((5, 5), np.uint8)) > 0
    ink[protected] = 0
    alpha = ink.astype(np.float32)/255
    a = a*(1-alpha) + float(rng.uniform(25, 125))*alpha
    b[ink >= 128] = 5
    c[ink >= 128] = 16
    return a, b, c


def appearance(a, rng, stratum):
    """Change appearance, not semantic identity. Ink always keeps contrast.

    Thin strokes are attenuated, not fully erased; completely invisible notes
    would train hallucination. Bleed-through is faint background-only texture.
    """
    if stratum in ('clean', 'warp'):
        return a.astype(np.float32, copy=True)
    h, w = a.shape
    yy, xx = np.mgrid[:h, :w].astype(np.float32)
    paper = np.full(a.shape, float(rng.uniform(229, 253)), np.float32)
    strength = np.full(a.shape, float(rng.uniform(.42, .88)), np.float32)
    if stratum in ('shadow', 'combined'):
        x0 = float(rng.uniform(-.2*w, 1.2*w))
        shade = np.exp(-((xx-x0)/float(rng.uniform(.15*w, .55*w)))**2)
        paper -= float(rng.uniform(25, 75))*shade
        paper += np.linspace(-8, 8, h, dtype=np.float32)[:, None]
    if stratum in ('faint', 'combined'):
        strength *= np.linspace(float(rng.uniform(.48, .7)), 1., w, dtype=np.float32)[None]
    if stratum in ('thin', 'combined'):
        distance = cv2.distanceTransform((a < 180).astype(np.uint8), cv2.DIST_L2, 3)
        weak = (distance > 0) & (distance < 1.5)
        # Local, mildly interrupted print, with >=18% of original contrast.
        pattern = .5 + .5*np.sin(xx/float(rng.uniform(3, 9)) + yy/float(rng.uniform(2, 6)))
        strength[weak] *= (.45 + .45*pattern[weak])
    contrast = (255-a)/255
    result = paper - contrast * (paper-15) * np.maximum(.18, strength)
    if stratum in ('faint', 'combined'):
        result = cv2.GaussianBlur(result, (5, 5), float(rng.uniform(.35, .9)))
    texture = cv2.resize(rng.normal(0, 2, (24, 24)).astype(np.float32), (w, h))
    result += texture + rng.normal(0, float(rng.uniform(.3, 1.8)), a.shape)
    return np.clip(result, 0, 255).astype(np.float32)


def transform(a, b, c, centres, boxes, rng, stratum, clutter=False):
    if clutter:
        a, b, c = add_clutter(a, b, c, rng)
    if stratum in ('warp', 'combined'):
        geometry = BookGeometry.sample(rng)
        shape = a.shape
        a, masks, centres, _ = warp_book(a, [b, c], centres, geometry)
        b, c = masks
        moved = []
        for left, top, right, bottom in boxes:
            # Sample all edges, not only corners, for a curved box envelope.
            x = np.concatenate([np.linspace(left, right, 9)]*2 + [np.full(9, left), np.full(9, right)])
            y = np.concatenate([np.full(9, top), np.full(9, bottom)] + [np.linspace(top, bottom, 9)]*2)
            x, y = geometry.forward(x, y, shape)
            moved.append([float(x.min()), float(y.min()), float(x.max()), float(y.max())])
        boxes = moved
    return appearance(a, rng, stratum), b, c, centre_heatmap(centres, a.shape), boxes


def blank(rng):
    a = appearance(np.full((320, 320), 255, np.float32), rng, 'shadow')
    b = np.zeros(a.shape, np.uint8)
    a, b, c = add_clutter(a, b, b.copy(), rng)
    # Small faint scanner flecks retain a background label, unlike annotations.
    for _ in range(16):
        x, y = rng.integers(0, 320, 2)
        if b[y, x] == 0:
            a[y, x] = max(150., float(a[y, x])-35.)
    return a, b, c, np.zeros(a.shape, np.float32), []


def training_sample(pools, specs, seed):
    rng = np.random.default_rng(seed)
    if rng.random() < .06:
        return blank(rng)
    weights = np.asarray([s['weight'] for s in specs], dtype=float)
    spec = specs[int(rng.choice(len(specs), p=weights/weights.sum()))]
    pool = pools[spec['name']]['train']
    page = pool[int(rng.integers(len(pool)))]
    stratum = str(rng.choice(STRATA, p=[.35, .18, .15, .10, .10, .12]))
    a, b, c, centres, boxes, _ = crop(page, rng, scale=stratum != 'clean')
    return transform(a, b, c, centres, boxes, rng, stratum,
                     clutter=stratum != 'clean' and rng.random() < .35)
