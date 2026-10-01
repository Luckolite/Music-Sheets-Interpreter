// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original angular ink; no source score images or geometry fixtures. */
public class WideCaretArticulationTest {
    static final int W = 2048, H = 1600;

    private int detect(boolean inverted, boolean crossbar, boolean curve) {
        byte[] gray = new byte[W * H], labels = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int x = 100; x <= 140; x++) {
            double t = (x - 100) / 40d;
            int y =
                    (int)
                            Math.round(
                                    80
                                            + (inverted
                                                            ? 1 - Math.abs(2 * t - 1)
                                                            : Math.abs(2 * t - 1))
                                                    * 20
                                            + (curve ? 6 * Math.sin(Math.PI * t) : 0));
            for (int dy = -1; dy <= 1; dy++) gray[(y + dy) * W + x] = 0;
        }
        if (crossbar)
            for (int x = 109; x <= 131; x++)
                for (int dy = -1; dy <= 1; dy++) gray[(91 + dy) * W + x] = 0;
        return NoteArticulationDetector.detect(
                labels, gray, W, H, List.of(new NoteArticulationDetector.Anchor(120, 135, 24, 0)))[
                0];
    }

    @Test
    public void wideDetachedPeakIsMarcato() {
        assertEquals(NoteArticulation.MARCATO, detect(false, false, false));
    }

    @Test
    public void UpBowIsNotMarcato() {
        assertEquals(0, detect(true, false, false) & NoteArticulation.MARCATO);
    }

    @Test
    public void LetterCrossbarIsNotMarcato() {
        assertEquals(0, detect(false, true, false) & NoteArticulation.MARCATO);
    }

    @Test
    public void CurvedRoofIsNotMarcato() {
        assertEquals(0, detect(false, false, true) & NoteArticulation.MARCATO);
    }

    private int feetOnRules(int x, int end, boolean parallel, boolean inverted) {
        byte[] gray = new byte[W * H], labels = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int row : parallel ? new int[] {100, 124, 148, 172, 196} : new int[] {100})
            for (int xx = 30; xx <= end; xx++)
                for (int dy = -1; dy <= 1; dy++) gray[(row + dy) * W + xx] = 0;
        for (int xx = x - 20; xx <= x + 20; xx++) {
            double fraction = Math.abs(xx - x) / 20d;
            int y = (int) Math.round(80 + (inverted ? 1 - fraction : fraction) * 20);
            for (int dy = -1; dy <= 1; dy++) gray[(y + dy) * W + xx] = 0;
        }
        return NoteArticulationDetector.detect(
                labels, gray, W, H, List.of(new NoteArticulationDetector.Anchor(x, 135, 24, 0)))[0];
    }

    @Test
    public void caretFeetCanEndOnStaffRule() {
        assertEquals(NoteArticulation.MARCATO, feetOnRules(150, 300, true, false));
    }

    @Test
    public void staffEndUsesProvedParallelRules() {
        assertEquals(NoteArticulation.MARCATO, feetOnRules(220, 240, true, false));
    }

    @Test
    public void loneShortLineIsNotStaffProof() {
        assertEquals(0, feetOnRules(220, 240, false, false) & NoteArticulation.MARCATO);
    }

    @Test
    public void ruleTouchingUpBowIsRejected() {
        assertEquals(0, feetOnRules(220, 240, true, true) & NoteArticulation.MARCATO);
    }
}
