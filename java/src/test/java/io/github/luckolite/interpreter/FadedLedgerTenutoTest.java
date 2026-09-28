// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** Original gray ruled strokes; no private score raster. */
public class FadedLedgerTenutoTest {
    private int detect(int lines, int first, int spacing, boolean thick, boolean shortLines) {
        return detect(lines, first, spacing, thick, shortLines, 0);
    }

    private int detect(
            int lines, int first, int spacing, boolean thick, boolean shortLines, int stemWidth) {
        int w = 800, h = 500;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        int half = shortLines ? 12 : 16;
        for (int i = 0; i < lines; i++)
            for (int x = 150 - half; x <= 150 + half; x++)
                for (int dy = thick ? -5 : 0; dy <= (thick ? 5 : 0); dy++)
                    gray[(first + i * spacing + dy) * w + x] = (byte) 200;
        for (int x = 146; x <= 157; x++) gray[first * w + x] = 100;
        for (int x = 134; x < 134 + stemWidth; x++)
            for (int y = first - 8; y <= first + 2 * spacing + 8; y++)
                if ((gray[y * w + x] & 255) > 200) gray[y * w + x] = (byte) 200;
        byte[] copy = gray.clone();
        int result =
                NoteArticulationDetector.detect(
                        labels,
                        gray,
                        w,
                        h,
                        List.of(new NoteArticulationDetector.Anchor(150, 100, 16, 0)))[0];
        assertArrayEquals(copy, gray);
        return result;
    }

    @Test
    public void fadedLedgerCoreIsNotTenuto() {
        assertEquals(0, detect(3, 116, 16, false, false));
    }

    @Test
    public void independentTenutoIsRetained() {
        assertEquals(NoteArticulation.TENUTO, detect(0, 116, 16, false, false));
    }

    @Test
    public void twoLinesDoNotProveLedgerStack() {
        assertEquals(NoteArticulation.TENUTO, detect(2, 116, 16, false, false));
    }

    @Test
    public void irregularSpacingDoesNotProveLedgerStack() {
        assertEquals(NoteArticulation.TENUTO, detect(3, 116, 19, false, false));
    }

    @Test
    public void broadGrayBandsAreNotLedgerRules() {
        assertEquals(NoteArticulation.TENUTO, detect(3, 116, 16, true, false));
    }

    @Test
    public void shortDashesDoNotProveLedgerRules() {
        assertEquals(NoteArticulation.TENUTO, detect(3, 116, 16, false, true));
    }

    @Test
    public void InconsistentHeadPhaseKeepsTenuto() {
        assertEquals(NoteArticulation.TENUTO, detect(3, 112, 16, false, false));
    }

    @Test
    public void continuousNarrowStemDoesNotHideLedgerProof() {
        assertEquals(0, detect(3, 116, 16, false, false, 6));
    }

    @Test
    public void broadGrayBlockIsNotAStem() {
        assertEquals(NoteArticulation.TENUTO, detect(3, 116, 16, false, false, 33));
    }
}
