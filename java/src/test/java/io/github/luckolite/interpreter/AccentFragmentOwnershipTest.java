// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class AccentFragmentOwnershipTest {
    private int detect(boolean separateDash, boolean eligibleAccent) {
        int w = 1200, h = 1000;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int x = 0; x < 25; x++)
            for (int y = 0; y < 13; y++) {
                double upper = 1 + 5 * x / 24.0, lower = 11 - 5 * x / 24.0;
                if (Math.abs(y - upper) > 1.25 && Math.abs(y - lower) > 1.25) continue;
                int p = (100 + y) * w + 100 + x;
                gray[p] = (byte) (y == 10 && x <= 10 || y <= 3 && x < 10 ? 120 : 175);
                if (!eligibleAccent && y < 8) labels[p] = OmrMeasurePostProcessor.NOTEHEAD;
            }
        if (separateDash)
            for (int x = 104; x <= 119; x++) for (int y = 126; y <= 127; y++) gray[y * w + x] = 0;
        return NoteArticulationDetector.detect(
                labels, gray, w, h, List.of(new NoteArticulationDetector.Anchor(112, 150, 16, 0)))[
                0];
    }

    @Test
    public void accentArmIsNotAlsoTenuto() {
        assertEquals(NoteArticulation.ACCENT, detect(false, true));
    }

    @Test
    public void independentTenutoIsPreserved() {
        assertEquals(NoteArticulation.ACCENT | NoteArticulation.TENUTO, detect(true, true));
    }

    @Test
    public void unclaimedAccentCannotSuppressDarkDash() {
        assertEquals(NoteArticulation.TENUTO, detect(false, false));
    }
}
