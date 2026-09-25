// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Recover a compact, curved grey fill even when its dark outline has scan gaps. */
final class ShadedNoteheadRecovery {
    private ShadedNoteheadRecovery() { }
    record Head(int left,int top,int right,int bottom,float centerX,float centerY) { }
    static List<Head> find(byte[] labels,byte[] gray,int width,int height,float gap,int top,int bottom) {
        List<Head> result=new ArrayList<>();
        if(gray==null||labels==null||width<3||height<3||gray.length!=(long)width*height
                ||labels.length!=gray.length||gap<6||!Float.isFinite(gap))return result;
        top=Math.max(1,top);bottom=Math.min(height-2,bottom);if(bottom<=top)return result;
        int size=width*(bottom-top+1);boolean[] core=new boolean[size],seen=new boolean[size];int[] queue=new int[size];
        for(int y=top;y<=bottom;y++)for(int x=1;x<width-1;x++) {
            int p=y*width+x;
            core[(y-top)*width+x]=middle(gray[p])&&middle(gray[p-1])&&middle(gray[p+1])
                    &&middle(gray[p-width])&&middle(gray[p+width]);
        }
        for(int seed=0;seed<size;seed++) {
            if(seen[seed]||!core[seed])continue;
            int read=0,end=1;queue[0]=seed;seen[seed]=true;
            int left=width,right=0,upper=bottom,lower=top;
            while(read<end) {
                int p=queue[read++],x=p%width,y=top+p/width;
                left=Math.min(left,x);right=Math.max(right,x);upper=Math.min(upper,y);lower=Math.max(lower,y);
                for(int direction=0;direction<4;direction++) {
                    int nx=x+(direction==0?-1:direction==1?1:0),ny=y+(direction==2?-1:direction==3?1:0);
                    if(nx<0||nx>=width||ny<top||ny>bottom)continue;
                    int next=(ny-top)*width+nx;
                    if(!seen[next]&&core[next]){seen[next]=true;queue[end++]=next;}
                }
            }
            int w=right-left+1,h=lower-upper+1;
            if(w<gap*.55f||w>gap*1.35f||h<gap*.35f||h>gap*1.05f||w<h*.65f||w>h*2.4f
                    ||end<w*h*.65f||upper==top||lower==bottom)continue;
            int[] rowLeft=new int[h],rowRight=new int[h],values=new int[end];Arrays.fill(rowLeft,width);
            for(int i=0;i<end;i++) {
                int x=queue[i]%width,y=top+queue[i]/width;values[i]=gray[y*width+x]&255;
                rowLeft[y-upper]=Math.min(rowLeft[y-upper],x);rowRight[y-upper]=Math.max(rowRight[y-upper],x);
            }
            int middle=h/2,edge=Math.max(0,Math.round(h*.12f));
            float bow=Math.max((rowLeft[edge]+rowLeft[h-1-edge])*.5f-rowLeft[middle],
                    rowRight[middle]-(rowRight[edge]+rowRight[h-1-edge])*.5f);
            if(bow<gap*.05f)continue;
            Arrays.sort(values);int fill=values[end/2];
            int[] paper=new int[18];int count=0;float cx=(left+right)*.5f,cy=(upper+lower)*.5f;
            for(int side:new int[]{-1,1})for(int dy=-1;dy<=1;dy++)for(int dx=0;dx<3;dx++) {
                int x=Math.round(cx+side*gap*(1.3f+dx*.2f)),y=Math.round(cy+dy*gap*.2f);
                if(x>=0&&x<width&&y>=0&&y<height)paper[count++]=gray[y*width+x]&255;
            }
            Arrays.sort(paper,0,count);if(count<12||paper[count*3/4]<220||paper[count*3/4]-fill<30)continue;
            int down=stem(labels,gray,width,height,left-3,cy,gap,1);
            int up=stem(labels,gray,width,height,right+3,cy,gap,-1);
            if(down<0&&up<0)continue;
            int headLeft=Math.max(0,down<0?left-3:Math.min(left-3,down));
            int headRight=Math.min(width-1,up<0?right+3:Math.max(right+3,up));
            result.add(new Head(headLeft,Math.max(0,upper-1),headRight,Math.min(height-1,lower+1),
                    Math.max(headLeft,Math.min(headRight,cx)),cy));
        }
        return result;
    }
    private static boolean middle(byte pixel){int value=pixel&255;return value>=155&&value<=205;}
    private static int stem(byte[] labels,byte[] gray,int width,int height,int edge,float cy,float gap,int direction) {
        for(int x=Math.max(0,edge-3);x<=Math.min(width-1,edge+3);x++) {
            int ink=0,semantic=0,total=0;
            for(int d=Math.round(gap*.75f);d<=gap*2.3f;d++) {
                int y=Math.round(cy)+direction*d;if(y<0||y>=height)return -1;
                total++;if((gray[y*width+x]&255)<160)ink++;
                if(labels[y*width+x]==OmrMeasurePostProcessor.STEM_OR_REST)semantic++;
            }
            if(total>0&&ink>=total*.85f&&semantic>=total*.5f)return x;
        }
        return -1;
    }
}
