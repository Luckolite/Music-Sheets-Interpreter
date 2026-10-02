// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original fading curves and isolated wedges versus letters in a printed word. */
public class RecoveredArticulationOwnershipTest {
    private static final int W = 1280, H = 1280;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    private void paper(int v) {
        Arrays.fill(gray, (byte) v);
        Arrays.fill(labels, (byte) 0);
    }

    private void px(int x, int y, int tone) {
        gray[y * W + x] = (byte) tone;
        labels[y * W + x] = OmrMeasurePostProcessor.SYMBOL;
    }

    private void box(int l, int t, int r, int b, int tone) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) px(x, y, tone);
    }

    private int mark(float x) {
        return NoteArticulationDetector.detect(
                labels, gray, W, H, List.of(new NoteArticulationDetector.Anchor(x, 260, 14, 0)))[0];
    }

    private void island(int cy) {
        box(474, cy + 1, 486, cy + 2, 35);
        box(474, cy + 3, 475, cy + 5, 145);
    }

    private void curve(int cy, int tone) {
        for (int x = 430; x <= 530; x++) {
            int y = cy + (int) Math.round(Math.pow(x - 480, 2) * .004);
            box(x, y, x, y + 2, tone);
        }
    }

    private void wedge() {
        for (int y = 276; y <= 290; y++) {
            int half = 1 + (y - 276) / 5;
            box(480 - half, y, 480 + half, y, 35);
        }
    }

    private void recoveredWedge() {
        wedge();
        box(475, 276, 476, 290, 145);
    }

    private void angularLetters() {
        for (int cx : new int[] {455, 467, 493, 505})
            for (int x = cx - 4; x <= cx + 4; x++) {
                int y = 279 + Math.abs(x - cx) * 2;
                box(x, y, x, y + 1, 35);
            }
    }

    private void word(boolean reflect, int cy) {
        for (int i = 0; i < 3; i++) {
            int l = 489 + i * 12;
            if (reflect) l = 465 - i * 12;
            box(l, cy, l + 6, cy + 1, 35);
            box(l, cy + 10, l + 6, cy + 11, 35);
            box(l, cy, l + 1, cy + 11, 35);
            box(l + 5, cy, l + 6, cy + 11, 35);
        }
    }

    @Test
    public void fadingCurveIslandIsNotTenuto() {
        paper(255);
        curve(230, 180);
        island(230);
        assertEquals(0, mark(480));
    }

    @Test
    public void offAxisCurveIslandIsNotTenuto() {
        paper(255);
        curve(230, 180);
        island(230);
        assertEquals(0, mark(488));
    }

    @Test
    public void isolatedPrintedDashKeepsTenuto() {
        paper(255);
        island(230);
        assertEquals(NoteArticulation.TENUTO, mark(480));
    }

    @Test
    public void disconnectedSlurAboveDashKeepsTenuto() {
        paper(255);
        curve(220, 180);
        island(230);
        assertEquals(NoteArticulation.TENUTO, mark(480));
    }

    @Test
    public void printedDashOnShadeKeepsTenuto() {
        paper(180);
        island(230);
        assertEquals(NoteArticulation.TENUTO, mark(480));
    }

    @Test
    public void letterLikeWedgeInsideRightwardWordIsNotArticulation() {
        paper(255);
        wedge();
        word(false, 277);
        assertEquals(0, mark(480));
    }

    @Test
    public void letterLikeWedgeInsideLeftwardWordIsNotArticulation() {
        paper(255);
        wedge();
        word(true, 277);
        assertEquals(0, mark(480));
    }

    @Test
    public void recoveredDescenderBelowRightwardLetterBodiesIsNotWedge() {
        paper(255);
        wedge();
        box(476, 276, 476, 290, 145);
        word(false, 267);
        assertEquals(0, mark(480));
    }

    @Test
    public void recoveredDescenderBelowLeftwardLetterBodiesIsNotWedge() {
        paper(255);
        wedge();
        box(476, 276, 476, 290, 145);
        word(true, 267);
        assertEquals(0, mark(480));
    }

    @Test
    public void recoveredWedgeBetweenAngularLetterFragmentsIsText() {
        paper(255);
        recoveredWedge();
        angularLetters();
        assertEquals(0, mark(480));
    }

    @Test
    public void isolatedHaloWedgeRetainsItsMark() {
        paper(255);
        recoveredWedge();
        assertEquals(NoteArticulation.STACCATISSIMO, mark(480));
    }

    @Test
    public void notationAngularShapesDoNotBecomeWord() {
        paper(255);
        recoveredWedge();
        angularLetters();
        for (int y = 276; y <= 289; y++)
            for (int x = 450; x <= 510; x++)
                if ((x < 475 || x > 483) && labels[y * W + x] != 0)
                    labels[y * W + x] = OmrMeasurePostProcessor.NOTEHEAD;
        assertEquals(NoteArticulation.STACCATISSIMO, mark(480));
    }

    @Test
    public void isolatedWedgeRemainsStaccatissimo() {
        paper(255);
        wedge();
        assertEquals(NoteArticulation.STACCATISSIMO, mark(480));
    }

    @Test
    public void verticallySeparateWordDoesNotClaimWedge() {
        paper(255);
        wedge();
        word(false, 216);
        assertEquals(NoteArticulation.STACCATISSIMO, mark(480));
    }

    @Test
    public void notationFragmentsDoNotSupplyWordEvidence() {
        paper(255);
        wedge();
        word(false, 277);
        for (int y = 277; y <= 288; y++)
            for (int x = 489; x < 521; x++)
                if (labels[y * W + x] != 0) labels[y * W + x] = OmrMeasurePostProcessor.NOTEHEAD;
        assertEquals(NoteArticulation.STACCATISSIMO, mark(480));
    }

    @Test
    public void incompleteOneSidedCurveDoesNotErasePrintedDash() {
        paper(255);
        for (int x = 430; x < 474; x++) box(x, 231, x, 233, 180);
        island(230);
        assertEquals(NoteArticulation.TENUTO, mark(480));
    }

    @Test
    public void ownershipChecksPreserveInputPlanes() {
        paper(255);
        curve(230, 180);
        island(230);
        byte[] a = gray.clone(), b = labels.clone();
        mark(480);
        assertArrayEquals(a, gray);
        assertArrayEquals(b, labels);
    }
}
