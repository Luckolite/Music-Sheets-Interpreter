// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original drawn symbols on dark photographed paper, with intact notation and curves. */
public class DarkPaperArticulationTest {
    private static final int W = 1280, H = 1280;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    private void paper(int tone) {
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                gray[y * W + x] = (byte) (tone + ((x * 17 + y * 13) % 7) - 3);
    }

    private void pixel(int x, int y, int tone, byte label) {
        gray[y * W + x] = (byte) tone;
        labels[y * W + x] = label;
    }

    private void box(int l, int t, int r, int b, int tone, byte label) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) pixel(x, y, tone, label);
    }

    private void dash(int cy) {
        box(471, cy - 1, 490, cy + 1, 30, OmrMeasurePostProcessor.SYMBOL);
    }

    private void dot(int cy) {
        box(478, cy - 2, 482, cy + 1, 30, OmrMeasurePostProcessor.SYMBOL);
    }

    private void caret(int cy) {
        for (int x = 473; x <= 487; x++) {
            int y = cy + (int) Math.round(Math.abs(x - 480) * 1.35);
            box(x, y - 1, x, y + 1, 30, OmrMeasurePostProcessor.SYMBOL);
        }
    }

    private void accent(int cy) {
        for (int x = 470; x <= 490; x++) {
            int d = (int) Math.round((490 - x) * .32);
            box(x, cy - d - 1, x, cy - d + 1, 30, OmrMeasurePostProcessor.SYMBOL);
            box(x, cy + d - 1, x, cy + d + 1, 30, OmrMeasurePostProcessor.SYMBOL);
        }
    }

    private void slur(int cy) {
        for (int x = 445; x <= 515; x++) {
            int y = cy + (int) Math.round(Math.pow((x - 480) / 35., 2) * 12);
            box(x, y, x, y + 1, 30, OmrMeasurePostProcessor.SYMBOL);
        }
    }

    private int mark() {
        return NoteArticulationDetector.detect(
                labels, gray, W, H, List.of(new NoteArticulationDetector.Anchor(480, 260, 14, 0)))[
                0];
    }

    @Test
    public void darkPaperRetainsCompleteTenuto() {
        paper(145);
        dash(234);
        assertEquals(NoteArticulation.TENUTO, mark());
    }

    @Test
    public void darkPaperRetainsCompleteCaret() {
        paper(145);
        caret(216);
        assertEquals(NoteArticulation.MARCATO, mark());
    }

    @Test
    public void darkPaperRetainsCompleteAccent() {
        paper(145);
        accent(230);
        assertEquals(NoteArticulation.ACCENT, mark());
    }

    @Test
    public void darkPaperRetainsDetachedStaccato() {
        paper(145);
        dot(239);
        assertEquals(NoteArticulation.STACCATO, mark());
    }

    @Test
    public void darkPaperRetainsTenutoAndAccentTogether() {
        paper(145);
        dash(280);
        accent(292);
        assertEquals(NoteArticulation.TENUTO | NoteArticulation.ACCENT, mark());
    }

    @Test
    public void supplementalContextDoesNotDuplicateTwoNeighborBodiesIntoWord() {
        paper(175);
        caret(216);
        box(461, 216, 467, 227, 30, OmrMeasurePostProcessor.SYMBOL);
        box(493, 216, 499, 227, 30, OmrMeasurePostProcessor.SYMBOL);
        assertEquals(NoteArticulation.MARCATO, mark());
    }

    @Test
    public void blankDarkPaperCreatesNoMarks() {
        paper(145);
        assertEquals(0, mark());
    }

    @Test
    public void darkPaperSlurCreatesNoMark() {
        paper(145);
        slur(232);
        assertEquals(0, mark());
    }

    @Test
    public void faintContinuationKeepsBeamFragmentOutOfTenuto() {
        paper(145);
        box(430, 234, 530, 236, 80, OmrMeasurePostProcessor.SYMBOL);
        dash(235);
        assertEquals(0, mark());
    }

    @Test
    public void faintContinuationKeepsTaperedCurveOutOfWedge() {
        paper(145);
        box(410, 220, 480, 221, 80, OmrMeasurePostProcessor.SYMBOL);
        for (int y = 220; y <= 232; y++) {
            int half = (232 - y) / 2;
            box(480 - half, y, 480 + half, y, 30, OmrMeasurePostProcessor.SYMBOL);
        }
        assertEquals(0, mark());
    }

    @Test
    public void darkPaperThinStaffDoesNotBecomeTenuto() {
        paper(145);
        box(410, 234, 550, 235, 30, OmrMeasurePostProcessor.STAFF);
        assertEquals(0, mark());
    }

    @Test
    public void semanticStemDoesNotBecomeStaccatissimo() {
        paper(145);
        box(478, 218, 481, 241, 30, OmrMeasurePostProcessor.STEM_OR_REST);
        assertEquals(0, mark());
    }

    @Test
    public void ambiguousVeryDarkPaperAbstains() {
        paper(80);
        dash(234);
        assertEquals(0, mark());
    }

    @Test
    public void recoveredMarkNeedsAcceptedNoteOwnership() {
        paper(145);
        dash(234);
        assertEquals(
                0,
                NoteArticulationDetector.detect(
                        labels,
                        gray,
                        W,
                        H,
                        List.of(new NoteArticulationDetector.Anchor(580, 260, 14, 0)))[0]);
    }

    @Test
    public void paperRecoveryPreservesBothSourcePlanes() {
        paper(145);
        accent(230);
        byte[] g = gray.clone(), l = labels.clone();
        mark();
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }
}
