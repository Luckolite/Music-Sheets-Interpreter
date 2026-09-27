// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric naturals and counterexamples, with no source score pixels. */
public final class BeamOccludedNaturalTest {
    private static final int W = 160, H = 140;
    private final byte[] gray = new byte[W * H];

    private void rect(int l, int t, int r, int b) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) gray[y * W + x] = 0;
    }

    private void page(boolean beam, boolean bridge, boolean descender) {
        Arrays.fill(gray, (byte) 255);
        rect(65, 42, 66, 82);
        rect(75, 56, 76, descender ? 96 : 82);
        if (bridge) rect(65, 79, 76, 82);
        if (beam)
            for (int x = 20; x <= 125; x++) {
                int y = Math.round(58 - (x - 65) * .2f);
                rect(x, y, x, y + 4);
            }
    }

    private boolean match() {
        return BeamOccludedNatural.matches(gray, W, H, 65, 76, 70, 20);
    }

    @Test
    public void beamMayOccludeOnlyTheUpperCrossbar() {
        page(true, true, true);
        assertTrue(match());
    }

    @Test
    public void lowerBridgeIsRequired() {
        page(true, false, true);
        assertFalse(match());
    }

    @Test
    public void flatWithoutRightDescenderIsNotNatural() {
        page(true, true, false);
        assertFalse(match());
    }

    @Test
    public void anUnverifiedStrokeCannotReplaceTheUpperCrossbar() {
        page(false, true, true);
        assertFalse(match());
    }

    @Test
    public void bothDescendingSpinesAreNotNatural() {
        page(true, true, true);
        rect(65, 82, 66, 96);
        assertFalse(match());
    }

    @Test
    public void closedCounterCannotBeNatural() {
        page(true, true, true);
        rect(66, 69, 75, 78);
        assertFalse(match());
    }

    @Test
    public void longFollowingStemCannotSupplyTheDescender() {
        page(true, true, true);
        rect(75, 96, 76, 125);
        assertFalse(match());
    }

    @Test
    public void sourcePixelsAreNotEdited() {
        page(true, true, true);
        var copy = gray.clone();
        match();
        assertArrayEquals(copy, gray);
    }
}
