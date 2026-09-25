// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Uniform grey engraving fill is not a white counter in a beamed notehead. */
final class PaleFilledHeadCore {
    private PaleFilledHeadCore() { }
    static boolean isFilled(byte[] gray,int width,int height,int left,int top,int right,int bottom,
                            float centerX,float centerY,float gap) {
        if(gray==null||gray.length!=width*height||gap<6)return false;
        float w=right-left+1,h=bottom-top+1;
        if(w<gap*1.05f||h<gap*.75f||w>gap*1.9f||h>gap*1.4f)return false;
        int[] shades=new int[256];int total=0,margin=Math.round(gap);
        for(int y=Math.max(0,top-margin);y<=Math.min(height-1,bottom+margin);y++)
            for(int x=Math.max(0,left-margin);x<=Math.min(width-1,right+margin);x++) {
                shades[gray[y*width+x]&255]++;total++;
            }
        int paper=255,seen=0;
        for(int value=0;value<256;value++)if((seen+=shades[value])>=total*.8){paper=value;break;}
        if(paper<210)return false;
        int count=0,filled=0,pale=0;
        for(int y=Math.max(0,Math.round(centerY-h*.25f));y<=Math.min(height-1,Math.round(centerY+h*.25f));y++)
            for(int x=Math.max(0,Math.round(centerX-w*.2f));x<=Math.min(width-1,Math.round(centerX+w*.2f));x++) {
                int value=gray[y*width+x]&255;count++;
                if(value<=paper-25)filled++;
                if(value>=145&&value<=paper-25)pale++;
            }
        return count>=12&&filled>=count*.95f&&pale>=count*.7f;
    }
}
