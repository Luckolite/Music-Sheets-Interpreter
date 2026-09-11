// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original block numeral and generated notes. */
public class QuintupletTest {
    private static final List<MeasureRegion> BARS=List.of(new MeasureRegion(0,1,.1f,.8f));
    private byte[] ink(int top) {
        byte[] gray=new byte[500*240];Arrays.fill(gray,(byte)255);
        for(int y=0;y<22;y++) for(int x=0;x<12;x++) {
            boolean dark=y<3 || y<9&&x<3 || y>=9&&y<12 || y>=12&&y<19&&x>=9
                    || y>=15&&y<19&&x<3 || y>=19&&x>=2&&x<=8;
            if(dark)gray[(top+y)*500+174+x]=0;
        }
        return gray;
    }
    private List<ScoreNoteEvent> run(int count) {
        var notes=new ArrayList<ScoreNoteEvent>();
        for(int i=0;i<count;i++) notes.add(new ScoreNoteEvent(0,.2f+i*.08f,i,0,1,.4f,false,0,2,2,0));
        return notes;
    }
    private List<ScoreNoteEvent> apply(List<ScoreNoteEvent> notes,byte[] gray) {
        return TripletRhythmDetector.apply(notes,BARS,gray,500,240);
    }
    @Test public void fiveSixteenthsFillOneBeat() {
        var result=apply(run(5),ink(145));
        for(int i=0;i<5;i++) {
            assertEquals(5,result.get(i).tupletDivisor());
            assertEquals(.2,ScoreNoteTiming.writtenDurationBeats(result.get(i)),1e-8);
            assertEquals(i*.2,ScoreNoteTiming.beatInMeasure(result.get(i),result,4),1e-6);
        }
    }
    @Test public void numeralCanBeAboveTheStaff() {
        for(var n:apply(run(5),ink(30)))assertEquals(5,n.tupletDivisor());
    }
    @Test public void aLongerRunCannotBeCutIntoFive() {
        assertEquals(run(6),apply(run(6),ink(145)));
        assertEquals(run(4),apply(run(4),ink(145)));
    }
    @Test public void missingNumeralPreservesOrdinaryNotes() {
        byte[] gray=new byte[500*240];Arrays.fill(gray,(byte)255);
        assertEquals(run(5),apply(run(5),gray));
    }
    @Test public void nextQuarterBeginsAfterOneBeat() {
        var notes=run(5);notes.add(new ScoreNoteEvent(0,.8f,8,0,1,.4f,false,0,0,2,1));
        var result=apply(notes,ink(145));
        assertEquals(1,ScoreNoteTiming.beatInMeasure(result.get(5),result,2),1e-6);
    }
    @Test public void aRestOccupiesOneQuintupletSlot() {
        var notes=run(5);notes.remove(2);
        var rest=new ScoreRestEvent(0,.36f,.4f,.04f,0,1,.25);
        var result=TripletRhythmDetector.withRests(notes,List.of(rest),BARS,ink(145),500,240);
        assertEquals(.2,result.rests().get(0).durationBeats(),1e-8);
        for(var n:result.notes())assertEquals(5,n.tupletDivisor());
    }
}
