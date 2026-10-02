// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original tilted strokes keep their complete thin-column or two-arm proof. */
public class TiltedPrintedArticulationTest {
    private static final int W = 1280, H = 1280;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    private void paper(int tone) {
        Arrays.fill(gray, (byte) tone);
    }

    private void pixel(int x, int y, int tone) {
        gray[y * W + x] = (byte) tone;
        labels[y * W + x] = OmrMeasurePostProcessor.SYMBOL;
    }

    private void dash(boolean below) {
        int cy = below ? 286 : 234;
        for (int y = cy - 5; y <= cy + 5; y++) for (int x = 469; x <= 491; x++) pixel(x, y, 145);
        for (int x = 472; x <= 488; x++) {
            int y = cy + (int) Math.round((x - 480) * -.18 + .35);
            for (int dy = -1; dy <= 1; dy++) pixel(x, y + dy, 30);
        }
    }

    private void accent(float shear) {
        for (int x = 0; x <= 20; x++) {
            int d = 20 - x;
            for (int side : new int[] {-1, 1}) {
                int y = 226 + (int) Math.round(-shear * d + side * d * .30);
                for (int dy = -1; dy <= 1; dy++) pixel(470 + x, y + dy, 30);
            }
        }
    }

    private int mark() {
        return NoteArticulationDetector.detect(
                labels, gray, W, H, List.of(new NoteArticulationDetector.Anchor(480, 260, 14, 0)))[
                0];
    }

    @Test
    public void boundingAspectDoesNotEraseStraightTiltedTenuto() {
        paper(180);
        dash(false);
        assertEquals(NoteArticulation.TENUTO, mark());
    }

    @Test
    public void boundingAspectDoesNotEraseTiltedTenutoBelowHead() {
        paper(180);
        dash(true);
        assertEquals(NoteArticulation.TENUTO, mark());
    }

    @Test
    public void darkPaperAndTiltKeepIndependentTenutoProof() {
        paper(145);
        dash(false);
        assertEquals(NoteArticulation.TENUTO, mark());
    }

    @Test
    public void twoStraightUnequalArmsKeepTiltedAccent() {
        paper(180);
        accent(-.18f);
        assertEquals(NoteArticulation.ACCENT, mark());
    }

    @Test
    public void oppositeTiltKeepsCompleteAccent() {
        paper(180);
        accent(.18f);
        assertEquals(NoteArticulation.ACCENT, mark());
    }

    @Test
    public void darkPaperAndTiltKeepCompleteAccent() {
        paper(145);
        accent(-.18f);
        assertEquals(NoteArticulation.ACCENT, mark());
    }

    @Test
    public void correctionPreservesOriginalPlanes() {
        paper(180);
        accent(-.18f);
        byte[] g = gray.clone(), l = labels.clone();
        mark();
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }
}
