// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

import java.util.*;

/** Curly braces identify a shared keyboard part, not a square-bracketed ensemble. */
final class GrandStaffDynamics {
    record Pair(PlayingTechniqueDetector.Staff upper,PlayingTechniqueDetector.Staff lower) { }
    static List<Pair> bracedPairs(List<PlayingTechniqueDetector.Staff> staffs,List<MeasureRegion> measures,
                                  byte[] gray,int width,int height) {
        List<Pair> result=new ArrayList<>();
        if(gray==null||gray.length!=width*height)return result;
        for(int i=0;i+1<staffs.size();i++) {
            var a=staffs.get(i);var b=staffs.get(i+1);
            if(a.index()!=0||b.index()!=1||a.count()!=2||b.count()!=2)continue;
            float left=1;
            for(var bar:measures)if(bar.top()*height<=a.bottom()&&bar.bottom()*height>=b.top())left=Math.min(left,bar.left());
            if(left==1)continue;
            if(hasBrace(gray,width,height,Math.round(left*width),a.top(),b.bottom(),(a.gap()+b.gap())*.5f))result.add(new Pair(a,b));
        }
        return result;
    }
    static PlayingTechniqueDetector.Staff between(List<Pair> pairs,float top,float bottom) {
        for(var pair:pairs)if(top>pair.upper.bottom()&&bottom<pair.lower.top())return pair.upper;
        return null;
    }
    static boolean hasBrace(byte[] gray,int width,int height,int beforeX,float top,float bottom,float gap) {
        int left=Math.max(0,Math.round(beforeX-14*gap)),right=Math.min(width,beforeX);
        int y0=Math.max(0,Math.round(top-gap)),y1=Math.min(height,Math.round(bottom+gap));
        int w=right-left,h=y1-y0;if(w<=0||h<=0)return false;
        boolean[] seen=new boolean[w*h];int[] queue=new int[w*h];
        for(int p=0;p<seen.length;p++) {
            if(seen[p]||(gray[(p/w+y0)*width+p%w+left]&255)>=145)continue;
            int n=1,start=0;queue[0]=p;seen[p]=true;int minX=p%w,maxX=minX,minY=p/w,maxY=minY;
            while(start<n) {
                int at=queue[start++],x=at%w,y=at/w;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int nx=x+dx,ny=y+dy;if(nx<0||nx>=w||ny<0||ny>=h)continue;int next=ny*w+nx;
                    if(!seen[next]&&(gray[(ny+y0)*width+nx+left]&255)<145){seen[next]=true;queue[n++]=next;}
                }
            }
            if(Math.abs(minY+y0-top)>gap||Math.abs(maxY+y0-bottom)>gap||maxX-minX<gap*.4||maxX-minX>gap*2.5)continue;
            double[] sum=new double[3];int[] count=new int[3];
            for(int q=0;q<n;q++) {
                int at=queue[q];double fraction=(at/w-minY)/(double)Math.max(1,maxY-minY);
                for(int band=0;band<3;band++)if(Math.abs(fraction-(.35+band*.15))<.025){sum[band]+=at%w;count[band]++;}
            }
            if(count[0]==0||count[1]==0||count[2]==0)continue;
            double a=sum[0]/count[0],mid=sum[1]/count[1],b=sum[2]/count[2];
            // A brace has an outward central cusp and inward shoulders. A bracket is straight.
            if(a-mid>gap*.22&&b-mid>gap*.22&&Math.abs(a-b)<gap*.35)return true;
        }
        return false;
    }
}
