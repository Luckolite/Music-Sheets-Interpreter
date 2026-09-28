// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic ink, not derived from a score image. */
public class SeededFaintRestTest {
    private static final int W = 400, H = 220;

    private byte[] page(boolean darkBulbs, int background, int shift) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) background);
        for (int line = 0; line < 5; line++)
            for (int x = 10; x < W - 10; x++) gray[Math.round(80 + line * 14.5f) * W + x] = 0;
        for (int y = 97; y <= 135; y++) {
            int x = 180 - (y - 97) * 10 / 38;
            gray[(y + shift) * W + x] = (byte) 200;
            gray[(y + shift) * W + x + 1] = (byte) 200;
        }
        for (int y = 99; y <= 101; y++)
            for (int x = 173; x <= 180; x++)
                gray[(y + shift) * W + x] = (byte) (darkBulbs ? 80 : 200);
        for (int y = 113; y <= 115; y++)
            for (int x = 169; x <= 176; x++)
                gray[(y + shift) * W + x] = (byte) (darkBulbs ? 80 : 200);
        return gray;
    }

    private List<ScoreRestEvent> detect(byte[] gray, List<ScoreNoteEvent> notes) {
        return SixteenthRestDetector.detect(
                gray,
                W,
                H,
                List.of(new MeasureRegion(.02f, .98f, .2f, .8f)),
                List.of(new SixteenthRestDetector.Staff(80, 138, 14.5f, 0, 1)),
                notes);
    }

    @Test
    public void darkBulbsSupportFadedTail() {
        var rests = detect(page(true, 255, 0), List.of());
        assertEquals(rests.toString(), 1, rests.size());
        assertEquals(.25, rests.get(0).durationBeats(), 0);
    }

    @Test
    public void uniformlyPaleGlyphDoesNotGainSeedFromStaff() {
        assertTrue(detect(page(false, 255, 0), List.of()).isEmpty());
    }

    @Test
    public void lowContrastTailRemainsUnproved() {
        assertTrue(detect(page(true, 225, 0), List.of()).isEmpty());
    }

    @Test
    public void noteStillOwnsColumn() {
        assertTrue(
                detect(
                                page(true, 255, 0),
                                List.of(
                                        new ScoreNoteEvent(
                                                0, .43f, 3, 0, 1, 114f / H, false, 0, 1)))
                        .isEmpty());
    }

    @Test
    public void belowStaffFaintGlyphIsNotOrdinaryRest() {
        assertTrue(detect(page(true, 255, 44), List.of()).isEmpty());
    }

    @Test
    public void originalRasterRemainsUnchanged() {
        byte[] gray = page(true, 255, 0), before = gray.clone();
        detect(gray, List.of());
        assertArrayEquals(before, gray);
    }
}
