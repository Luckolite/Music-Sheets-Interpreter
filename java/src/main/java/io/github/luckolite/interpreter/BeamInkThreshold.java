// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Keeps shaded paper out of the narrow ink bands used to count beams. */
final class BeamInkThreshold {
    private BeamInkThreshold() { }
    static int at(byte[] gray,int width,int height,int x,int top,int bottom,float gap) {
        if(gray==null||gap<3)return 165;
        int[] histogram=new int[256];int total=0;
        int margin=Math.max(4,Math.round(gap));
        for(int y=Math.max(0,top-margin);y<=Math.min(height-1,bottom+margin);y++)
            for(int xx=Math.max(0,x-margin*2);xx<=Math.min(width-1,x+margin*2);xx++) {
                histogram[gray[y*width+xx]&255]++;total++;
            }
        int target=(total*3+3)/4,seen=0,paper=255;
        for(int i=0;i<256;i++){seen+=histogram[i];if(seen>=target){paper=i;break;}}
        return Math.max(40,Math.min(165,paper-32));
    }
}
