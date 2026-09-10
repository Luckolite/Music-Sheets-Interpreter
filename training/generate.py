# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Generate original exercises and renderer-derived labels with recorded provenance.

No score downloads, pretrained models, or model-generated pseudo-labels are used.
Splits are assigned before rendering; each exercise seed belongs to one split.
"""
from pathlib import Path
import argparse, collections, concurrent.futures, copy, hashlib, io, json, random, sys
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parent
import verovio, resvg_py
import numpy as np
import cv2
from PIL import Image

VERSION='independent-exercises-v3'
PALETTE=np.array([[255,255,255],[40,110,225],[28,165,90],[192,65,181],[235,105,30],[105,105,125]],dtype=np.uint8)
PATTERNS=[[4,4,4,4],[8,8],[16],[2]*8,[4,2,2,4,2,2],[6,2,4,4],[3,1,3,1,4,4],[8,4,4]]
DURATION={.5:'dur="32"',1:'dur="16"',1.5:'dur="16" dots="1"',2:'dur="8"',3:'dur="8" dots="1"',4:'dur="4"',6:'dur="4" dots="1"',8:'dur="2"',12:'dur="2" dots="1"',16:'dur="1"'}
METER_PATTERNS={
    2:[[4,4],[8],[2]*4,[3,1,3,1],[.5,1.5,2,4]],
    3:[[4,4,4],[12],[8,4],[6,2,2,2],[2]*6,[3,1]*3,[.5,.5,1,2]*3],
    4:PATTERNS+[[.5,1.5,2,4]*2,[1.5,.5]*8],
}

def music(seed):
    rng=random.Random(seed); staffs=rng.choice([1,2,2,3]); bars=rng.choice([12,16,20,24])
    unit=rng.choice([5.,6.,7.,8.,9.]); base_octave=4
    meter=rng.choice([2,3,3,4,4]);key=rng.choice(['0','1s','2s','3s','1f','2f','3f'])
    uid=0; notes=[]; ties=[]; parts=[];last_notes={}
    for m in range(bars):
        short=(m==5 and seed%3==0)
        if short:parts.append('<scoreDef meter.count="1" meter.unit="4"/>')
        elif m==6 and seed%3==0:parts.append(f'<scoreDef meter.count="{meter}" meter.unit="4"/>')
        if m==bars//2:parts.append(f'<scoreDef key.sig="{rng.choice(["0","1s","2s","1f","2f"])}"/>')
        if m and m%4==0:parts.append('<sb/>')
        parts.append(f'<measure n="{m+1}">')
        ornaments=[]
        for s in range(1,staffs+1):
            parts.append(f'<staff n="{s}"><layer n="1">')
            independent=s==2 and not short and meter in [3,4] and rng.random()<.35
            pattern=[4] if short else [2]*(meter*2) if independent else rng.choice(METER_PATTERNS[meter])
            assert sum(pattern)==(4 if short else meter*4)
            prev=last_notes.get(s) if rng.random()<.25 else None; beam=False; onset=0
            for i,dur in enumerate(pattern):
                want_beam=dur<=2 and (i+1<len(pattern) and pattern[i+1]<=2 or beam)
                if want_beam and not beam:parts.append('<beam>');beam=True
                if not want_beam and beam:parts.append('</beam>');beam=False
                is_rest=(i%2==1) if independent else rng.random()<0.17
                pitch=rng.randint(0,11) if s==1 else rng.randint(-1,9)
                octave=base_octave if s==1 else 2
                chord=(not independent and rng.random()<(0.28 if dur>=4 else .10))
                pitches=[pitch,pitch+rng.choice([1,2,3])] if chord else [pitch]
                if chord and rng.random()<0.22:pitches.append(pitches[-1]+2)
                tied=(prev is not None and dur>=4 and rng.random()<0.23)
                if tied:pitches=prev[0];is_rest=False
                identifiers=[]
                if is_rest:parts.append(f'<rest {DURATION[dur]}/>');prev=None
                else:
                    if len(pitches)>1:parts.append(f'<chord {DURATION[dur]}>')
                    for p in pitches:
                        uid+=1; nid=f'n{seed}_{uid}';identifiers.append(nid)
                        pname='cdefgab'[p%7];oct=octave+p//7
                        acc='' if tied or rng.random()>0.08 else rng.choice(['s','f','n'])
                        duration='' if len(pitches)>1 else DURATION[dur]
                        accidental=f' accid="{acc}"' if acc else ''
                        articulation=rng.choice(['','','','','stacc','acc','ten'])
                        mark=f' artic="{articulation}"' if articulation else ''
                        parts.append(f'<note xml:id="{nid}" pname="{pname}" oct="{oct}" {duration}{accidental}{mark}/>')
                        notes.append({'id':nid,'measure':m,'staff':s-1,'pname':pname,'octave':oct,
                                      'duration_quarters':dur/4,'onset_quarters':onset/4,'accidental':acc,'articulation':articulation})
                    if len(pitches)>1:parts.append('</chord>')
                    if tied:
                        for old,new in zip(prev[1],identifiers):ties.append((m,old,new))
                    prev=(pitches,identifiers)
                    if rng.random()<.025:ornaments.append(f'<fermata startid="#{identifiers[0]}"/>')
                    if rng.random()<.015:ornaments.append(f'<trill startid="#{identifiers[0]}"/>')
                onset+=dur
            if beam:parts.append('</beam>')
            last_notes[s]=prev
            parts.append('</layer>')
            if independent:
                uid+=1;nid=f'n{seed}_{uid}';pname=rng.choice('cdefg');octave=2
                parts.append(f'<layer n="2"><note xml:id="{nid}" pname="{pname}" oct="{octave}" {DURATION[meter*4]}/></layer>')
                notes.append({'id':nid,'measure':m,'staff':s-1,'pname':pname,'octave':octave,
                              'duration_quarters':meter,'onset_quarters':0,'accidental':'','independent_sustain':True})
            parts.append('</staff>')
        parts.extend(ornaments)
        parts.append(f'<!--ties-{m}-->')
        if m%5==0:
            bpm=rng.choice([60,72,88,98,104,108,120,132])
            parts.append(f'<tempo staff="1" tstamp="1" midi.bpm="{bpm}">{rng.choice(["Andante","Moderato","Allegro"])} = {bpm}</tempo>')
        if m%3==0:parts.append(f'<dynam staff="1" tstamp="1">{rng.choice(["p","mp","mf","f","ff"])}</dynam>')
        parts.append('</measure>')
    defs=''.join(f'<staffDef n="{s}" lines="5" clef.shape="{"G" if s==1 else "F"}" clef.line="{2 if s==1 else 4}"/>' for s in range(1,staffs+1))
    body=''.join(parts);note_measures={n['id']:n['measure'] for n in notes}
    for m in range(bars):
        # MEI cross-measure ties belong to the measure containing the start note.
        controls=''.join(f'<tie startid="#{a}" endid="#{b}"/>' for _,a,b in ties if note_measures[a]==m)
        body=body.replace(f'<!--ties-{m}-->',controls)
    mei=f'<mei xmlns="http://www.music-encoding.org/ns/mei" meiversion="5.0"><meiHead><fileDesc><titleStmt><title>Original exercise {seed}</title></titleStmt><pubStmt/></fileDesc></meiHead><music><body><mdiv><score><scoreDef meter.count="{meter}" meter.unit="4" key.sig="{key}"><staffGrp symbol="brace" bar.thru="true">{defs}</staffGrp></scoreDef><section>{body}</section></score></mdiv></body></music></mei>'
    return mei,{'staffs':staffs,'bars':bars,'meter':meter,'initial_key':key,'unit':unit,'notes':notes,'ties':ties}

def raster(svg):
    payload=resvg_py.svg_to_bytes(svg_string=svg,width=2048,skip_system_fonts=True,
        font_files=[str(ROOT/'fonts/NotoSerif.ttf')],font_family='Noto Serif',serif_family='Noto Serif')
    return np.asarray(Image.open(io.BytesIO(payload)).convert('RGBA'))

def semantic_maps(svg):
    tree=ET.fromstring(svg);draw_tags={'path','use','polygon','polyline','line','rect','ellipse','circle','text'}
    classes={'notehead':2,'stem':1,'rest':1,'barLine':1,'clef':3,'keySig':3,'keyAccid':3,'accid':3,
             'beam':5,'flag':5,'tie':5,'slur':5,'dots':5,'meterSig':5,'tempo':5,'dynam':5}
    heads=[]
    def mark(node,semantic=5,head=0,parent='',note=''):
        tag=node.tag.rsplit('}',1)[-1]
        if tag=='defs':return
        own=node.get('class','').split()
        if 'note' in own:note=node.get('id','')
        for name in own:
            if name in classes:semantic=classes[name]
        if 'notehead' in own:heads.append(note);head=len(heads)
        if tag in draw_tags:
            code=4 if parent=='staff' and tag=='path' else 1 if parent=='system' and tag=='path' else semantic
            node.set('data-semantic',str(code));node.set('data-head',str(head))
        for child in node:mark(child,semantic,head,' '.join(own),note)
    mark(tree)
    def colored(instance=False):
        clone=copy.deepcopy(tree)
        def visit(node):
            for child in list(node):
                if child.tag.rsplit('}',1)[-1]=='defs':continue
                if 'data-semantic' in child.attrib:
                    label=int(child.get('data-semantic'));head=int(child.get('data-head'))
                    if instance and not head:node.remove(child);continue
                    # Consecutive low RGB integers alias into other instance IDs at
                    # antialiased edges. Space codes 32 levels apart in each channel.
                    assert head <= 512, 'Instance color space exhausted'
                    code=head-1
                    channels=[16+32*((code>>shift)&7) for shift in (6,3,0)]
                    color='#'+''.join(f'{n:02x}' for n in channels) if instance else '#'+''.join(f'{n:02x}' for n in PALETTE[label])
                    child.set('color',color)
                    child.set('style',f'color:{color};fill:{color};stroke:{color}')
                visit(child)
        visit(clone)
        return raster(ET.tostring(clone,encoding='unicode'))
    pixels=colored();active=pixels[:,:,3]>=128
    labels=np.zeros(active.shape,dtype=np.uint8)
    rgb=pixels[:,:,:3][active].astype(np.int16)
    # Classify only foreground pixels; avoid allocating full-page six-class logits.
    distances=((rgb[:,None].astype(np.int32)-PALETTE[None,1:].astype(np.int32))**2).sum(-1)
    labels[active]=np.argmin(distances,axis=1)+1
    instances=colored(True)
    codes=np.clip(np.rint((instances[:,:,:3].astype(np.float32)-16)/32),0,7).astype(np.int32)
    ids=(codes[:,:,0]<<6)+(codes[:,:,1]<<3)+codes[:,:,2]+1
    ids[(instances[:,:,3]<128)|(ids>len(heads))]=0
    ys,xs=np.nonzero(ids);values=ids[ys,xs]
    lowx=np.full(len(heads)+1,2048,dtype=np.int32);lowy=np.full(len(heads)+1,ids.shape[0],dtype=np.int32)
    highx=np.zeros(len(heads)+1,dtype=np.int32);highy=np.zeros(len(heads)+1,dtype=np.int32)
    np.minimum.at(lowx,values,xs);np.minimum.at(lowy,values,ys);np.maximum.at(highx,values,xs);np.maximum.at(highy,values,ys)
    boxes=[]
    for i,note in enumerate(heads,1):
        if highx[i]<lowx[i] or highy[i]<lowy[i]:continue
        x,y,r,b=map(int,[lowx[i],lowy[i],highx[i]+1,highy[i]+1])
        mask=(ids[y:b,x:r]==i).astype(np.uint8)
        # Blending two touching instance colors can create a third valid code.
        # Keep this ID's principal connected glyph, excluding remote edge specks.
        count,components,stats,_=cv2.connectedComponentsWithStats(mask,8)
        principal=1+int(np.argmax(stats[1:,cv2.CC_STAT_AREA]))
        bx,by,bw,bh,_=map(int,stats[principal])
        mask=(components[by:by+bh,bx:bx+bw]==principal).astype(np.uint8)
        x+=bx;y+=by;r=x+bw;b=y+bh
        # A single ordinary notehead has a convex silhouette. The hull fills its
        # hollow interior even if an antialiased rim has a one-pixel opening.
        points=cv2.findNonZero(mask)
        cv2.fillConvexPoly(mask,cv2.convexHull(points),1)
        assert r-x<=80 and b-y<=60, f'Implausible notehead instance {note}: {(x,y,r,b)}'
        labels[y:b,x:r][mask.astype(bool)]=2
        boxes.append({'id':note,'box':[x,y,r,b]})
    assert len(boxes)==len(heads), 'Missing rendered notehead instances'
    return labels,boxes

def create_job(job):
    seed,split,font,out=job;out=Path(out);folder=out/f'{seed:07d}';folder.mkdir(parents=True,exist_ok=True)
    mei,meta=music(seed);tk=verovio.toolkit();tk.setOptions({'font':font,'unit':meta['unit'],'pageWidth':2100,'pageHeight':2970,
        'breaks':'encoded','scale':40,'adjustPageHeight':True,'header':'none','footer':'none','svgViewBox':True,
        'spacingStaff':random.Random(seed).choice([8,12,16]),'spacingSystem':8})
    assert tk.loadData(mei)
    (folder/'score.mei').write_text(mei,encoding='utf-8')
    pages=[]
    for page in range(1,tk.getPageCount()+1):
        svg=tk.renderToSVG(page).replace('font-family="Times, serif"','font-family="Noto Serif, serif"')
        rgba=raster(svg);alpha=rgba[:,:,3:4].astype(np.float32)/255
        rgb=(rgba[:,:,:3]*alpha+255*(1-alpha)).astype(np.uint8)
        gray=cv2.cvtColor(rgb,cv2.COLOR_RGB2GRAY);labels,boxes=semantic_maps(svg)
        prefix=f'page-{page}'
        Image.fromarray(gray).save(folder/(prefix+'.png'),compress_level=1)
        Image.fromarray(labels).save(folder/(prefix+'.labels.png'),compress_level=1)
        if seed%100<2:(folder/(prefix+'.svg')).write_text(svg,encoding='utf-8')
        pages.append({'image':str((folder/(prefix+'.png')).relative_to(out)).replace('\\','/'),
                      'labels':str((folder/(prefix+'.labels.png')).relative_to(out)).replace('\\','/'),
                      'image_sha256':hashlib.sha256((folder/(prefix+'.png')).read_bytes()).hexdigest(),
                      'labels_sha256':hashlib.sha256((folder/(prefix+'.labels.png')).read_bytes()).hexdigest(),
                      'shape':list(gray.shape),'noteheads':boxes,'class_pixels':np.bincount(labels.ravel(),minlength=6).tolist()})
    meta.update({'seed':seed,'split':split,'font':font,'generator_version':VERSION,'pages':pages,
        'source':'newly generated exercise','pretrained_labels':False,'verovio':tk.getVersion()})
    (folder/'metadata.json').write_text(json.dumps(meta,indent=2),encoding='utf-8')
    return meta

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--out',type=Path,required=True);ap.add_argument('--count',type=int,default=400)
    ap.add_argument('--workers',type=int,default=4);ap.add_argument('--start',type=int,default=1000000);args=ap.parse_args()
    args.out.mkdir(parents=True,exist_ok=True)
    jobs=[]
    for i in range(args.count):
        seed=args.start+i;bucket=i%10;split='train' if bucket<8 else 'validation' if bucket==8 else 'test'
        font='Leland' if split=='test' else 'Bravura'
        jobs.append((seed,split,font,str(args.out)))
    records=[]
    with concurrent.futures.ProcessPoolExecutor(max_workers=args.workers) as pool:
        for record in pool.map(create_job,jobs):
            records.append(record)
            if len(records)%20==0:print(f'Generated {len(records)}/{len(jobs)} exercises',flush=True)
    records.sort(key=lambda r:r['seed'])
    manifest={'version':VERSION,'generator_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        'third_party_score_sources':[],'pretrained_model_sources':[],'split_policy':'exercise seeds disjoint; Leland held out for test',
        'fonts':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'fonts').iterdir() if p.is_file()},
        'exercises':records}
    (args.out/'manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    print(json.dumps({'exercises':len(records),'pages':sum(len(r['pages']) for r in records),
                     'splits':dict(collections.Counter(r['split'] for r in records))}),flush=True)

if __name__=='__main__':main()
