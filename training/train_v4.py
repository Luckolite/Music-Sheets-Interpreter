# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Fine-tune v4 on an original, renderer-labelled corpus.

This is a portable fine-tuning entry point, not a replay of the historical
multi-corpus training run. Only train pages update weights; validation selects
the checkpoint. Test pages are not evaluated by this command.
"""
import argparse
import hashlib
import json
from pathlib import Path

import cv2
import numpy as np
import torch
from torch.nn import functional as F

from model_v4 import Segmenter, export_arrays
from train import read_corpus, load_page, scores


def crop(root, page, rng, augment):
    gray, labels, points = load_page(str(root), page['image'], page['labels'])
    side = min(int(rng.uniform(250, 430)) if augment else 320, *gray.shape)
    if len(points) and rng.random() < .9:
        cy, cx = points[int(rng.integers(len(points)))]; jitter = side // 4 if augment else 0
        cx += int(rng.integers(-jitter, jitter + 1)); cy += int(rng.integers(-jitter, jitter + 1))
        x = int(np.clip(cx - side // 2, 0, gray.shape[1] - side))
        y = int(np.clip(cy - side // 2, 0, gray.shape[0] - side))
    else:
        x = int(rng.integers(gray.shape[1] - side + 1)); y = int(rng.integers(gray.shape[0] - side + 1))
    a = cv2.resize(gray[y:y+side, x:x+side], (320, 320), interpolation=cv2.INTER_AREA).astype(np.float32)
    b = cv2.resize(labels[y:y+side, x:x+side], (320, 320), interpolation=cv2.INTER_NEAREST)
    affine = cv2.getRotationMatrix2D((160, 160), float(rng.uniform(-2, 2)) if augment and rng.random() < .3 else 0, 1.)
    heat = np.zeros((320, 320), np.float32)
    for head in page['noteheads']:
        left, top, right, bottom = head['box']; scale = 320 / side
        cx, cy = affine @ np.array([((left + right) / 2 - x) * scale, ((top + bottom) / 2 - y) * scale, 1.])
        if not (0 <= cx < 320 and 0 <= cy < 320): continue
        sigma = max(1., min(right-left, bottom-top) * scale / 5); ix, iy = min(319, max(0, int(round(cx)))), min(319, max(0, int(round(cy))))
        radius = int(np.ceil(3 * sigma)); l, r = max(0, ix-radius), min(320, ix+radius+1)
        t, bot = max(0, iy-radius), min(320, iy+radius+1)
        yy, xx = np.mgrid[t:bot, l:r]
        heat[t:bot, l:r] = np.maximum(heat[t:bot, l:r], np.exp(-((xx-ix)**2+(yy-iy)**2)/(2*sigma*sigma)))
    if augment:
        a = cv2.warpAffine(a, affine, (320, 320), flags=cv2.INTER_LINEAR, borderValue=255)
        b = cv2.warpAffine(b, affine, (320, 320), flags=cv2.INTER_NEAREST, borderValue=0)
        if rng.random() < .5: a = cv2.GaussianBlur(a, (5, 5), float(rng.uniform(.25, 1.4)))
        black, white = float(rng.uniform(0, 120)), float(rng.uniform(210, 255))
        a = black + (white-black)*a/255 + rng.normal(0, float(rng.uniform(0, 2.5)), a.shape)
    return np.clip(a, 0, 255).astype(np.float32), b.astype(np.int64), heat


def losses(logits, centres, labels, heat):
    weights = logits.new_tensor([.2, 2., 3., 4., 1., 2.])
    ce = F.cross_entropy(logits, labels, weight=weights)
    probabilities = logits.float().softmax(1); truth = F.one_hot(labels, 6).permute(0, 3, 1, 2).float()
    dice = 1 - (2*(probabilities*truth).sum((0, 2, 3))+1)/((probabilities+truth).sum((0, 2, 3))+1)
    p = centres[:, 0].float().sigmoid().clamp(1e-5, 1-1e-5); positive = (heat == 1).float()
    focal = (-torch.log(p)*(1-p).square()*positive-torch.log1p(-p)*p.square()*(1-heat).pow(4)*(heat < 1)).sum()/positive.sum().clamp_min(1)
    return ce + .4*dice[1:].mean() + .25*focal


def validate(model, root, pages, device):
    model.eval(); rng = np.random.default_rng(614); conf = np.zeros((6, 6), np.int64)
    with torch.inference_mode():
        for page in pages:
            for _ in range(4):
                a, b, _ = crop(root, page, rng, False)
                out = model(torch.from_numpy(a[None, None]).to(device)).argmax(1)[0].cpu().numpy()
                conf += np.bincount((b*6+out).ravel(), minlength=36).reshape(6, 6)
    model.train(); result = scores(conf); result['scope'] = 'Four fixed crops per original validation page; semantic pixel metrics, not instance or pitch accuracy.'
    return result


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--corpus', type=Path, required=True); ap.add_argument('--checkpoint', type=Path, required=True)
    ap.add_argument('--out', type=Path, required=True); ap.add_argument('--steps', type=int, default=3000)
    ap.add_argument('--batch', type=int, default=8); ap.add_argument('--lr', type=float, default=2e-5)
    ap.add_argument('--validate-every', type=int, default=500); ap.add_argument('--seed', type=int, default=20260912)
    ap.add_argument('--device', default='cuda' if torch.cuda.is_available() else 'cpu')
    args = ap.parse_args(); assert args.steps > 0 and args.batch > 0 and args.validate_every > 0 and args.lr > 0
    args.out.mkdir(parents=True, exist_ok=False); cv2.setNumThreads(1); torch.set_num_threads(2); torch.manual_seed(args.seed)
    _, splits = read_corpus(args.corpus); rng = np.random.default_rng(args.seed)
    saved = torch.load(args.checkpoint, map_location='cpu', weights_only=True)
    model = Segmenter(); assert saved['architecture_config'] == model.config
    model.load_state_dict(saved['state_dict']); model.to(args.device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-5)
    best = -1.; history = []; parent_sha = hashlib.sha256(args.checkpoint.read_bytes()).hexdigest()
    corpus_sha = hashlib.sha256((args.corpus/'manifest.json').read_bytes()).hexdigest()
    for step in range(1, args.steps+1):
        batch = [crop(args.corpus, splits['train'][int(rng.integers(len(splits['train'])))], rng, True) for _ in range(args.batch)]
        x = torch.from_numpy(np.stack([b[0] for b in batch])[:, None]).to(args.device)
        y = torch.from_numpy(np.stack([b[1] for b in batch])).to(args.device)
        h = torch.from_numpy(np.stack([b[2] for b in batch])).to(args.device)
        optimizer.zero_grad(set_to_none=True); logits, centres = model.forward_with_centres(x)
        loss = losses(logits, centres, y, h); loss.backward(); torch.nn.utils.clip_grad_norm_(model.parameters(), 5.); optimizer.step()
        if step % args.validate_every == 0 or step == args.steps:
            report = validate(model, args.corpus, splits['validation'], args.device)
            history.append(dict(step=step, loss=float(loss.detach()), validation=report)); print(json.dumps(history[-1]), flush=True)
            if report['foreground_mean_iou'] > best:
                best = report['foreground_mean_iou']
                torch.save(dict(architecture=model.config['architecture'], architecture_config=model.config,
                    state_dict={k:v.detach().cpu() for k,v in model.state_dict().items()}, step=step, seed=args.seed,
                    initialization=dict(type='fine_tuned_own_model', parent_checkpoint_sha256=parent_sha),
                    corpus_sha256=corpus_sha, pretrained_model_sources=[]), args.out/'best.pt')
    selected = torch.load(args.out/'best.pt', map_location='cpu', weights_only=True); model.load_state_dict(selected['state_dict'])
    samples = np.stack([crop(args.corpus, p, np.random.default_rng(614+i), False)[0] for i,p in enumerate(splits['train'][:32])])[:, None].repeat(3, axis=1)
    export_arrays(model, args.out/'weights.npz', samples)
    (args.out/'training.json').write_text(json.dumps(dict(parent_checkpoint_sha256=parent_sha, corpus_sha256=corpus_sha,
        selected_step=selected['step'], steps=args.steps, history=history, test_evaluated=False,
        reproduction_scope='Portable fine-tuning recipe; not the historical v4 multi-corpus training schedule.'), indent=2)+'\n', encoding='utf-8')


if __name__ == '__main__': main()
