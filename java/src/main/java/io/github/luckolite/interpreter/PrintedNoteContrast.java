// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Rejects semantic noteheads painted on shaded paper texture, not printed ink. */
final class PrintedNoteContrast {
    private PrintedNoteContrast() { }

    static boolean paperTexture(byte[] gray,int width,int height,int left,int top,int right,int bottom) {
        if(gray==null||gray.length!=(long)width*height||left<0||top<0
                ||right>=width||bottom>=height||right<left||bottom<top)return false;
        int margin=Math.max(4,Math.max(right-left+1,bottom-top+1)/2);
        int[] paper=new int[256],head=new int[256];int papers=0,heads=0;
        for(int y=Math.max(0,top-margin);y<=Math.min(height-1,bottom+margin);y++)
            for(int x=Math.max(0,left-margin);x<=Math.min(width-1,right+margin);x++) {
                int value=gray[y*width+x]&255;
                if(x>=left&&x<=right&&y>=top&&y<=bottom){head[value]++;heads++;}
                else {paper[value]++;papers++;}
            }
        if(papers<16||heads<12)return false;
        int background=percentile(paper,papers,75);
        // White-paper faded notation and genuinely dark ink remain untouched.
        if(background>=220||percentile(head,heads,10)<120)return false;
        // The model can label only a pale head interior, leaving its printed
        // outline and attached stem just outside the semantic bounds.
        int fringe=Math.max(2,Math.round(Math.min(right-left+1,bottom-top+1)*.25f));
        int printed=0,limit=Math.min(160,background-35);
        for(int y=Math.max(0,top-fringe);y<=Math.min(height-1,bottom+fringe);y++)
            for(int x=Math.max(0,left-fringe);x<=Math.min(width-1,right+fringe);x++)
                if((gray[y*width+x]&255)<=limit)printed++;
        if(printed>=Math.max(4,heads*.06f))return false;
        return background-percentile(head,heads,25)<20;
    }

    private static int percentile(int[] histogram,int total,int percent) {
        int count=0,target=Math.max(1,(total*percent+99)/100);
        for(int i=0;i<256;i++){count+=histogram[i];if(count>=target)return i;}
        return 255;
    }
}
