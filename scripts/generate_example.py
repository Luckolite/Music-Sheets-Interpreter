#!/usr/bin/env python3
# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Render an original scale for public examples; no third-party score is used."""
import hashlib
import io
import json
from pathlib import Path
import sys
import verovio
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'training'))
from generate import raster


def main():
    pitches=[('g',4),('a',4),('b',4),('c',5),('d',5),('e',5),('f',5),('g',5)]
    measures=[]
    for index in range(2):
        notes=''.join(f'<note pname="{name}" oct="{octave}" dur="4"/>' for name,octave in pitches[index*4:index*4+4])
        measures.append(f'<measure n="{index+1}"><staff n="1"><layer n="1">{notes}</layer></staff></measure>')
    mei='<mei xmlns="http://www.music-encoding.org/ns/mei" meiversion="5.0"><meiHead><fileDesc><titleStmt><title>Original scale example</title></titleStmt><pubStmt/></fileDesc></meiHead><music><body><mdiv><score><scoreDef meter.count="4" meter.unit="4" key.sig="0"><staffGrp><staffDef n="1" lines="5" clef.shape="G" clef.line="2"/></staffGrp></scoreDef><section>'+''.join(measures)+'</section></score></mdiv></body></music></mei>'
    tk=verovio.toolkit();tk.setOptions({'font':'Bravura','unit':7,'pageWidth':2100,'pageHeight':2970,
        'breaks':'encoded','scale':40,'adjustPageHeight':True,'header':'none','footer':'none','svgViewBox':True})
    if not tk.loadData(mei):raise RuntimeError('Example did not render')
    rgba=Image.fromarray(raster(tk.renderToSVG(1)))
    image=Image.new('RGBA',rgba.size,'white');image.alpha_composite(rgba);image=image.convert('L')
    folder=ROOT/'examples';image.save(folder/'scale.png');image.save(folder/'scale.pdf',resolution=144)
    (folder/'scale.mei').write_text(mei+'\n',encoding='utf-8')
    (folder/'expected.json').write_text(json.dumps({'source':'Original generated scale, not used in model training',
        'license':'Apache-2.0','midi':[67,69,71,72,74,76,77,79],'durationsQuarterBeats':[1]*8,
        'measures':2,'meter':[4,4],'verovio':tk.getVersion(),
        'pngSha256':hashlib.sha256((folder/'scale.png').read_bytes()).hexdigest()},indent=2)+'\n')
    print('Rendered',image.size)


if __name__=='__main__':main()
