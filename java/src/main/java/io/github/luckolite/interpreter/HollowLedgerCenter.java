// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Two enclosed white pockets straddle the printed line of a hollow ledger head. */
final class HollowLedgerCenter {
    private HollowLedgerCenter() { }
    static boolean straddles(byte[] gray,int width,int height,float x,float line,float gap) {
        if(gray==null||width<1||height<1||gray.length!=(long)width*height||gap<8||!Float.isFinite(gap)
                ||!Float.isFinite(x)||!Float.isFinite(line))return false;
        int cx=Math.round(x),cy=Math.round(line),rx=Math.round(gap*.8f),ry=Math.round(gap*.72f);
        if(cx-rx<0||cx+rx>=width||cy-ry<0||cy+ry>=height)return false;
        int[] area=new int[2],rows=new int[2];long[] sumX=new long[2];
        for(int side=0;side<2;side++)for(int d=2;d<=gap*.5f;d++) {
            int y=cy+(side==0?-d:d),count=0;
            for(int xx=cx-Math.round(gap*.38f);xx<=cx+Math.round(gap*.38f);xx++) {
                int paper=gray[y*width+xx]&255;if(paper<185)continue;
                int left=255,right=255,above=255,below=255;
                for(int a=cx-rx;a<xx;a++)left=Math.min(left,gray[y*width+a]&255);
                for(int a=xx+1;a<=cx+rx;a++)right=Math.min(right,gray[y*width+a]&255);
                for(int b=cy-ry;b<y;b++)above=Math.min(above,gray[b*width+xx]&255);
                for(int b=y+1;b<=cy+ry;b++)below=Math.min(below,gray[b*width+xx]&255);
                if(Math.max(Math.max(left,right),Math.max(above,below))<=Math.min(160,paper-45)) {
                    count++;sumX[side]+=xx;
                }
            }
            area[side]+=count;if(count>=2)rows[side]++;
        }
        return area[0]>=Math.max(4,Math.round(gap*gap*.025f))&&area[1]>=Math.max(4,Math.round(gap*gap*.025f))
                &&rows[0]>=2&&rows[1]>=2&&Math.abs(sumX[0]/(float)area[0]-sumX[1]/(float)area[1])<gap*.55f;
    }
}
