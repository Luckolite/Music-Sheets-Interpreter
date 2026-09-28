// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural controls; no private notation pixels. */
public class MordentContourTest {
    private static final int W = 40, H = 18;

    private byte[] wave(double cycles, boolean inverted) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int x = 0; x < W; x++) {
            double cy =
                    8.5 + (inverted ? 1 : -1) * 3.5 * Math.sin(cycles * 2 * Math.PI * x / (W - 1));
            for (int y = 0; y < H; y++) if (Math.abs(y - cy) <= 2) gray[y * W + x] = 60;
        }
        return gray;
    }

    private boolean match(byte[] gray) {
        return MordentContour.matches(gray, W, new PortableNoteOrnaments.Bounds(0, 0, W, H));
    }

    @Test
    public void twoCyclesHaveAlternatingLobes() {
        assertTrue(match(wave(2, false)));
    }

    @Test
    public void oneCycleIsNotMordent() {
        assertFalse(match(wave(1, false)));
    }

    @Test
    public void threeCyclesAreNotCompactMordent() {
        assertFalse(match(wave(3, false)));
    }

    @Test
    public void oppositePhaseIsNotThisFallback() {
        assertFalse(match(wave(2, true)));
    }

    @Test
    public void rectangleIsNotOscillation() {
        byte[] g = new byte[W * H];
        Arrays.fill(g, (byte) 60);
        assertFalse(match(g));
    }

    @Test
    public void separatedHorizontalStrokesAreNotOpenContour() {
        byte[] g = new byte[W * H];
        Arrays.fill(g, (byte) 255);
        for (int x = 0; x < W; x++) {
            g[4 * W + x] = 0;
            g[13 * W + x] = 0;
        }
        assertFalse(match(g));
    }

    @Test
    public void missingColumnAbstains() {
        byte[] g = wave(2, false);
        for (int y = 0; y < H; y++) g[y * W + 20] = (byte) 255;
        assertFalse(match(g));
    }

    @Test
    public void inputUnchanged() {
        byte[] g = wave(2, false), before = g.clone();
        match(g);
        assertArrayEquals(before, g);
    }

    @Test
    public void centralSlashIsNotUnslashedMordent() {
        byte[] g = wave(2, false);
        for (int y = 0; y < H; y++) g[y * W + 20] = 0;
        assertFalse(match(g));
    }
}
