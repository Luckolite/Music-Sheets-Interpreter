// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original merged/staggered seconds, exercising extraction rather than a helper alone. */
public class DisplacedSecondAccidentalTest extends TouchingChordAccidentalTest {
    void broadHead(int x, int y) {
        for (int yy = y - 7; yy <= y + 7; yy++)
            for (int xx = x - 12; xx <= x + 12; xx++)
                if ((xx - x) * (xx - x) / 144d + (yy - y) * (yy - y) / 49d <= 1)
                    rect(xx, yy, xx, yy, 2);
    }

    void mergedHeads() {
        broadHead(220, 120);
        broadHead(242, 112);
        rect(229, 115, 233, 117, 2);
        rect(231, 78, 232, 115, 1);
    }

    void flat(int x, int y) {
        rect(x, y - 28, x + 2, y + 7, 3);
        for (int xx = 1; xx <= 14; xx++) {
            double t = xx / 14d;
            int upper = (int) Math.round(y - 11 + 7 * t * t),
                    lower = (int) Math.round(y + 7 - 14 * t);
            for (int yy : new int[] {upper - 1, upper, upper + 1, lower - 1, lower, lower + 1})
                rect(x + xx, yy, x + xx, yy, 3);
        }
    }

    int near(List<ScoreNoteEvent> notes, int step) {
        var found = notes.stream().filter(n -> n.staffStep() == step).toList();
        assertEquals("step " + step, 1, found.size());
        return found.get(0).writtenAccidental();
    }

    @Test
    public void oneNaturalDoesNotCancelBothDisplacedTones() {
        natural(200, 120);
        mergedHeads();
        var result = notes();
        assertEquals(2, result.size());
        assertEquals(0, near(result, 3));
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY, near(result, 4));
    }

    @Test
    public void separateFlatAndNaturalSurviveOneMergedHeadComponent() {
        flat(175, 126);
        natural(200, 112);
        mergedHeads();
        var result = notes();
        assertEquals(2, result.size());
        assertEquals(-1, near(result, 3));
        assertEquals(0, near(result, 4));
    }
}
