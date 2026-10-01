// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original event geometry: a dotted beamed voice overlaps an undotted moving line. */
public class ParallelDottedVoiceTimingTest {
    private static ScoreNoteEvent note(float x, int pitch, int beams, int dots) {
        return new ScoreNoteEvent(
                0,
                x,
                pitch,
                0,
                1,
                .3f,
                false,
                dots,
                beams,
                ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                beams == 0 ? 1 : 0);
    }

    private static List<ScoreNoteEvent> score(boolean secondVoice) {
        var result = new ArrayList<ScoreNoteEvent>();
        float[] positions = {.08f, .19f, .27f, .36f, .47f, .55f, .65f, .83f};
        int[] beams = {1, 2, 2, 1, 2, 2, 0, 0};
        for (int i = 0; i < positions.length; i++) {
            result.add(note(positions[i], i * 2, beams[i], 0));
            if (i == 0 || i == 3 && secondVoice)
                result.add(note(positions[i] + .0004f, 18 + i, 1, 1));
        }
        return result;
    }

    @Test
    public void movingLineAdvancesByItsOwnSymbols() {
        var notes = score(true);
        double[] expected = {0, .5, .75, 1, 1.5, 1.75, 2, 3};
        int i = 0;
        for (var n : notes)
            if (n.staffStep() < 18)
                assertEquals(
                        "moving pitch " + n.staffStep(),
                        expected[i++],
                        ScoreNoteTiming.beatInMeasure(n, notes, 4),
                        .00001);
    }

    @Test
    public void eachCoincidentVoiceRetainsItsWrittenDuration() {
        var notes = score(true);
        for (var n : notes)
            assertEquals(
                    ScoreNoteTiming.writtenDurationBeats(n),
                    ScoreNoteTiming.resolvedWrittenDurationBeats(n, notes, 4),
                    .00001);
    }

    @Test
    public void dottedVoiceIsIndependentAndCachedClockMatches() {
        var notes = score(true);
        try (var session = ScoreNoteTiming.beginTimingSession()) {
            for (var n : notes)
                if (n.staffStep() >= 18) {
                    assertTrue(ScoreNoteTiming.hasIndependentDuration(n, notes));
                    assertEquals(
                            n.staffStep() == 18 ? 0 : 1,
                            ScoreNoteTiming.beatInMeasure(n, notes, 4),
                            .00001);
                    assertEquals(
                            .75, ScoreNoteTiming.resolvedWrittenDurationBeats(n, notes, 4), .00001);
                }
        }
    }

    @Test
    public void isolatedDotDisagreementDoesNotProveRepeatedVoices() {
        var notes = score(false);
        assertFalse(ScoreNoteTiming.hasIndependentDuration(notes.get(1), notes));
    }

    @Test
    public void ordinaryDottedChordKeepsItsSharedValue() {
        var notes =
                List.of(
                        note(.1f, 0, 1, 1),
                        note(.1f, 4, 1, 1),
                        note(.35f, 2, 2, 0),
                        note(.55f, 4, 0, 0),
                        note(.8f, 6, 0, 0));
        assertEquals(
                .75, ScoreNoteTiming.resolvedWrittenDurationBeats(notes.get(0), notes, 3), .00001);
        assertEquals(.75, ScoreNoteTiming.beatInMeasure(notes.get(2), notes, 3), .00001);
    }
}
