// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;

/** Conservative isolated-ink recognition. Unknown marks never shorten a note. */
final class NoteArticulationDetector {
    record Anchor(float x,float y,float gap,int staff) { }
    private record Glyph(int left,int top,int right,int bottom,int count,int[] pixels) {
        float x(){return (left+right)*.5f;}
        float y(){return (top+bottom)*.5f;}
    }
    private NoteArticulationDetector() { }

    /** Recover an angular mark whose mask was mistaken for a small notehead.
     * Staff rules may cross its tip or feet; exclude only proven horizontal rules. */
    static boolean marcatoAtHead(byte[] gray,int width,int height,int left,int top,int right,int bottom,
                                  float gap,boolean above) {
        if(gray==null||gray.length!=width*height)return false;
        int padding=Math.max(3,Math.round(gap*1.1f));
        int x0=Math.max(0,left-padding),x1=Math.min(width-1,right+padding);
        int y0=Math.max(0,top-padding),y1=Math.min(height-1,bottom+padding);
        int w=x1-x0+1,h=y1-y0+1;boolean[] rules=new boolean[h],seen=new boolean[w*h];
        for(int y=y0;y<=y1;y++)rules[y-y0]=horizontalRuleInk(gray,width,height,(left+right)/2,y,gap,155,.85f);
        int[] queue=new int[w*h];List<Integer> pixels=new ArrayList<>();
        int gx0=width,gx1=0,gy0=height,gy1=0;
        for(int origin=0;origin<seen.length;origin++) {
            int ox=origin%w,oy=origin/w;
            if(seen[origin]||rules[oy]||(gray[(y0+oy)*width+x0+ox]&255)>=155)continue;
            int size=1,take=0;queue[0]=origin;seen[origin]=true;boolean clipped=false;
            while(take<size) {
                int at=queue[take++],x=at%w,y=at/w;
                if(x==0||x==w-1||y==0||y==h-1)clipped=true;
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int nx=x+dx,ny=y+dy;
                    if(nx<0||nx>=w||ny<0||ny>=h)continue;
                    int next=ny*w+nx;
                    if(!seen[next]&&!rules[ny]&&(gray[(y0+ny)*width+x0+nx]&255)<155){seen[next]=true;queue[size++]=next;}
                }
            }
            if(clipped||size<3)continue;
            int partLeft=w,partRight=0,partTop=h,partBottom=0;
            for(int i=0;i<size;i++) {
                int x=queue[i]%w,y=queue[i]/w;
                partLeft=Math.min(partLeft,x);partRight=Math.max(partRight,x);
                partTop=Math.min(partTop,y);partBottom=Math.max(partBottom,y);
            }
            if(partRight-partLeft+1<gap*.2f&&partBottom-partTop+1>gap*.45f)continue;
            for(int i=0;i<size;i++) {
                int x=x0+queue[i]%w,y=y0+queue[i]/w;
                gx0=Math.min(gx0,x);gx1=Math.max(gx1,x);gy0=Math.min(gy0,y);gy1=Math.max(gy1,y);pixels.add(y*width+x);
            }
        }
        if(pixels.isEmpty()||right<gx0||left>gx1||bottom<gy0||top>gy1)return false;
        Glyph glyph=new Glyph(gx0,gy0,gx1,gy1,pixels.size(),pixels.stream().mapToInt(Integer::intValue).toArray());
        return classify(glyph,width,gap,above)==NoteArticulation.MARCATO;
    }

    static int[] detect(byte[] labels,byte[] gray,int width,int height,List<Anchor> notes) {
        int[] result=new int[notes.size()];
        if(notes.isEmpty()||labels==null||labels.length!=width*height)return result;
        // Raw components retain symbols that the semantic model omitted. Never erase staff
        // lines: doing so would manufacture dashes/dots from beams, stems and noteheads.
        boolean raw=gray!=null&&gray.length==labels.length;
        boolean[] seen=new boolean[labels.length];
        int[] queue=new int[labels.length];
        for(int p=0;p<labels.length;p++) {
            if(seen[p]||!ink(labels,gray,p,raw))continue;
            int start=0,end=1;queue[0]=p;seen[p]=true;
            int left=p%width,right=left,top=p/width,bottom=top;
            while(start<end) {
                int point=queue[start++],x=point%width,y=point/width;
                left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int nx=x+dx,ny=y+dy;
                    if(nx<0||ny<0||nx>=width||ny>=height)continue;
                    int next=ny*width+nx;
                    if(!seen[next]&&ink(labels,gray,next,raw)){seen[next]=true;queue[end++]=next;}
                }
            }
            if(end<3||right-left>width*.025f||bottom-top>height*.018f)continue;
            // Semantic notation normally vetoes an articulation. A raw isolated
            // dot may have been mislabeled as a head and rejected by the pitch
            // reader; it can still be staccato if it is not an accepted head.
            int notation=0;
            for(int i=0;i<end;i++) {
                byte label=labels[queue[i]];
                if(label==OmrMeasurePostProcessor.NOTEHEAD||label==OmrMeasurePostProcessor.STEM_OR_REST
                        ||label==OmrMeasurePostProcessor.CLEF_OR_KEY||label==OmrMeasurePostProcessor.STAFF)notation++;
            }
            boolean semanticNotation=notation>end*.25f;
            Glyph glyph=new Glyph(left,top,right,bottom,end,java.util.Arrays.copyOf(queue,end));
            int best=-1,mark=0;double distance=Double.MAX_VALUE;
            for(int n=0;n<notes.size();n++) {
                Anchor note=notes.get(n);
                float dx=Math.abs(glyph.x()-note.x)/note.gap,dy=Math.abs(glyph.y()-note.y)/note.gap;
                if(dx>.7f||dy<.7f||dy>5.5f)continue;
                int candidate=classify(glyph,width,note.gap,glyph.y()<note.y);
                if(candidate==0)continue;
                if(semanticNotation&&(!raw||candidate!=NoteArticulation.STACCATO||nearHead(glyph,notes)))continue;
                if(candidate==NoteArticulation.TENUTO&&nearHead(glyph,notes))continue;
                if(raw&&candidate==NoteArticulation.TENUTO
                        &&glyph.bottom-glyph.top+1<=Math.max(2,Math.round(note.gap*.23f))
                        &&horizontalRuleInk(gray,width,height,Math.round(glyph.x()),Math.round(glyph.y()),note.gap,235,.50f))continue;
                if(candidate==NoteArticulation.STACCATO && (durationDot(glyph,notes)
                        || (raw&&shelteredDot(glyph,labels,gray,width,height,note.gap))))continue;
                // A small off-axis dot beside a head is a duration dot, not staccato.
                if(candidate==NoteArticulation.STACCATO&&dx>.35f)continue;
                double score=dx*3+dy;
                if(score<distance){distance=score;best=n;mark=candidate;}
            }
            if(best<0)continue;
            Anchor owner=notes.get(best);
            // One mark on a chord affects that chord, never another staff or another onset.
            for(int n=0;n<notes.size();n++) {
                Anchor note=notes.get(n);
                if(note.staff==owner.staff&&Math.abs(note.x-owner.x)<owner.gap*.45f)result[n]|=mark;
            }
        }
        return result;
    }

    private static boolean ink(byte[] labels,byte[] gray,int p,boolean raw) {
        return raw?(gray[p]&255)<155:labels[p]==OmrMeasurePostProcessor.SYMBOL;
    }

    private static boolean durationDot(Glyph glyph,List<Anchor> notes) {
        for(Anchor note:notes) {
            float dx=(glyph.x()-note.x)/note.gap,dy=Math.abs(glyph.y()-note.y)/note.gap;
            if(dx>.4f&&dx<1.8f&&dy<.6f)return true;
        }
        return false;
    }

    private static boolean nearHead(Glyph glyph,List<Anchor> notes) {
        for(Anchor note:notes)if(Math.abs(glyph.x()-note.x)<note.gap*1.2f
                &&Math.abs(glyph.y()-note.y)<note.gap*.7f)return true;
        return false;
    }

    /** The dot under a fermata arch is not a staccato instruction. */
    private static boolean shelteredDot(Glyph glyph,byte[] labels,byte[] gray,int width,int height,float gap) {
        for(int direction:new int[]{-1,1}) {
            int occupied=0;
            for(int bin=-2;bin<=2;bin++) {
                int x=Math.round(glyph.x()+bin*gap*.3f);
                boolean found=false;
                for(int d=Math.max(1,Math.round(gap*.35f));d<=Math.round(gap*1.45f);d++) {
                    int y=Math.round(glyph.y())+direction*d;
                    if(x<0||x>=width||y<0||y>=height)continue;
                    int p=y*width+x;
                    // Nearby notation does not shelter a fermata dot.
                    if(labels[p]==OmrMeasurePostProcessor.NOTEHEAD
                            ||labels[p]==OmrMeasurePostProcessor.STAFF
                            ||labels[p]==OmrMeasurePostProcessor.STEM_OR_REST)continue;
                    if(horizontalStaffInk(gray,width,height,x,y,gap))continue;
                    if((gray[p]&255)<155)found=true;
                }
                if(found)occupied++;
            }
            if(occupied==5)return true;
        }
        return false;
    }

    /** A missed semantic staff stripe must not become a fermata roof over a dot. */
    private static boolean horizontalStaffInk(byte[] gray,int width,int height,int x,int y,float gap) {
        return horizontalRuleInk(gray,width,height,x,y,gap,155,.85f);
    }

    private static boolean horizontalRuleInk(byte[] gray,int width,int height,int x,int y,float gap,
            int threshold,float coverage) {
        int near=Math.max(3,Math.round(gap*1.5f)),far=Math.round(gap*4f);
        for(int direction:new int[]{-1,1}) {
            int hits=0,samples=0;
            for(int d=near;d<=far;d++) {
                int xx=x+direction*d;
                if(xx<0||xx>=width)return false;
                samples++;
                for(int yy=Math.max(0,y-1);yy<=Math.min(height-1,y+1);yy++)
                    if((gray[yy*width+xx]&255)<threshold){hits++;break;}
            }
            if(samples==0||hits<samples*coverage)return false;
        }
        return true;
    }

    private static int classify(Glyph g,int width,float gap,boolean above) {
        float w=g.right-g.left+1,h=g.bottom-g.top+1;
        float density=g.count/(w*h);
        if(w>=gap*.26f&&w<=gap*.62f&&h>=gap*.26f&&h<=gap*.62f&&g.count>=gap*gap*.065f
                &&w/h>.65f&&w/h<1.55f&&density>.6f)return NoteArticulation.STACCATO;
        if(w>=gap*.65f&&w<=gap*1.65f&&h<=gap*.4f&&w/h>=3.5f&&density>.7f)
            return NoteArticulation.TENUTO;
        if(w>=gap*.75f&&w<=gap*2.1f&&h>=gap*.35f&&h<=gap*1.25f&&w/h>=1.25f
                &&fit(g,width,0))return NoteArticulation.ACCENT;
        // A V above a note is an up-bow. Only an upward peak above / downward peak below
        // may be marcato; flat-topped down-bow squares fail the two-line fit.
        if(w>=gap*.5f&&w<=gap*1.3f&&h>=gap*.6f&&h<=gap*1.7f&&h/w>=.8f
                &&chevronVertical(g,width,above))return NoteArticulation.MARCATO;
        if(w>=gap*.18f&&w<=gap*.65f&&h>=gap*.7f&&h<=gap*1.65f&&h/w>=1.8f
                &&density>.45f&&density<.82f&&wedge(g,width,above))return NoteArticulation.STACCATISSIMO;
        return 0;
    }

    private static boolean chevronVertical(Glyph g,int width,boolean above) {
        return fit(g,width,above?1:2);
    }
    /** Both precision and coverage matter: a slur, text letter or isolated slash is not a >. */
    private static boolean fit(Glyph g,int width,int kind) {
        int hits=0;boolean[] bins=new boolean[12];
        for(int p:g.pixels) {
            double x=(p%width-g.left)/(double)Math.max(1,g.right-g.left);
            double y=(p/width-g.top)/(double)Math.max(1,g.bottom-g.top);
            double a,b;
            if(kind==0){a=x;b=y;}else{a=kind==1?1-y:y;b=x;}
            double expected=1-2*Math.abs(b-.5);
            // Small printed accents have antialiased, rounded stroke edges. Allow less than
            // one source pixel of edge rounding while retaining two-arm coverage.
            double tolerance=.22+.75/Math.max(1,kind==0?g.right-g.left:g.bottom-g.top);
            if(Math.abs(a-expected)<tolerance) {hits++;bins[Math.min(11,(int)(b*12))]=true;}
        }
        int covered=0;for(boolean bin:bins)if(bin)covered++;
        return hits>=g.count*.78&&covered>=10;
    }
    private static boolean wedge(Glyph g,int width,boolean above) {
        int broad=0,tip=0;
        for(int p:g.pixels) {
            double y=(p/width-g.top)/(double)Math.max(1,g.bottom-g.top);
            if(!above)y=1-y;
            if(y<.33)broad++;if(y>.67)tip++;
        }
        return broad>=tip*1.6&&tip>0;
    }
}
