// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original numeral pixels and chord events; no score scans. */
public class ChordTripletTest {
    private static final List<MeasureRegion> BARS=List.of(new MeasureRegion(0,1,.1f,.8f));
    private static byte[] ink(boolean closed) {
        String[] rows={"..#######...", ".##########.", "###......###", "####.....###",
                "####.....###", "####.....###", ".##.....####", ".......####.",
                "......####..", "....#####...", "....#####...", "....#####...",
                "......####..", ".......####.", "##.....####.", "###....####.",
                "###....####.", "###....####.", ".###....###.", "..########..",
                "..########..", "....####...."};
        byte[] gray=new byte[400*240];Arrays.fill(gray,(byte)255);
        for(int y=0;y<rows.length;y++)for(int x=0;x<12;x++)
            if(rows[y].charAt(x)=='#'||closed&&x<2&&y>1&&y<rows.length-2)gray[(145+y)*400+119+x]=0;
        return gray;
    }
    private static ScoreNoteEvent note(float x,int step,int beams) {
        return new ScoreNoteEvent(0,x,step,0,1,.4f+step*.03f,false,0,beams,2,beams==0?1:0,1);
    }
    private static List<ScoreNoteEvent> chords(int size) {
        List<ScoreNoteEvent> result=new ArrayList<>();
        for(float x:new float[]{.25f,.3125f,.375f})for(int j=0;j<size;j++)result.add(note(x+j*.001f,j,1));
        return result;
    }
    private static List<ScoreNoteEvent> apply(List<ScoreNoteEvent> notes,byte[] gray) {
        return TripletRhythmDetector.apply(notes,BARS,gray,400,240);
    }
    private static List<ScoreNoteEvent> clean(List<ScoreNoteEvent> notes,byte[] gray) {
        return TripletRhythmDetector.withoutNumeralHeads(notes,BARS,gray,400,240);
    }
    private static ScoreNoteEvent numeral(int beams,float y) {
        return new ScoreNoteEvent(0,.3125f,-4,0,1,y,false,0,beams,2,2,1);
    }
    @Test public void everyDyadMemberReceivesOneThirdBeat() {
        var result=apply(chords(2),ink(false));assertEquals(6,result.size());
        for(var n:result){assertEquals(3,n.tupletDivisor());assertEquals(1.0/3,ScoreNoteTiming.writtenDurationBeats(n),.0001);}
    }
    @Test public void unsortedTriadsKeepInputOrderAndAllPitches() {
        var notes=chords(3);Collections.reverse(notes);var result=apply(notes,ink(false));
        for(int i=0;i<notes.size();i++){assertEquals(notes.get(i).staffStep(),result.get(i).staffStep());assertEquals(notes.get(i).positionInMeasure(),result.get(i).positionInMeasure(),0);assertEquals(3,result.get(i).tupletDivisor());}
    }
    @Test public void heldQuarterAtEveryAttackKeepsItsOwnValue() {
        var notes=chords(2);for(float x:new float[]{.25f,.3125f,.375f})notes.add(note(x,3,0));
        var result=apply(notes,ink(false));
        for(int i=0;i<6;i++)assertEquals(3,result.get(i).tupletDivisor());
        for(int i=6;i<9;i++){assertEquals(1,result.get(i).tupletDivisor());assertEquals(1,ScoreNoteTiming.writtenDurationBeats(result.get(i)),0);}
    }
    @Test public void numeralAmongChordsIsRemoved() {
        var notes=chords(2);notes.add(numeral(0,161/240f));assertEquals(chords(2),clean(notes,ink(false)));
    }
    @Test public void spuriousBeamPredictionOnNumeralDoesNotMakeItANote() {
        var notes=chords(3);notes.add(numeral(3,161/240f));assertEquals(chords(3),clean(notes,ink(false)));
    }
    @Test public void closedEightCannotEraseAHeadOrChangeChordTiming() {
        var notes=chords(2);notes.add(numeral(3,161/240f));assertEquals(notes,clean(notes,ink(true)));
        assertEquals(chords(2),apply(chords(2),ink(true)));
    }
    @Test public void aRealBeamedHeadAwayFromGlyphRemains() {
        var notes=chords(2);notes.add(numeral(3,.82f));assertEquals(notes,clean(notes,ink(false)));
    }
    @Test public void aDifferentAttackBetweenChordsCannotBeSkipped() {
        var notes=chords(2);notes.add(note(.28f,1,0));assertEquals(notes,apply(notes,ink(false)));
    }
    @Test public void twoChordAttacksAreNotThreeNotesOfATriplet() {
        var notes=new ArrayList<>(chords(3).subList(0,6));assertEquals(notes,apply(notes,ink(false)));
    }
    @Test public void absentMiddleRhythmicValueCannotBorrowAHeldVoice() {
        var notes=chords(2);notes.set(2,note(.3125f,0,0));notes.set(3,note(.3135f,1,0));
        assertEquals(notes,apply(notes,ink(false)));
    }
    @Test public void pixelsAndSourceEventsAreUnchanged() {
        var notes=chords(2);var saved=List.copyOf(notes);byte[] gray=ink(false),copy=gray.clone();
        apply(notes,gray);clean(notes,gray);assertEquals(saved,notes);assertArrayEquals(copy,gray);
    }
}
