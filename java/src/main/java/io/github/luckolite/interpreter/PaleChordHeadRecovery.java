// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Recover a vertically touching chord only from a closed, shaded, lobed printed pocket. */
final class PaleChordHeadRecovery {
    private PaleChordHeadRecovery() { }
    record Head(int left,int top,int right,int bottom,float centerX,float centerY,float stemHeadY) { }

    static List<Head> find(byte[] labels,byte[] gray,int width,int height,float gap,int top,int bottom) {
        List<Head> result=new ArrayList<>();
        if(width<1||height<1||gray==null||labels==null||gray.length!=(long)width*height
                ||labels.length!=gray.length||gap<6||!Float.isFinite(gap))return result;
        top=Math.max(0,top);bottom=Math.min(height-1,bottom);
        if(bottom<=top)return result;
        int size=width*(bottom-top+1);boolean[] seen=new boolean[size];int[] queue=new int[size];
        for(int seed=0;seed<size;seed++) {
            if(seen[seed]||(gray[top*width+seed]&255)<=150)continue;
            int read=0,end=1;queue[0]=seed;seen[seed]=true;
            int left=width,right=0,upper=bottom,lower=top;
            while(read<end) {
                int p=queue[read++],x=p%width,y=top+p/width;
                left=Math.min(left,x);right=Math.max(right,x);upper=Math.min(upper,y);lower=Math.max(lower,y);
                for(int direction=0;direction<4;direction++) {
                    int nx=x+(direction==0?-1:direction==1?1:0),ny=y+(direction==2?-1:direction==3?1:0);
                    if(nx<0||nx>=width||ny<top||ny>bottom)continue;
                    int next=(ny-top)*width+nx;
                    if(!seen[next]&&(gray[ny*width+nx]&255)>150){seen[next]=true;queue[end++]=next;}
                }
            }
            int w=right-left+1,h=lower-upper+1;
            if(left==0||right==width-1||upper==top||lower==bottom||w<gap*.55f||w>gap*1.5f
                    ||h<gap*1.45f||h>gap*3.85f||end<w*h*.55f)continue;
            int heads=Math.round(h/gap+.4f);
            if(heads<2||heads>4||Math.abs(h/gap-(heads-.4f))>.32f)continue;
            int[] values=new int[end],rowLeft=new int[h],rowRight=new int[h];Arrays.fill(rowLeft,width);
            for(int i=0;i<end;i++) {
                int p=queue[i],x=p%width,y=top+p/width;
                values[i]=gray[y*width+x]&255;
                rowLeft[y-upper]=Math.min(rowLeft[y-upper],x);rowRight[y-upper]=Math.max(rowRight[y-upper],x);
            }
            Arrays.sort(values);int interior=values[end/2];if(interior>205)continue;
            int[] paper=new int[18];int count=0,cx=(left+right)/2,cy=(upper+lower)/2;
            for(int side:new int[]{-1,1})for(int dy=-1;dy<=1;dy++)for(int dx=0;dx<3;dx++) {
                int x=cx+side*Math.round(gap*(1.3f+dx*.2f)),y=cy+Math.round(dy*gap*.2f);
                if(x>=0&&x<width&&y>=0&&y<height)paper[count++]=gray[y*width+x]&255;
            }
            if(count<12)continue;
            Arrays.sort(paper,0,count);
            if(paper[count*3/4]<190||paper[count*3/4]-interior<22)continue;
            // Inter-head necks return inward relative to the neighbouring lobes.
            // Staff cells and long rectangular stem gaps cannot supply this outline.
            int proven=0;
            for(int i=0;i<heads-1;i++) {
                float a=gap*(.4f+i),b=a+gap,neck=(a+b)*.5f;
                float la=average(rowLeft,a,gap*.13f),lb=average(rowLeft,b,gap*.13f);
                float ln=average(rowLeft,neck,gap*.10f);
                float ra=average(rowRight,a,gap*.13f),rb=average(rowRight,b,gap*.13f);
                float rn=average(rowRight,neck,gap*.10f);
                if(Math.max(ln-(la+lb)*.5f,(ra+rb)*.5f-rn)>=Math.max(.7f,gap*.045f))proven++;
            }
            // At most one neck may be buried under a staff/ledger crossing;
            // the complete closed pocket still fixes the number and spacing.
            if(proven<Math.max(1,heads-2))continue;
            boolean up=stem(labels,gray,width,height,right+2,upper+gap*.4f,gap,-1);
            boolean down=stem(labels,gray,width,height,left-2,lower-gap*.2f,gap,1);
            if(!up&&!down)continue;
            float stemHeadY=upper+gap*(.4f+(up?0:heads-1));
            for(int i=0;i<heads;i++) {
                float y=upper+gap*(.4f+i);
                result.add(new Head(left-2,Math.round(y-gap*.45f),right+2,Math.round(y+gap*.45f),(left+right)*.5f,y,stemHeadY));
            }
        }
        return result;
    }
    private static float average(int[] values,float center,float radius) {
        int first=Math.max(0,Math.round(center-radius)),last=Math.min(values.length-1,Math.round(center+radius));
        float sum=0;for(int i=first;i<=last;i++)sum+=values[i];return sum/Math.max(1,last-first+1);
    }
    private static boolean stem(byte[] labels,byte[] gray,int width,int height,int edge,float cy,float gap,int direction) {
        for(int x=Math.max(0,edge-3);x<=Math.min(width-1,edge+3);x++) {
            int ink=0,semantic=0,total=0;
            for(int d=Math.round(gap*.7f);d<=gap*2.4f;d++) {
                int y=Math.round(cy)+direction*d;if(y<0||y>=height)return false;
                total++;if((gray[y*width+x]&255)<150)ink++;
                if(labels[y*width+x]==OmrMeasurePostProcessor.STEM_OR_REST)semantic++;
            }
            if(total>0&&ink>=total*.85f&&semantic>=total*.5f)return true;
        }
        return false;
    }
}
