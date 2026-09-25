// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** A tiny semantic island at one end of a complete beam owned by two real stems. */
final class StemOwnedBeamTip {
    private StemOwnedBeamTip() { }
    static boolean matches(byte[] gray,int w,int h,int[] a,int[] b,float cx,float cy,
            int boxWidth,int boxHeight,int area,float gap) {
        if(gray==null||gap<8||a==null||b==null||a[2]!=b[2]
                ||boxWidth>gap*.75f||boxHeight>gap*.5f||area>gap*gap*.25f)return false;
        int[] left=a[0]<b[0]?a:b,right=a[0]<b[0]?b:a;
        float dx=right[0]-left[0],slope=(right[1]-left[1])/dx;
        if(dx<gap*2||dx>gap*6||Math.abs(slope)>.6f
                ||Math.min(Math.abs(cx-left[0]),Math.abs(cx-right[0]))>gap*.5f)return false;
        int threshold=BeamInkThreshold.at(gray,w,h,Math.round(cx),Math.round(cy-gap*2),Math.round(cy+gap*2),gap);
        int margin=Math.max(3,Math.round(gap*.22f)),radius=Math.round(gap);
        for(int shift=-Math.round(gap*.5f);shift<=Math.round(gap*.5f);shift++) {
            if(Math.abs(left[1]+slope*(cx-left[0])+shift-cy)>gap*.22f)continue;
            int total=0,valid=0,near=0,nearValid=0;
            for(int x=left[0]+margin;x<=right[0]-margin;x++) {
                int y=Math.round(left[1]+slope*(x-left[0])+shift);
                if(x<0||x>=w||y-radius<0||y+radius>=h)return false;
                total++;boolean okay=false;
                if((gray[y*w+x]&255)<threshold) {
                    int top=y,bottom=y;
                    while(top>y-radius&&(gray[(top-1)*w+x]&255)<threshold)top--;
                    while(bottom<y+radius&&(gray[(bottom+1)*w+x]&255)<threshold)bottom++;
                    int span=bottom-top+1;
                    okay=span>=gap*.25f&&span<=gap*.9f;
                }
                if(okay)valid++;
                if(Math.abs(x-cx)<=gap*.85f){near++;if(okay)nearValid++;}
            }
            if(total>=gap*1.5f&&valid>=total*.9f&&near>=4&&nearValid>=near*.9f)return true;
        }
        return false;
    }
}
