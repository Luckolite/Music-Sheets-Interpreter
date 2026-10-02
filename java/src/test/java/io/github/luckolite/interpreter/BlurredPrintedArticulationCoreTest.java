// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original detached printed bodies with shaded antialiased edges. */
public class BlurredPrintedArticulationCoreTest {
    private static final int W = 960, H = 480;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    private void paper(int tone) {
        Arrays.fill(gray, (byte) tone);
        Arrays.fill(labels, (byte) 0);
    }

    private void box(int x0, int y0, int x1, int y1, int tone) {
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                gray[y * W + x] = (byte) tone;
                labels[y * W + x] = OmrMeasurePostProcessor.SYMBOL;
            }
    }

    private int mark(float gap, boolean raw) {
        return NoteArticulationDetector.detect(
                labels,
                raw ? gray : null,
                W,
                H,
                List.of(new NoteArticulationDetector.Anchor(480, 260, gap, 0)))[0];
    }

    private void dot(int paper, int cy) {
        paper(paper);
        box(476, cy - 3, 484, cy + 3, 145);
        box(478, cy - 2, 482, cy + 2, 35);
    }

    @Test
    public void shadedHaloRetainsDarkRoundDot() {
        dot(180, 224);
        assertEquals(NoteArticulation.STACCATO, mark(12, true));
    }

    @Test
    public void whiteHaloRetainsDarkRoundDot() {
        dot(255, 224);
        assertEquals(NoteArticulation.STACCATO, mark(12, true));
    }

    @Test
    public void belowHeadHaloRetainsDarkRoundDot() {
        dot(180, 296);
        assertEquals(NoteArticulation.STACCATO, mark(12, true));
    }

    @Test
    public void scaledRoundDotNeedsScaledBody() {
        paper(190);
        box(474, 208, 486, 218, 145);
        box(477, 210, 483, 216, 40);
        assertEquals(NoteArticulation.STACCATO, mark(16, true));
    }

    @Test
    public void shadedHaloRetainsDetachedTenuto() {
        paper(180);
        box(468, 222, 492, 226, 145);
        box(472, 223, 488, 225, 35);
        assertEquals(NoteArticulation.TENUTO, mark(12, true));
    }

    @Test
    public void belowHeadHaloRetainsDetachedTenuto() {
        paper(180);
        box(468, 294, 492, 298, 145);
        box(472, 295, 488, 297, 35);
        assertEquals(NoteArticulation.TENUTO, mark(12, true));
    }

    @Test
    public void paleFlatGrainDoesNotSupplyPrintedBody() {
        paper(180);
        box(476, 221, 484, 227, 120);
        assertEquals(0, mark(12, true));
    }

    @Test
    public void tinyDarkSpeckDoesNotSupplyRoundBody() {
        paper(180);
        box(476, 221, 484, 227, 145);
        box(480, 224, 481, 225, 35);
        assertEquals(0, mark(12, true));
    }

    @Test
    public void severalSpecksDoNotBecomeOneBody() {
        paper(180);
        box(473, 221, 487, 227, 145);
        box(473, 223, 475, 225, 35);
        box(485, 223, 487, 225, 35);
        assertEquals(0, mark(12, true));
    }

    @Test
    public void thinObliqueStrokeIsNotAccentOrDot() {
        paper(180);
        box(474, 220, 486, 228, 145);
        for (int i = 0; i < 9; i++) box(476 + i, 220 + i, 476 + i, 220 + i, 35);
        assertEquals(0, mark(12, true));
    }

    @Test
    public void missingGrayDoesNotInventPrintedCore() {
        dot(180, 224);
        assertEquals(0, mark(12, false));
    }

    @Test
    public void classificationPreservesRawAndSemanticPixels() {
        dot(180, 224);
        byte[] g = gray.clone(), l = labels.clone();
        mark(12, true);
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }
}
