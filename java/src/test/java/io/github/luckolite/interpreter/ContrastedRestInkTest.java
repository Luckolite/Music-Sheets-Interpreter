// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generated paired bulbs; no score pixels. */
public class ContrastedRestInkTest {
    private static final int W = 400, H = 220;

    private byte[] page(int shade, int background, boolean secondBulb) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) background);
        for (int line = 0; line < 5; line++)
            for (int x = 10; x < W - 10; x++) gray[Math.round(80 + line * 14.5f) * W + x] = 0;
        for (int y = 97; y <= 135; y++) {
            int x = 180 - (y - 97) * 10 / 38;
            gray[y * W + x] = (byte) shade;
            gray[y * W + x + 1] = (byte) shade;
        }
        for (int y = 99; y <= 101; y++)
            for (int x = 173; x <= 180; x++) gray[y * W + x] = (byte) shade;
        if (secondBulb)
            for (int y = 113; y <= 115; y++)
                for (int x = 169; x <= 176; x++) gray[y * W + x] = (byte) shade;
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
    public void locallyContrastedFaintPairedBulbsRecoverSixteenth() {
        var rests = detect(page(180, 255, true), List.of());
        assertEquals(rests.toString(), 1, rests.size());
        assertEquals(.25, rests.get(0).durationBeats(), 0);
    }

    @Test
    public void lowContrastDoesNotPromoteNoise() {
        assertTrue(detect(page(180, 210, true), List.of()).isEmpty());
    }

    @Test
    public void outsideFaintBandDoesNotPromoteNoise() {
        assertTrue(detect(page(190, 255, true), List.of()).isEmpty());
    }

    @Test
    public void singleBulbIsNotSixteenth() {
        assertTrue(
                detect(page(180, 255, false), List.of()).stream()
                        .noneMatch(r -> r.durationBeats() == .25));
    }

    @Test
    public void noteStillOwnsColumn() {
        var note = new ScoreNoteEvent(0, .43f, 3, 0, 1, 114f / H, false, 0, 1);
        assertTrue(detect(page(180, 255, true), List.of(note)).isEmpty());
    }

    @Test
    public void duplicatePassDoesNotDuplicateDarkRest() {
        assertEquals(1, detect(page(0, 255, true), List.of()).size());
    }

    @Test
    public void sourcePixelsAreUnchanged() {
        byte[] gray = page(180, 255, true), before = gray.clone();
        detect(gray, List.of());
        assertArrayEquals(before, gray);
    }
}
