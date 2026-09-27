// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original line-art word and extender fixtures, not score-derived images. */
public class ArticulationTextRunTest {
    private static final int W = 1500, H = 2000;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    public ArticulationTextRunTest() {
        Arrays.fill(gray, (byte) 255);
    }

    private void box(int l, int t, int r, int b) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) gray[y * W + x] = 0;
    }

    private void word() {
        for (int x : new int[] {300, 318, 336}) {
            box(x, 510, x + 2, 523);
            box(x, 510, x + 12, 512);
            box(x, 521, x + 12, 523);
        }
    }

    private void dashes() {
        for (int x : new int[] {370, 410, 450}) box(x, 516, x + 14, 518);
    }

    private int marks(int x) {
        return NoteArticulationDetector.detect(
                labels, gray, W, H, List.of(new NoteArticulationDetector.Anchor(x, 480, 16, 0)))[0];
    }

    @Test
    public void wordExtensionDashesAreNotTenuto() {
        word();
        dashes();
        assertEquals(0, marks(457));
    }

    @Test
    public void firstContinuationDashUsesFollowingRun() {
        word();
        dashes();
        assertEquals(0, marks(377));
    }

    @Test
    public void repeatedTenutoWithoutWordRemains() {
        dashes();
        assertEquals(NoteArticulation.TENUTO, marks(457));
    }

    @Test
    public void disconnectedWordDoesNotCancelTenuto() {
        word();
        box(800, 516, 814, 518);
        assertEquals(NoteArticulation.TENUTO, marks(807));
    }

    @Test
    public void punctuationAfterWordIsNotStaccato() {
        word();
        box(354, 519, 358, 523);
        assertEquals(0, marks(356));
    }

    @Test
    public void isolatedDotStillStaccato() {
        box(354, 519, 358, 523);
        assertEquals(NoteArticulation.STACCATO, marks(356));
    }

    @Test
    public void acceptedNotationDoesNotBecomeWord() {
        word();
        dashes();
        for (int p = 0; p < labels.length; p++)
            if ((gray[p] & 255) < 155 && p % W < 350) labels[p] = OmrMeasurePostProcessor.NOTEHEAD;
        assertEquals(NoteArticulation.TENUTO, marks(457));
    }

    @Test
    public void tallCapitalAndJoinedLetterKeepWordContext() {
        for (int x : new int[] {255, 280, 310}) {
            box(x, 500, x + 2, 530);
            box(x, 500, x + 21, 502);
            box(x, 528, x + 21, 530);
        }
        dashes();
        assertEquals(0, marks(457));
    }

    @Test
    public void abbreviatedWordPeriodOwnsSingleFollowingDash() {
        word();
        box(353, 520, 356, 523);
        box(370, 516, 384, 518);
        assertEquals(0, marks(377));
    }

    @Test
    public void wordWithoutPeriodDoesNotOwnSingleSeparatedDash() {
        word();
        box(370, 516, 384, 518);
        assertEquals(NoteArticulation.TENUTO, marks(377));
    }
}
