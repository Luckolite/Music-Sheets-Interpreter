// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original event sequences distinguish close engraving from simultaneous voices. */
public class DenseIndependentVoiceTest {
    private static ScoreNoteEvent note(float x,int step,int beams) {
        return new ScoreNoteEvent(0,x,step,0,1,.3f+step*.006f,false,0,beams,2,beams==0?1:0);
    }
    private static List<ScoreNoteEvent> passage() {
        List<ScoreNoteEvent> ns=new ArrayList<>();
        for(int i=0;i<12;i++)ns.add(note(.05f+i*.03f,i%7,i==4?0:2));
        ns.set(5,note(.186f,5,2));return ns;
    }
    @Test public void successiveDenseAttacksDoNotEstablishParallelVoices(){var ns=passage();assertFalse(ScoreNoteTiming.hasIndependentDuration(ns.get(4),ns));}
    @Test public void oneClosePairCannotProtectEveryMissingBeam(){var ns=passage();var missing=note(.32f,4,0);ns.set(9,missing);assertFalse(ScoreNoteTiming.hasIndependentDuration(missing,ns));}
    @Test public void exactlySharedOnsetPreservesQuarter(){var ns=passage();ns.set(5,note(ns.get(4).positionInMeasure(),5,2));assertTrue(ScoreNoteTiming.hasIndependentDuration(ns.get(4),ns));assertEquals(1,ScoreNoteTiming.resolvedWrittenDurationBeats(ns.get(4),ns,4),0);}
    @Test public void smallChordCentreOffsetPreservesQuarter(){var ns=passage();ns.set(5,note(ns.get(4).positionInMeasure()+.001f,5,2));assertTrue(ScoreNoteTiming.hasIndependentDuration(ns.get(4),ns));}
    @Test public void manyOffsetChordMembersDoNotShrinkAttackSpacing(){var ns=new ArrayList<ScoreNoteEvent>();for(int i=0;i<10;i++)for(int j=0;j<5;j++)ns.add(note(.05f+i*.05f+j*.001f,j,j==0?0:2));assertTrue(ScoreNoteTiming.hasIndependentDuration(ns.get(0),ns));}
    @Test public void sparseVoiceKeepsExistingTolerance(){var a=note(.1f,1,0);assertTrue(ScoreNoteTiming.hasIndependentDuration(a,List.of(a,note(.115f,4,2),note(.4f,3,2))));}
    @Test public void genuinePolyphonyProtectsItsLaterQuarter(){var ns=passage();ns.set(5,note(ns.get(4).positionInMeasure(),5,2));var later=note(.39f,2,0);ns.add(later);assertTrue(ScoreNoteTiming.hasIndependentDuration(later,ns));}
    @Test public void hollowSustainNeverDependsOnTheSpacingVote(){var ns=passage();var half=new ScoreNoteEvent(0,.17f,2,0,1,.4f,false,0,0,2,2);ns.add(half);assertTrue(ScoreNoteTiming.hasIndependentDuration(half,ns));}
    @Test public void orderingDoesNotChangeVoiceEvidence(){var ns=passage();var target=ns.get(4);Collections.reverse(ns);assertFalse(ScoreNoteTiming.hasIndependentDuration(target,ns));}
    @Test public void inputEventsAndOrderRemainUnchanged(){var ns=passage();var before=List.copyOf(ns);ScoreNoteTiming.hasIndependentDuration(ns.get(4),ns);assertEquals(before,ns);}
}
