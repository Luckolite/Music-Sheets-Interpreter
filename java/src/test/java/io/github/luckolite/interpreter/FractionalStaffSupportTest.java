// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original thin pale rules with quantized coverage at a fractional sample threshold. */
public class FractionalStaffSupportTest {
    static final int W = 803, H = 180;

    byte[] page(int firstLength, boolean sixth, boolean thick) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int line = 0; line < (sixth ? 6 : 5); line++) {
            int length = line == 0 ? firstLength : 675;
            for (int dy = thick ? -3 : 0; dy <= (thick ? 3 : 0); dy++)
                for (int x = 80; x < 80 + length; x++)
                    gray[(40 + line * 15 + dy) * W + x] = (byte) 210;
        }
        return gray;
    }

    boolean proof(byte[] gray) throws Exception {
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "completeContrastedFadedStaff",
                        byte[].class,
                        int.class,
                        int.class,
                        RawStaffLineDetector.StaffLines.class,
                        float.class);
        method.setAccessible(true);
        return (boolean)
                method.invoke(
                        null,
                        gray,
                        W,
                        H,
                        new RawStaffLineDetector.StaffLines(new int[] {40, 55, 70, 85, 100}, 15),
                        0f);
    }

    @Test
    public void nearestWholeSampleMeetsFractionalCoverage() throws Exception {
        assertTrue(proof(page(506, false, false)));
    }

    @Test
    public void oneSampleBelowRoundedCoverageFails() throws Exception {
        assertFalse(proof(page(505, false, false)));
    }

    @Test
    public void missingRuleFails() throws Exception {
        assertFalse(proof(page(0, false, false)));
    }

    @Test
    public void sixthRuleStillFails() throws Exception {
        assertFalse(proof(page(506, true, false)));
    }

    @Test
    public void thickBandsStillFail() throws Exception {
        assertFalse(proof(page(506, false, true)));
    }

    @Test
    public void proofPreservesPixels() throws Exception {
        byte[] gray = page(506, false, false), copy = gray.clone();
        proof(gray);
        assertArrayEquals(copy, gray);
    }
}
