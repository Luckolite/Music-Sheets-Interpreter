// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original returning contours with procedural upright/italic letter stems, without score imagery. */
public class TieLetterBranchTest {
    private static final int W = 240, H = 180, L = 60, R = 160;

    private static byte[] image(int side, boolean branches, boolean slanted, int ink) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int x = L; x <= R; x++) {
            double t = (x - L) / (double) (R - L);
            int y = (int) Math.round(90 + side * 20 * (.5 + .6 * 4 * t * (1 - t)));
            for (int dy = -1; dy <= 1; dy++) gray[(y + dy) * W + x] = (byte) ink;
            if (branches && x >= 92 && x <= 98 || branches && x >= 122 && x <= 128)
                for (int d = 1; d <= 22; d++) {
                    int xx = x + (slanted ? -d / 7 : 0);
                    gray[(y + side * d) * W + xx] = (byte) ink;
                }
        }
        return gray;
    }

    private static boolean tie(byte[] gray) throws Exception {
        Method method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "hasContinuousTieArc",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        float.class,
                        float.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, new byte[gray.length], gray, W, H, L, R, 90f, 20f);
    }

    @Test
    public void thinReturningContoursRemainTiesAboveAndBelowHeads() throws Exception {
        assertTrue(tie(image(1, false, false, 0)));
        assertTrue(tie(image(-1, false, false, 0)));
    }

    @Test
    public void archWithDescendingLetterStemsCannotBecomeATie() throws Exception {
        assertFalse(tie(image(1, true, false, 0)));
        assertFalse(tie(image(-1, true, false, 0)));
    }

    @Test
    public void italicLetterStemsCannotEscapeTheOutwardBranchCheck() throws Exception {
        assertFalse(tie(image(1, true, true, 0)));
        assertFalse(tie(image(-1, true, true, 0)));
    }

    @Test
    public void recognitionLeavesTheRasterIntact() throws Exception {
        var gray = image(1, true, true, 100);
        var before = gray.clone();
        tie(gray);
        assertArrayEquals(before, gray);
    }
}
