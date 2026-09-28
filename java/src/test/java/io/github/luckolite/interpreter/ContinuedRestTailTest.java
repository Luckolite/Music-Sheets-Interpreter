// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original connected and bounded diagonal strokes at a virtual crop boundary. */
public class ContinuedRestTailTest {
    private static final int W = 180, H = 160;
    private final byte[] gray = new byte[W * H];

    public ContinuedRestTailTest() {
        Arrays.fill(gray, (byte) 255);
    }

    private void tail(int last, int radius) {
        for (int y = 72; y <= last; y++) {
            int x = Math.round(90 - (y - 80) * .4f);
            for (int dx = -radius; dx <= radius; dx++) gray[y * W + x + dx] = 35;
        }
    }

    private boolean continued() throws Exception {
        var m =
                SixteenthRestDetector.class.getDeclaredMethod(
                        "continuedRestTail",
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        float.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, gray, W, H, 86, 98, 80, 16f);
    }

    @Test
    public void substantialConnectedTailCrossesCropBoundary() throws Exception {
        tail(92, 1);
        assertTrue(continued());
    }

    @Test
    public void genuineTailEndsAtBoundary() throws Exception {
        tail(80, 1);
        assertFalse(continued());
    }

    @Test
    public void antialiasFringeIsInsufficient() throws Exception {
        tail(82, 1);
        assertFalse(continued());
    }

    @Test
    public void isolatedSinglePixelDiagonalIsInsufficient() throws Exception {
        tail(92, 0);
        assertFalse(continued());
    }

    @Test
    public void separatedStrokeCannotExtendTail() throws Exception {
        tail(92, 1);
        for (int y = 81; y <= 84; y++) Arrays.fill(gray, y * W, (y + 1) * W, (byte) 255);
        assertFalse(continued());
    }

    @Test
    public void horizontalRuleCannotExtendTail() throws Exception {
        tail(80, 1);
        for (int x = 60; x < 125; x++) gray[87 * W + x] = 35;
        assertFalse(continued());
    }

    @Test
    public void pixelsAreUnchanged() throws Exception {
        tail(92, 1);
        var before = gray.clone();
        continued();
        assertArrayEquals(before, gray);
    }
}
