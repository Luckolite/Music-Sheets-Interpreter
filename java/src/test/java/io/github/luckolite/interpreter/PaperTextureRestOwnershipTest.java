// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Uses the original counted-rest drawing with separately predicted paper texture. */
public class PaperTextureRestOwnershipTest {
    private CountedRestBodyTest.Page page(int background, int ink, boolean printedHead) {
        var p = new CountedRestBodyTest.Page(true, true, true);
        for (int i = 0; i < p.gray.length; i++)
            if ((p.gray[i] & 255) == 255) p.gray[i] = (byte) background;
        for (int y = 173; y <= 187; y++)
            for (int x = 128; x <= 150; x++) {
                p.labels[y * CountedRestBodyTest.W + x] = 2;
                p.gray[y * CountedRestBodyTest.W + x] = (byte) ink;
            }
        if (printedHead)
            for (int y = 169; y <= 190; y++) p.gray[y * CountedRestBodyTest.W + 153] = 0;
        return p;
    }

    private List<MeasureNumberReconciler.NumberToken> counts(CountedRestBodyTest.Page p) {
        return MultiMeasureRestDetector.detect(
                p.normalize(),
                p.gray,
                CountedRestBodyTest.W,
                CountedRestBodyTest.H,
                CountedRestBodyTest.REGIONS,
                List.of());
    }

    @Test
    public void paperTextureDoesNotMakeACountedRestSounding() {
        var p = page(175, 166, false);
        assertEquals(1, counts(p).size());
        assertEquals(2, counts(p).get(0).value());
    }

    @Test
    public void paperTextureIsRemovedBeforeGeometryOwnership() {
        var p = page(175, 166, false);
        assertEquals(0, p.normalize()[180 * CountedRestBodyTest.W + 140]);
    }

    @Test
    public void aPrintedLedgerHeadStillBlocksTheRest() {
        var p = page(175, 25, true);
        assertEquals(2, p.normalize()[180 * CountedRestBodyTest.W + 140]);
        assertTrue(counts(p).isEmpty());
    }

    @Test
    public void aFadedHeadOnWhitePaperIsPreserved() {
        var p = page(255, 210, false);
        assertEquals(2, p.normalize()[180 * CountedRestBodyTest.W + 140]);
        assertTrue(counts(p).isEmpty());
    }

    @Test
    public void contrastedInkOnShadedPaperIsPreserved() {
        var p = page(185, 150, false);
        assertEquals(2, p.normalize()[180 * CountedRestBodyTest.W + 140]);
        assertTrue(counts(p).isEmpty());
    }

    @Test
    public void originalPixelsAndSemanticLabelsArePreserved() {
        var p = page(175, 166, false);
        byte[] gray = p.gray.clone(), labels = p.labels.clone();
        p.normalize();
        assertArrayEquals(gray, p.gray);
        assertArrayEquals(labels, p.labels);
    }
}
