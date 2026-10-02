// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generated varying-width strokes; their returning center distinguishes ties. */
public class SlopedTieStrokeCenterTest {
    boolean detect(int side, boolean interrupted, boolean returning) throws Exception {
        int w = 300, h = 180, left = 60, right = 120;
        float gap = 16;
        byte[] labels = new byte[w * h], gray = new byte[w * h];
        Arrays.fill(gray, (byte) 245);
        for (int x = left; x <= right; x++) {
            if (interrupted && (x - left) % 8 == 4) continue;
            float t = (x - left) / (float) (right - left);
            int center =
                    Math.round(
                            100
                                    + side
                                            * (returning
                                                    ? 5 + 12 * 4 * t * (1 - t)
                                                    : 11 + .08f * (x - left)));
            int radius = 1 + Math.round(3 * 4 * t * (1 - t));
            for (int y = center - radius; y <= center + radius; y++) {
                gray[y * w + x] = 25;
                labels[y * w + x] = 5;
            }
        }
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "hasPrintedTieArc",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        float.class,
                        float.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, labels, gray, w, h, left, right, 100f, gap);
    }

    @Test
    public void upperSlopingRuleHasNoReturningCenter() throws Exception {
        assertFalse(detect(-1, false, false));
    }

    @Test
    public void lowerSlopingRuleHasNoReturningCenter() throws Exception {
        assertFalse(detect(1, false, false));
    }

    @Test
    public void fadedUpperRuleCannotBorrowCurvatureFromItsEdges() throws Exception {
        assertFalse(detect(-1, true, false));
    }

    @Test
    public void fadedLowerRuleCannotBorrowCurvatureFromItsEdges() throws Exception {
        assertFalse(detect(1, true, false));
    }

    @Test
    public void varyingWidthUpperTieRetainsItsReturningCenter() throws Exception {
        assertTrue(detect(-1, false, true));
    }

    @Test
    public void varyingWidthLowerTieRetainsItsReturningCenter() throws Exception {
        assertTrue(detect(1, false, true));
    }

    @Test
    public void interruptedUpperTieRetainsItsReturningCenter() throws Exception {
        assertTrue(detect(-1, true, true));
    }

    @Test
    public void interruptedLowerTieRetainsItsReturningCenter() throws Exception {
        assertTrue(detect(1, true, true));
    }
}
