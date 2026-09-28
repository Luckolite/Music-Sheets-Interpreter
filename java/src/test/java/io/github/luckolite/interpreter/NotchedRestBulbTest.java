// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original paired rest bulbs with a bounded raster-width dip. */
public class NotchedRestBulbTest {
    private static final int W = 400, H = 220;

    private byte[] page(boolean second) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int line = 0; line < 5; line++)
            for (int x = 10; x < W - 10; x++) gray[Math.round(80 + line * 14.5f) * W + x] = 0;
        for (int y = 97; y <= 135; y++) {
            int x = 180 - (y - 97) * 10 / 38;
            gray[y * W + x] = 0;
            gray[y * W + x + 1] = 0;
        }
        for (int y = 99; y <= 101; y++) for (int x = 173; x <= 180; x++) gray[y * W + x] = 0;
        for (int y = 99; y <= 101; y++) gray[y * W + 181] = (byte) 255;
        if (second)
            for (int y = 113; y <= 115; y++) for (int x = 169; x <= 176; x++) gray[y * W + x] = 0;
        return gray;
    }

    private List<ScoreRestEvent> detect(byte[] gray) {
        return SixteenthRestDetector.detect(
                gray,
                W,
                H,
                List.of(new MeasureRegion(.02f, .98f, .2f, .8f)),
                List.of(new SixteenthRestDetector.Staff(80, 138, 14.5f, 0, 1)),
                List.of());
    }

    @Test
    public void onePixelOneRowDipDoesNotSplitBulb() {
        var gray = page(true);
        gray[100 * W + 173] = (byte) 255;
        var rests = detect(gray);
        assertEquals(1, rests.size());
        assertEquals(.25, rests.get(0).durationBeats(), 0);
    }

    @Test
    public void twoPixelWidthDeficitIsNotBridged() {
        var gray = page(true);
        gray[100 * W + 173] = (byte) 255;
        gray[100 * W + 174] = (byte) 255;
        var rests = detect(gray);
        assertTrue(rests.toString(), rests.stream().noneMatch(r -> r.durationBeats() == .25));
    }

    @Test
    public void twoRowDeficitIsNotBridged() {
        var gray = page(true);
        gray[100 * W + 173] = (byte) 255;
        gray[101 * W + 173] = (byte) 255;
        var rests = detect(gray);
        assertTrue(rests.toString(), rests.stream().noneMatch(r -> r.durationBeats() == .25));
    }

    @Test
    public void blankGapIsNotBridged() {
        var gray = page(true);
        for (int x = 173; x <= 181; x++) gray[100 * W + x] = (byte) 255;
        var rests = detect(gray);
        assertTrue(rests.toString(), rests.stream().noneMatch(r -> r.durationBeats() == .25));
    }

    @Test
    public void singleBulbDoesNotBecomeSixteenth() {
        var gray = page(false);
        gray[100 * W + 173] = (byte) 255;
        assertTrue(detect(gray).stream().noneMatch(r -> r.durationBeats() == .25));
    }

    @Test
    public void intactBulbsRemainSixteenth() {
        assertEquals(.25, detect(page(true)).get(0).durationBeats(), 0);
    }

    @Test
    public void sourcePixelsRemainUnchanged() {
        var gray = page(true);
        gray[100 * W + 173] = (byte) 255;
        var before = gray.clone();
        detect(gray);
        assertArrayEquals(before, gray);
    }
}
