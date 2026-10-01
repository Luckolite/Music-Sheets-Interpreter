// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic polyphonic fixtures; no score scans or device data. */
public class ScoreRhythmGroupReuseTest {
    private static List<ScoreNoteEvent> score() {
        var notes = new ArrayList<ScoreNoteEvent>();
        for (int bar = 0; bar < 4; bar++)
            for (int staff = 0; staff < 2; staff++)
                for (int beat = 0; beat < 4; beat++) {
                    int beam = bar % 2 == 0 ? 1 : 0;
                    notes.add(
                            new ScoreNoteEvent(
                                            bar,
                                            .06f + beat * .21f,
                                            0,
                                            staff,
                                            2,
                                            .2f + staff * .2f,
                                            false,
                                            beat == 1 ? 1 : 0,
                                            beam,
                                            ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                                            beam == 0 ? 1 : 0,
                                            bar == 2 ? 3 : 1)
                                    .withLeadingRest(beat == 0 ? .125f * (staff + 1) : 0));
                    notes.add(
                            new ScoreNoteEvent(
                                            bar,
                                            .06f + beat * .21f,
                                            4,
                                            staff,
                                            2,
                                            .2f + staff * .2f,
                                            false,
                                            0,
                                            0,
                                            ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                                            beat == 0 ? 2 : 1)
                                    .withLeadingRest(beat == 0 ? .25f : 0));
                }
        notes.add(
                new ScoreNoteEvent(
                                1,
                                .01f,
                                2,
                                0,
                                2,
                                .2f,
                                false,
                                0,
                                2,
                                ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                                0)
                        .withArticulations(NoteOrnament.GRACE));
        Collections.shuffle(notes, new Random(817));
        return List.copyOf(notes);
    }

    private static long[] values(ScoreNoteEvent note, List<ScoreNoteEvent> notes, float beats) {
        return new long[] {
            Double.doubleToLongBits(ScoreNoteTiming.beatInMeasure(note, notes, beats)),
            Double.doubleToLongBits(
                    ScoreNoteTiming.resolvedWrittenDurationBeats(note, notes, beats)),
            ScoreNoteTiming.hasIndependentDuration(note, notes) ? 1 : 0
        };
    }

    @Test
    public void repeatedGroupsKeepUncachedResultsAcrossMetersAndGracePolyphony() {
        var notes = score();
        var expected = new ArrayList<long[]>();
        float[] meters = {4, 3, 6, Float.NaN, Float.POSITIVE_INFINITY, .125f, 128};
        for (float beats : meters) for (var note : notes) expected.add(values(note, notes, beats));
        try (var session = ScoreNoteTiming.beginTimingSession()) {
            int i = 0;
            for (float beats : meters)
                for (var note : notes)
                    assertArrayEquals(expected.get(i++), values(note, notes, beats));
            int built = session.rhythmGroupCalculationCount();
            int independent = session.independentDurationCalculationCount();
            assertTrue("Groups assembled", built > 0);
            for (var note : notes) values(note, notes, 4);
            assertEquals(
                    "The same voice snapshots reuse their groups",
                    built,
                    session.rhythmGroupCalculationCount());
            assertEquals(
                    "Repeated notes reuse independent-duration decisions",
                    independent,
                    session.independentDurationCalculationCount());
        }
    }

    @Test
    public void separateSnapshotsAndNestedSessionsDoNotShareGroups() {
        var notes = score();
        var changed = new ArrayList<>(notes);
        changed.add(
                new ScoreNoteEvent(
                        0, .34f, 7, 0, 2, .2f, false, 0, 2, ScoreNoteEvent.ACCIDENTAL_FROM_KEY, 0));
        var target =
                notes.stream()
                        .filter(
                                note ->
                                        !Arrays.equals(
                                                values(note, notes, 4), values(note, changed, 4)))
                        .findFirst()
                        .orElseThrow(
                                () -> new AssertionError("The changed snapshot must alter timing"));
        long[] original = values(target, notes, 4), modified = values(target, changed, 4);
        try (var outer = ScoreNoteTiming.beginTimingSession()) {
            assertArrayEquals(original, values(target, notes, 4));
            int count = outer.rhythmGroupCalculationCount();
            try (var inner = ScoreNoteTiming.beginTimingSession()) {
                assertArrayEquals(modified, values(target, changed, 4));
                assertTrue(inner.rhythmGroupCalculationCount() > 0);
            }
            assertArrayEquals(original, values(target, notes, 4));
            assertEquals(count, outer.rhythmGroupCalculationCount());
            assertArrayEquals(modified, values(target, changed, 4));
        }
        assertArrayEquals(original, values(target, notes, 4));
    }

    @Test
    public void boundedIndependentDecisionsRecomputeAfterEviction() {
        var notes = new ArrayList<ScoreNoteEvent>();
        for (int measure = 0; measure < 8300; measure++)
            notes.add(
                    new ScoreNoteEvent(
                            measure,
                            .1f,
                            0,
                            0,
                            1,
                            .2f,
                            false,
                            0,
                            1,
                            ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                            0));
        try (var session = ScoreNoteTiming.beginTimingSession()) {
            for (var note : notes) assertFalse(ScoreNoteTiming.hasIndependentDuration(note, notes));
            assertEquals(notes.size(), session.independentDurationCalculationCount());
            assertFalse(ScoreNoteTiming.hasIndependentDuration(notes.get(0), notes));
            assertEquals(notes.size() + 1, session.independentDurationCalculationCount());
        }
    }
}
