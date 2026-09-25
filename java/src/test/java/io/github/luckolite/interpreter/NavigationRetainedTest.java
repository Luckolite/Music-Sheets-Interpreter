// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScorePlaybackDirection.Kind.*;

/** Original record-level controls; app conversion joining is intentionally outside this module. */
public class NavigationRetainedTest {
    private static ScorePlaybackDirection d(int n,ScorePlaybackDirection.Kind k) {
        return new ScorePlaybackDirection(n,k);
    }
    @Test public void outgoingAndIncomingSignsBelongToOppositePagesAtSharedBoundary() {
        var signs=List.of(d(0,SEGNO),d(2,TO_CODA),d(2,CODA),d(4,DAL_SEGNO_AL_CODA));
        assertEquals(List.of(d(0,SEGNO),d(2,TO_CODA)),ScorePageInterpretation.directionsOnPage(signs,0,2));
        assertEquals(List.of(d(0,CODA),d(2,DAL_SEGNO_AL_CODA)),ScorePageInterpretation.directionsOnPage(signs,2,4));
    }
    @Test public void emptyOutputPageCannotDuplicateSigns() {
        assertTrue(ScorePageInterpretation.directionsOnPage(List.of(d(2,CODA)),2,2).isEmpty());
    }
    @Test public void oldConstructorKeepsOrdinaryScoreLinear() {
        assertTrue(new ScorePageInterpretation(List.of(),List.of()).playbackDirections().isEmpty());
    }
    @Test public void fixedHairpinTargetAndIndependentSharedTimingSurviveOffset() {
        var dynamic=new ScoreDynamicChange(0,.1f,1,2,1,.8f,-3,1,false,true,true);
        var moved=dynamic.offset(4);
        assertEquals(4,moved.measureIndex());
        assertEquals(5,moved.endMeasureIndex());
        assertEquals(.1f,moved.positionInMeasure(),0);
        assertEquals(.8f,moved.endPosition(),0);
        assertEquals(1,moved.staffIndex());
        assertEquals(2,moved.staffCount());
        assertEquals(-3,moved.decibels(),0);
        assertEquals(1,moved.direction());
        assertFalse(moved.sharedStaffs());
        assertTrue(moved.fixedTarget());
        assertTrue(moved.sharedTiming());
    }
    @Test public void legacyHairpinsStayRelative() {
        assertFalse(new ScoreDynamicChange(0,0,0,1,1,1,0,1,true).fixedTarget());
    }
    @Test public void sharedOwnershipAlwaysRetainsSharedTiming() {
        assertTrue(new ScoreDynamicChange(0,0,0,2,1,1,0,1,true,false,false).sharedTiming());
    }
}
