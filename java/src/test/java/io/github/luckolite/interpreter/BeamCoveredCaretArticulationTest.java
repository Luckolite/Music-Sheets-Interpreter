// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original small caret touching a beam-covered staff rule. */
public class BeamCoveredCaretArticulationTest {
    private int detect(boolean parallel, boolean inverted, boolean crossbar, boolean broken) {
        int w = 2048, h = 1600;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int y : parallel ? new int[] {100, 114, 128, 142, 156} : new int[] {100})
            for (int x = 210; x <= 480; x++) if (!broken || x < 365 || x > 390) gray[y * w + x] = 0;
        for (int y = 100; y <= 106; y++) for (int x = 230; x <= 360; x++) gray[y * w + x] = 0;
        for (int y = 101; y <= 145; y++)
            for (int x = 229; x <= 230; x++) {
                gray[y * w + x] = 0;
                labels[y * w + x] = OmrMeasurePostProcessor.STEM_OR_REST;
            }
        for (int x = 210; x <= 230; x++) {
            double fraction = Math.abs(x - 220) / 10d;
            int y = (int) Math.round(86 + (inverted ? 1 - fraction : fraction) * 14);
            gray[y * w + x] = 0;
            gray[(y + 1) * w + x] = 0;
        }
        if (crossbar)
            for (int x = 214; x <= 226; x++) for (int y = 94; y <= 95; y++) gray[y * w + x] = 0;
        return NoteArticulationDetector.detect(
                labels, gray, w, h, List.of(new NoteArticulationDetector.Anchor(220, 145, 14, 0)))[
                0];
    }

    @Test
    public void uncoveredParallelRulesProveCoveredCaretFeet() {
        assertEquals(NoteArticulation.MARCATO, detect(true, false, false, false));
    }

    @Test
    public void loneCoveredRuleCannotProveStaff() {
        assertEquals(0, detect(false, false, false, false) & NoteArticulation.MARCATO);
    }

    @Test
    public void joinedUpBowIsNotMarcato() {
        assertEquals(0, detect(true, true, false, false) & NoteArticulation.MARCATO);
    }

    @Test
    public void letterCrossbarRemainsText() {
        assertEquals(0, detect(true, false, true, false) & NoteArticulation.MARCATO);
    }

    @Test
    public void disconnectedDistantStaffDoesNotProveRule() {
        assertEquals(0, detect(true, false, false, true) & NoteArticulation.MARCATO);
    }
}
