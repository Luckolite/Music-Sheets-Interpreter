// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Compare tie endpoints against locally proved rules on a sloping printed staff. */
final class LocalTieStaffAlignment {
    private LocalTieStaffAlignment() { }
    static boolean same(byte[] labels,byte[] gray,int width,int height,
            float ax,float ay,int al,int ar,float ag,float bx,float by,int bl,int br,float bg,int step) {
        if(labels==null||gray==null||width<=0||height<=0
                ||labels.length!=(long)width*height||gray.length!=labels.length
                ||!Float.isFinite(ax)||!Float.isFinite(ay)||!Float.isFinite(bx)||!Float.isFinite(by)
                ||!Float.isFinite(ag)||!Float.isFinite(bg)||ag<5||bg<5)return false;
        float gap=(ag+bg)*.5f;
        if(Math.abs(ay-by)>gap*3||Math.abs(ag-bg)>gap*.12f)return false;
        float[] a=StaffPitchTrack.localRules(labels,gray,width,height,ax,al,ar,ay+step*ag*.5f,ag);
        float[] b=StaffPitchTrack.localRules(labels,gray,width,height,bx,bl,br,by+step*bg*.5f,bg);
        if(a==null||b==null||Math.abs(a[1]-b[1])>gap*.08f)return false;
        if(ambiguous(gray,width,height,ax,a)||ambiguous(gray,width,height,bx,b))return false;
        float ap=(a[0]-ay)/a[1],bp=(b[0]-by)/b[1];
        return Math.abs(ap-bp)<=.22f&&Math.abs(ap-step*.5f)<=.22f&&Math.abs(bp-step*.5f)<=.22f;
    }
    private static boolean ambiguous(byte[] gray,int width,int height,float x,float[] rules) {
        int left=Math.max(0,Math.round(x-rules[1]*3)),right=Math.min(width-1,Math.round(x+rules[1]*3));
        int radius=Math.max(1,Math.round(rules[1]*.2f)),flank=Math.max(2,Math.round(rules[1]*.32f));
        for(float outside:new float[]{rules[0]+rules[1],rules[0]-5*rules[1]}) {
            int columns=0;
            for(int xx=left;xx<=right;xx++)for(int y=Math.max(flank,Math.round(outside)-radius);
                    y<=Math.min(height-1-flank,Math.round(outside)+radius);y++) {
                int value=gray[y*width+xx]&255;
                if(value<=205&&(gray[(y-flank)*width+xx]&255)>=value+12
                        &&(gray[(y+flank)*width+xx]&255)>=value+12){columns++;break;}
            }
            if(columns>=(right-left+1)*.6f)return true;
        }
        return false;
    }
}
