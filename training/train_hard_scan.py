# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Resumable, development-gated hard-scan fine-tuning. Never deploys weights.

Only original TRAIN pages update parameters. Fixed original validation families
provide development comparisons, NOT independent accuracy. Sealed test images
are not opened. Native decoding, source-note checks, sealed testing and export
parity remain separate mandatory promotion gates.
"""
import argparse
import json
import os
from pathlib import Path
import time

import cv2
import numpy as np
import torch

from hard_scan_data import STRATA, blank, corpus_records, crop, sha, training_sample, transform
from hard_scan_metrics import head_counts
from model_v4 import Segmenter
from train import scores
from train_v4 import losses


def write_json(path, value):
    temporary = path.with_suffix(path.suffix + '.tmp')
    temporary.write_text(json.dumps(value, indent=2, allow_nan=False) + '\n', encoding='utf-8')
    temporary.replace(path)


def save_checkpoint(path, value):
    temporary = path.with_suffix('.tmp')
    torch.save(value, temporary)
    temporary.replace(path)


class Samples(torch.utils.data.Dataset):
    def __init__(self, pools, specs, seed, start, stop):
        self.pools, self.specs, self.seed = pools, specs, seed
        self.start, self.stop = start, stop

    def __len__(self):
        return self.stop-self.start

    def __getitem__(self, index):
        cv2.setNumThreads(1)
        a, b, _, h, _ = training_sample(self.pools, self.specs, self.seed+self.start+index)
        return (torch.from_numpy(a[None]), torch.from_numpy(b.astype(np.int64)), torch.from_numpy(h))


def development(pools, per_corpus=8):
    samples, origins = [], []
    for kind in sorted(pools):
        pages = pools[kind]['validation']
        if not pages:
            continue
        # Deterministic spread across complete validation exercise families.
        indices = np.linspace(0, len(pages)-1, min(per_corpus, len(pages)), dtype=int)
        for index in indices:
            seed = 80260924 + int(pages[index]['seed'])
            a, b, c, centres, boxes, origin = crop(pages[index], np.random.default_rng(seed))
            for number, stratum in enumerate(STRATA):
                transformed = transform(a, b, c, centres, boxes, np.random.default_rng(seed+number),
                                        stratum, clutter=stratum == 'combined')
                samples.append((stratum, transformed))
                origins.append(dict(origin, stratum=stratum, augmentation_seed=seed+number))
    for index in range(24):
        samples.append(('blank', blank(np.random.default_rng(90460224+index))))
        origins.append(dict(stratum='blank', procedural_seed=90460224+index))
    return samples, origins


def evaluate(model, samples, device):
    model.eval()
    accum = {}
    with torch.inference_mode():
        for offset in range(0, len(samples), 6):
            portion = samples[offset:offset+6]
            images = np.stack([row[1][0] for row in portion])[:, None]
            predictions = model(torch.from_numpy(images).to(device)).argmax(1).cpu().numpy()
            for prediction, (stratum, (_, labels, categories, _, boxes)) in zip(predictions, portion):
                stats = accum.setdefault(stratum, dict(conf=np.zeros((6, 6), np.int64),
                    tp=0, fp=0, fn=0, crops=0, category_correct=np.zeros(17, np.int64),
                    category_total=np.zeros(17, np.int64), crop_heads=[]))
                # Exclude a fixed 64px source-crop boundary for every stratum.
                # The paired warp can move clipped edge fragments inward by
                # up to ~57px. These crop gates are not full-page evaluation.
                interior_labels = labels[64:-64, 64:-64]
                interior_prediction = prediction[64:-64, 64:-64]
                stats['conf'] += np.bincount((interior_labels.astype(np.int64)*6+interior_prediction).ravel(), minlength=36).reshape(6, 6)
                counts = head_counts(prediction, boxes, boundary_margin=64)
                stats['crop_heads'].append(counts)
                for key in ('tp', 'fp', 'fn'):
                    stats[key] += counts[key]
                stats['crops'] += 1
                for category in range(3, 17):
                    selected = categories[64:-64, 64:-64] == category
                    stats['category_total'][category] += int(selected.sum())
                    stats['category_correct'][category] += int((selected & (interior_prediction == interior_labels)).sum())
    result = {}
    for stratum, stats in accum.items():
        tp, fp, fn = (stats[k] for k in ('tp', 'fp', 'fn'))
        result[stratum] = dict(pixel=scores(stats['conf']), crops=stats['crops'],
            heads=dict(tp=tp, fp=fp, fn=fn, precision=tp/max(1, tp+fp), recall=tp/max(1, tp+fn)),
            crop_heads=stats['crop_heads'], boundary_exclusion_pixels=64,
            renderer_category_pixel_recall={str(i):dict(correct=int(stats['category_correct'][i]),
                pixels=int(stats['category_total'][i]),
                recall=float(stats['category_correct'][i]/max(1, stats['category_total'][i])))
                for i in range(3, 17) if stats['category_total'][i]})
    model.train()
    return result


def gate(report, baseline):
    """Conservative development gates; never an automatic release approval."""
    failures = []
    for stratum, old in baseline.items():
        new = report[stratum]
        if stratum == 'blank':
            if new['heads']['fp'] > old['heads']['fp']:
                failures.append('blank false heads increased')
            continue
        if stratum == 'clean':
            for index, (before, after) in enumerate(zip(old['crop_heads'], new['crop_heads'])):
                if after['tp'] < before['tp'] or after['fp'] > before['fp'] or after['fn'] > before['fn']:
                    failures.append('clean crop head retention: ' + str(index))
        for metric in ('precision', 'recall'):
            if new['heads'][metric] < old['heads'][metric] - .01:
                failures.append(stratum + ' head ' + metric)
        for name, previous in old['pixel']['per_class'].items():
            current = new['pixel']['per_class'][name]
            if previous['pixels'] and current['recall'] < previous['recall'] - .015:
                failures.append(stratum + ' semantic recall: ' + name)
            if previous['pixels'] and current['precision'] < previous['precision'] - .015:
                failures.append(stratum + ' semantic precision: ' + name)
        for category, previous in old['renderer_category_pixel_recall'].items():
            current = new['renderer_category_pixel_recall'][category]
            if previous['pixels'] >= 100 and current['recall'] < previous['recall'] - .02:
                failures.append(stratum + ' renderer-category pixel recall: ' + category)
    hard = ('warp', 'faint', 'shadow', 'thin', 'combined')
    def f1(head):
        return 2*head['tp']/max(1, 2*head['tp']+head['fp']+head['fn'])
    gain = float(np.mean([f1(report[k]['heads'])-f1(baseline[k]['heads']) for k in hard]))
    if gain < .003:
        failures.append('hard-scan head F1 gain below 0.003')
    return dict(passed=not failures, failures=failures, mean_hard_head_f1_gain=gain,
                scope='Development screening only; source, sealed, export and native gates still required')


def run(args):
    config = json.loads(args.config.read_text(encoding='utf-8'))
    out = args.out.resolve()
    if args.resume:
        if sha(out/'config.json') != sha(args.config):
            raise ValueError('Resume config differs from frozen run')
    else:
        out.mkdir(parents=True, exist_ok=False)
        (out/'config.json').write_bytes(args.config.read_bytes())
    if sha(config['checkpoint']) != config['parent_sha256']:
        raise ValueError('Parent checkpoint changed')
    dependencies = ('hard_scan_data.py', 'hard_scan_metrics.py', 'train_hard_scan.py',
                    'model.py', 'model_v4.py', 'train.py', 'train_v4.py', 'book_geometry.py')
    code_hashes = {name: sha(Path(__file__).parent/name) for name in dependencies}
    if args.resume and json.loads((out/'code-hashes.json').read_text()) != code_hashes:
        raise ValueError('Training code changed; use a new experiment')
    write_json(out/'code-hashes.json', code_hashes)
    snapshot = out/'source'
    snapshot.mkdir(exist_ok=True)
    for name in dependencies:
        if not (snapshot/name).exists():
            (snapshot/name).write_bytes((Path(__file__).parent/name).read_bytes())
    pools, provenance = corpus_records(config['corpora'])
    write_json(out/'data-provenance.json', provenance)
    cv2.setNumThreads(1)
    torch.set_num_threads(2)
    torch.manual_seed(config['seed'])
    if not torch.cuda.is_available():
        raise RuntimeError('This run requires a CUDA device; no silent CPU fallback')
    device = torch.device('cuda:0')
    model = Segmenter().to(device)
    parent = torch.load(config['checkpoint'], map_location='cpu', weights_only=True)
    if parent['architecture_config'] != model.config:
        raise ValueError('Parent architecture mismatch')
    model.load_state_dict(parent['state_dict'])
    optimizer = torch.optim.AdamW(model.parameters(), lr=config['lr'], weight_decay=1e-5)
    amp_enabled = config.get('mixed_precision', False)
    scaler = torch.cuda.amp.GradScaler(enabled=amp_enabled)
    samples, origins = development(pools, config.get('validation_pages_per_corpus', 8))
    write_json(out/'development-crops.json', origins)
    start, best = 0, -1.
    if args.resume:
        saved = torch.load(out/'last.pt', map_location='cpu', weights_only=True)
        model.load_state_dict(saved['state_dict'])
        optimizer.load_state_dict(saved['optimizer'])
        scaler.load_state_dict(saved['scaler'])
        start, best = saved['step'], saved['best_development_gain']
        if (out/'best-development.pt').exists():
            selected = torch.load(out/'best-development.pt', map_location='cpu', weights_only=True)
            best = max(best, selected['development_gain'])
        baseline = json.loads((out/'baseline-development.json').read_text())
    else:
        print(json.dumps(dict(event='baseline_started', crops=len(samples), gpu=torch.cuda.get_device_name(0))), flush=True)
        baseline = evaluate(model, samples, device)
        write_json(out/'baseline-development.json', baseline)
    write_json(out/'runtime.json', dict(pid=os.getpid(), torch=torch.__version__, numpy=np.__version__,
        opencv=cv2.__version__, gpu=torch.cuda.get_device_name(0), cuda=torch.version.cuda,
        started_utc=time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), start_step=start,
        test_evaluated=False, deployed=False, candidate='hard-scan-74'))
    dataset = Samples(pools, config['corpora'], config['seed'], start*config['batch'], config['steps']*config['batch'])
    workers = config.get('workers', 2)
    loader = torch.utils.data.DataLoader(dataset, batch_size=config['batch'], num_workers=workers,
        pin_memory=True, persistent_workers=workers > 0, shuffle=False)
    started = time.monotonic()
    model.train()
    for step, (x, labels, heat) in enumerate(loader, start+1):
        x, labels, heat = x.to(device, non_blocking=True), labels.to(device, non_blocking=True), heat.to(device, non_blocking=True)
        optimizer.zero_grad(set_to_none=True)
        with torch.cuda.amp.autocast(enabled=amp_enabled):
            logits, centres = model.forward_with_centres(x)
            loss = losses(logits, centres, labels, heat)
        if not bool(torch.isfinite(loss)):
            raise FloatingPointError('Non-finite training loss')
        scaler.scale(loss).backward()
        scaler.unscale_(optimizer)
        gradient = torch.nn.utils.clip_grad_norm_(model.parameters(), 5., error_if_nonfinite=True)
        scaler.step(optimizer)
        scaler.update()
        report = None
        if step % config['validate_every'] == 0 or step == config['steps']:
            report = evaluate(model, samples, device)
            screening = gate(report, baseline)
            write_json(out/f'development-{step:05d}.json', dict(step=step, report=report, screening=screening))
            gain = screening['mean_hard_head_f1_gain']
            if screening['passed'] and gain > best:
                best = gain
                save_checkpoint(out/'best-development.pt', dict(architecture_config=model.config,
                    state_dict={k:v.detach().cpu() for k,v in model.state_dict().items()}, step=step,
                    parent_sha256=config['parent_sha256'], status='NOT_QUALIFIED_FOR_DEPLOYMENT',
                    development_gain=gain, test_evaluated=False, external_pretrained_sources=[]))
            print(json.dumps(dict(event='development', step=step, screening=screening)), flush=True)
        if step == 1 or step % config['save_every'] == 0 or step == config['steps']:
            save_checkpoint(out/'last.pt', dict(architecture_config=model.config,
                state_dict={k:v.detach().cpu() for k,v in model.state_dict().items()},
                optimizer=optimizer.state_dict(), scaler=scaler.state_dict(), step=step,
                best_development_gain=best, parent_sha256=config['parent_sha256'], test_evaluated=False))
        if step == 1 or step % 10 == 0 or report is not None:
            status = dict(status='training' if step < config['steps'] else 'training_complete_NOT_DEPLOYED',
                step=step, total_steps=config['steps'], loss=float(loss.detach()),
                gradient_norm=float(gradient), elapsed_seconds=time.monotonic()-started,
                best_development_gain=best, pid=os.getpid(), test_evaluated=False, deployed=False)
            write_json(out/'status.json', status)
            print(json.dumps(status), flush=True)
    write_json(out/'completion.json', dict(training_complete=True, steps=config['steps'],
        development_candidate_exists=(out/'best-development.pt').exists(), test_evaluated=False,
        deployed=False, remaining=['frozen-decoder source-note retention', 'sealed original holdout',
                                  'export/native parity', 'latency', 'public model lineage/release review']))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--resume', action='store_true')
    args = parser.parse_args()
    try:
        run(args)
    except Exception as error:
        if args.out.is_dir():
            write_json(args.out/'error.json', dict(error=repr(error), pid=os.getpid(), deployed=False))
        raise


if __name__ == '__main__':
    main()
