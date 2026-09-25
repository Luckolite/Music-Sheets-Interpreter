// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;

/** Platform-independent ornament templates and bilinear grayscale comparison. */
final class PortableOrnamentGlyphs {
    static final int W=48,H=40;
    private record Template(int kind,float aspect,float[] mask) { }
    record Match(int kind,float score,float margin) {
        boolean accepted(){return kind!=0&&score>=.50f&&margin>=.07f
                ||kind==NoteOrnament.TRILL&&score>=.44f&&margin>=.18f;}
    }
    private final List<Template> ornaments=new ArrayList<>(),accidentals=new ArrayList<>();
    void add(byte[] gray,int width,int height,int kind,boolean accidental) {
        if(gray==null||width<1||height<1||gray.length!=(long)width*height)throw new IllegalArgumentException("Invalid glyph raster");
        float[] pixels=mask(gray,width,0,0,width,height);float aspect=width/(float)height;
        List<Template> list=accidental?accidentals:ornaments;
        list.add(new Template(kind,aspect,pixels));
        if(!accidental&&(kind==NoteOrnament.MORDENT||kind==NoteOrnament.INVERTED_MORDENT))
            for(float factor:new float[]{.6f,.75f,.9f})list.add(new Template(kind,aspect*factor,pixels));
    }
    Match match(byte[] gray,int width,PortableNoteOrnaments.Bounds bounds){return match(gray,width,bounds,ornaments);}
    Match accidental(byte[] gray,int width,PortableNoteOrnaments.Bounds bounds){return match(gray,width,bounds,accidentals);}
    private Match match(byte[] gray,int width,PortableNoteOrnaments.Bounds bounds,List<Template> choices) {
        if(gray==null||width<1||bounds.left<0||bounds.top<0||bounds.right>width||bounds.bottom>gray.length/width
                ||bounds.width()<1||bounds.height()<1)return new Match(0,0,0);
        float[] candidate=mask(gray,width,bounds.left,bounds.top,bounds.right,bounds.bottom);
        float aspect=bounds.width()/(float)bounds.height();Map<Integer,Float> scores=new HashMap<>();
        for(var t:choices) {
            double ratio=Math.abs(Math.log(aspect/t.aspect));if(ratio>.45)continue;
            float overlap=0,total=0;
            for(int i=0;i<candidate.length;i++){overlap+=Math.min(candidate[i],t.mask[i]);total+=Math.max(candidate[i],t.mask[i]);}
            scores.merge(t.kind,overlap/Math.max(.001f,total)-(float)ratio*.20f,Math::max);
        }
        int kind=0;float best=0,second=0;
        for(var e:scores.entrySet())if(e.getValue()>best){second=best;best=e.getValue();kind=e.getKey();}
            else second=Math.max(second,e.getValue());
        return new Match(kind,best,best-second);
    }
    private static float[] mask(byte[] gray,int width,int left,int top,int right,int bottom) {
        float[] values=new float[W*H];
        for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
            float sx=left+(x+.5f)*(right-left)/W-.5f,sy=top+(y+.5f)*(bottom-top)/H-.5f;
            int x0=Math.max(left,Math.min(right-1,(int)Math.floor(sx))),y0=Math.max(top,Math.min(bottom-1,(int)Math.floor(sy)));
            int x1=Math.min(right-1,x0+1),y1=Math.min(bottom-1,y0+1);
            float fx=Math.max(0,Math.min(1,sx-x0)),fy=Math.max(0,Math.min(1,sy-y0));
            float shade=(gray[y0*width+x0]&255)*(1-fx)*(1-fy)+(gray[y0*width+x1]&255)*fx*(1-fy)
                    +(gray[y1*width+x0]&255)*(1-fx)*fy+(gray[y1*width+x1]&255)*fx*fy;
            values[y*W+x]=1-shade/255f;
        }
        return values;
    }
}
