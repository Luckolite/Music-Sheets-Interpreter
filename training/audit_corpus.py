# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Fail closed on hash, split, missing-instance and oversized/hollow-mask defects."""
import argparse, collections, json
from pathlib import Path
import numpy as np
from PIL import Image
from train import read_corpus


def main():
    ap=argparse.ArgumentParser();ap.add_argument('corpus',type=Path);ap.add_argument('--out',type=Path)
    args=ap.parse_args();manifest,splits=read_corpus(args.corpus)
    errors=[];heads=0;hollow=0;sizes=[];fonts=collections.defaultdict(set)
    for exercise in manifest['exercises']:
        notes={note['id']:note for note in exercise['notes']};observed=[]
        fonts[exercise['split']].add(exercise['font'])
        for page in exercise['pages']:
            labels=np.asarray(Image.open(args.corpus/page['labels']))
            for head in page['noteheads']:
                observed.append(head['id']);heads+=1
                x,y,r,b=head['box'];width=r-x;height=b-y;sizes.append((width,height))
                if not (0<=x<r<=labels.shape[1] and 0<=y<b<=labels.shape[0] and width<=80 and height<=60):
                    errors.append({'id':head['id'],'reason':'invalid bounds','box':head['box']});continue
                note=notes[head['id']]
                if note['duration_quarters']>=2:hollow+=1
                if labels[(y+b)//2,(x+r)//2]!=2:
                    errors.append({'id':head['id'],'reason':'head center is not class 2'})
        if set(observed)!=set(notes) or len(observed)!=len(set(observed)):
            errors.append({'seed':exercise['seed'],'reason':'missing or duplicate note IDs'})
    assert set.intersection(fonts['train'],fonts['test'])==set()
    result={'generator':manifest['version'],'exercises':len(manifest['exercises']),
            'pages':{k:len(v) for k,v in splits.items()},'noteheads':heads,'hollow_noteheads':hollow,
            'max_head_width':max(w for w,h in sizes),'max_head_height':max(h for w,h in sizes),
            'error_count':len(errors),'errors':errors,'passed':not errors}
    if args.out:args.out.write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k!='errors'},indent=2))
    if errors:raise SystemExit(1)


if __name__=='__main__':main()
