// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two-staff timing examples with one damaged rhythm. */
public class CompleteRestVoiceAnchorTest {
    private static ScoreNoteEvent note(int staff,float x,int beams,float following,float leading) {
        return new ScoreNoteEvent(0,x,0,staff,2,.3f+staff*.2f,false,0,beams,2,0,1,following).withLeadingRest(leading);
    }
    private static List<ScoreNoteEvent> upper() {
        return List.of(note(0,.17f,1,.5f,.5f),note(0,.39f,1,.5f,0),note(0,.72f,1,.5f,0),note(0,.94f,1,0,0));
    }
    @Test public void completePrintedRestsPreventAnEarlierGeometricCrossStaffAnchor() {
        var upper=upper();var lowerFirst=note(1,.28f,2,0,1);var lowerSecond=note(1,.395f,1,0,0);
        var notes=new ArrayList<>(upper);notes.add(lowerFirst);notes.add(lowerSecond);
        assertEquals(1.5,ScoreNoteTiming.beatInMeasure(upper.get(1),notes,4),.0001);
        assertEquals(1.5,ScoreNoteTiming.beatInMeasure(lowerSecond,notes,4),.0001);
    }
    @Test public void contradictoryCompleteVoicesKeepIndependentOnsets() {
        var upper=upper();var lower=List.of(note(1,.17f,1,.5f,.75f),note(1,.39f,1,.5f,0),
                note(1,.72f,1,.25f,0),note(1,.94f,1,0,0));
        var notes=new ArrayList<>(upper);notes.addAll(lower);
        assertEquals(1.5,ScoreNoteTiming.beatInMeasure(upper.get(1),notes,4),.0001);
        assertEquals(1.75,ScoreNoteTiming.beatInMeasure(lower.get(1),notes,4),.0001);
    }
}
