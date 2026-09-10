// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Explicit rest sequences in optically incomplete measures, independent of score titles. */
public class IncompleteRestClockTest {
    private static ScoreNoteEvent n(float x,int beams,int dots,float duration,float before,float after){
        return new ScoreNoteEvent(0,x,1,0,1,.5f,false,dots,beams,2,duration,1,after).withLeadingRest(before);
    }
    @Test public void aReadLeadingRestDoesNotGainAnEngravingInset(){
        var a=n(.175f,2,0,0,.5f,1.25f);var b=n(.619f,1,1,0,0,0);var notes=List.of(a,b);
        assertEquals(.5,ScoreNoteTiming.beatInMeasure(a,notes,4),.0001);
        assertEquals(2,ScoreNoteTiming.beatInMeasure(b,notes,4),.0001);
        assertEquals(.75,ScoreNoteTiming.resolvedWrittenDurationBeats(b,notes,4),.0001);
    }
    @Test public void anInteriorSixteenthRestStillSeparatesItsNeighbours(){
        var notes=List.of(n(.328f,0,0,1,1.75f,.25f),n(.651f,2,0,0,0,0),n(.767f,2,0,0,0,0),n(.883f,2,0,0,0,0));
        double[] expected={1.75,3,3.25,3.5};for(int i=0;i<4;i++)assertEquals(expected[i],ScoreNoteTiming.beatInMeasure(notes.get(i),notes,4),.0001);
    }
    @Test public void aHalfRestAndShortRestRetainTheirCombinedSilence(){
        var a=n(.526f,2,0,0,2.25f,1);var b=n(.879f,2,0,0,0,0);var notes=List.of(a,b);
        assertEquals(2.25,ScoreNoteTiming.beatInMeasure(a,notes,4),.0001);
        assertEquals(3.5,ScoreNoteTiming.beatInMeasure(b,notes,4),.0001);
    }
    @Test public void aRecognizedRestIsNotCountedAgainFromItsWideSpacing(){
        var a=n(.04f,2,0,0,0,1);var b=n(.62f,2,0,0,0,0);var notes=List.of(a,b);
        assertEquals(1.25,ScoreNoteTiming.beatInMeasure(b,notes,4),.0001);
    }
    @Test public void partialRestEvidenceDoesNotEraseAnUnrecognizedLargeLeadingGap(){
        var a=n(.53f,2,0,0,.25f,1);var b=n(.88f,2,0,0,0,0);
        assertTrue(ScoreNoteTiming.beatInMeasure(a,List.of(a,b),4)>=2);
    }
}
