// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural rest ink on photographed, uneven paper. */
public class ShadedRestPaperTest {
    private static final int W = 768, H = 256;

    private byte[] page(boolean glyph, boolean tail, boolean crossed, boolean shaded) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        if (shaded)
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++) {
                    int grain = (x * 37 + y * 53 + x * y * 7) % 31;
                    gray[y * W + x] = (byte) (145 + grain + x * 20 / W);
                }
        for (int line = 0; line < 5; line++)
            for (int x = 20; x < W - 20; x++) gray[(80 + line * 16) * W + x] = 0;
        if (glyph)
            for (int y = 97; y <= 107; y++)
                for (int x = 235; x <= 247; x++)
                    if ((x - 241) * (x - 241) / 36.0 + (y - 102) * (y - 102) / 25.0 <= 1)
                        gray[y * W + x] = 0;
        if (tail)
            for (int y = 98; y <= 129; y++) {
                int x = 250 - (y - 98) * 12 / 31;
                gray[y * W + x] = 0;
                gray[y * W + x + 1] = 0;
            }
        if (crossed) {
            for (int k = -6; k <= 6; k++)
                for (int t = 0; t < 2; t++) {
                    gray[(102 + k) * W + 241 + k + t] = 0;
                    gray[(102 - k) * W + 241 + k + t] = 0;
                }
            for (int y = 103; y <= 166; y++) for (int x = 235; x <= 237; x++) gray[y * W + x] = 0;
        }
        return gray;
    }

    private List<ScoreRestEvent> read(byte[] gray) {
        return SixteenthRestDetector.detect(
                gray,
                W,
                H,
                List.of(new MeasureRegion(0, 1, .2f, .9f)),
                List.of(new SixteenthRestDetector.Staff(80, 144, 16, 0, 1)),
                List.of());
    }

    @Test
    public void shadedPaperKeepsTheCompleteEighthRest() {
        var rests = read(page(true, true, false, true));
        assertEquals(rests.toString(), 1, rests.size());
        assertEquals(.5, rests.get(0).durationBeats(), 0);
    }

    @Test
    public void cleanPaperKeepsTheCompleteEighthRest() {
        var rests = read(page(true, true, false, false));
        assertEquals(rests.toString(), 1, rests.size());
        assertEquals(.5, rests.get(0).durationBeats(), 0);
    }

    @Test
    public void shadedPaperWithoutPrintedInkCannotCreateSilence() {
        assertTrue(read(page(false, false, false, true)).isEmpty());
    }

    @Test
    public void aBulbWithoutItsTailIsStillRejectedOnShadedPaper() {
        assertTrue(read(page(true, false, false, true)).isEmpty());
    }

    @Test
    public void aCrossedHeadAndLongStemDoNotCreateAnEighthRest() {
        assertTrue(read(page(false, false, true, true)).isEmpty());
    }

    @Test
    public void decodingLeavesTheShadedPageUnchanged() {
        byte[] gray = page(true, true, false, true), before = gray.clone();
        read(gray);
        assertArrayEquals(before, gray);
    }
}
