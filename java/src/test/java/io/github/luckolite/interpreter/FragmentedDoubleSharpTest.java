// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two-note staff with a split-labelled accidental, not score pixels. */
public class FragmentedDoubleSharpTest {
    List<ScoreNoteEvent> notes(boolean split, boolean missingArm) {
        int w = 420, h = 220;
        byte[] l = new byte[w * h], g = new byte[w * h];
        Arrays.fill(g, (byte) 255);
        for (int y = 80; y <= 144; y += 16)
            for (int x = 15; x < 405; x++) {
                l[y * w + x] = 4;
                g[y * w + x] = 0;
            }
        for (int cx : new int[] {190, 250}) {
            for (int y = 106; y <= 118; y++)
                for (int x = cx - 10; x <= cx + 10; x++)
                    if (Math.pow((x - cx) / 10., 2) + Math.pow((y - 112) / 6., 2) <= 1) {
                        l[y * w + x] = 2;
                        g[y * w + x] = 0;
                    }
            for (int y = 65; y <= 112; y++) {
                l[y * w + cx + 10] = 1;
                g[y * w + cx + 10] = 0;
            }
        }
        for (int y = 0; y < 18; y++)
            for (int x = 0; x < 18; x++)
                if (Math.abs(x - y) <= 4 || Math.abs(x + y - 17) <= 4) {
                    if (missingArm && y > 11) continue;
                    int at = (103 + y) * w + 218 + x;
                    g[at] = 0;
                    l[at] = (byte) (split && y >= 7 && y <= 9 ? 1 : x < 8 ? 3 : 5);
                }
        return OmrScoreInterpreter.extract(
                l, g, w, h, List.of(new MeasureRegion(.02f, .98f, .25f, .8f)));
    }

    @Test
    public void threeFragmentsKeepPrintedDoubleSharp() {
        var n = notes(true, false);
        assertEquals(2, n.size());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_DOUBLE_SHARP, n.get(1).writtenAccidental());
    }

    @Test
    public void accidentalArmsAreNotPreviousNoteDots() {
        var n = notes(false, false);
        assertEquals(2, n.size());
        assertEquals(0, n.get(0).augmentationDots());
    }

    @Test
    public void missingArmsCannotBeInvented() {
        var n = notes(true, true);
        assertEquals(2, n.size());
        assertNotEquals(ScoreNoteEvent.ACCIDENTAL_DOUBLE_SHARP, n.get(1).writtenAccidental());
    }
}
