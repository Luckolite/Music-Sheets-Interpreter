// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Terminal ornaments borrow from the preceding written value, never extend the measure. */
public class AfterGraceTimingTest {
    private static ScoreNoteEvent note(float x,float value){return new ScoreNoteEvent(0,x,2,0,1,.4f,false,0,value<1?1:0,2,value<1?0:value);}
    private static ScoreNoteEvent grace(float x){return new ScoreNoteEvent(0,x,2,0,1,.4f,false,0,2,2,0).withArticulations(NoteOrnament.GRACE);}
    private static void assertTiming(ScoreNoteEvent n,List<ScoreNoteEvent> all,double onset,double duration){assertEquals(onset,ScoreNoteTiming.beatInMeasure(n,all,4),.0001);assertEquals(duration,ScoreNoteTiming.resolvedWrittenDurationBeats(n,all,4),.0001);}
    @Test public void terminalPairBorrowsFromFinalQuarter(){var n=List.of(note(.1f,.5f),note(.2f,.5f),note(.3f,2),note(.7f,1),grace(.85f),grace(.95f));assertTiming(n.get(3),n,3,.75);assertTiming(n.get(4),n,3.75,.125);assertTiming(n.get(5),n,3.875,.125);}
    @Test public void wholeNoteWithAfterGracesStillFillsFourBeats(){var n=List.of(note(.1f,4),grace(.8f),grace(.9f));assertTiming(n.get(0),n,0,3.75);assertTiming(n.get(1),n,3.75,.125);assertTiming(n.get(2),n,3.875,.125);}
    @Test public void prefixGraceKeepsItsExistingDirection(){var n=List.of(grace(.1f),grace(.2f),note(.3f,4));assertTiming(n.get(0),n,0,.125);assertTiming(n.get(1),n,.125,.125);assertTiming(n.get(2),n,.25,3.75);}
    @Test public void principalCanHaveBothPrefixAndTerminalOrnaments(){var n=List.of(grace(.1f),note(.3f,4),grace(.8f),grace(.9f));assertTiming(n.get(0),n,0,.25);assertTiming(n.get(1),n,.25,3.5);assertTiming(n.get(2),n,3.75,.125);assertTiming(n.get(3),n,3.875,.125);}
    @Test public void timingSessionDoesNotChangeAfterGracePlacement(){var n=List.of(note(.1f,4),grace(.8f),grace(.9f));try(var session=ScoreNoteTiming.beginTimingSession()){assertTiming(n.get(0),n,0,3.75);assertTiming(n.get(1),n,3.75,.125);assertTiming(n.get(2),n,3.875,.125);}}
    @Test public void nextMeasureDoesNotDonateTimeAcrossBarline(){var next=new ScoreNoteEvent(1,.1f,2,0,1,.5f,false,0,0,2,4);var n=List.of(note(.1f,4),grace(.8f),grace(.9f),next);assertTiming(n.get(2),n,3.875,.125);assertTiming(next,n,0,4);}
}
