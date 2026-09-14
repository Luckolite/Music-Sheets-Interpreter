// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Compact diagonal cross: four arms, a filled centre, and open upper/lower notches. */
final class DoubleSharpGlyph {
    static boolean matches(byte[] pixels,int width,int height,int left,int top,int right,int bottom,
                           byte label,float gap) {
        int w=right-left+1,h=bottom-top+1;
        if(left<0||top<0||right>=width||bottom>=height||w<5||h<5
                ||w<gap*.55f||w>gap*1.4f||h<gap*.55f||h>gap*1.4f
                ||w>h*1.5f||h>w*1.5f)return false;
        double[][] fill=new double[3][3];int[][] count=new int[3][3];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int row=Math.min(2,y*3/h),col=Math.min(2,x*3/w);count[row][col]++;
            if(pixels[(top+y)*width+left+x]==label)fill[row][col]++;
        }
        for(int y=0;y<3;y++)for(int x=0;x<3;x++)fill[y][x]/=Math.max(1,count[y][x]);
        return fill[1][1]>.6 && fill[0][1]<.4 && fill[2][1]<.4
                && fill[0][0]>.15 && fill[0][2]>.3 && fill[2][0]>.3 && fill[2][2]>.3;
    }
}
