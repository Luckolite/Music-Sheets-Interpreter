// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic ink components, without score scans or copied font outlines. */
public class DynamicStemCropTest {
    private final int w = 100, h = 240;

    private byte[] raster() {
        var a = new byte[w * h];
        Arrays.fill(a, (byte) 255);
        return a;
    }

    private void rect(byte[] a, int l, int t, int r, int b) {
        for (int y = t; y < b; y++) for (int x = l; x < r; x++) a[y * w + x] = 0;
    }

    private List<PlayingTechniqueDetector.Staff> staffs() {
        return List.of(
                new PlayingTechniqueDetector.Staff(70, 110, 12, 0, 2),
                new PlayingTechniqueDetector.Staff(170, 210, 12, 1, 2));
    }

    private PlayingTechniqueDetector.Word word() {
        return new PlayingTechniqueDetector.Word("pp", .2f, 125f / h, .65f, 160f / h);
    }

    private void glyph(byte[] a) {
        rect(a, 24, 132, 38, 150);
        rect(a, 43, 132, 57, 150);
        rect(a, 22, 148, 25, 150);
    }

    private void beam(byte[] a) {
        rect(a, 45, 154, 65, 158);
        rect(a, 48, 155, 50, 186);
    }

    @Test
    public void separateClippedBeamIsRemovedAndLeadingSerifPreserved() {
        var a = raster();
        glyph(a);
        beam(a);
        var before = a.clone();
        var crop = DynamicStemCrop.crop(word(), a, w, h, staffs());
        assertNotNull(crop);
        assertEquals(35, crop.width());
        assertEquals(18, crop.height());
        assertEquals(.22f, crop.word().left(), .00001f);
        assertEquals(132f / h, crop.word().top(), .00001f);
        assertArrayEquals(before, a);
    }

    @Test
    public void thinOutgoingSlurDoesNotStretchTheGlyphCrop() {
        var a = raster();
        glyph(a);
        beam(a);
        rect(a, 56, 130, 72, 132);
        var crop = DynamicStemCrop.crop(word(), a, w, h, staffs());
        assertNotNull(crop);
        assertEquals(.22f, crop.word().left(), .00001f);
        assertEquals(.57f, crop.word().right(), .00001f);
        assertEquals(132f / h, crop.word().top(), .00001f);
    }

    @Test
    public void attachedNoteStemCannotLeaveItsOwnNoteheadAsADynamic() {
        var a = raster();
        rect(a, 24, 132, 38, 150);
        rect(a, 28, 140, 30, 186);
        assertNull(DynamicStemCrop.crop(word(), a, w, h, staffs()));
    }

    @Test
    public void normalGlyphWithoutClippedComponentDoesNotUseRecovery() {
        var a = raster();
        glyph(a);
        assertNull(DynamicStemCrop.crop(word(), a, w, h, staffs()));
    }

    @Test
    public void removedStemWithoutRemainingInkIsRejected() {
        var a = raster();
        beam(a);
        assertNull(DynamicStemCrop.crop(word(), a, w, h, staffs()));
    }

    @Test
    public void separateSystemsCannotBeCombined() {
        var a = raster();
        glyph(a);
        beam(a);
        var systems =
                List.of(
                        new PlayingTechniqueDetector.Staff(70, 110, 10, 0, 1),
                        new PlayingTechniqueDetector.Staff(170, 210, 10, 0, 1));
        assertNull(DynamicStemCrop.crop(word(), a, w, h, systems));
    }

    @Test
    public void componentClippedAboveTheWordIsAlsoRemoved() {
        var a = raster();
        glyph(a);
        rect(a, 28, 100, 30, 129);
        rect(a, 25, 125, 35, 130);
        var crop = DynamicStemCrop.crop(word(), a, w, h, staffs());
        assertNotNull(crop);
        assertEquals(132f / h, crop.word().top(), .00001f);
    }

    @Test
    public void proseAndMissingPixelsAreRejected() {
        var a = raster();
        glyph(a);
        beam(a);
        assertNull(
                DynamicStemCrop.crop(
                        new PlayingTechniqueDetector.Word("dolce", .2f, 125f / h, .65f, 160f / h),
                        a,
                        w,
                        h,
                        staffs()));
        assertNull(DynamicStemCrop.crop(word(), null, w, h, staffs()));
        assertNull(DynamicStemCrop.crop(word(), new byte[10], w, h, staffs()));
    }
}
