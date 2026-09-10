# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Train and evaluate only on the recorded original-exercise corpus."""
import argparse, collections, functools, hashlib, json, math, random, time
from pathlib import Path
import cv2
import numpy as np
from PIL import Image
import torch
from torch.nn import functional as F
from model import Segmenter, ARCHITECTURE, CLASS_NAMES, export_arrays

def read_corpus(path):
    manifest=json.loads((path/'manifest.json').read_text(encoding='utf-8'))
    assert manifest['third_party_score_sources']==[] and manifest['pretrained_model_sources']==[]
    seen=set();splits=collections.defaultdict(list)
    for exercise in manifest['exercises']:
        assert exercise['seed'] not in seen;seen.add(exercise['seed'])
        assert not exercise['pretrained_labels']
        for page in exercise['pages']:
            height,width=page['shape']
            for head in page['noteheads']:
                left,top,right,bottom=head['box']
                assert 0<=left<right<=width and 0<=top<bottom<=height
                assert right-left<=80 and bottom-top<=60, 'Corrupt notehead instance; regenerate corpus with v2 or later'
            for field in ['image','labels']:
                item=(path/page[field]).resolve();assert item.is_relative_to(path.resolve())
                assert hashlib.sha256(item.read_bytes()).hexdigest()==page[field+'_sha256'],str(item)
            splits[exercise['split']].append(page)
    assert all(splits[k] for k in ['train','validation','test'])
    return manifest,splits

@functools.lru_cache(maxsize=96)
def load_page(root,image,labels):
    gray=np.asarray(Image.open(Path(root)/image).convert('L'))
    mask=np.asarray(Image.open(Path(root)/labels))
    assert gray.shape==mask.shape and set(np.unique(mask)).issubset(set(range(6)))
    if min(gray.shape)<320:
        h=max(320,gray.shape[0]);w=max(320,gray.shape[1])
        gray=np.pad(gray,((0,h-gray.shape[0]),(0,w-gray.shape[1])),constant_values=255)
        mask=np.pad(mask,((0,h-mask.shape[0]),(0,w-mask.shape[1])))
    points=np.column_stack(np.nonzero((mask>0)&(mask!=4)))
    return gray,mask,points

def crop(root,page,rng,augment):
    gray,labels,points=load_page(str(root),page['image'],page['labels'])
    side=int(rng.uniform(250,430)) if augment else 320
    side=min(side,min(gray.shape))
    if len(points) and rng.random()<0.85:
        y,x=points[rng.integers(len(points))];x+=rng.integers(-side//3,side//3);y+=rng.integers(-side//3,side//3)
        x=int(np.clip(x-side//2,0,gray.shape[1]-side));y=int(np.clip(y-side//2,0,gray.shape[0]-side))
    else:x=int(rng.integers(gray.shape[1]-side+1));y=int(rng.integers(gray.shape[0]-side+1))
    a=cv2.resize(gray[y:y+side,x:x+side],(320,320),interpolation=cv2.INTER_AREA).astype(np.float32)
    b=cv2.resize(labels[y:y+side,x:x+side],(320,320),interpolation=cv2.INTER_NEAREST)
    if augment:
        if rng.random()<0.3:
            affine=cv2.getRotationMatrix2D((160,160),float(rng.uniform(-2,2)),1.)
            a=cv2.warpAffine(a,affine,(320,320),flags=cv2.INTER_LINEAR,borderValue=255)
            b=cv2.warpAffine(b,affine,(320,320),flags=cv2.INTER_NEAREST,borderValue=0)
        if rng.random()<0.35:a=cv2.GaussianBlur(a,(3,3),float(rng.uniform(0.3,0.85)))
        black=float(rng.uniform(0,65));white=float(rng.uniform(205,255))
        a=black+(white-black)*a/255
        a+=rng.normal(0,float(rng.uniform(0,3)),a.shape).astype(np.float32)
        a+=np.linspace(0,float(rng.uniform(-18,18)),320,dtype=np.float32)[None,:]
    return np.clip(a,0,255),b.astype(np.int64)

def scores(confusion):
    c=confusion.astype(float);tp=c.diagonal();gt=c.sum(1);pred=c.sum(0)
    union=gt+pred-tp
    iou=np.divide(tp,union,out=np.zeros(6),where=union>0)
    recall=np.divide(tp,gt,out=np.zeros(6),where=gt>0)
    precision=np.divide(tp,pred,out=np.zeros(6),where=pred>0)
    return {'mean_iou':float(iou.mean()),'foreground_mean_iou':float(iou[1:].mean()),
            'per_class':{name:{'iou':float(iou[i]),'recall':float(recall[i]),'precision':float(precision[i]),'pixels':int(gt[i])} for i,name in enumerate(CLASS_NAMES)}}

def evaluate(model,root,pages,device,crops_per_page=4):
    model.eval();rng=np.random.default_rng(614);
    batch=[];targets=[];conf=np.zeros((6,6),dtype=np.int64)
    with torch.inference_mode():
        for page in pages:
            for _ in range(crops_per_page):
                a,b=crop(root,page,rng,False);batch.append(a);targets.append(b)
                if len(batch)==8:
                    x=torch.from_numpy(np.stack(batch)[:,None].repeat(3,axis=1)).to(device)
                    out=model(x).argmax(1).cpu().numpy();truth=np.stack(targets)
                    conf+=np.bincount((truth*6+out).ravel(),minlength=36).reshape(6,6);batch=[];targets=[]
        if batch:
            x=torch.from_numpy(np.stack(batch)[:,None].repeat(3,axis=1)).to(device)
            out=model(x).argmax(1).cpu().numpy();truth=np.stack(targets)
            conf+=np.bincount((truth*6+out).ravel(),minlength=36).reshape(6,6)
    model.train();result=scores(conf);result['evaluated_crops']=len(pages)*crops_per_page
    return result

def starts(size):
    result=[]
    for x in range(0,size,192):
        pos=min(x,max(0,size-320))
        if not result or result[-1]!=pos:result.append(pos)
        if pos==max(0,size-320):break
    return result

def infer_page(model,gray,device):
    h,w=gray.shape;out=np.zeros((h,w),dtype=np.uint8);confidence=np.zeros((h,w),dtype=np.int16)
    yy,xx=np.mgrid[:320,:320];edge=np.minimum.reduce([xx,yy,319-xx,319-yy])+1
    coords=[(x,y) for y in starts(h) for x in starts(w)]
    with torch.inference_mode():
        for offset in range(0,len(coords),8):
            portion=coords[offset:offset+8];batch=[]
            for x,y in portion:
                a=np.full((320,320),255,dtype=np.float32);piece=gray[y:y+320,x:x+320];a[:piece.shape[0],:piece.shape[1]]=piece;batch.append(a)
            predictions=model(torch.from_numpy(np.stack(batch)[:,None].repeat(3,axis=1)).to(device)).argmax(1).cpu().numpy()
            for (x,y),p in zip(portion,predictions):
                ch,cw=min(320,h-y),min(320,w-x);wins=edge[:ch,:cw]>=confidence[y:y+ch,x:x+cw]
                out[y:y+ch,x:x+cw][wins]=p[:ch,:cw][wins];confidence[y:y+ch,x:x+cw][wins]=edge[:ch,:cw][wins]
    return out

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--corpus',type=Path,required=True);ap.add_argument('--out',type=Path,required=True)
    ap.add_argument('--steps',type=int,default=3000);ap.add_argument('--batch',type=int,default=8);ap.add_argument('--seed',type=int,default=20260909)
    ap.add_argument('--resume',type=Path);args=ap.parse_args();args.out.mkdir(parents=True,exist_ok=True)
    cv2.setNumThreads(1);torch.set_num_threads(2);torch.manual_seed(args.seed);rng=np.random.default_rng(args.seed)
    manifest,splits=read_corpus(args.corpus);device='cuda' if torch.cuda.is_available() else 'cpu'
    model=Segmenter().to(device);initialization={'type':'random','seed':args.seed}
    if args.resume:
        checkpoint=torch.load(args.resume,map_location=device);assert checkpoint['architecture']==ARCHITECTURE
        assert checkpoint['pretrained_model_sources']==[];model.load_state_dict(checkpoint['state_dict'])
        initialization={'type':'continued_own_pilot','checkpoint_sha256':hashlib.sha256(args.resume.read_bytes()).hexdigest(),
                        'parent_corpus_sha256':checkpoint['corpus_sha256'],'parent_step':checkpoint['step']}
    optimizer=torch.optim.AdamW(model.parameters(),lr=0.001,weight_decay=0.00001)
    scheduler=torch.optim.lr_scheduler.CosineAnnealingLR(optimizer,T_max=args.steps,eta_min=0.0001)
    weights=torch.tensor([0.2,2.,3.,2.,1.,1.5],device=device)
    best=-1.;history=[];began=time.perf_counter();model.train()
    for step in range(1,args.steps+1):
        batch=[crop(args.corpus,splits['train'][int(rng.integers(len(splits['train'])))],rng,True) for _ in range(args.batch)]
        x=torch.from_numpy(np.stack([a for a,b in batch])[:,None].repeat(3,axis=1)).to(device)
        y=torch.from_numpy(np.stack([b for a,b in batch])).to(device)
        optimizer.zero_grad(set_to_none=True);logits=model(x)
        ce=F.cross_entropy(logits,y,weight=weights)
        probabilities=logits.softmax(1);truth=F.one_hot(y,6).permute(0,3,1,2).float()
        dice=1-(2*(probabilities*truth).sum((0,2,3))+1)/((probabilities+truth).sum((0,2,3))+1)
        loss=ce+0.4*dice[1:].mean();loss.backward();torch.nn.utils.clip_grad_norm_(model.parameters(),5.)
        optimizer.step();scheduler.step()
        if step%100==0:print(json.dumps({'step':step,'loss':round(float(loss.detach()),4),'elapsed_seconds':round(time.perf_counter()-began,1)}),flush=True)
        if step%500==0 or step==args.steps:
            metrics=evaluate(model,args.corpus,splits['validation'],device)
            record={'step':step,'validation':metrics};history.append(record)
            print(json.dumps({'step':step,'validation_foreground_iou':metrics['foreground_mean_iou']}),flush=True)
            if metrics['foreground_mean_iou']>best:
                best=metrics['foreground_mean_iou'];torch.save({'architecture':ARCHITECTURE,'state_dict':model.state_dict(),
                    'step':step,'seed':args.seed,'pretrained_model_sources':[], 'initialization':initialization,
                    'corpus_sha256':hashlib.sha256((args.corpus/'manifest.json').read_bytes()).hexdigest()},args.out/'best.pt')
            (args.out/'history.json').write_text(json.dumps(history,indent=2),encoding='utf-8')
    saved=torch.load(args.out/'best.pt',map_location=device);model.load_state_dict(saved['state_dict']);model.eval()
    test=evaluate(model,args.corpus,splits['test'],device,crops_per_page=6);model.eval()
    # Export calibration/parity arrays use training pages, never the held-out test set.
    samples=np.stack([crop(args.corpus,p,rng,False)[0] for p in splits['train'][:32]])[:,None].repeat(3,axis=1)
    export_arrays(model,args.out/'weights.npz',samples)
    model=model.to(device).eval()
    page=splits['test'][0];gray=np.asarray(Image.open(args.corpus/page['image']).convert('L'))
    labels=np.asarray(Image.open(args.corpus/page['labels']));prediction=infer_page(model,gray,device)
    Image.fromarray(prediction).save(args.out/'heldout-prediction.png')
    palette=np.array([[255,255,255],[40,110,225],[28,165,90],[192,65,181],[235,105,30],[105,105,125]],dtype=np.uint8)
    preview=Image.new('RGB',(gray.shape[1]*3,gray.shape[0]),'white')
    for i,a in enumerate([np.repeat(gray[:,:,None],3,axis=2),palette[labels],palette[prediction]]):preview.paste(Image.fromarray(a),(i*gray.shape[1],0))
    preview.thumbnail((2400,1600));preview.save(args.out/'heldout-preview.png')
    result={'architecture':ARCHITECTURE,'parameters':sum(p.numel() for p in model.parameters()),'best_step':saved['step'],
        'steps_run':args.steps,'elapsed_seconds':time.perf_counter()-began,'device':device,'pretrained_model_sources':[],
        'initialization':initialization,
        'corpus_sha256':hashlib.sha256((args.corpus/'manifest.json').read_bytes()).hexdigest(),
        'pages':{k:len(v) for k,v in splits.items()},'validation_best_foreground_iou':best,'heldout_font_test':test,
        'test_scope':'six fixed foreground-biased crops per held-out Leland page; not real-score or phone accuracy',
        'quality_approved':False,'production_install_allowed':False}
    (args.out/'training.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps(result,indent=2),flush=True)

if __name__=='__main__':main()
