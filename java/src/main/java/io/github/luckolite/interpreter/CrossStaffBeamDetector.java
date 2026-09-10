// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

/** Proves an inter-staff beam from raw ink and both attached stems, not x spacing. */
final class CrossStaffBeamDetector {
    private CrossStaffBeamDetector() { }
    static boolean connected(byte[] gray, int width, int height, float x1, float y1,
                             float x2, float y2, float gap) {
        if (gray == null || gap < 2 || x2 <= x1 || Math.abs(y2-y1) < gap*6
                || x2-x1 < gap*1.5 || x2-x1 > gap*9) return false;
        // Up-stems attach to the right of a head; down-stems to the left.
        boolean firstUp = y1 > y2;
        int stem1 = Math.round(x1 + (firstUp ? .6f : -.6f)*gap);
        int stem2 = Math.round(x2 + (firstUp ? -.6f : .6f)*gap);
        if (stem2-stem1 < gap) return false;
        int top = Math.max(1, Math.round(Math.min(y1,y2)+gap*2));
        int bottom = Math.min(height-2, Math.round(Math.max(y1,y2)-gap*2));
        for (int a=top;a<=bottom;a++) {
            if (!stem(gray,width,height,stem1,y1,a,gap)) continue;
            for (int b=Math.max(top,a-(int)(gap*1.8));b<=Math.min(bottom,a+(int)(gap*1.8));b++) {
                if (!stem(gray,width,height,stem2,y2,b,gap)) continue;
                int hits=0,total=0,thick=0;
                for(int x=stem1+3;x<stem2-2;x++) {
                    int y=Math.round(a+(b-a)*(x-stem1)/(float)(stem2-stem1));
                    total++;
                    if(dark(gray,width,height,x,y,1))hits++;
                    if(dark(gray,width,height,x,y-1,0)&&dark(gray,width,height,x,y+1,0))thick++;
                }
                if(total>0&&hits>=total*.93&&thick>=total*.45)return true;
            }
        }
        return false;
    }
    private static boolean stem(byte[] gray,int w,int h,int x,float headY,int end,float gap) {
        int direction=end>headY?1:-1;
        int start=Math.round(headY+direction*gap*.6f);
        int hits=0,total=0;
        for(int y=start;direction>0?y<end:y>end;y+=direction) {
            total++;if(dark(gray,w,h,x,y,2))hits++;
        }
        return total>=gap*1.4&&hits>=total*.92;
    }
    private static boolean dark(byte[] gray,int w,int h,int x,int y,int radius) {
        if(y<0||y>=h)return false;
        for(int k=Math.max(0,x-radius);k<=Math.min(w-1,x+radius);k++)
            if((gray[y*w+k]&255)<150)return true;
        return false;
    }
}
