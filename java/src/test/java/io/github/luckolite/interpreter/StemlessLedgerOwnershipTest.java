// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original raw staff, ovals and annotation strokes; no score image or OCR text. */
public class StemlessLedgerOwnershipTest {
    private static final int W = 500, H = 300;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    public StemlessLedgerOwnershipTest() {
        Arrays.fill(gray, (byte) 255);
        for (int i = 0; i < 5; i++)
            for (int x = 40; x <= 460; x++) {
                gray[(100 + i * 16) * W + x] = 0;
                labels[(100 + i * 16) * W + x] = OmrMeasurePostProcessor.STAFF;
            }
    }

    private void oval(int x, int y, int rx, int ry) {
        for (int yy = y - ry; yy <= y + ry; yy++)
            for (int xx = x - rx; xx <= x + rx; xx++)
                if ((xx - x) * (xx - x) / (double) (rx * rx)
                                + (yy - y) * (yy - y) / (double) (ry * ry)
                        <= 1) {
                    gray[yy * W + xx] = 0;
                    labels[yy * W + xx] = OmrMeasurePostProcessor.NOTEHEAD;
                }
    }

    private void rule(int x, int y) {
        for (int xx = x - 18; xx <= x + 18; xx++) gray[y * W + xx] = 0;
    }

    private List<ScoreNoteEvent> read() {
        return OmrScoreInterpreter.extract(
                labels, gray, W, H, List.of(new MeasureRegion(.08f, .92f, .28f, .62f)));
    }

    @Test
    public void stemlessFirstLedgerBlobNeedsItsPrintedRule() {
        oval(200, 84, 5, 4);
        assertTrue(read().toString(), read().isEmpty());
    }

    @Test
    public void genuineFirstLedgerOvalKeepsItsPrintedRule() {
        oval(200, 84, 5, 4);
        rule(200, 84);
        assertEquals(read().toString(), 1, read().size());
    }

    @Test
    public void closeStaffSpaceOvalDoesNotRequireFirstLedgerRule() {
        oval(200, 177, 6, 6);
        assertEquals(read().toString(), 1, read().size());
    }

    @Test
    public void thirdLedgerOvalNeedsBothInnerRulesNotOneNearbyStroke() {
        oval(200, 212, 8, 5);
        rule(200, 205);
        assertTrue(read().toString(), read().isEmpty());
    }

    @Test
    public void separatedInnerAndOuterRulesKeepRemoteOval() {
        oval(200, 212, 8, 5);
        rule(200, 212);
        rule(200, 196);
        rule(200, 180);
        assertEquals(read().toString(), 1, read().size());
    }
}
