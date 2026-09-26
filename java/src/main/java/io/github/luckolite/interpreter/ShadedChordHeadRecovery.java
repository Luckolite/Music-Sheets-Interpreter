// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;

/** Split a curved, uniformly shaded chord body using its independently printed lobes. */
final class ShadedChordHeadRecovery {
    private ShadedChordHeadRecovery() { }
    record Head(int left,int top,int right,int bottom,float centerX,float centerY) { }
    record Chord(int left,int top,int right,int bottom,List<Head> heads) { }
    static List<Chord> find(byte[] labels,byte[] gray,int width,int height,float gap,int top,int bottom) {
        List<Chord> result=find(labels,gray,width,height,gap,top,bottom,155);
        for(Chord candidate:find(labels,gray,width,height,gap,top,bottom,130)) {
            boolean overlaps=false;
            for(Chord existing:result)if(candidate.right>=existing.left&&candidate.left<=existing.right
                    &&candidate.bottom>=existing.top&&candidate.top<=existing.bottom){overlaps=true;break;}
            if(!overlaps)result.add(candidate);
        }
        return result;
    }
    private static List<Chord> find(byte[] labels,byte[] gray,int width,int height,float gap,int top,int bottom,int minimum) {
        List<Chord> result=new ArrayList<>();
        if(gray==null||labels==null||width<3||height<3||gray.length!=(long)width*height
                ||labels.length!=gray.length||gap<8||!Float.isFinite(gap))return result;
        top=Math.max(1,top);bottom=Math.min(height-2,bottom);if(bottom<=top)return result;
        int size=width*(bottom-top+1);boolean[] core=new boolean[size],seen=new boolean[size];int[] queue=new int[size];
        for(int y=top;y<=bottom;y++)for(int x=1;x<width-1;x++){
            int p=y*width+x;core[(y-top)*width+x]=middle(gray[p],minimum)&&middle(gray[p-1],minimum)&&middle(gray[p+1],minimum)&&middle(gray[p-width],minimum)&&middle(gray[p+width],minimum);
        }
        for(int seed=0;seed<size;seed++){
            if(seen[seed]||!core[seed])continue;
            int read=0,end=1;queue[0]=seed;seen[seed]=true;int left=width,right=0,upper=bottom,lower=top;
            while(read<end){
                int p=queue[read++],x=p%width,y=top+p/width;left=Math.min(left,x);right=Math.max(right,x);upper=Math.min(upper,y);lower=Math.max(lower,y);
                for(int direction=0;direction<4;direction++){
                    int nx=x+(direction==0?-1:direction==1?1:0),ny=y+(direction==2?-1:direction==3?1:0);
                    if(nx<0||nx>=width||ny<top||ny>bottom)continue;
                    int next=(ny-top)*width+nx;if(!seen[next]&&core[next]){seen[next]=true;queue[end++]=next;}
                }
            }
            int w=right-left+1,h=lower-upper+1;
            if(w<gap*.55f||w>gap*1.5f||h<gap*1.35f||h>gap*4.2f||end<w*h*.5f||upper==top||lower==bottom)continue;
            int count=Math.round(h/gap+.2f);if(count<2||count>4)continue;
            float headHeight=h-(count-1)*gap;
            if(headHeight<gap*.45f||headHeight>gap*1.15f)continue;
            int[] rowLeft=new int[h],rowRight=new int[h];Arrays.fill(rowLeft,width);
            for(int i=0;i<end;i++){int x=queue[i]%width,y=top+queue[i]/width;rowLeft[y-upper]=Math.min(rowLeft[y-upper],x);rowRight[y-upper]=Math.max(rowRight[y-upper],x);}
            float first=(h-1-(count-1)*gap)*.5f;int[] peaks=new int[count],peakRows=new int[count];
            boolean curved=true;
            for(int i=0;i<count;i++){
                int a=Math.max(0,Math.round(first+i*gap-gap*.28f)),b=Math.min(h-1,Math.round(first+i*gap+gap*.28f));
                for(int y=a;y<=b;y++){int span=rowRight[y]-rowLeft[y]+1;if(span>peaks[i]){peaks[i]=span;peakRows[i]=y;}}
                if(peaks[i]<gap*.55f)curved=false;
            }
            boolean necked=true;
            for(int i=1;i<count;i++){
                int neck=Integer.MAX_VALUE;
                for(int y=peakRows[i-1]+1;y<peakRows[i];y++)neck=Math.min(neck,rowRight[y]-rowLeft[y]+1);
                if(neck>Math.min(peaks[i-1],peaks[i])-Math.max(1.5f,gap*.1f))necked=false;
            }
            if(!necked&&!outlinedNecks(gray,width,height,left,right,upper,lower,first,count,gap))curved=false;
            if(rowRight[0]-rowLeft[0]+1>peaks[0]*.9f&&!clippedByRule(gray,width,height,left,right,upper,gap))curved=false;
            if(rowRight[h-1]-rowLeft[h-1]+1>peaks[count-1]*.9f&&!clippedByRule(gray,width,height,left,right,lower,gap))curved=false;
            if(!curved)continue;
            float cx=(left+right)*.5f,cy=(upper+lower)*.5f;
            int[] paper=new int[18];int samples=0;
            for(int side:new int[]{-1,1})for(int dy=-1;dy<=1;dy++)for(int dx=0;dx<3;dx++){
                int x=Math.round(cx+side*gap*(1.3f+dx*.2f)),y=Math.round(cy+dy*gap*.2f);
                if(x>=0&&x<width&&y>=0&&y<height)paper[samples++]=gray[y*width+x]&255;
            }
            Arrays.sort(paper,0,samples);if(samples<12)continue;
            int[] fills=new int[end];for(int i=0;i<end;i++)fills[i]=gray[(top+queue[i]/width)*width+queue[i]%width]&255;
            Arrays.sort(fills);int fill=fills[end/2];
            if(minimum==130) {
                if(fill>170||paper[samples*3/4]<190||paper[samples*3/4]-fill<50)continue;
            } else if(paper[samples*3/4]<205||paper[samples*3/4]<220&&paper[samples*3/4]-fill<50)continue;
            int down=stem(labels,gray,width,height,left-3,lower-first,gap,1);
            int up=stem(labels,gray,width,height,right+3,upper+first,gap,-1);
            if(down<0&&up<0)continue;
            int l=Math.max(0,down<0?left-3:Math.min(left-3,down)),r=Math.min(width-1,up<0?right+3:Math.max(right+3,up));
            List<Head> heads=new ArrayList<>();
            for(int i=0;i<count;i++){
                float y=upper+first+i*gap;
                heads.add(new Head(l,Math.max(0,Math.round(y-gap*.45f)),r,Math.min(height-1,Math.round(y+gap*.45f)),cx,y));
            }
            result.add(new Chord(l,Math.max(0,upper-3),r,Math.min(height-1,lower+3),List.copyOf(heads)));
        }
        return result;
    }
    private static boolean middle(byte pixel,int minimum){int value=pixel&255;return value>=minimum&&value<=205;}
    private static boolean outlinedNecks(byte[] gray,int width,int height,int left,int right,int top,int bottom,float first,int count,float gap) {
        for(int side:new int[]{-1,1}){
            int outer=Math.round((side<0?left:right)+side*gap*.6f);
            int inner=Math.round((side<0?left:right)-side*gap*.25f);
            if(outer<0||outer>=width||inner<0||inner>=width)continue;
            float[] edge=new float[bottom-top+1];Arrays.fill(edge,Float.NaN);
            for(int y=top;y<=bottom;y++)for(int x=outer;side<0?x<=inner:x>=inner;x-=side){
                if((gray[y*width+x]&255)<140){
                    if(Math.abs(x-outer)>1)edge[y-top]=x*side;
                    break;
                }
            }
            float[] lobes=new float[count];boolean complete=true;
            for(int i=0;i<count;i++){
                float peak=Float.NEGATIVE_INFINITY;
                int a=Math.max(0,Math.round(first+i*gap-gap*.3f)),b=Math.min(edge.length-2,Math.round(first+i*gap+gap*.3f));
                for(int y=a;y<=b;y++)if(Float.isFinite(edge[y])&&Float.isFinite(edge[y+1]))peak=Math.max(peak,Math.min(edge[y],edge[y+1]));
                lobes[i]=peak;if(!Float.isFinite(peak))complete=false;
            }
            for(int i=1;i<count;i++){
                float neck=Float.POSITIVE_INFINITY;
                int a=Math.max(0,Math.round(first+(i-.5f)*gap-gap*.3f)),b=Math.min(edge.length-2,Math.round(first+(i-.5f)*gap+gap*.3f));
                for(int y=a;y<=b;y++)if(Float.isFinite(edge[y])&&Float.isFinite(edge[y+1]))neck=Math.min(neck,Math.max(edge[y],edge[y+1]));
                if(!Float.isFinite(neck)||neck>Math.min(lobes[i-1],lobes[i])-Math.max(1.5f,gap*.1f))complete=false;
            }
            if(complete)return true;
        }
        return false;
    }
    private static boolean clippedByRule(byte[] gray,int width,int height,int left,int right,int edge,float gap) {
        int begin=Math.max(3,Math.round(gap*.45f)),reach=Math.max(begin+2,Math.round(gap*.75f)),offset=Math.max(2,Math.round(gap*.2f));
        if(left-reach<0||right+reach>=width)return false;
        for(int y=Math.max(offset,Math.round(edge-gap*.4f));y<=Math.min(height-1-offset,Math.round(edge+gap*.4f));y++) {
            boolean both=true;
            for(int side:new int[]{-1,1}) {
                int ink=0;
                for(int d=begin;d<=reach;d++) {
                    int x=side<0?left-d:right+d,v=gray[y*width+x]&255;
                    if(v<150&&(gray[(y-offset)*width+x]&255)>v+50&&(gray[(y+offset)*width+x]&255)>v+50)ink++;
                }
                if(ink<(reach-begin+1)*.65f)both=false;
            }
            if(both)return true;
        }
        return false;
    }
    /** Grey fill can occlude the middle of a ledger, but both short printed rails must remain. */
    static boolean hasLedgerRails(byte[] gray,int width,int height,int left,int right,float cy,float top,float bottom,float gap){
        if(gray==null||width<1||height<1||gray.length!=(long)width*height||gap<8||!Float.isFinite(gap)
                ||left<0||right<left||right>=width||!Float.isFinite(cy)||!Float.isFinite(top)||!Float.isFinite(bottom))return false;
        float distance=cy<top?top-cy:cy-bottom,direction=cy<top?-1:1,edge=cy<top?top:bottom;
        int count=(int)Math.floor(distance/gap+.3f);if(count<2||count>4)return false;
        int flank=Math.max(2,Math.round(gap*.2f));
        for(int i=1;i<=count;i++){
            float expected=edge+direction*gap*i;int[] centers=new int[2];
            for(int side=0;side<2;side++){
                int a=side==0?Math.max(0,left-Math.round(gap*.65f)):right+1;
                int b=side==0?left-1:Math.min(width-1,right+Math.round(gap*.65f));
                int peak=0,row=-1;
                for(int y=Math.max(flank,Math.round(expected-gap*.3f));y<=Math.min(height-1-flank,Math.round(expected+gap*.3f));y++){
                    int support=0;
                    for(int x=a;x<=b;x++){
                        int ink=gray[y*width+x]&255;
                        if(ink<160&&(gray[(y-flank)*width+x]&255)>ink+40&&(gray[(y+flank)*width+x]&255)>ink+40)support++;
                    }
                    if(support>peak){peak=support;row=y;}
                }
                if(peak<Math.max(3,Math.round(gap*.2f)))return false;centers[side]=row;
            }
            if(Math.abs(centers[0]-centers[1])>gap*.2f)return false;
        }
        return true;
    }
    private static int stem(byte[] labels,byte[] gray,int width,int height,int edge,float cy,float gap,int direction){
        for(int x=Math.max(0,edge-3);x<=Math.min(width-1,edge+3);x++){
            int ink=0,semantic=0,total=0;
            for(int d=Math.round(gap*.75f);d<=gap*2.3f;d++){
                int y=Math.round(cy)+direction*d;if(y<0||y>=height)return -1;
                total++;if((gray[y*width+x]&255)<160)ink++;if(labels[y*width+x]==OmrMeasurePostProcessor.STEM_OR_REST)semantic++;
            }
            if(total>0&&ink>=total*.85f&&semantic>=total*.5f)return x;
        }
        return -1;
    }
}
