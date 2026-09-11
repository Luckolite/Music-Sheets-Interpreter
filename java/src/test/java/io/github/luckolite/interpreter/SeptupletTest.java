// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original seven glyph and generated events; no commercial score pixels. */
public class SeptupletTest {
    private static final List<MeasureRegion> BARS=List.of(new MeasureRegion(0,1,.1f,.8f));
    private static byte[] ink(int top) {
        byte[] gray=new byte[500*240];Arrays.fill(gray,(byte)255);
        for(int y=0;y<22;y++) {
            int left=y<3?0:Math.max(0,9-(y-3)/2);
            int right=y<3?11:Math.min(11,left+3);
            for(int x=left;x<=right;x++)gray[(top+y)*500+214+x]=0;
        }
        return gray;
    }
    private static ScoreNoteEvent note(float x,int step,int beams) {
        return new ScoreNoteEvent(0,x,step,0,1,.4f,false,0,beams,2,beams==0?1:0,1);
    }
    private static List<ScoreNoteEvent> run(int size) {
        List<ScoreNoteEvent> notes=new ArrayList<>();
        for(int i=0;i<size;i++)notes.add(note(.20f+i*.08f,i,2));
        return notes;
    }
    private static List<ScoreNoteEvent> apply(List<ScoreNoteEvent> notes,byte[] gray) {
        return TripletRhythmDetector.apply(notes,BARS,gray,500,240);
    }
    @Test public void sevenSixteenthsOccupyOneQuarterBeat() {
        var result=apply(run(7),ink(145));
        for(int i=0;i<7;i++) {
            var n=result.get(i);assertEquals(7,n.tupletDivisor());
            assertEquals(1.0/7,ScoreNoteTiming.writtenDurationBeats(n),1e-8);
            assertEquals(i/7.0,ScoreNoteTiming.beatInMeasure(n,result,4),1e-6);
        }
    }
    @Test public void numeralAboveTheNotesWorksToo() {
        for(var n:apply(run(7),ink(30)))assertEquals(7,n.tupletDivisor());
    }
    @Test public void taperedSinglePixelTerminalStillReadsSeven() {
        byte[] gray=ink(145);
        for(int y=164;y<167;y++)for(int x=214;x<226;x++)gray[y*500+x]=(byte)255;
        for(int y=164;y<167;y++)gray[y*500+214]=0;
        for(var n:apply(run(7),gray))assertEquals(7,n.tupletDivisor());
    }
    @Test public void missingNumeralPreservesOrdinaryValues() {
        byte[] gray=new byte[500*240];Arrays.fill(gray,(byte)255);
        assertEquals(run(7),apply(run(7),gray));
    }
    @Test public void sixOrEightAttacksDoNotBecomeSeven() {
        assertEquals(run(6),apply(run(6),ink(145)));
        assertEquals(run(8),apply(run(8),ink(145)));
    }
    @Test public void interveningDifferentDurationBlocksGroup() {
        var notes=run(7);notes.set(3,note(.44f,3,0));assertEquals(notes,apply(notes,ink(145)));
    }
    @Test public void chordsCountAsSevenAttacksAndHeldVoiceKeepsItsLength() {
        var notes=run(7);
        for(int i=0;i<7;i++)notes.add(note(.201f+i*.08f,i+7,2));
        notes.add(note(.20f,14,0));
        var result=apply(notes,ink(145));
        for(int i=0;i<14;i++)assertEquals(7,result.get(i).tupletDivisor());
        assertEquals(1,ScoreNoteTiming.writtenDurationBeats(result.get(14)),0);
    }
    @Test public void unsortedInputKeepsPitchesAndOrder() {
        var notes=run(7);Collections.reverse(notes);var result=apply(notes,ink(145));
        for(int i=0;i<7;i++){assertEquals(notes.get(i).staffStep(),result.get(i).staffStep());assertEquals(7,result.get(i).tupletDivisor());}
    }
    @Test public void nextQuarterStartsAfterOneBeat() {
        var notes=run(7);notes.add(note(.90f,9,0));var result=apply(notes,ink(145));
        assertEquals(1,ScoreNoteTiming.beatInMeasure(result.get(7),result,2),1e-6);
    }
    @Test public void closedDigitCannotSetSeptuplets() {
        byte[] gray=ink(145);
        for(int y=145;y<167;y++)for(int x=214;x<217;x++)gray[y*500+x]=0;
        assertEquals(run(7),apply(run(7),gray));
    }
    @Test public void unrecognizedDivisorStillFallsBackSafely() {
        var n=new ScoreNoteEvent(0,.1f,0,0,1,.4f,false,0,2,2,0,5);
        assertEquals(1,n.tupletDivisor());assertEquals(.25,ScoreNoteTiming.writtenDurationBeats(n),0);
    }
    @Test public void aRestOccupiesOneOfTheSevenSlots() {
        var notes=run(7);notes.remove(3);
        var rest=new ScoreRestEvent(0,.44f,.4f,.04f,0,1,.25);
        var result=TripletRhythmDetector.withRests(notes,List.of(rest),BARS,ink(145),500,240);
        assertEquals(1.0/7,result.rests().get(0).durationBeats(),1e-8);
        for(var n:result.notes())assertEquals(7,n.tupletDivisor());
    }
}
