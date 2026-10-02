// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original compact ledger chains must reach five independently printed staff rails. */
public class CompactLedgerStaffArticulationTest {
    private static final int W = 1280, H = 900;

    private int mark(
            boolean above,
            int rails,
            int spacing,
            int railShift,
            boolean thick,
            boolean compact,
            boolean edge) {
        byte[] gray = new byte[W * H], labels = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        int x = edge ? 10 : 640, head = above ? 230 : 80, direction = above ? -1 : 1, gap = 14;
        int y = head + direction * 21;
        for (int row = 0; row < 2; row++)
            for (int xx = Math.max(0, x - 10); xx <= Math.min(W - 1, x + 10); xx++)
                gray[(y + direction * row * gap) * W + xx] = 30;
        for (int row = 0; row < rails; row++) {
            int cy = y + direction * (28 + row * spacing) + railShift;
            for (int xx = Math.max(0, x - (compact ? 10 : 80));
                    xx <= Math.min(W - 1, x + (compact ? 10 : 80));
                    xx++)
                for (int dy = thick ? -4 : 0; dy <= (thick ? 4 : 0); dy++)
                    gray[(cy + dy) * W + xx] = (byte) 200;
        }
        byte[] g = gray.clone(), l = labels.clone();
        int result =
                NoteArticulationDetector.detect(
                        labels,
                        gray,
                        W,
                        H,
                        List.of(new NoteArticulationDetector.Anchor(x, head, gap, 0)))[0];
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
        return result;
    }

    @Test
    public void lowerCompactLedgersReachCompleteStaff() {
        assertEquals(0, mark(false, 5, 14, 0, false, false, false));
    }

    @Test
    public void upperCompactLedgersReachCompleteStaff() {
        assertEquals(0, mark(true, 5, 14, 0, false, false, false));
    }

    @Test
    public void fourRailsCannotProveACompleteStaff() {
        assertEquals(NoteArticulation.TENUTO, mark(false, 4, 14, 0, false, false, false));
    }

    @Test
    public void threeRailsCannotProveACompleteStaff() {
        assertEquals(NoteArticulation.TENUTO, mark(false, 3, 14, 0, false, false, false));
    }

    @Test
    public void isolatedCompactDashesKeepTenuto() {
        assertEquals(NoteArticulation.TENUTO, mark(false, 0, 14, 0, false, false, false));
    }

    @Test
    public void compactDashChainCannotSubstituteForStaff() {
        assertEquals(NoteArticulation.TENUTO, mark(false, 5, 14, 0, false, true, false));
    }

    @Test
    public void wrongRailSpacingKeepsTenuto() {
        assertEquals(NoteArticulation.TENUTO, mark(false, 5, 18, 0, false, false, false));
    }

    @Test
    public void wrongRailPhaseKeepsTenuto() {
        assertEquals(NoteArticulation.TENUTO, mark(false, 5, 14, 5, false, false, false));
    }

    @Test
    public void broadGrayBandsKeepTenuto() {
        assertEquals(NoteArticulation.TENUTO, mark(false, 5, 14, 0, true, false, false));
    }

    @Test
    public void clippedFlanksCannotProveStaff() {
        assertEquals(NoteArticulation.TENUTO, mark(false, 5, 14, 0, false, false, true));
    }
}
