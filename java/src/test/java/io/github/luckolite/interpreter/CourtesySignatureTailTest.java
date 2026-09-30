// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original key-only tail with separate printed double-bar ink. */
public class CourtesySignatureTailTest {
    private final int w = 160, h = 120;
    private final byte[] gray = new byte[w * h], labels = new byte[w * h];

    public CourtesySignatureTailTest() {
        Arrays.fill(gray, (byte) 255);
        for (int y = 25; y < 70; y++) for (int x = 104; x < 114; x++) labels[y * w + x] = 3;
        bar(91);
        bar(96);
    }

    private void bar(int x) {
        for (int y = 30; y <= 70; y++) gray[y * w + x] = 30;
    }

    private boolean tail() {
        return OmrMeasurePostProcessor.courtesySignatureTail(
                labels, gray, w, h, 100, 135, 30, 70, 10, 20, 85);
    }

    @Test
    public void doubleBarAndKeyHaveNoDuration() {
        assertTrue(tail());
    }

    @Test
    public void actualNotePreservesFinalMeasure() {
        labels[50 * w + 120] = 2;
        assertFalse(tail());
    }

    @Test
    public void singleBarCannotSuppressMeasure() {
        for (int y = 30; y <= 70; y++) gray[y * w + 91] = (byte) 255;
        assertFalse(tail());
    }

    @Test
    public void restOnlyMeasureIsNotKeyTail() {
        Arrays.fill(labels, (byte) 0);
        labels[50 * w + 110] = 1;
        assertFalse(tail());
    }

    @Test
    public void missingRasterCannotSuppressMeasure() {
        assertFalse(
                OmrMeasurePostProcessor.courtesySignatureTail(
                        labels, null, w, h, 100, 135, 30, 70, 10, 20, 85));
    }
}
