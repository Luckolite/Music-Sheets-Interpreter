// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PaddedTempoDigitsTest {
    private List<ScoreTempoChange> detect(boolean connected) {
        int w=300,h=240;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int x=104;x<=116;x++){gray[48*w+x]=0;gray[52*w+x]=0;}
        if(connected)for(int y=48;y<=52;y++)gray[y*w+104]=0;
        for(int y=42;y<=59;y++)for(int x=123;x<=126;x++)gray[y*w+x]=0;
        return TempoChangeDetector.detect(List.of(new MeasureNumberReconciler.NumberToken(
                96,120f/w,22f/h,150f/w,65f/h)),gray,w,h,
                List.of(new MeasureRegion(.2f,.9f,.30f,.48f)));
    }
    @Test public void paddedDigitsStillVerifyNarrowSeparatedEqualsStrokes() {
        assertEquals(List.of(new ScoreTempoChange(0,0,96)),detect(false));
    }
    @Test public void connectedLetterStrokesAreStillRejected() {assertTrue(detect(true).isEmpty());}
}
