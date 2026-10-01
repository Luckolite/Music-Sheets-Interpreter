// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original raster with a reduced two-note staff inset above a full-size bar. */
public final class ShortInsetCueStaffTest {
    private static final int W = 1000, H = 500;

    private record Page(byte[] labels, byte[] gray) {}

    private static Page page(boolean heads, boolean shifted, boolean complete, boolean distant) {
        return page(heads, shifted, complete, distant, false);
    }

    private static Page page(
            boolean heads, boolean shifted, boolean complete, boolean distant, boolean wide) {
        return page(heads, shifted, complete, distant, wide, 0);
    }

    private static Page page(
            boolean heads,
            boolean shifted,
            boolean complete,
            boolean distant,
            boolean wide,
            int inset) {
        byte[] labels = new byte[W * H], gray = new byte[labels.length];
        Arrays.fill(gray, (byte) 255);
        int left = (wide ? 620 : 740) + inset;
        staff(labels, gray, 100, 9, left, 940, new int[] {left, 940}, complete ? 5 : 4);
        int top = distant ? 350 : 225;
        staff(
                labels,
                gray,
                top,
                12,
                60,
                940,
                wide
                        ? new int[] {60, 250, 440, 620, 940}
                        : new int[] {60, 250, 440, 620, shifted ? 710 : 740, 940},
                5);
        if (heads)
            for (int cx : new int[] {780, 870}) {
                int cy = cx == 780 ? 132 : 118;
                for (int y = cy - 4; y <= cy + 4; y++)
                    for (int x = cx - 7; x <= cx + 7; x++)
                        if ((x - cx) * (x - cx) / 49f + (y - cy) * (y - cy) / 16f <= 1) {
                            labels[y * W + x] = OmrMeasurePostProcessor.NOTEHEAD;
                            gray[y * W + x] = 0;
                        }
                for (int y = cy - 30; y <= cy; y++) {
                    labels[y * W + cx + 6] = OmrMeasurePostProcessor.STEM_OR_REST;
                    gray[y * W + cx + 6] = 0;
                }
            }
        return new Page(labels, gray);
    }

    private static void staff(
            byte[] labels,
            byte[] gray,
            int top,
            int gap,
            int left,
            int right,
            int[] bars,
            int lines) {
        for (int i = 0; i < lines; i++)
            for (int x = left; x <= right; x++) {
                labels[(top + i * gap) * W + x] = OmrMeasurePostProcessor.STAFF;
                gray[(top + i * gap) * W + x] = 0;
            }
        for (int x : bars)
            for (int y = top; y <= top + 4 * gap; y++) {
                labels[y * W + x] = OmrMeasurePostProcessor.STEM_OR_REST;
                gray[y * W + x] = 0;
            }
    }

    @Test
    public void alignedReducedInsetJoinsItsFullStaffWithoutBracket() {
        Page p = page(true, false, true, false);
        var m = OmrMeasurePostProcessor.process(p.labels, p.gray, W, H);
        assertEquals(5, m.size());
        assertTrue(m.get(4).top() < 100f / H);
        assertTrue(m.get(4).bottom() > 273f / H);
    }

    @Test
    public void widerInsetStillSharesItsPrintedFullSizeBar() {
        Page p = page(true, false, true, false, true);
        var m = OmrMeasurePostProcessor.process(p.labels, p.gray, W, H);
        assertEquals(4, m.size());
        assertTrue(m.get(3).top() < 100f / H);
        assertTrue(m.get(3).bottom() > 273f / H);
    }

    @Test
    public void cueOpeningMayInsetOneStaffSpaceFromSharedBar() {
        Page p = page(true, false, true, false, true, 12);
        var m = OmrMeasurePostProcessor.process(p.labels, p.gray, W, H);
        assertEquals(4, m.size());
        assertTrue(m.get(3).top() < 100f / H);
    }

    @Test
    public void distantCueOpeningDoesNotShareTheFullSizeBar() {
        Page p = page(true, false, true, false, true, 36);
        assertEquals(5, OmrMeasurePostProcessor.process(p.labels, p.gray, W, H).size());
    }

    @Test
    public void roundedSemanticRowsStillRequireFiveNearbyPrintedRules() {
        Page p = page(true, false, true, false, true);
        for (int line = 0; line < 5; line++)
            for (int x = 620; x <= 940; x++) {
                p.gray[(100 + line * 9) * W + x] = (byte) 255;
                p.gray[(102 + line * 9) * W + x] = 0;
            }
        var m = OmrMeasurePostProcessor.process(p.labels, p.gray, W, H);
        assertEquals(4, m.size());
        assertTrue(m.get(3).top() < 100f / H);
    }

    @Test
    public void rulesWithoutNotesCannotInventCuePart() {
        Page p = page(false, false, true, false);
        var m = OmrMeasurePostProcessor.process(p.labels, p.gray, W, H);
        assertEquals(5, m.size());
        assertTrue(m.get(4).top() > 180f / H);
    }

    @Test
    public void misalignedInsetCannotJoinAnotherBar() {
        Page p = page(true, true, true, false);
        var m = OmrMeasurePostProcessor.process(p.labels, p.gray, W, H);
        assertEquals(5, m.size());
        assertTrue(m.get(4).top() > 180f / H);
    }

    @Test
    public void fourRulesDoNotProveStaff() {
        Page p = page(true, false, false, false);
        var m = OmrMeasurePostProcessor.process(p.labels, p.gray, W, H);
        assertEquals(5, m.size());
        assertTrue(m.get(4).top() > 180f / H);
    }

    @Test
    public void distantRowCannotOwnInset() {
        Page p = page(true, false, true, true);
        var m = OmrMeasurePostProcessor.process(p.labels, p.gray, W, H);
        assertEquals(5, m.size());
        assertTrue(m.get(4).top() > 300f / H);
    }
}
