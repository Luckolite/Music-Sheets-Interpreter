// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original blurred dots, slurs, fermata roofs and printed horizontal strokes. */
public class RasterPrintedArticulationTest {
    private static final int W = 1280, H = 1280;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    private void paper() {
        Arrays.fill(gray, (byte) 180);
        Arrays.fill(labels, (byte) 0);
    }

    private void px(int x, int y, int tone) {
        gray[y * W + x] = (byte) tone;
        labels[y * W + x] = OmrMeasurePostProcessor.SYMBOL;
    }

    private void box(int l, int t, int r, int b, int tone) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) px(x, y, tone);
    }

    private int mark(float gap, boolean raw) {
        return NoteArticulationDetector.detect(
                labels,
                raw ? gray : null,
                W,
                H,
                List.of(new NoteArticulationDetector.Anchor(480, 260, gap, 0)))[0];
    }

    private void dot(int cy) {
        paper();
        box(474, cy - 3, 484, cy + 3, 145);
        box(478, cy - 2, 481, cy + 1, 35);
        px(478, cy - 2, 145);
        px(481, cy - 2, 145);
        px(481, cy + 1, 145);
    }

    private void slur(int cy) {
        for (int x = 448; x <= 514; x++) {
            int y = cy - 12 + (int) Math.round(Math.pow((x - 480) / 35.0, 2) * 4);
            box(x, y, x, y + 1, 30);
        }
    }

    private void roof(int cy) {
        for (int x = 467; x <= 493; x++) {
            int y = cy - 16 + (int) Math.round(Math.pow((x - 480) / 13.0, 2) * 9);
            box(x, y, x, y + 1, 30);
        }
    }

    private void dash(boolean below, boolean slope) {
        paper();
        int cy = below ? 286 : 234;
        box(468, cy - 4, 493, cy + 4, 145);
        for (int x = 471; x <= 490; x++) {
            int y = cy + (slope ? (int) Math.round((x - 480) * .1) : 0);
            box(x, y - 1, x, y + 1, 35);
        }
    }

    @Test
    public void oneRasterBoundaryPixelDoesNotLoseRoundCore() {
        dot(240);
        assertEquals(NoteArticulation.STACCATO, mark(14.5f, true));
    }

    @Test
    public void oneRasterBoundaryPixelBelowHeadKeepsRoundCore() {
        dot(280);
        assertEquals(NoteArticulation.STACCATO, mark(14.5f, true));
    }

    @Test
    public void blurredDotUnderLongSlurIsStaccato() {
        dot(240);
        slur(240);
        assertEquals(NoteArticulation.STACCATO, mark(14.5f, true));
    }

    @Test
    public void blurredDotUnderCompactFermataIsNotStaccato() {
        dot(240);
        roof(240);
        assertEquals(0, mark(14.5f, true) & NoteArticulation.STACCATO);
    }

    @Test
    public void longSlurDoesNotCreateMissingDot() {
        paper();
        slur(240);
        assertEquals(0, mark(14.5f, true));
    }

    @Test
    public void smallSpeckStillFailsScaledArea() {
        dot(240);
        px(479, 239, 145);
        px(480, 239, 145);
        assertEquals(0, mark(14.5f, true));
    }

    @Test
    public void raggedCoreCannotBecomeRoundDot() {
        paper();
        box(474, 235, 484, 243, 145);
        for (int i = 0; i < 7; i++) box(477 + i, 235 + i, 478 + i, 235 + i, 35);
        assertEquals(0, mark(14.5f, true));
    }

    @Test
    public void semanticOnlyHaloDoesNotRecoverRasterBody() {
        dot(240);
        assertEquals(0, mark(14.5f, false));
    }

    @Test
    public void blurredFlatDashKeepsCompleteColumnBody() {
        dash(false, false);
        assertEquals(NoteArticulation.TENUTO, mark(14.5f, true));
    }

    @Test
    public void gentlyTiltedBlurredDashKeepsTenuto() {
        dash(false, true);
        assertEquals(NoteArticulation.TENUTO, mark(14.5f, true));
    }

    @Test
    public void gentlyTiltedBlurredDashBelowHeadKeepsTenuto() {
        dash(true, true);
        assertEquals(NoteArticulation.TENUTO, mark(14.5f, true));
    }

    @Test
    public void twoSeparatedDashFragmentsDoNotBecomeTenuto() {
        dash(false, true);
        box(479, 230, 482, 238, 145);
        assertEquals(0, mark(14.5f, true));
    }

    @Test
    public void archedStrokeIsNotTenuto() {
        paper();
        box(468, 228, 493, 239, 145);
        for (int x = 471; x <= 490; x++) {
            int y = 230 + (int) Math.round(Math.pow((x - 480) / 10.0, 2) * 4);
            box(x, y, x, y + 1, 35);
        }
        assertEquals(0, mark(14.5f, true));
    }

    @Test
    public void abruptStepIsNotTenuto() {
        paper();
        box(468, 228, 493, 240, 145);
        for (int x = 471; x <= 490; x++) box(x, x < 481 ? 231 : 236, x, x < 481 ? 233 : 238, 35);
        assertEquals(0, mark(14.5f, true));
    }

    @Test
    public void distantDashWithoutOwnershipIsNotTenuto() {
        dash(false, true);
        assertEquals(
                0,
                NoteArticulationDetector.detect(
                        labels,
                        gray,
                        W,
                        H,
                        List.of(new NoteArticulationDetector.Anchor(480, 360, 14.5f, 0)))[0]);
    }

    @Test
    public void recoveryPreservesInputPlanes() {
        dash(false, true);
        byte[] g = gray.clone(), l = labels.clone();
        mark(14.5f, true);
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }
}
