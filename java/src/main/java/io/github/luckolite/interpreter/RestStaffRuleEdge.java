// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Extends an already proved staff-rule mask by one locally continuous edge row. */
final class RestStaffRuleEdge {
    private RestStaffRuleEdge() { }
    static boolean[] extend(byte[] gray,int width,int height,int top,float staffTop,float gap,boolean[] mask) {
        return extend(gray,width,height,top,staffTop,gap,mask,false);
    }
    static boolean[] extendContrasted(byte[] gray,int width,int height,int top,float staffTop,float gap,boolean[] mask) {
        return extend(gray,width,height,top,staffTop,gap,mask,true);
    }
    private static boolean[] extend(byte[] gray,int width,int height,int top,float staffTop,float gap,boolean[] mask,boolean contrasted) {
        if(gray==null||width<=0||height<=0||gray.length!=(long)width*height||mask==null
                ||top<0||(long)top+mask.length>height||gap<6||!Float.isFinite(gap)
                ||!Float.isFinite(staffTop))return mask;
        boolean[] result=mask;
        int minimum=Math.max(3,Math.round(gap*3));
        for(int i=0;i<mask.length;i++) {
            if(mask[i])continue;
            int y=top+i;
            float nearest=staffTop+Math.round((y-staffTop)/gap)*gap;
            if(Math.abs(y-nearest)>gap*.35f)continue;
            int adjacent=i>0&&mask[i-1]?y-1:i+1<mask.length&&mask[i+1]?y+1:-1;
            if(adjacent<0)continue;
            int run=0;boolean proven=false;
            for(int x=0;x<width;x++) {
                int value=gray[y*width+x]&255,other=gray[adjacent*width+x]&255;
                boolean ink=value<(contrasted?200:170)&&other<(contrasted?200:170);
                if(ink&&contrasted) {
                    int radius=Math.max(3,Math.round(gap*.35f));
                    ink=y>=radius&&y+radius<height
                            &&Math.min(gray[(y-radius)*width+x]&255,gray[(y+radius)*width+x]&255)
                            -Math.max(value,other)>=35;
                }
                if(ink) {
                    if(++run>=minimum){proven=true;break;}
                } else run=0;
            }
            if(proven){if(result==mask)result=mask.clone();result[i]=true;}
        }
        return result;
    }

    /** A lower-voice fallback must not reinterpret a moving note's downward flag. */
    static boolean noteStem(byte[] gray,int width,int height,int left,int right,int minY,
                            float gap,float noteX,float noteY) {
        if(gray==null||gray.length!=(long)width*height||gap<6||!Float.isFinite(gap)
                ||!Float.isFinite(noteX)||!Float.isFinite(noteY))return false;
        int from=Math.max(0,Math.round(noteY+gap*.2f)),to=Math.min(height-1,Math.round(minY+gap*.4f));
        if(minY-noteY<gap*.9f||minY-noteY>gap*4.5f||to-from<gap)return false;
        int first=Math.max(0,Math.max(left-Math.round(gap*.25f),Math.round(noteX-gap*.8f)));
        int last=Math.min(width-1,Math.min(right,Math.round(noteX+gap*.65f)));
        for(int x=first;x<=last;x++) {
            int dark=0,blank=0,longestBlank=0;
            for(int y=from;y<=to;y++) {
                if((gray[y*width+x]&255)<170){dark++;blank=0;}
                else longestBlank=Math.max(longestBlank,++blank);
            }
            if(dark>=(to-from+1)*.85f&&longestBlank<=Math.max(1,Math.round(gap*.15f)))return true;
        }
        return false;
    }
}
