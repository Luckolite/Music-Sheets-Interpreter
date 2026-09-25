// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;

/** Counts parallel dark cores joining two independently attached stems. */
final class PairedGraceBeamInk {
    private PairedGraceBeamInk() { }
    static int count(byte[] gray,int width,int height,int[] a,int[] b,float gap) {
        return count(gray,width,height,a,b,gap,1.5f);
    }
    static int countFullSize(byte[] gray,int width,int height,int[] a,int[] b,float gap) {
        return count(gray,width,height,a,b,gap,2.2f);
    }
    private static int count(byte[] gray,int width,int height,int[] a,int[] b,float gap,float inside) {
        if(gray==null||a==null||b==null||gap<4||a[2]!=b[2])return 0;
        int span=Math.abs(a[0]-b[0]);
        if(span<gap*.95f||span>gap*3||Math.abs(a[1]-b[1])>gap*.75f)return 0;
        int maximum=0;
        for(float fraction:new float[]{.5f,.75f,1f}) {
            int count=countAtContrast(gray,width,height,a,b,gap,fraction,inside);
            if(count>0&&inside==1.5f)return count;
            maximum=Math.max(maximum,count);
        }
        return maximum;
    }
    private static int countAtContrast(byte[] gray,int width,int height,int[] a,int[] b,float gap,float fraction,float inside) {
        int wanted=0;float[] previous=null;
        for(float f:new float[]{.25f,.5f,.75f}) {
            int x=Math.round(a[0]+(b[0]-a[0])*f);
            float end=a[1]+(b[1]-a[1])*f;
            int top=Math.max(1,Math.round(end-(a[2]<0?.35f:inside)*gap));
            int bottom=Math.min(height-2,Math.round(end+(a[2]<0?inside:.35f)*gap));
            int threshold=BeamInkThreshold.at(gray,width,height,x,top,bottom,gap);
            int darkest=threshold;
            for(int y=top;y<=bottom;y++)darkest=Math.min(darkest,gray[y*width+x]&255);
            threshold=darkest+Math.round((threshold-darkest)*fraction);
            List<Float> cores=new ArrayList<>();int start=-1;
            for(int y=top;y<=bottom+1;y++) {
                boolean ink=y<=bottom&&(gray[y*width+x-1]&255)<threshold
                        &&(gray[y*width+x]&255)<threshold&&(gray[y*width+x+1]&255)<threshold;
                if(ink&&start<0)start=y;
                if(!ink&&start>=0) {
                    int size=y-start;
                    if(size>=Math.max(3,Math.round(gap*(inside==1.5f?.18f:.28f)))&&size<=gap*(inside==1.5f?.6f:.8f))cores.add((start+y-1)*.5f-end);
                    start=-1;
                }
            }
            if(cores.size()<2||cores.size()>3)return 0;
            if(wanted!=0&&wanted!=cores.size())return 0;
            wanted=cores.size();float[] current=new float[wanted];
            for(int i=0;i<wanted;i++) {
                current[i]=cores.get(i);
                if(i>0&&(current[i]-current[i-1]<gap*.3f||current[i]-current[i-1]>gap*.9f))return 0;
                if(previous!=null&&Math.abs(current[i]-previous[i])>gap*.22f)return 0;
            }
            previous=current;
        }
        return wanted;
    }
}
