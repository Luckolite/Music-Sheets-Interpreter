// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original eighth-rest bulbs below a held upper voice; no score imagery. */
public class DeepPolyphonicRestTest {
    static final int W=800,H=320;
    final byte[] gray=new byte[W*H];
    void ellipse(int cx,int cy,int rx,int ry){for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1)gray[y*W+x]=0;}
    void page(boolean tail,boolean dot) {
        Arrays.fill(gray,(byte)255);
        for(int line=0;line<5;line++){int y=Math.round(80+line*14.25f);for(int x=20;x<=780;x++)gray[y*W+x]=0;}
        ellipse(177,157,6,5);
        if(tail)for(int y=154;y<=180;y++){int x=185-(y-154)*10/26;gray[y*W+x]=0;gray[y*W+x+1]=0;}
        if(dot)ellipse(202,159,3,3);
    }
    ScoreNoteEvent held(int measure,int staff,double duration) {
        return new ScoreNoteEvent(measure,.1f,5,staff,1,100/(float)H,false,0,0,2,(float)duration);
    }
    List<ScoreRestEvent> rests(List<ScoreNoteEvent> notes) {
        return SixteenthRestDetector.detect(gray,W,H,List.of(new MeasureRegion(0,1,.1f,.9f)),
                List.of(new SixteenthRestDetector.Staff(80,137,14.25f,0,1)),notes);
    }
    @Test public void completeEighthRestCanSitBelowStaff(){page(true,false);var r=rests(List.of(held(0,0,4)));assertEquals(1,r.size());assertEquals(.5,r.get(0).durationBeats(),0);}
    @Test public void displacedDotKeepsDottedEighthValue(){page(true,true);var r=rests(List.of(held(0,0,4)));assertEquals(1,r.size());assertEquals(.75,r.get(0).durationBeats(),0);}
    @Test public void noHeldVoiceDoesNotAuthorizeDeepPlacement(){page(true,false);assertTrue(rests(List.of()).isEmpty());}
    @Test public void shortNoteIsNotTheIndependentHeldVoice(){page(true,false);assertTrue(rests(List.of(held(0,0,1))).isEmpty());}
    @Test public void otherStaffCannotAuthorizeDeepPlacement(){page(true,false);assertTrue(rests(List.of(held(0,1,4))).isEmpty());}
    @Test public void otherMeasureCannotAuthorizeDeepPlacement(){page(true,false);assertTrue(rests(List.of(held(1,0,4))).isEmpty());}
    @Test public void bulbWithoutTailCannotBecomeSilence(){page(false,false);assertTrue(rests(List.of(held(0,0,4))).isEmpty());}
    @Test public void aRealHeadInsideTheGlyphBlocksRestOwnership(){
        page(true,false);
        var note=new ScoreNoteEvent(0,177/(float)W,-3,0,1,157/(float)H,false,0,1,2,0);
        assertTrue(rests(List.of(held(0,0,4),note)).isEmpty());
    }
}
