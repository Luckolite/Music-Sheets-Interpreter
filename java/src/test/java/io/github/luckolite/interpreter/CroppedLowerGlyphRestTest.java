// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original hooked text-like stroke cropped by a distant band in a lowered rest scan. */
public class CroppedLowerGlyphRestTest {
    private static final int W = 800, H = 280;
    private final byte[] gray = new byte[W * H];

    private void setup(int end, int mask, boolean bulb) {
        Arrays.fill(gray, (byte) 255);
        for (int y = 80; y <= 144; y += 16) for (int x = 20; x < 780; x++) gray[y * W + x] = 0;
        if (bulb)
            for (int y = 159; y <= 167; y++)
                for (int x = 171; x <= 183; x++)
                    if (Math.pow((x - 177) / 6.0, 2) + Math.pow((y - 163) / 4.0, 2) <= 1)
                        gray[y * W + x] = 35;
        for (int y = 160; y <= end; y++) {
            int x = Math.round(185 - (y - 160) * .5f);
            for (int dx = 0; dx < 3; dx++) gray[y * W + x + dx] = 35;
        }
        if (mask > 0)
            for (int y = mask; y <= mask + 7; y++)
                for (int x = 400; x < 780; x++) gray[y * W + x] = 0;
    }

    private List<ScoreRestEvent> rests() {
        var note = new ScoreNoteEvent(0, .5f, 0, 0, 1, 120 / (float) H, false, 0, 0, 2, 2);
        return SixteenthRestDetector.detect(
                gray,
                W,
                H,
                List.of(new MeasureRegion(0, 1, .1f, .95f)),
                List.of(new SixteenthRestDetector.Staff(80, 144, 16, 0, 1)),
                List.of(note));
    }

    @Test
    public void croppedLongGlyphCannotBecomeEighthRest() {
        setup(204, 190, true);
        assertTrue(rests().isEmpty());
    }

    @Test
    public void neighboringMaskPositionsCannotHideContinuation() {
        for (int mask : new int[] {188, 192}) {
            setup(204, mask, true);
            assertTrue("mask=" + mask, rests().isEmpty());
        }
    }

    @Test
    public void boundedGenuineLowerRestRemains() {
        setup(187, 190, true);
        var r = rests();
        assertEquals(r.toString(), 1, r.size());
        assertEquals(.5, r.get(0).durationBeats(), 0);
    }

    @Test
    public void noBulbCannotCreateRest() {
        setup(204, 190, false);
        assertTrue(rests().isEmpty());
    }

    @Test
    public void sourcePixelsArePreserved() {
        setup(204, 190, true);
        var before = gray.clone();
        rests();
        assertArrayEquals(before, gray);
    }
}
