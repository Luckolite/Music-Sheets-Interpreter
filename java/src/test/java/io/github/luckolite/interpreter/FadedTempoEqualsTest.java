// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original synthetic pale tempo strokes; no score pixels. */
public class FadedTempoEqualsTest {
    private List<ScoreTempoChange> detect(int paper,int ink,boolean connected,boolean second) {
        int w=300,h=240;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)paper);
        for(int x=104;x<=116;x++){gray[48*w+x]=(byte)ink;if(second)gray[54*w+x]=(byte)ink;}
        if(connected)for(int y=48;y<=54;y++)gray[y*w+104]=(byte)ink;
        for(int y=42;y<=61;y++)for(int x=123;x<=126;x++)gray[y*w+x]=(byte)ink;
        return TempoChangeDetector.detect(List.of(new MeasureNumberReconciler.NumberToken(
                132,120f/w,38f/h,150f/w,65f/h)),gray,w,h,
                List.of(new MeasureRegion(.2f,.9f,.30f,.48f)));
    }
    @Test public void paleSeparatedEqualsIsRecovered(){assertEquals(List.of(new ScoreTempoChange(0,0,132)),detect(250,170,false,true));}
    @Test public void darkEqualsRemainsRecognized(){assertEquals(1,detect(250,30,false,true).size());}
    @Test public void shadowCannotTriggerPaleFallback(){assertTrue(detect(200,170,false,true).isEmpty());}
    @Test public void connectedLetterIsRejected(){assertTrue(detect(250,170,true,true).isEmpty());}
    @Test public void singleStrokeIsRejected(){assertTrue(detect(250,170,false,false).isEmpty());}
}
