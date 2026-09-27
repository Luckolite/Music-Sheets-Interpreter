// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original paired-beam drawings; no score-derived pixels. */
public final class NarrowPairedBeamTest {
    static final int W = 220, H = 220, G = 20;

    byte[] draw(boolean pair, boolean bridge) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int x = 80; x <= 112; x++) {
            int y = 110 - Math.round((x - 80) * .625f);
            for (int dy = 0; dy <= 10; dy++) gray[(y + dy) * W + x] = 0;
            if (pair) for (int dy = 13; dy <= 23; dy++) gray[(y + dy) * W + x] = 0;
            if (bridge) for (int dy = 11; dy <= 12; dy++) gray[(y + dy) * W + x] = 0;
        }
        return gray;
    }

    boolean matches(byte[] gray, int[] a, int[] b, float y) {
        return OmrScoreInterpreter.narrowParallelBeamInk(gray, W, H, a, b, 96, y, G);
    }

    @Test
    public void steepShortParallelBeamsOwnTheirIsland() {
        assertTrue(
                matches(draw(true, false), new int[] {80, 110, -1}, new int[] {112, 90, -1}, 118));
    }

    @Test
    public void singleBeamCannotRemoveSmallNote() {
        assertFalse(
                matches(draw(false, false), new int[] {80, 110, -1}, new int[] {112, 90, -1}, 105));
    }

    @Test
    public void solidBlobHasNoIndependentBeamCores() {
        assertFalse(
                matches(draw(true, true), new int[] {80, 110, -1}, new int[] {112, 90, -1}, 118));
    }

    @Test
    public void oppositeStemsCannotProveOneBeamGroup() {
        assertFalse(
                matches(draw(true, false), new int[] {80, 110, -1}, new int[] {112, 90, 1}, 118));
    }

    @Test
    public void nearbyNoteOutsideBeamInkIsPreserved() {
        assertFalse(
                matches(draw(true, false), new int[] {80, 110, -1}, new int[] {112, 90, -1}, 145));
    }

    @Test
    public void widePairIsOutsideThisRecovery() {
        assertFalse(
                matches(draw(true, false), new int[] {60, 110, -1}, new int[] {112, 90, -1}, 118));
    }

    @Test
    public void imageIsNotChanged() {
        byte[] g = draw(true, false), before = g.clone();
        matches(g, new int[] {80, 110, -1}, new int[] {112, 90, -1}, 118);
        assertArrayEquals(before, g);
    }
}
