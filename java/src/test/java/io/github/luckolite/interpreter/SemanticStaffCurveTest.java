// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original curves with a misclassified crest; raw rules remain independently visible. */
public class SemanticStaffCurveTest {
    private boolean arc(int mode, boolean below) throws Exception {
        int width = 320, height = 260, left = 40, right = 280;
        float center = 140, gap = 20;
        int side = below ? 1 : -1;
        byte[] gray = new byte[width * height], labels = new byte[width * height];
        Arrays.fill(gray, (byte) 255);
        for (int distance : new int[] {40, 60})
            for (int x = 0; x < width; x++) {
                int y = Math.round(center + side * distance);
                gray[y * width + x] = 0;
                labels[y * width + x] = 4;
            }
        if (mode != 1)
            for (int x = left; x <= right; x++) {
                float t = (x - left) / (float) (right - left);
                if (mode == 3 && t > .55f || mode == 4 && t > .42f && t < .58f) continue;
                float bend = mode == 2 ? 20 * t : mode == 5 ? 0 : 20 * 4 * t * (1 - t);
                int y = Math.round(center + side * (10 + bend));
                gray[y * width + x] = 0;
                labels[y * width + x] = (byte) (t > .28f && t < .72f ? 4 : 5);
            }
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "hasContinuousTieArc",
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
    public void curvedRawCrestAboveIsNotErasedByStaffLabel() throws Exception {
        assertTrue(arc(0, false));
    }

    @Test
    public void curvedRawCrestBelowIsNotErasedByStaffLabel() throws Exception {
        assertTrue(arc(0, true));
    }

    @Test
    public void completeStaffRulesAloneDoNotTie() throws Exception {
        assertFalse(arc(1, false));
    }

    @Test
    public void slopedBeamDoesNotReturn() throws Exception {
        assertFalse(arc(2, false));
    }

    @Test
    public void oneShoulderCannotCompleteCurve() throws Exception {
        assertFalse(arc(3, false));
    }

    @Test
    public void missingMiddleCannotBeInventedFromLabel() throws Exception {
        assertFalse(arc(4, false));
    }

    @Test
    public void partialStraightRuleStillDoesNotTie() throws Exception {
        assertFalse(arc(5, false));
    }
}
