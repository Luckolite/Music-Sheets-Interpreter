# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Full-page synthetic test evaluation, including notehead bounding-box matching.

Instance results use renderer boxes and connected-component boxes, not semantic
pitch recognition. Touching predicted heads deliberately count as merged objects.
"""
import argparse, hashlib, json
from pathlib import Path
import cv2
import numpy as np
from PIL import Image
import torch
from model import Segmenter, ARCHITECTURE
from train import read_corpus, infer_page, scores


def instances(prediction, truth):
    count, _, stats, _ = cv2.connectedComponentsWithStats((prediction == 2).astype(np.uint8), 8)
    boxes = [[x, y, x+w, y+h] for x,y,w,h,area in stats[1:] if area >= 3]
    a = np.array(truth, dtype=float).reshape(-1,4)
    b = np.array(boxes, dtype=float).reshape(-1,4)
    if len(a) and len(b):
        intersection = np.maximum(0, np.minimum(a[:,None,2:],b[None,:,2:])-
                                  np.maximum(a[:,None,:2],b[None,:,:2])).prod(2)
        areas_a=(a[:,2:]-a[:,:2]).prod(1)
        areas_b=(b[:,2:]-b[:,:2]).prod(1)
        iou=intersection/(areas_a[:,None]+areas_b[None,:]-intersection)
        eligible=np.argwhere(iou >= .5)
        eligible=sorted(eligible, key=lambda pair: -iou[tuple(pair)])
        used_a=set(); used_b=set()
        for i,j in eligible:
            if i not in used_a and j not in used_b: used_a.add(i);used_b.add(j)
        overlap=intersection/np.minimum(areas_a[:,None],areas_b[None,:]) >= .2
        splits=int((overlap.sum(1)>1).sum());merges=int((overlap.sum(0)>1).sum())
        matches=len(used_a)
    else: matches=splits=merges=0
    return {'truth':len(a),'predictions':len(b),'matched':matches,
            'unmatched_truth':len(a)-matches,'unmatched_predictions':len(b)-matches,
            'possible_splits':splits,'possible_merges':merges}


def main():
    ap=argparse.ArgumentParser();ap.add_argument('--corpus',type=Path,required=True)
    ap.add_argument('--checkpoint',type=Path,required=True);ap.add_argument('--out',type=Path,required=True)
    args=ap.parse_args();args.out.mkdir(parents=True,exist_ok=True)
    cv2.setNumThreads(1);torch.set_num_threads(2)
    _,splits=read_corpus(args.corpus)
    saved=torch.load(args.checkpoint,map_location='cpu')
    assert saved['architecture']==ARCHITECTURE and saved['pretrained_model_sources']==[]
    assert saved['corpus_sha256']==hashlib.sha256((args.corpus/'manifest.json').read_bytes()).hexdigest()
    device='cuda' if torch.cuda.is_available() else 'cpu'
    model=Segmenter().to(device);model.load_state_dict(saved['state_dict']);model.eval()
    confusion=np.zeros((6,6),dtype=np.int64);records=[];oracles=[]
    for page in splits['test']:
        gray=np.asarray(Image.open(args.corpus/page['image']).convert('L'))
        truth=np.asarray(Image.open(args.corpus/page['labels']))
        prediction=infer_page(model,gray,device)
        confusion+=np.bincount((truth.astype(np.int64)*6+prediction).ravel(),minlength=36).reshape(6,6)
        record=instances(prediction,[head['box'] for head in page['noteheads']])
        oracles.append(instances(truth,[head['box'] for head in page['noteheads']]))
        records.append({'image':page['image'],**record})
        print(json.dumps(records[-1]),flush=True)
    totals={key:sum(row[key] for row in records) for key in record}
    totals['precision']=totals['matched']/max(1,totals['predictions'])
    totals['recall']=totals['matched']/max(1,totals['truth'])
    oracle={key:sum(row[key] for row in oracles) for key in record}
    oracle['precision']=oracle['matched']/max(1,oracle['predictions'])
    oracle['recall']=oracle['matched']/max(1,oracle['truth'])
    result={'pages':len(records),'segmentation':scores(confusion),'notehead_boxes':totals,'per_page':records,
            'ground_truth_mask_component_reference':oracle,
            'scope':'Full held-out Leland pages; synthetic music only. Greedy one-to-one box IoU >= 0.5. '
                    'Split/merge counts are possible box-overlap events at intersection/min-area >= 0.2; '
                    'not a pixel-instance oracle or pitch, timing, tie or BPM evaluation. '
                    'The same detector on ground-truth masks provides a reference for touching heads.',
            'checkpoint_sha256':hashlib.sha256(args.checkpoint.read_bytes()).hexdigest(),
            'quality_approved':False,'production_install_allowed':False}
    (args.out/'full-pages.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k!='per_page'},indent=2),flush=True)


if __name__=='__main__':main()
