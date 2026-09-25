// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;

/** Conservative continuation of an established staff into a curled scan edge. */
final class CurvedStaffTail {
    private CurvedStaffTail() { }

    static int closingBar(byte[] gray,int width,int height,int right,float bottom,float gap,float slope) {
        Track track=track(gray,width,height,right,bottom,gap,slope);
        return track==null?right:track.end();
    }

    record Track(int end,List<float[]> points) { }

    static Track track(byte[] gray,int width,int height,int right,float bottom,float gap,float slope) {
        if(gray==null||gap<6||right<gap*6||right>=width-gap*3)return null;
        int start=Math.max(3,Math.round(right-gap*4));
        float reference=bottom+slope*(start-width*.5f),spacing=gap,best=-1,tracked=reference;
        // A mildly compressed seed must not force the continuation onto a beam.
        // Calibrate against all five contrasting rules, near the original phase.
        int initialStart=start;
        for(int sx=initialStart;sx<=initialStart+gap*2;sx+=3)for(int gs=-4;gs<=6;gs++) {
            float candidateGap=gap*(1+gs*.025f);
            for(int y=Math.round(reference-gap*.45f);y<=reference+gap*.45f;y++) {
                float score=rules(gray,width,height,sx,y,candidateGap);
                if(score<5000)continue;
                score-=Math.abs(y-reference)*3+Math.abs(candidateGap-gap)*8;
                if(score>best){best=score;tracked=y;spacing=candidateGap;start=sx;}
            }
        }
        if(best<0)return null;
        int missing=0,complete=0,samples=0,lastComplete=start;
        float initial=tracked;
        List<float[]> points=new ArrayList<>();points.add(new float[]{start,tracked,spacing});
        for(int x=start+3;x<Math.min(width-6,right+width/4);x+=3) {
            // Stop at the FIRST complete isolated rule; never bridge a real bar
            // and silently fold another measure into the cropped ending.
            if(x>=right-Math.round(gap))for(int bx=x-1;bx<=x+1;bx++) {
                boolean bar=false;
                for(int dy=-3;dy<=3&&!bar;dy++)bar=closingRule(gray,width,height,bx,tracked+dy,spacing);
                if(bar) {
                    boolean curved=Math.abs(tracked-initial-slope*(bx-start))>gap*.65f;
                    return bx>right+gap*2&&curved&&complete>=8&&complete>=samples*.45f
                            ?new Track(bx,List.copyOf(points)):null;
                }
            }
            best=-1;float next=tracked;
            for(int y=Math.round(tracked)-3;y<=Math.round(tracked)+3;y++) {
                float score=rules(gray,width,height,x,y,spacing);
                if(score<4000)continue;
                score-=Math.abs(y-tracked)*3;
                if(score>best){best=score;next=y;}
            }
            samples++;
            if(best<0){if((missing+=3)>gap)return null;continue;}
            missing=0;tracked=next;
            points.add(new float[]{x,tracked,spacing});
            if(best>=5000){complete++;lastComplete=x;}
            if(x-lastComplete>gap*3)return null;
        }
        return null;
    }

    private static float rules(byte[] gray,int w,int h,int x,float bottom,float gap) {
        int count=0,total=0,flank=Math.max(2,Math.round(gap*.32f));
        for(int line=0;line<5;line++) {
            int row=Math.round(bottom-line*gap),best=0;
            if(row-flank-1<0||row+flank+1>=h)return 0;
            for(int dy=-1;dy<=1;dy++) {
                int contrast=0,y=row+dy;
                for(int dx=-2;dx<=2;dx++) {
                    int ink=gray[y*w+x+dx]&255;
                    if(ink<=180)contrast+=Math.max(0,Math.min(gray[(y-flank)*w+x+dx]&255,
                            gray[(y+flank)*w+x+dx]&255)-ink);
                }
                best=Math.max(best,contrast);
            }
            if(best>=60)count++;
            total+=Math.min(200,best);
        }
        return count*1000+total;
    }

    private static boolean closingRule(byte[] gray,int w,int h,int x,float bottom,float gap) {
        int top=Math.round(bottom-4*gap),last=Math.round(bottom),hit=0,spaces=0;
        int radius=Math.max(1,Math.round(gap*.20f)),flank=radius+3;
        if(top-gap<0||last+gap>=h||x<flank||x+flank>=w)return false;
        for(int y=top;y<=last;y++) {
            float phase=(bottom-y)/gap;
            if(Math.abs(phase-Math.round(phase))<.22f)continue;
            spaces++;
            int ink=255;
            for(int dx=-radius;dx<=radius;dx++)ink=Math.min(ink,gray[y*w+x+dx]&255);
            if(ink<170&&(gray[y*w+x-flank]&255)>ink+16&&(gray[y*w+x+flank]&255)>ink+16)hit++;
        }
        if(spaces<8||hit<spaces*.9f)return false;
        int outside=0,samples=0;
        for(int d=Math.max(3,Math.round(gap*.35f));d<=gap;d++)for(int y:new int[]{top-d,last+d}) {
            samples++;
            int ink=255;
            for(int dx=-radius;dx<=radius;dx++)ink=Math.min(ink,gray[y*w+x+dx]&255);
            if(ink<170&&(gray[y*w+x-flank]&255)>ink+16&&(gray[y*w+x+flank]&255)>ink+16)outside++;
        }
        return outside<=samples*.15f;
    }
}
