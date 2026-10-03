// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PedalBracketDetectorTest {
    private static final int W = 320, H = 230;
    private static final List<PlayingTechniqueDetector.Staff> STAFFS =
            List.of(
                    new PlayingTechniqueDetector.Staff(25, 65, 10, 1, 2),
                    new PlayingTechniqueDetector.Staff(190, 230, 10, 0, 2));

    private static byte[] page(int hooks, boolean dashed, double slope, int thick) {
        var ink = new byte[W * H];
        Arrays.fill(ink, (byte) 255);
        for (int x = 45; x <= 270; x++) {
            int y = 120 + (int) Math.round((x - 45) * slope);
            if (!dashed || x % 18 < 10)
                for (int dy = 0; dy < thick; dy++) ink[(y - dy) * W + x] = 0;
        }
        for (int dy = 0; dy <= 10; dy++) {
            if ((hooks & 1) != 0) ink[(120 - dy) * W + 45] = 0;
            if ((hooks & 2) != 0) ink[(120 + (int) Math.round(225 * slope) - dy) * W + 270] = 0;
        }
        return ink;
    }

    @Test
    public void continuousTwoHookPedalIsOwnedAndPixelsArePreserved() {
        var ink = page(3, false, 0, 2);
        var before = ink.clone();
        var found = PedalBracketDetector.detect(ink, W, H, STAFFS);
        assertEquals(1, found.size());
        assertEquals(1, found.get(0).staffIndex());
        assertEquals(45, found.get(0).left(), 0);
        assertEquals(270, found.get(0).right(), 0);
        assertArrayEquals(before, ink);
    }

    @Test
    public void inclinedPedalRetainsTwoFiniteUpwardHooks() {
        var found = PedalBracketDetector.detect(page(3, false, .08, 2), W, H, STAFFS);
        assertEquals(1, found.size());
        assertEquals(.08, found.get(0).slope(), .005);
    }

    @Test
    public void octaveDashesAndOneEndedContinuationAreRejected() {
        for (int hooks : new int[] {0, 1, 2})
            assertTrue(
                    PedalBracketDetector.detect(page(hooks, false, 0, 2), W, H, STAFFS).isEmpty());
        assertTrue(PedalBracketDetector.detect(page(3, true, 0, 2), W, H, STAFFS).isEmpty());
    }

    @Test
    public void beamThicknessCannotBecomeAPedal() {
        assertTrue(PedalBracketDetector.detect(page(3, false, 0, 5), W, H, STAFFS).isEmpty());
    }

    @Test
    public void longStemsAndDownwardHooksAreRejected() {
        var ink = page(3, false, 0, 2);
        for (int dy = 0; dy <= 35; dy++) ink[(120 - dy) * W + 45] = 0;
        assertTrue(PedalBracketDetector.detect(ink, W, H, STAFFS).isEmpty());
        ink = page(0, false, 0, 2);
        for (int dy = 0; dy <= 10; dy++) {
            ink[(120 + dy) * W + 45] = 0;
            ink[(120 + dy) * W + 270] = 0;
        }
        assertTrue(PedalBracketDetector.detect(ink, W, H, STAFFS).isEmpty());
    }

    @Test
    public void bracketCrossingNextStaffCannotAcquirePriorOwnership() {
        var close = List.of(STAFFS.get(0), new PlayingTechniqueDetector.Staff(115, 155, 10, 0, 2));
        assertTrue(PedalBracketDetector.detect(page(3, false, 0, 2), W, H, close).isEmpty());
    }

    @Test
    public void malformedRasterIsRejected() {
        assertTrue(PedalBracketDetector.detect(null, W, H, STAFFS).isEmpty());
        assertTrue(PedalBracketDetector.detect(new byte[8], W, H, STAFFS).isEmpty());
    }
}
