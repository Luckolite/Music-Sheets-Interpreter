// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ledger flanks with a stronger separate returning-curve shoulder. */
public class LedgerCurveFlankTest {
    private static final int W = 240, H = 280;

    private int pitch(boolean lower, boolean ledger, boolean oneSided, boolean leftCurve)
            throws Exception {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        int outer = lower ? 208 : 80, inward = lower ? -1 : 1;
        for (int y : new int[] {outer + inward * 16, outer + inward * 32})
            for (int x = 70; x <= 130; x++) gray[y * W + x] = 0;
        if (ledger)
            for (int dy = 0; dy < 2; dy++)
                for (int x = 86; x <= 114; x++)
                    if (!oneSided || x < 100) gray[(outer + inward * dy) * W + x] = 0;
        int l = leftCurve ? 84 : 110, r = leftCurve ? 90 : 116;
        for (int x = l; x <= r; x++) gray[(outer + inward * 4) * W + x] = 0;
        var hc = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = hc.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        float y = outer + inward * 1.8f;
        Object head = ctor.newInstance(190, 91, 109, Math.round(y) - 7, Math.round(y) + 7, 100f, y);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "printedPitchStep",
                        byte[].class,
                        int.class,
                        int.class,
                        hc,
                        float.class,
                        float.class);
        method.setAccessible(true);
        return (int) method.invoke(null, gray, W, H, head, 160f, 16f);
    }

    @Test
    public void upperLedgerSurvivesStrongerRightCurve() throws Exception {
        assertEquals(10, pitch(false, true, false, false));
    }

    @Test
    public void lowerLedgerSurvivesStrongerRightCurve() throws Exception {
        assertEquals(-6, pitch(true, true, false, false));
    }

    @Test
    public void strongerLeftCurveIsEquivalent() throws Exception {
        assertEquals(10, pitch(false, true, false, true));
    }

    @Test
    public void missingOuterLedgerStillResolvesPrintedSpace() throws Exception {
        assertEquals(9, pitch(false, false, false, false));
    }

    @Test
    public void oneFlankCannotProveTheOuterLedger() throws Exception {
        assertEquals(9, pitch(false, true, true, false));
    }
}
