#!/usr/bin/env python3
"""Reproduce recognition rasters from a supplied SIL-OFL Bravura font, never score pixels."""
import argparse
# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
import hashlib
import json
from pathlib import Path
import struct
from PIL import Image, ImageDraw, ImageFont, ImageChops

def raster(font, code, height=64):
    l,t,r,b=font.getbbox(chr(code))
    canvas=Image.new('L',(r-l+8,b-t+8),255)
    ImageDraw.Draw(canvas).text((4-l,4-t),chr(code),font=font,fill=0)
    box=ImageChops.invert(canvas).getbbox()
    if box is None:
        raise ValueError(f'Empty glyph U+{code:04X}')
    image=canvas.crop(box)
    return image.resize((round(image.width*height/image.height),height),Image.Resampling.LANCZOS)

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--font',type=Path,required=True)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--fixture-dir',type=Path,help='Also render the original synthetic test sprites at 96 pixels high')
    args=p.parse_args()
    font=ImageFont.truetype(str(args.font),256)
    glyphs=[]
    for start in (0xECA2,0xE1D2):
        # Whole has no stem orientation; keep one kind for its duplicate template.
        for offset in range(11):
            kind=1 if offset==0 else offset+2
            glyphs.append((kind,start+offset,raster(font,start+offset)))
    for code in (0xE4CE,0xE4CF):
        glyphs.append((13+code-0xE4CE,code,raster(font,code)))
    data=bytearray(struct.pack('>II',0x45584731,len(glyphs)))
    for kind,code,image in glyphs:
        data.extend(struct.pack('>III',kind,image.width,image.height))
        data.extend(image.tobytes())
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_bytes(data)
    args.output.with_suffix('.json').write_text(json.dumps(dict(
        license='SIL-OFL-1.1',source='https://github.com/steinbergmedia/bravura',
        smufl=['https://smufl.formats.music/latest/tables/metronome-marks.html',
               'https://smufl.formats.music/latest/tables/holds-and-pauses.html'],
        font_sha256=hashlib.sha256(args.font.read_bytes()).hexdigest(),
        raster_sha256=hashlib.sha256(data).hexdigest(),
        rendering=dict(fontSize=256,height=64,resize='Pillow LANCZOS',grayscale=True),
        glyphs=[dict(kind=kind,codepoint=f'U+{code:04X}',width=i.width,height=i.height)
                for kind,code,i in glyphs]),indent=2)+'\n',encoding='utf-8')
    if args.fixture_dir:
        args.fixture_dir.mkdir(parents=True,exist_ok=True)
        for name,code in {'whole':0xECA2,'half':0xECA3,'half-down':0xECA4,
                          'quarter':0xECA5,'quarter-down':0xECA6,'eighth':0xECA7,
                          'sixteenth':0xECA9,'breath-comma':0xE4CE,
                          'breath-tick':0xE4CF,'upbow':0xE612}.items():
            raster(font,code,96).save(args.fixture_dir/(name+'.png'))

if __name__=='__main__':
    main()
