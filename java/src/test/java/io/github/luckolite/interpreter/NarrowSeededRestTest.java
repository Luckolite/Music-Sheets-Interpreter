// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Procedural narrow bulb contours, not score pixels. */
public class NarrowSeededRestTest {
    private static final int W = 400, H = 220;

    private byte[] page(boolean lower, int shade) {
        byte[] g = new byte[W * H];
        Arrays.fill(g, (byte) 255);
        for (int line = 0; line < 5; line++)
            for (int x = 10; x < W - 10; x++) g[Math.round(80 + line * 14.5f) * W + x] = 0;
        for (int y = 97; y <= 135; y++) {
            int x = 180 - (y - 97) * 10 / 38;
            g[y * W + x] = (byte) 200;
            g[y * W + x + 1] = (byte) 200;
        }
        for (int y = 99; y <= 101; y++)
            for (int x = 174; x <= 180; x++) g[y * W + x] = (byte) shade;
        if (lower)
            for (int y = 113; y <= 115; y++)
                for (int x = 170; x <= 176; x++) g[y * W + x] = (byte) shade;
        return g;
    }

    private List<ScoreRestEvent> detect(byte[] g) {
        return SixteenthRestDetector.detect(
                g,
                W,
                H,
                List.of(new MeasureRegion(.02f, .98f, .2f, .8f)),
                List.of(new SixteenthRestDetector.Staff(80, 138, 14.5f, 0, 1)),
                List.of());
    }

    @Test
    public void narrowDarkBulbsWithFaintTailRecover() {
        var rests = detect(page(true, 80));
        assertEquals(rests.toString(), 1, rests.size());
        assertEquals(.25, rests.get(0).durationBeats(), 0);
    }

    @Test
    public void oneBulbCannotAuthorizeSixteenth() {
        assertTrue(detect(page(false, 80)).stream().noneMatch(r -> r.durationBeats() == .25));
    }

    @Test
    public void narrowPaleBulbsStillNeedDarkSeeds() {
        assertTrue(detect(page(true, 200)).isEmpty());
    }
}
