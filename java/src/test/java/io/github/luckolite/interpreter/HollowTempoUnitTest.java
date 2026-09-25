// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original note, equals and digit silhouettes on plain or shaded paper. */
public class HollowTempoUnitTest {
    private List<ScoreTempoChange> detect(int paper,boolean hollow,boolean dotted,boolean open,boolean inside) {
        int w=300,h=200;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)paper);
        rect(gray,w,80,30,82,60,45);rect(gray,w,69,56,81,64,45);
        if(hollow)rect(gray,w,72,59,78,61,paper);
        if(open)rect(gray,w,69,59,73,61,paper);
        if(dotted)rect(gray,w,88,59,90,61,45);
        rect(gray,w,100,50,111,51,25);rect(gray,w,100,56,111,57,25);
        rect(gray,w,120,43,124,64,25);
        return TempoChangeDetector.detect(List.of(new MeasureNumberReconciler.NumberToken(
                60,120f/w,38f/h,145f/w,83f/h)),gray,w,h,
                List.of(new MeasureRegion(.2f,.9f,(inside?55f:74f)/h,.8f)));
    }
    private void rect(byte[] a,int w,int l,int t,int r,int b,int value){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)a[y*w+x]=(byte)value;}
    @Test public void hollowHalfNoteKeepsTwoQuarterBeatPulse(){assertEquals(List.of(new ScoreTempoChange(0,0,120,2)),detect(255,true,false,false,false));}
    @Test public void shadowPaperDoesNotFillHollowTempoHead(){assertEquals(List.of(new ScoreTempoChange(0,0,120,2)),detect(192,true,false,false,false));}
    @Test public void dottedHalfUsesThreeQuarterBeats(){assertEquals(List.of(new ScoreTempoChange(0,0,180,3)),detect(255,true,true,false,false));}
    @Test public void filledHeadRemainsQuarter(){assertEquals(List.of(new ScoreTempoChange(0,0,60,1)),detect(255,false,false,false,false));}
    @Test public void openHookDoesNotProveHalfNote(){assertEquals(1,detect(255,true,false,true,false).get(0).beatUnit(),0);}
    @Test public void inkActuallyInsideStaffStillCannotBeTempo(){assertTrue(detect(255,true,false,false,true).isEmpty());}
}
