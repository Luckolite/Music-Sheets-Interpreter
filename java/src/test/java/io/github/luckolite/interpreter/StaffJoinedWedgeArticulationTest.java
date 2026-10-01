// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original solid triangle and independent staff-rule geometry. */
public class StaffJoinedWedgeArticulationTest {
    private int marks(boolean below, boolean rule, boolean accepted, boolean open) {
        return marks(below, rule, accepted, open, false);
    }

    private int marks(
            boolean below, boolean rule, boolean accepted, boolean open, boolean continuing) {
        int w = 2048, h = 1600;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        if (rule)
            for (int row :
                    below ? new int[] {32, 49, 66, 83, 100} : new int[] {100, 117, 134, 151, 168})
                for (int x = 230; x <= 370; x++)
                    for (int dy = -1; dy <= 1; dy++) {
                        gray[(row + dy) * w + x] = 0;
                        labels[(row + dy) * w + x] = OmrMeasurePostProcessor.STAFF;
                    }
        for (int row = 0; row < 19; row++) {
            int span = 1 + (int) Math.round(9 * row / 18d);
            int left = 300 - span / 2;
            for (int x = left; x < left + span; x++) {
                if (open && x > left && x < left + span - 1) continue;
                int y = below ? 100 + row : 100 - row;
                gray[y * w + x] = 0;
                labels[y * w + x] = OmrMeasurePostProcessor.SYMBOL;
            }
        }
        if (continuing)
            for (int row = 2; row <= 9; row++)
                for (int x = 299; x <= 301; x++) gray[(below ? 100 - row : 100 + row) * w + x] = 0;
        List<NoteArticulationDetector.Anchor> anchors = new ArrayList<>();
        anchors.add(new NoteArticulationDetector.Anchor(300, below ? 75 : 125, 17, 0));
        if (accepted)
            anchors.add(new NoteArticulationDetector.Anchor(300, below ? 110 : 90, 17, 0));
        return NoteArticulationDetector.detect(labels, gray, w, h, anchors)[0]
                & NoteArticulation.STACCATISSIMO;
    }

    @Test
    public void lowerFilledTipOnRuleIsWedge() {
        assertEquals(NoteArticulation.STACCATISSIMO, marks(true, true, false, false));
    }

    @Test
    public void upperFilledTipOnRuleIsWedge() {
        assertEquals(NoteArticulation.STACCATISSIMO, marks(false, true, false, false));
    }

    @Test
    public void detachedFilledWedgeStillWorks() {
        assertEquals(NoteArticulation.STACCATISSIMO, marks(true, false, false, false));
    }

    @Test
    public void acceptedHeadStillVetoesJoinedWedge() {
        assertEquals(0, marks(true, true, true, false));
    }

    @Test
    public void lowerFlagContinuingThroughRuleIsNotWedge() {
        assertEquals(0, marks(true, true, false, false, true));
    }

    @Test
    public void upperFlagContinuingThroughRuleIsNotWedge() {
        assertEquals(0, marks(false, true, false, false, true));
    }

    @Test
    public void upperAcceptedHeadStillVetoesJoinedWedge() {
        assertEquals(0, marks(false, true, true, false));
    }

    @Test
    public void openCaretIsNotFilledWedge() {
        assertEquals(0, marks(true, true, false, true));
    }
}
