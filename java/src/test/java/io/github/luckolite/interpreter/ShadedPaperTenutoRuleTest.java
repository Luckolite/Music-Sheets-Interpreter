// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original thin duration-neutral dashes and pale rules on shaded paper. */
public class ShadedPaperTenutoRuleTest {
    private static final int W = 960, H = 480;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    private void page(int paper, boolean below) {
        Arrays.fill(gray, (byte) paper);
        Arrays.fill(labels, (byte) 0);
        int cy = below ? 125 : 75;
        for (int y = cy - 1; y <= cy + 1; y++)
            for (int x = 92; x <= 108; x++) {
                gray[y * W + x] = 0;
                labels[y * W + x] = OmrMeasurePostProcessor.SYMBOL;
            }
    }

    private int mark(boolean raw) {
        return NoteArticulationDetector.detect(
                labels,
                raw ? gray : null,
                W,
                H,
                List.of(new NoteArticulationDetector.Anchor(100, 100, 16, 0)))[0];
    }

    @Test
    public void shadedPaperCannotSupplyLongStaffRule() {
        page(180, false);
        assertEquals(NoteArticulation.TENUTO, mark(true));
    }

    @Test
    public void lighterShadedPaperCannotSupplyLongStaffRule() {
        page(225, false);
        assertEquals(NoteArticulation.TENUTO, mark(true));
    }

    @Test
    public void whitePaperRetainsIsolatedTenuto() {
        page(255, false);
        assertEquals(NoteArticulation.TENUTO, mark(true));
    }

    @Test
    public void belowHeadTenutoAlsoNeedsRealRuleVeto() {
        page(180, true);
        assertEquals(NoteArticulation.TENUTO, mark(true));
    }

    @Test
    public void gradedPaperDoesNotBecomeStaffStripe() {
        page(190, false);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                if ((gray[y * W + x] & 255) != 0) gray[y * W + x] = (byte) (180 + x / 12);
        assertEquals(NoteArticulation.TENUTO, mark(true));
    }

    @Test
    public void realPaleRuleOnWhitePaperStillVetoesDash() {
        page(255, false);
        for (int x = 0; x < W; x++) if (x < 92 || x > 108) gray[75 * W + x] = (byte) 225;
        assertEquals(0, mark(true));
    }

    @Test
    public void realPaleRuleOnShadedPaperStillVetoesDash() {
        page(220, false);
        for (int x = 0; x < W; x++) if (x < 92 || x > 108) gray[75 * W + x] = (byte) 200;
        assertEquals(0, mark(true));
    }

    @Test
    public void belowHeadPaleRuleIsAlsoRejected() {
        page(220, true);
        for (int x = 0; x < W; x++) if (x < 92 || x > 108) gray[125 * W + x] = (byte) 200;
        assertEquals(0, mark(true));
    }

    @Test
    public void missingRawPageKeepsSemanticFallback() {
        page(180, false);
        assertEquals(NoteArticulation.TENUTO, mark(false));
    }

    @Test
    public void readingDoesNotChangePageOrLabels() {
        page(180, false);
        var beforeGray = gray.clone();
        var beforeLabels = labels.clone();
        mark(true);
        assertArrayEquals(beforeGray, gray);
        assertArrayEquals(beforeLabels, labels);
    }
}
