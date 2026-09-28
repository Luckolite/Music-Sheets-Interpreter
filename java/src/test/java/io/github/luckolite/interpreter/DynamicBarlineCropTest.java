// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric marks; no private score pixels or copied font outlines. */
public class DynamicBarlineCropTest {
    private final int w = 100, h = 240;

    private byte[] raster() {
        var gray = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        return gray;
    }

    private void rect(byte[] gray, int l, int t, int r, int b) {
        for (int y = t; y < b; y++) for (int x = l; x < r; x++) gray[y * w + x] = 0;
    }

    private List<PlayingTechniqueDetector.Staff> staffs() {
        return List.of(
                new PlayingTechniqueDetector.Staff(70, 110, 10, 0, 2),
                new PlayingTechniqueDetector.Staff(170, 210, 10, 1, 2));
    }

    private PlayingTechniqueDetector.Word word() {
        return new PlayingTechniqueDetector.Word("pp", .2f, 125f / h, .65f, 160f / h);
    }

    private void body(byte[] gray) {
        rect(gray, 24, 132, 38, 150);
        rect(gray, 43, 132, 57, 150);
    }

    @Test
    public void verifiedGrandStaffBarlineIsMaskedWithoutChangingThePage() {
        var gray = raster();
        body(gray);
        rect(gray, 28, 70, 30, 211);
        var before = gray.clone();
        var result = DynamicBarlineCrop.crop(word(), gray, w, h, staffs());
        assertNotNull(result);
        assertEquals(33, result.width());
        assertEquals(18, result.height());
        assertEquals(.24f, result.word().left(), .00001f);
        assertEquals(132f / h, result.word().top(), .00001f);
        assertEquals(255, result.gray()[4] & 255);
        assertEquals(0, result.gray()[0] & 255);
        assertArrayEquals(before, gray);
    }

    @Test
    public void continuingNoteStemCannotStandInForGrandStaffBarline() {
        var gray = raster();
        body(gray);
        rect(gray, 28, 100, 30, 191);
        assertNull(DynamicBarlineCrop.crop(word(), gray, w, h, staffs()));
    }

    @Test
    public void lineWithoutGlyphEvidenceDoesNotBecomeDynamic() {
        var gray = raster();
        rect(gray, 28, 70, 30, 211);
        assertNull(DynamicBarlineCrop.crop(word(), gray, w, h, staffs()));
    }

    @Test
    public void broadOcclusionCannotBeMaskedAway() {
        var gray = raster();
        body(gray);
        rect(gray, 28, 70, 35, 211);
        assertNull(DynamicBarlineCrop.crop(word(), gray, w, h, staffs()));
    }

    @Test
    public void separateSystemsAreNotAKeyboardPair() {
        var gray = raster();
        body(gray);
        rect(gray, 28, 70, 30, 211);
        var separate =
                List.of(
                        new PlayingTechniqueDetector.Staff(70, 110, 10, 0, 1),
                        new PlayingTechniqueDetector.Staff(170, 210, 10, 0, 1));
        assertNull(DynamicBarlineCrop.crop(word(), gray, w, h, separate));
    }

    @Test
    public void missingOrIncompletePixelsAreRejected() {
        assertNull(DynamicBarlineCrop.crop(word(), null, w, h, staffs()));
        assertNull(DynamicBarlineCrop.crop(word(), new byte[10], w, h, staffs()));
    }
}
