// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original events in incomplete bars with independently recognized leading silence. */
public class PrintedLeadingRestTest {
    private static ScoreNoteEvent note(float x,float leading) {
        return new ScoreNoteEvent(0,x,0,0,1,.5f,false,0,1,2,0,1).withLeadingRest(leading);
    }
    @Test public void doubleDottedRestCannotBeShortenedByTheNoteInset() {
        var a=note(.32f,1.75f);var b=note(.57f,0);var c=note(.81f,0);
        assertEquals(1.75,ScoreNoteTiming.beatInMeasure(a,List.of(a,b,c),4),.0001);
    }
    @Test public void anUnrecognizedEarlierRestCanStillLeaveExtraLeadingSpace() {
        var a=note(.75f,1);var b=note(.875f,0);
        assertEquals(3,ScoreNoteTiming.beatInMeasure(a,List.of(a,b),4),.0001);
    }
    @Test public void noRestDoesNotCreateASilentPrefix() {
        var a=note(.1f,0);var b=note(.3f,0);
        assertEquals(0,ScoreNoteTiming.beatInMeasure(a,List.of(a,b),4),.0001);
    }
    @Test public void inconsistentRestDurationCannotPushAnAttackIntoTheNextBar() {
        var a=note(.1f,5);var b=note(.3f,0);
        assertTrue(ScoreNoteTiming.beatInMeasure(a,List.of(a,b),4)<4);
    }
}
