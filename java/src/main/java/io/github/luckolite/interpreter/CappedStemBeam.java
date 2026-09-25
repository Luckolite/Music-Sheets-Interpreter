// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Extends a trace stopped by the nine-gap work bound only to a proven beam. */
final class CappedStemBeam {
    private CappedStemBeam() { }
    static int[] extend(byte[] gray,int width,int height,int[] stem,float headY,float gap,int threshold) {
        if(gray==null||stem==null||Math.abs(stem[1]-headY)<gap*8.8f)return stem;
        int x=stem[0],end=stem[1],blank=0;
        if(x<1||x>=width-1)return stem;
        for(int d=1;d<=Math.round(gap*5);d++) {
            int y=stem[1]+stem[2]*d;
            if(y<1||y>=height-1)break;
            if((gray[y*width+x]&255)<threshold){end=y;blank=0;}
            else if(++blank>1)break;
        }
        if(Math.abs(end-stem[1])<gap*.5f||Math.abs(end-stem[1])>gap*4.8f)return stem;
        for(int side:new int[]{-1,1}) {
            int run=0;
            for(int d=0;d<=Math.round(gap*.6f);d++) {
                int y=end-stem[2]*d;if(y<1||y>=height-1)break;
                int ink=0,total=0;
                for(int dx=Math.max(2,Math.round(gap*.25f));dx<=Math.round(gap*1.1f);dx++) {
                    int xx=x+side*dx;if(xx<0||xx>=width)continue;
                    total++;if((gray[y*width+xx]&255)<threshold)ink++;
                }
                if(total>=gap*.7f&&ink>=total*.85f)run++;else run=0;
                if(run>=Math.max(3,Math.round(gap*.2f)))return new int[]{x,end,stem[2]};
            }
        }
        return stem;
    }
}
