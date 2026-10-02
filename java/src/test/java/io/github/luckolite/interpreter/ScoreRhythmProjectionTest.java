// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic incomplete melody aligned to a fully written accompaniment. */
public class ScoreRhythmProjectionTest {
    private static ScoreNoteEvent note(float position, int staff, int beams, float duration) {
        return new ScoreNoteEvent(
                        0,
                        position,
                        staff == 0 ? 3 : 0,
                        staff,
                        2,
                        .2f + staff * .3f,
                        false,
                        0,
                        beams,
                        ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                        duration)
                .withClef(staff == 0 ? ScoreNoteEvent.CLEF_TREBLE : ScoreNoteEvent.CLEF_BASS);
    }

    private static List<ScoreNoteEvent> melody() {
        return List.of(note(.12f, 0, 0, 1), note(.4f, 0, 0, 1), note(.92f, 0, 1, 0));
    }

    private static List<ScoreNoteEvent> source() {
        var all = new ArrayList<>(melody());
        for (float x : new float[] {.12f, .22f, .32f, .4f, .52f, .62f, .72f, .92f})
            all.add(note(x, 1, 1, 0));
        return List.copyOf(all);
    }

    @Test
    public void omittedAccompanimentCannotPullFinalEighthEarlier() {
        var selected = melody();
        assertEquals(3.25, ScoreNoteTiming.beatInMeasure(selected.get(2), selected, 4), 0);
        var placed = ScoreRhythmProjection.resolve(source(), selected, 4, List.of());
        assertEquals(3.5, placed.get(2).onsetBeats(), 0);
        assertEquals(.5, placed.get(2).durationBeats(), 0);
    }

    @Test
    public void selectionOrderDefinesIndicesAndResultIsImmutable() {
        var notes = melody();
        var selected = List.of(notes.get(2), notes.get(0));
        var placed = ScoreRhythmProjection.resolve(source(), selected, 4, List.of());
        assertEquals(3.5, placed.get(0).onsetBeats(), 0);
        assertEquals(0, placed.get(1).onsetBeats(), 0);
        assertThrows(UnsupportedOperationException.class, () -> placed.clear());
    }

    @Test
    public void combinedRecordWithoutSourceOwnerDoesNotInventPlacement() {
        assertTrue(
                ScoreRhythmProjection.resolve(source(), List.of(note(.73f, 0, 0, 2)), 4, List.of())
                        .isEmpty());
    }

    @Test
    public void invalidResolvedValuesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScoreRhythmProjection.Placement(-1, .5, false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScoreRhythmProjection.Placement(0, Double.NaN, false));
    }
}
