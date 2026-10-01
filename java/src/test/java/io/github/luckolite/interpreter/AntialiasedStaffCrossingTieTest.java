// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original returning curves with antialiased shoulders and occluding staff rules. */
public class AntialiasedStaffCrossingTieTest {
    private boolean arc(int mode, boolean below) throws Exception {
        int width = 360, height = 240, left = 60, right = 285;
        float center = 120, gap = 18;
        int side = below ? 1 : -1;
        byte[] gray = new byte[width * height], labels = new byte[width * height];
        Arrays.fill(gray, (byte) 255);
        for (int dy = -2; dy <= 2; dy++)
            for (int x = 0; x < width; x++) {
                int y = Math.round(center + side * 27) + dy;
                if (Math.abs(dy) <= 1) gray[y * width + x] = 0;
                labels[y * width + x] = 4;
            }
        if (mode != 1)
            for (int x = left; x <= right; x++) {
                float t = (x - left) / (float) (right - left);
                if (mode == 3 && t > .55f || mode == 4 && t > .42f && t < .58f) continue;
                float bend = mode == 2 ? 17.1f * t : 17.1f * 4 * t * (1 - t);
                int y = Math.round(center + side * (17.1f + bend));
                int shade = mode == 5 || t < .08f || t > .92f ? 195 : 0;
                for (int dy = 0; dy <= 0; dy++) {
                    int at = (y + dy) * width + x;
                    gray[at] = (byte) Math.min(gray[at] & 255, shade);
                    if (labels[at] != 4) labels[at] = 5;
                }
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
                        float.class,
                        Class.forName(OmrScoreInterpreter.class.getName() + "$Component"),
                        int.class);
        method.setAccessible(true);
        return (boolean)
                method.invoke(
                        null, labels, gray, width, height, left, right, center, gap, null, 205);
    }

    @Test
    public void darkCoreAndPaleReturningShouldersAboveRuleRemainTie() throws Exception {
        assertTrue(arc(0, false));
    }

    @Test
    public void darkCoreAndPaleReturningShouldersBelowRuleRemainTie() throws Exception {
        assertTrue(arc(0, true));
    }

    @Test
    public void ruleAloneCannotSupplyReturningShoulders() throws Exception {
        assertFalse(arc(1, false));
    }

    @Test
    public void slopedBeamCannotSupplyBothReturningShoulders() throws Exception {
        assertFalse(arc(2, false));
    }

    @Test
    public void oneShoulderCannotSupplyCompleteTie() throws Exception {
        assertFalse(arc(3, false));
    }

    @Test
    public void missingMiddleAwayFromRuleIsNotOcclusion() throws Exception {
        assertFalse(arc(4, false));
    }

    @Test
    public void uniformlyPaleCurveCannotSupplyDarkCore() throws Exception {
        assertFalse(arc(5, false));
    }
}
