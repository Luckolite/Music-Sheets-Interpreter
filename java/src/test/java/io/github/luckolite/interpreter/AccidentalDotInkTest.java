// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccidentalDotInkTest {
    private static final int W = 140, H = 140;

    private byte[] page(boolean left, boolean right, boolean bridge) {
        byte[] g = new byte[W * H];
        Arrays.fill(g, (byte) 255);
        if (left) for (int y = 35; y <= 83; y++) g[y * W + 60] = (byte) 180;
        if (right) for (int y = 50; y <= 98; y++) g[y * W + 70] = (byte) 180;
        if (bridge) for (int y = 59; y <= 63; y++) for (int x = 60; x <= 70; x++) g[y * W + x] = 80;
        return g;
    }

    @Test
    public void faintPairedShaftsOwnTheirDarkCrossbar() {
        assertTrue(AccidentalDotInk.matches(page(true, true, true), W, H, 61, 59, 69, 63, 16));
    }

    @Test
    public void isolatedRealDotIsPreserved() {
        assertFalse(AccidentalDotInk.matches(page(false, false, true), W, H, 61, 59, 69, 63, 16));
    }

    @Test
    public void oneAdjacentStemIsInsufficient() {
        assertFalse(AccidentalDotInk.matches(page(true, false, true), W, H, 61, 59, 69, 63, 16));
    }

    @Test
    public void separateInkBetweenTwoBarsIsNotConnected() {
        byte[] g = page(true, true, false);
        for (int y = 59; y <= 63; y++) for (int x = 64; x <= 66; x++) g[y * W + x] = 80;
        assertFalse(AccidentalDotInk.matches(g, W, H, 64, 59, 66, 63, 16));
    }

    @Test
    public void interruptedShaftsDoNotProveAnAccidental() {
        byte[] g = page(true, true, true);
        for (int y = 40; y <= 55; y++) {
            g[y * W + 60] = (byte) 255;
            g[y * W + 70] = (byte) 255;
        }
        assertFalse(AccidentalDotInk.matches(g, W, H, 61, 59, 69, 63, 16));
    }
}
