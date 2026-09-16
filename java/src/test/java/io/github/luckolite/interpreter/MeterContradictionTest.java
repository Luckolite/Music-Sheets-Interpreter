// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MeterContradictionTest {
    @Test public void repeatedWrittenBarsRejectImpossibleSingleOcrReading() {
        assertTrue(MeterChangeDetector.contradictedByWrittenBars(
                new ScoreMeterChange(0,1,4),bars(6,3),3));
        assertTrue(MeterChangeDetector.contradictedByWrittenBars(
                new ScoreMeterChange(0,14,4),bars(4,3),3));
    }

    @Test public void realAndInconclusiveMetersArePreserved() {
        assertFalse(MeterChangeDetector.contradictedByWrittenBars(
                new ScoreMeterChange(0,12,8),bars(6,2),2));
        assertFalse("One contrary bar is insufficient",MeterChangeDetector.contradictedByWrittenBars(
                new ScoreMeterChange(0,4,4),bars(3,1),1));
    }

    private static List<ScoreNoteEvent> bars(int beats,int count) {
        var notes=new ArrayList<ScoreNoteEvent>();
        for(int bar=0;bar<count;bar++)for(int beat=0;beat<beats;beat++)
            notes.add(new ScoreNoteEvent(bar,beat/(float)beats,0,0,1,.3f,false,
                    0,0,ScoreNoteEvent.ACCIDENTAL_FROM_KEY,1));
        return notes;
    }
}
