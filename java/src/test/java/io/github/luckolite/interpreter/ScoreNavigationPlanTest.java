// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScorePlaybackDirection.Kind.*;

/** Original logical scores only, without title-specific geometry or source pages. */
public class ScoreNavigationPlanTest {
    private static ScorePlaybackDirection d(int boundary,ScorePlaybackDirection.Kind kind){return new ScorePlaybackDirection(boundary,kind);}
    private static List<ScorePlaybackDirection> directions(){return List.of(d(2,SEGNO),d(5,TO_CODA),d(8,DAL_SEGNO_AL_CODA),d(8,CODA));}
    private static List<Integer> linear(){return List.of(0,1,2,3,4,5,6,7,8,9);}
    @Test public void defaultScoreIsExactlyLinear(){assertEquals(linear(),ScoreNavigationPlan.create(10,List.of()).sourceMeasures());}
    @Test public void nullDirectionsRemainLinear(){assertEquals(linear(),ScoreNavigationPlan.create(10,null).sourceMeasures());}
    @Test public void jumpBackOnceAndArmCodaOnlyOnSecondPass(){
        var plan=ScoreNavigationPlan.create(10,directions());
        assertTrue(plan.dalSegnoApplied());assertEquals(List.of(0,1,2,3,4,5,6,7,2,3,4,8,9),plan.sourceMeasures());
    }
    @Test public void sourceLookupMapsRepeatedMeasuresToSamePrintedBar(){
        var plan=ScoreNavigationPlan.create(10,directions());assertEquals(2,plan.sourceMeasure(2));assertEquals(2,plan.sourceMeasure(8));assertEquals(8,plan.sourceMeasure(11));
    }
    @Test public void missingEachAnchorCannotInventNavigation(){
        for(int i=0;i<4;i++){var marks=new ArrayList<>(directions());marks.remove(i);assertEquals(linear(),ScoreNavigationPlan.create(10,marks).sourceMeasures());}
    }
    @Test public void duplicateStaffSignsAreDeduplicated(){var marks=new ArrayList<>(directions());marks.addAll(directions());assertEquals(13,ScoreNavigationPlan.create(10,marks).measureCount());}
    @Test public void ambiguousDestinationsRemainLinear(){var marks=new ArrayList<>(directions());marks.add(d(1,SEGNO));assertEquals(linear(),ScoreNavigationPlan.create(10,marks).sourceMeasures());}
    @Test public void codaAfterUnplayedTransitionSkipsOnlyTheTransition(){
        var plan=ScoreNavigationPlan.create(10,List.of(d(1,SEGNO),d(3,TO_CODA),d(6,DAL_SEGNO_AL_CODA),d(8,CODA)));
        assertEquals(List.of(0,1,2,3,4,5,1,2,8,9),plan.sourceMeasures());
    }
    @Test public void codaAtSourceEndWithoutMusicIsInvalid(){assertFalse(ScoreNavigationPlan.create(10,List.of(d(1,SEGNO),d(3,TO_CODA),d(8,DAL_SEGNO_AL_CODA),d(10,CODA))).dalSegnoApplied());}
    @Test public void reversedOrCoincidentReturnRangeCannotLoop(){
        for(int to:new int[]{0,2,9})assertFalse(ScoreNavigationPlan.create(10,List.of(d(2,SEGNO),d(to,TO_CODA),d(8,DAL_SEGNO_AL_CODA),d(8,CODA))).dalSegnoApplied());
    }
    @Test public void outOfPageBoundaryIsRejectedWithoutPartialRouting(){var marks=new ArrayList<>(directions());marks.add(d(12,CODA));assertEquals(linear(),ScoreNavigationPlan.create(10,marks).sourceMeasures());}
    @Test public void nullEntryDoesNotInventAnAnchor(){var marks=new ArrayList<>(directions());marks.add(null);assertEquals(linear(),ScoreNavigationPlan.create(10,marks).sourceMeasures());}
    @Test public void routeLengthIsBoundedByTwiceTheSource(){
        for(int n=4;n<60;n++)for(int segno=0;segno<n-2;segno++){
            var plan=ScoreNavigationPlan.create(n,List.of(d(segno,SEGNO),d(n-2,TO_CODA),d(n-1,DAL_SEGNO_AL_CODA),d(n-1,CODA)));
            assertTrue(plan.dalSegnoApplied());assertTrue(plan.measureCount()<2*n);
        }
    }
    @Test public void immutablePlanCannotBeChangedByCaller(){var marks=new ArrayList<>(directions());var plan=ScoreNavigationPlan.create(10,marks);marks.clear();assertEquals(13,plan.measureCount());try{plan.sourceMeasures().clear();fail();}catch(UnsupportedOperationException expected){}}
    @Test public void pageLocalDirectionsOffsetWithoutLosingKind(){assertEquals(d(9,CODA),d(2,CODA).offset(7));}
    @Test public void emptyScoreHasNoInventedBars(){assertEquals(List.of(),ScoreNavigationPlan.create(0,List.of()).sourceMeasures());}
    @Test public void sourceTapUsesNearestPerformanceOccurrence(){
        var plan=ScoreNavigationPlan.create(10,directions());
        assertEquals(2,plan.playbackMeasure(2,0));assertEquals(8,plan.playbackMeasure(2,9));
        assertEquals(2,plan.playbackMeasure(2,5));
    }
    @Test public void omittedSourceMeasureHasNoPlaybackOccurrence(){
        var plan=ScoreNavigationPlan.create(10,List.of(d(1,SEGNO),d(3,TO_CODA),d(6,DAL_SEGNO_AL_CODA),d(8,CODA)));
        assertEquals(-1,plan.playbackMeasure(7,7));
    }
}
