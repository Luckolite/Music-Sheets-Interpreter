// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic ruled staves; no score-derived image fixtures. */
public class CurvedStaffTailTest {
    private static final int W=800,H=260,RIGHT=570,END=730;
    private static float bottom(int x){return 190-Math.max(0,x-540)*Math.max(0,x-540)*.0008f;}
    private static byte[] page(int lines,boolean bar,boolean beam,boolean broken) {
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)210);
        for(int x=40;x<=END;x++) {
            if(broken&&x>610&&x<645)continue;
            for(int line=0;line<lines;line++) {
                int y=Math.round(bottom(x)-12*line);
                gray[y*W+x]=45;gray[(y+1)*W+x]=70;
            }
            if(beam&&x>630&&x<647)for(int dy=-5;dy<=5;dy++)
                gray[(Math.round(bottom(x)-24)+dy)*W+x]=20;
        }
        if(bar)bar(gray,END);
        return gray;
    }
    private static void bar(byte[] g,int x) {
        for(int y=Math.round(bottom(x)-48);y<=Math.round(bottom(x));y++)g[y*W+x]=30;
    }
    private static int end(byte[] gray){return CurvedStaffTail.closingBar(gray,W,H,RIGHT,190,12,0);}
    @Test public void followsCurledFiveRulesToClosingBar(){assertEquals(END,end(page(5,true,false,false)),2);}
    @Test public void bridgesBriefBeamOcclusion(){assertEquals(END,end(page(5,true,true,false)),2);}
    @Test public void doesNotInventClosingBar(){assertEquals(RIGHT,end(page(5,false,false,false)));}
    @Test public void rejectsFourRuleCandidate(){assertEquals(RIGHT,end(page(4,true,false,false)));}
    @Test public void rejectsDisconnectedContinuation(){assertEquals(RIGHT,end(page(5,true,false,true)));}
    @Test public void stopsAtFirstBarRatherThanMergingMeasures(){
        byte[] gray=page(5,true,false,false);bar(gray,680);assertEquals(680,end(gray),2);
    }
    @Test public void earlierBarWithoutEnoughCurvatureKeepsOriginalExtent(){
        byte[] gray=page(5,true,false,false);bar(gray,650);assertEquals(RIGHT,end(gray));
    }
    @Test public void existingBarNearExtentIsNotCrossed(){
        byte[] gray=page(5,true,false,false);bar(gray,RIGHT);assertEquals(RIGHT,end(gray));
    }
    @Test public void flatPaperAndPageEdgesAreSafe(){
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)110);
        assertEquals(RIGHT,end(gray));
        assertEquals(W-2,CurvedStaffTail.closingBar(gray,W,H,W-2,190,12,0));
    }
    @Test public void shadowedPaperDoesNotLookLikeAnExtendedBar(){
        byte[] gray=page(5,true,false,false);
        for(int i=0;i<gray.length;i++)if((gray[i]&255)==210)gray[i]=(byte)155;
        assertEquals(END,end(gray),2);
    }
    @Test public void closedTailPreservesMainStaffAndFollowsRepeatedPitch(){
        byte[] gray=page(5,true,false,false);
        StaffPitchTrack track=StaffPitchTrack.closedTail(gray,W,H,190,12);
        assertNotNull(track);
        assertArrayEquals(new float[]{190,12},track.at(100),.001f);
        assertArrayEquals(new float[]{190,12},track.at(790),.001f);
        for(int x:new int[]{670,695,715}) {
            float[] local=track.at(x);
            assertEquals(12,Math.round((local[0]-(bottom(x)-72))*2/local[1]));
        }
    }
}
