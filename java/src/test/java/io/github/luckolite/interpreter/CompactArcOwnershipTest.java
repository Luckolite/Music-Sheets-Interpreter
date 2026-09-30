// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original parametric strokes, independent of any published sheet. */
public final class CompactArcOwnershipTest {
    private boolean detect(float gap, int side, boolean longSpan, boolean deep) throws Exception {
        int width = 360, height = 200, left = 35;
        int right = left + Math.round(gap * (longSpan ? 8 : 2.2f));
        float center = 100;
        byte[] gray = new byte[width * height], labels = new byte[width * height];
        Arrays.fill(gray, (byte) 255);
        for (int x = left; x <= right; x++) {
            float t = (x - left) / (float) (right - left);
            int y =
                    Math.round(
                            center
                                    + side
                                            * gap
                                            * (deep
                                                    ? .85f + .95f * 4 * t * (1 - t)
                                                    : .3f + .55f * 4 * t * (1 - t)));
            for (int dy = -1; dy <= 1; dy++) {
                gray[(y + dy) * width + x] = 0;
                labels[(y + dy) * width + x] = OmrMeasurePostProcessor.SYMBOL;
            }
        }
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "hasPrintedTieArc",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        float.class,
                        float.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, labels, gray, width, height, left, right, center, gap);
    }

    @Test
    public void deepCompactForeignGlyphDoesNotJoinHeads() throws Exception {
        for (float gap : new float[] {16, 20, 24})
            for (int side : new int[] {-1, 1}) assertFalse(detect(gap, side, false, true));
    }

    @Test
    public void compactReturningTiesRemainAboveAndBelow() throws Exception {
        for (float gap : new float[] {16, 20, 24})
            for (int side : new int[] {-1, 1}) assertTrue(detect(gap, side, false, false));
    }

    @Test
    public void longDeepReturningTiesAreNotRestricted() throws Exception {
        for (float gap : new float[] {16, 20, 24})
            for (int side : new int[] {-1, 1}) assertTrue(detect(gap, side, true, true));
    }
}
