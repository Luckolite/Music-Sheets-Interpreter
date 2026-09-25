// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** A small inner outlined hook lies opposite its actual note across one common stem. */
final class OutlinedHookHead {
    private OutlinedHookHead() { }
    static boolean matches(byte[] gray,int width,int height,float x,float y,
            int boxWidth,int boxHeight,int area,float mainX,float mainY,int mainArea,int[] stem,float gap) {
        if(gray==null||width<1||height<1||gray.length!=(long)width*height||stem==null||stem.length<3
                ||gap<8||!Float.isFinite(gap)||!Float.isFinite(x)||!Float.isFinite(y)
                ||!Float.isFinite(mainX)||!Float.isFinite(mainY)||boxWidth<1||boxHeight<1||area<1||mainArea<1
                ||x<0||x>=width||y<0||y>=height||mainX<0||mainX>=width||mainY<0||mainY>=height
                ||stem[0]<0||stem[0]>=width||stem[1]<0||stem[1]>=height||Math.abs(stem[2])!=1
                ||boxWidth>gap*.65f||boxHeight>gap*.6f||area>gap*gap*.25f||mainArea<area*3
                ||mainArea<gap*gap*.55f)return false;
        float separation=(y-mainY)*stem[2],fromTip=(stem[1]-y)*stem[2];
        if(separation<gap*1.65f||separation>gap*4.5f||fromTip<gap*.6f||fromTip>gap*1.65f
                ||Math.abs(x-stem[0])>gap*.85f||Math.abs(x-mainX)>gap*1.5f
                ||(x-stem[0])*(mainX-stem[0])>=0)return false;
        return OutlinedBeamInk.count(gray,width,height,stem,mainY,gap)>=2;
    }
}
