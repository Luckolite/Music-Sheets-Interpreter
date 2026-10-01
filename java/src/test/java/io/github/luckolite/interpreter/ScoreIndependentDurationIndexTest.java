// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScoreIndependentDurationIndexTest {
    @Test
    public void indexedParallelVoiceDetectionMatchesWholeScoreChecks() {
        var notes = new ArrayList<ScoreNoteEvent>();
        for (int m = 0; m < 16; m++)
            for (int staff = 0; staff < 3; staff++)
                for (int beat = 0; beat < 4; beat++) {
                    notes.add(
                            new ScoreNoteEvent(
                                    m,
                                    .05f + beat * .22f,
                                    2,
                                    staff,
                                    3,
                                    .2f + staff * .2f,
                                    false,
                                    0,
                                    0,
                                    ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                                    1));
                    notes.add(
                            new ScoreNoteEvent(
                                    m,
                                    .05f + beat * .22f,
                                    4,
                                    staff,
                                    3,
                                    .2f + staff * .2f,
                                    false,
                                    0,
                                    m % 3 == 0 ? 1 : 0,
                                    ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                                    m % 3 == 0 ? 0 : 2,
                                    m % 4 == 0 ? 3 : 1));
                }
        Collections.shuffle(notes, new Random(817));
        var expected =
                notes.stream().map(n -> ScoreNoteTiming.hasIndependentDuration(n, notes)).toList();
        try (var session = ScoreNoteTiming.beginTimingSession()) {
            for (int i = 0; i < notes.size(); i++)
                assertEquals(
                        expected.get(i),
                        ScoreNoteTiming.hasIndependentDuration(notes.get(i), notes));
            assertEquals(
                    "One reused index for the immutable score",
                    1,
                    session.scoreIndexCalculationCount());
        }
    }
}
