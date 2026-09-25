// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Resolves a locally occluded five-rule phase from independent nearby windows. */
final class NeighboringStaffPhase {
    private NeighboringStaffPhase() {}

    /** Interpolate a missing semantic strip only between complete raw five-rule
     * witnesses on both sides. The intervening ink must also support the same
     * five rules and must not support a competing sixth rule. */
    static float[] rawBracket(byte[] gray,int width,int height,float x,float bottom,float gap) {
        if(gray==null||gap<5)return null;
        List<float[]> samples=new ArrayList<>();
        int leftCount=0,rightCount=0;
        for(int distance=2;distance<=20;distance++) {
            for(int side:new int[]{-1,1}) {
                if(side<0&&leftCount>=3||side>0&&rightCount>=3)continue;
                float at=x+side*distance*gap;
                if(at<gap*4||at>width-gap*4)continue;
                float[] found=StaffPitchTrack.localCurledEdgeRules(gray,width,height,at,
                        Math.round(at-gap*.65f),Math.round(at+gap*.65f),bottom,gap);
                if(found==null)continue;
                samples.add(new float[]{at,found[0],found[1]});
                if(side<0)leftCount++;else rightCount++;
            }
            if(leftCount>=3&&rightCount>=3)break;
        }
        if(leftCount<2||rightCount<2)return null;
        samples.sort((a,b)->Float.compare(a[0],b[0]));
        List<Float> slopes=new ArrayList<>();
        for(float[] a:samples)for(float[] b:samples)if(a[0]<x&&b[0]>x)
            slopes.add((b[1]-a[1])/(b[0]-a[0]));
        float slope=median(slopes);
        if(Math.abs(slope)>.16f)return null;
        List<Float> centers=new ArrayList<>(),gaps=new ArrayList<>();
        for(float[] sample:samples){centers.add(sample[1]+slope*(x-sample[0]));gaps.add(sample[2]);}
        float base=median(centers),spacing=median(gaps);
        if(Math.abs(base-bottom)<gap*.3f||Math.abs(base-bottom)>gap*2.5f
                ||Math.abs(spacing-gap)>gap*.08f)return null;
        for(float[] sample:samples)if(Math.abs(sample[1]+slope*(x-sample[0])-base)>gap*.2f
                ||Math.abs(sample[2]-spacing)>gap*.06f)return null;
        int left=Math.max(0,Math.round(samples.get(0)[0]));
        int right=Math.min(width-1,Math.round(samples.get(samples.size()-1)[0]));
        int occluded=0;
        for(int line=-1;line<=5;line++) {
            float score=support(gray,width,height,left,right,x,base-line*spacing,slope,spacing);
            if(line>=0&&line<5) {
                if(score<.38f||score<.57f&&++occluded>1)return null;
            } else {
                float outsideLeft=support(gray,width,height,Math.max(0,Math.round(left-gap*4)),left,
                        x,base-line*spacing,slope,spacing);
                float outsideRight=support(gray,width,height,right,Math.min(width-1,Math.round(right+gap*4)),
                        x,base-line*spacing,slope,spacing);
                if(score>.5f||outsideLeft>.6f&&outsideRight>.6f)return null;
            }
        }
        return new float[]{base,spacing};
    }

    static float[] resolve(byte[] labels,byte[] gray,int width,int height,float x,float bottom,float gap) {
        if(labels==null||gray==null||gap<5)return null;
        List<float[]> samples=new ArrayList<>();
        for(int offset=-3;offset<=3;offset++) {
            float at=x+offset*gap*2;
            if(at<gap*4||at>width-gap*4)continue;
            float[] found=StaffPitchTrack.localRules(labels,gray,width,height,at,
                    Math.round(at-gap*.65f),Math.round(at+gap*.65f),bottom,gap,true);
            if(found!=null)samples.add(new float[]{at,found[0],found[1]});
        }
        if(samples.size()<4)return null;
        List<Float> slopes=new ArrayList<>();
        for(int i=0;i<samples.size();i++)for(int j=i+1;j<samples.size();j++)
            slopes.add((samples.get(j)[1]-samples.get(i)[1])/(samples.get(j)[0]-samples.get(i)[0]));
        float slope=median(slopes);
        if(Math.abs(slope)>.12f)return null;
        List<Float> centers=new ArrayList<>(),gaps=new ArrayList<>();
        for(float[] s:samples){centers.add(s[1]+slope*(x-s[0]));gaps.add(s[2]);}
        float base=median(centers),spacing=median(gaps);
        // This fallback resolves a displaced phase, not fine scale calibration.
        // A tiny consensus offset can accumulate a half-step on distant ledgers.
        if(Math.abs(base-bottom)<gap*.3f)return null;
        List<float[]> agreed=new ArrayList<>();
        for(float[] s:samples)if(Math.abs(s[1]+slope*(x-s[0])-base)<=gap*.18f
                &&Math.abs(s[2]-spacing)<=gap*.04f)agreed.add(s);
        if(agreed.size()<4||agreed.get(agreed.size()-1)[0]-agreed.get(0)[0]<gap*6
                ||Math.abs(base-bottom)>gap*1.5f||Math.abs(spacing-gap)>gap*.08f)return null;
        int left=Math.max(0,Math.round(agreed.get(0)[0]-gap)),right=Math.min(width-1,Math.round(agreed.get(agreed.size()-1)[0]+gap));
        // Five complete thin rules must survive across the wider consensus area.
        // A sixth parallel rule makes a one-line phase shift ambiguous.
        for(int line=-1;line<=5;line++) {
            float support=support(gray,width,height,left,right,x,base-line*spacing,slope,spacing);
            if(line>=0&&line<5&&support<.57f)return null;
            if((line<0||line==5)&&support>.5f)return null;
        }
        return new float[]{base,spacing};
    }
    private static float support(byte[] gray,int width,int height,int left,int right,float x,float row,float slope,float gap) {
        int band=Math.max(1,Math.round(gap*.13f)),flank=Math.max(2,Math.round(gap*.3f));
        int total=0,ink=0;
        for(int xx=left;xx<=right;xx++) {
            total++;int center=Math.round(row+slope*(xx-x));
            for(int y=Math.max(flank,center-band);y<=Math.min(height-1-flank,center+band);y++) {
                int v=gray[y*width+xx]&255;
                if(v<=205&&(gray[(y-flank)*width+xx]&255)>=v+15&&(gray[(y+flank)*width+xx]&255)>=v+15){ink++;break;}
            }
        }
        return total==0?0:ink/(float)total;
    }
    private static float median(List<Float> values) {
        float[] sorted=new float[values.size()];for(int i=0;i<sorted.length;i++)sorted[i]=values.get(i);
        Arrays.sort(sorted);return sorted[sorted.length/2];
    }
}
