// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Explicit written grace metadata; no source scans, font assets or geometric beat guesses. */
public class WrittenGracePlacementTest {
    private ScoreNoteEvent normal(float x, int step) {
        return new ScoreNoteEvent(0, x, step, 0, 1, .3f, false, 0, 2, 2, 1);
    }

    private ScoreNoteEvent grace(float x, int step) {
        return new ScoreNoteEvent(0, x, step, 0, 1, .3f, false, 0, 3, 2, 1)
                .withArticulations(NoteOrnament.GRACE);
    }

    private List<ScoreNoteEvent> prefix() {
        var notes = new ArrayList<ScoreNoteEvent>();
        for (int i = 0; i < 12; i++) {
            if (i == 4) {
                notes.add(grace(.36f, -7));
                notes.add(grace(.38f, -6));
            }
            notes.add(normal(.12f + i * .07f, i));
        }
        return notes;
    }

    private Optional<?> placement(ScoreNoteEvent target, List<ScoreNoteEvent> notes, float beats)
            throws Exception {
        try {
            return (Optional<?>)
                    ScoreNoteTiming.class
                            .getMethod(
                                    "writtenPlacement",
                                    ScoreNoteEvent.class,
                                    List.class,
                                    float.class)
                            .invoke(null, target, notes, beats);
        } catch (NoSuchMethodException absent) {
            return Optional.empty();
        }
    }

    private Object field(Object placement, String name) throws Exception {
        return placement.getClass().getMethod(name).invoke(placement);
    }

    @Test
    public void prefixKeepsWrittenGraceValueAndPrincipalIdentity() throws Exception {
        var n = prefix();
        var p = placement(n.get(4), n, 3).orElseThrow();
        assertEquals(1.0, (double) field(p, "onsetBeats"), 1e-7);
        assertEquals(.125, (double) field(p, "durationBeats"), 1e-7);
        assertEquals(6, field(p, "principalIndex"));
        assertEquals(0, field(p, "ordinal"));
        assertEquals(false, field(p, "afterGrace"));
        assertEquals(12.5, (double) field(p, "stealPercent"), 1e-7);
    }

    @Test
    public void principalKeepsItsFullWrittenSixteenth() throws Exception {
        var n = prefix();
        var p = placement(n.get(6), n, 3).orElseThrow();
        assertEquals(1.0, (double) field(p, "onsetBeats"), 1e-7);
        assertEquals(.25, (double) field(p, "durationBeats"), 1e-7);
        assertEquals(-1, field(p, "principalIndex"));
    }

    @Test
    public void trailingPairAnchorsAtWrittenRelease() throws Exception {
        var main = new ScoreNoteEvent(0, .1f, 0, 0, 1, .3f, false, 0, 0, 2, 4);
        var n = List.of(main, grace(.8f, 1), grace(.9f, 2));
        var p = placement(n.get(2), n, 4).orElseThrow();
        assertEquals(4.0, (double) field(p, "onsetBeats"), 1e-7);
        assertEquals(true, field(p, "afterGrace"));
        assertEquals(1, field(p, "ordinal"));
        assertEquals(0, field(p, "principalIndex"));
        assertEquals(
                4.0, (double) field(placement(main, n, 4).orElseThrow(), "durationBeats"), 1e-7);
    }

    @Test
    public void unownedGraceDoesNotInventAMetricAttack() throws Exception {
        var n = List.of(grace(.2f, 1), grace(.3f, 2));
        assertTrue(placement(n.get(0), n, 3).isEmpty());
    }

    @Test
    public void ordinaryFastNoteRemainsMetrical() throws Exception {
        var n = List.of(new ScoreNoteEvent(0, .2f, 1, 0, 1, .3f, false, 0, 3, 2, 1));
        var p = placement(n.get(0), n, 3).orElseThrow();
        assertEquals(-1, field(p, "principalIndex"));
    }

    @Test
    public void engravingDoesNotChangePlaybackBudgets() throws Exception {
        var n = prefix();
        assertTrue(placement(n.get(4), n, 3).isPresent());
        assertEquals(.03125, ScoreNoteTiming.resolvedWrittenDurationBeats(n.get(4), n, 3), 1e-7);
        assertEquals(1.0625, ScoreNoteTiming.beatInMeasure(n.get(6), n, 3), 1e-7);
        assertEquals(.1875, ScoreNoteTiming.resolvedWrittenDurationBeats(n.get(6), n, 3), 1e-7);
    }

    @Test
    public void timingSessionKeepsTheSameWrittenAndPerformanceClocks() throws Exception {
        var n = prefix();
        var expected = placement(n.get(4), n, 3).orElseThrow();
        try (var session = ScoreNoteTiming.beginTimingSession()) {
            for (int i = 0; i < 3; i++) {
                assertEquals(expected, placement(n.get(4), n, 3).orElseThrow());
                assertEquals(1.0625, ScoreNoteTiming.beatInMeasure(n.get(6), n, 3), 1e-7);
                assertEquals(
                        1.0,
                        (double) field(placement(n.get(6), n, 3).orElseThrow(), "onsetBeats"),
                        1e-7);
            }
        }
    }

    @Test
    public void equalRecordCloneCannotClaimAnotherSourcesIdentity() throws Exception {
        var n = prefix();
        var original = n.get(4);
        var clone = grace(original.positionInMeasure(), original.staffStep());
        assertEquals(original, clone);
        assertNotSame(original, clone);
        assertTrue(placement(clone, n, 3).isEmpty());
        assertTrue(placement(original, n, Float.NaN).isEmpty());
    }
}
