// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generated geometry; no score images or library data. */
public final class StaggeredCueStaffSystemTest {
    private static final int WIDTH = 840, HEIGHT = 520;

    private record Page(byte[] labels, byte[] gray) {}

    @Test
    public void boundedBracketJoinsReducedStaffEnteringAtSecondBar() {
        Page p = page(true, false, false);
        var measures = OmrMeasurePostProcessor.process(p.labels, p.gray, WIDTH, HEIGHT);
        assertEquals(4, measures.size());
        for (var measure : measures) {
            assertTrue(measure.top() < 100f / HEIGHT);
            assertTrue(measure.bottom() > 368f / HEIGHT);
        }
        assertTrue(measures.get(0).left() < 260f / WIDTH);
        assertTrue(measures.get(1).left() > 260f / WIDTH);
    }

    @Test
    public void nestedRowsWithoutBracketStaySeparate() {
        Page p = page(false, false, false);
        var measures = OmrMeasurePostProcessor.process(p.labels, p.gray, WIDTH, HEIGHT);
        assertEquals(7, measures.size());
        assertTrue(measures.get(0).bottom() < 320f / HEIGHT);
    }

    @Test
    public void creaseExtendingAboveStaffCannotJoinRows() {
        Page p = page(true, true, false);
        assertEquals(7, OmrMeasurePostProcessor.process(p.labels, p.gray, WIDTH, HEIGHT).size());
    }

    @Test
    public void differentPrintedBarPositionsCannotJoinRows() {
        Page p = page(true, false, true);
        assertEquals(7, OmrMeasurePostProcessor.process(p.labels, p.gray, WIDTH, HEIGHT).size());
    }

    private static Page page(boolean bracket, boolean crease, boolean shifted) {
        byte[] labels = new byte[WIDTH * HEIGHT], gray = new byte[labels.length];
        Arrays.fill(gray, (byte) 255);
        staff(labels, gray, 100, 9, 260, 780, new int[] {260, shifted ? 475 : 440, 610, 780});
        staff(labels, gray, 320, 12, 60, 780, new int[] {60, 260, 440, 610, 780});
        if (bracket) for (int y = crease ? 70 : 100; y <= 368; y++) gray[y * WIDTH + 60] = 0;
        return new Page(labels, gray);
    }

    private static void staff(
            byte[] labels, byte[] gray, int top, int gap, int left, int right, int[] bars) {
        for (int line = 0; line < 5; line++)
            for (int x = left; x <= right; x++) {
                labels[(top + line * gap) * WIDTH + x] = OmrMeasurePostProcessor.STAFF;
                gray[(top + line * gap) * WIDTH + x] = 0;
            }
        for (int x : bars)
            for (int y = top; y <= top + 4 * gap; y++) {
                labels[y * WIDTH + x] = OmrMeasurePostProcessor.STEM_OR_REST;
                gray[y * WIDTH + x] = 0;
            }
    }
}
