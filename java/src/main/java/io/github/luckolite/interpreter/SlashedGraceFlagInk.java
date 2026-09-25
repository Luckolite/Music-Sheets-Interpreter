// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;

/** Separates descending grace flags from the oppositely sloping acciaccatura slash. */
final class SlashedGraceFlagInk {
    private SlashedGraceFlagInk() { }
    static int count(byte[] gray,int width,int height,float headX,float headY,int stemX,float gap) {
        if(gray==null||width<=0||height<=0||gray.length!=(long)width*height||gap<5||!Float.isFinite(gap)
                ||!Float.isFinite(headX)||!Float.isFinite(headY)||stemX<0||stemX>=width)return 0;
        int left=stemX+Math.max(2,Math.round(gap*.15f)),right=stemX+Math.max(5,Math.round(gap*.65f));
        int top=Math.max(1,Math.round(headY-gap*2.8f)),bottom=Math.min(height-2,Math.round(headY-gap*.45f));
        if(left<1||right>=width-1||top>=bottom)return 0;
        int threshold=BeamInkThreshold.at(gray,width,height,stemX,top,bottom,gap);
        for(float contrast:new float[]{.8f,1f}) {
            int th=Math.round(threshold*contrast);var roots=new ArrayList<Integer>();boolean slash=false;
            for(int y=top;y<=bottom;y++) {
                boolean down=false;
                for(float slope:new float[]{.75f,1f,1.25f,1.5f,1.75f,2f}) {
                    if(y+(right-left)*slope<=bottom)down|=line(gray,width,height,left,right,y,slope,th);
                    slash|=line(gray,width,height,left,right,y,-slope,th);
                }
                if(down)roots.add(y);
            }
            if(!slash||roots.isEmpty())continue;
            int bands=1,previous=roots.get(0);for(int y:roots){if(y-previous>Math.max(2,Math.round(gap*.16f)))bands++;previous=y;}
            if(bands>0&&bands<=3)return bands;
        }
        return 0;
    }
    private static boolean line(byte[] gray,int width,int height,int left,int right,int y,float slope,int threshold) {
        int hits=0,total=0;
        for(int x=left;x<=right;x++){int yy=Math.round(y+(x-left)*slope);if(yy<1||yy>=height-1)return false;total++;if((gray[yy*width+x]&255)<threshold||(gray[(yy-1)*width+x]&255)<threshold||(gray[(yy+1)*width+x]&255)<threshold)hits++;}
        return hits>=total*.90f;
    }
}
