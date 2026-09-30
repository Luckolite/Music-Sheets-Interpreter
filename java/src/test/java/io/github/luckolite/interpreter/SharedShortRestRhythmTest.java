// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original abbreviated two-staff bar with explicit closing rests. */
public class SharedShortRestRhythmTest {
    private static ScoreNoteEvent n(int staff, float x, float before, float after) {
        return new ScoreNoteEvent(
                        1, x, 7, staff, 2, staff == 0 ? .1f : .2f, false, 0, 3, 2, 0, 1, after)
                .withLeadingRest(before);
    }

    private static List<ScoreNoteEvent> notes(float bassRest) {
        var notes = new java.util.ArrayList<ScoreNoteEvent>();
        for (int staff = 0; staff < 2; staff++)
            for (float x : new float[] {.04f, .27f, .5f, .73f})
                notes.add(
                        new ScoreNoteEvent(
                                0, x, 7, staff, 2, staff == 0 ? .1f : .2f, false, 0, 0, 2, 1, 1));
        notes.addAll(
                List.of(
                        n(1, .06f, 0, bassRest),
                        n(0, .217f, .125f, 0),
                        n(0, .374f, 0, 0),
                        n(0, .531f, 0, .5f)));
        return notes;
    }

    @Test
    public void matchingShortRestBoundariesKeepWrittenAttackTimes() {
        var notes = notes(.875f);
        double[] onset = {0, .125, .25, .375};
        try (var session = ScoreNoteTiming.beginTimingSession()) {
            for (int i = 0; i < 4; i++) {
                assertEquals(
                        onset[i], ScoreNoteTiming.beatInMeasure(notes.get(i + 8), notes, 4), .0001);
                assertEquals(
                        .125,
                        ScoreNoteTiming.resolvedWrittenDurationBeats(notes.get(i + 8), notes, 4),
                        .0001);
            }
        }
    }

    @Test
    public void mismatchedRestBoundariesDoNotProveAShortBar() {
        var notes = notes(.5f);
        assertTrue(ScoreNoteTiming.beatInMeasure(notes.get(9), notes, 4) > .125);
    }

    @Test
    public void missingClosingRestDoesNotProveAShortBar() {
        var notes = notes(0);
        assertTrue(ScoreNoteTiming.beatInMeasure(notes.get(9), notes, 4) > .125);
    }
}
