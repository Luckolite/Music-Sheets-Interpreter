// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import org.junit.Test;

/** A long score must learn staff padding once, rather than allocating a score-sized list per note. */
public final class LargeScoreTimingSessionTest {
    @Test public void leadingInsetIsReusedAcrossTheScore() {
        var notes=new ArrayList<ScoreNoteEvent>();
        for(int measure=0;measure<400;measure++) {
            notes.add(new ScoreNoteEvent(measure,.08f,0,0,1));
            notes.add(new ScoreNoteEvent(measure,.52f,1,0,1));
        }
        try(var timing=ScoreNoteTiming.beginTimingSession()) {
            for(int measure=0;measure<400;measure++)
                ScoreNoteTiming.beatInMeasure(notes.get(measure*2),notes,4);
            assertEquals(1,timing.leadingInsetCalculationCount());
        }
    }

    @Test public void graceFilteringAndIndexesAreBoundedForBookSizedScores() {
        var notes=new ArrayList<ScoreNoteEvent>();
        for(int measure=0;measure<2250;measure++) {
            for(int slot=0;slot<8;slot++)
                notes.add(new ScoreNoteEvent(measure,.06f+slot*.12f,slot%7,0,1));
        }
        notes.set(0,notes.get(0).withArticulations(NoteOrnament.GRACE));
        try(var timing=ScoreNoteTiming.beginTimingSession()) {
            for(ScoreNoteEvent note:notes)
                ScoreNoteTiming.beatInMeasure(note,notes,4);
            assertEquals(1,timing.metricalListCalculationCount());
            assertTrue(timing.scoreIndexCalculationCount()<=2);
        }
    }
}
