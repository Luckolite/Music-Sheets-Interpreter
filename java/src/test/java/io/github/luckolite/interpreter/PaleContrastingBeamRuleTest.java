// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original finite beams over thin photocopied rules with locally shaded paper. */
public class PaleContrastingBeamRuleTest {
    private static final int W = 720, H = 200;
    private final byte[] gray = new byte[W * H];

    private void rectangle(int l, int r, int t, int b, int shade) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) gray[y * W + x] = (byte) shade;
    }

    private void setup(int paper, int rule, int thickness) {
        Arrays.fill(gray, (byte) paper);
        for (int y = 84; y <= 148; y += 16) rectangle(0, W - 1, y, y + thickness - 1, rule);
        rectangle(96, 624, 80, 86, 40);
    }

    private boolean finite() throws Exception {
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var ctor = type.getDeclaredConstructor(float.class, float.class, float.class);
        ctor.setAccessible(true);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "finiteBeamOverRule",
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        type,
                        int.class);
        method.setAccessible(true);
        return (boolean)
                method.invoke(null, gray, W, H, 352, 80, 86, ctor.newInstance(84f, 148f, 16f), 165);
    }

    @Test
    public void contrastingPaleRuleBeyondBothEnds() throws Exception {
        setup(250, 224, 2);
        assertTrue(finite());
    }

    @Test
    public void localPaperSetsTheContrast() throws Exception {
        setup(254, 228, 2);
        assertTrue(finite());
    }

    @Test
    public void gentlyVaryingPaleRuleStaysContinuous() throws Exception {
        setup(250, 224, 2);
        for (int x = 0; x < W; x++) rectangle(x, x, 84, 85, 222 + (x / 7) % 5);
        rectangle(96, 624, 80, 86, 40);
        assertTrue(finite());
    }

    @Test
    public void palePaperTextureHasInsufficientContrast() throws Exception {
        setup(250, 230, 2);
        assertFalse(finite());
    }

    @Test
    public void absentRuleDoesNotBecomeAContinuingWitness() throws Exception {
        setup(250, 250, 2);
        assertFalse(finite());
    }

    @Test
    public void broadPaleStripeIsNotAThinRule() throws Exception {
        setup(250, 224, 5);
        assertFalse(finite());
    }

    @Test
    public void oneEndWithContrastingRuleIsInsufficient() throws Exception {
        setup(250, 224, 2);
        rectangle(0, 95, 80, 86, 40);
        assertFalse(finite());
    }

    @Test
    public void unboundedThickBodyIsInsufficient() throws Exception {
        setup(250, 224, 2);
        rectangle(0, W - 1, 80, 86, 40);
        assertFalse(finite());
    }

    @Test
    public void inputsRemainUnchanged() throws Exception {
        setup(250, 224, 2);
        byte[] before = gray.clone();
        finite();
        assertArrayEquals(before, gray);
    }

    @Test
    public void threePixelAntialiasedRuleStillProvesBothEnds() throws Exception {
        setup(250, 224, 1);
        rectangle(0, W - 1, 85, 86, 227);
        rectangle(96, 624, 80, 86, 40);
        assertTrue(finite());
    }

    @Test
    public void twoStaffOverlaidBandsRemainSixteenths() throws Exception {
        setup(250, 224, 2);
        rectangle(96, 624, 96, 102, 40);
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var ctor = type.getDeclaredConstructor(float.class, float.class, float.class);
        ctor.setAccessible(true);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "thickNonHeadBands",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        type);
        method.setAccessible(true);
        assertEquals(
                2,
                (int)
                        method.invoke(
                                null,
                                gray,
                                new byte[W * H],
                                W,
                                H,
                                352,
                                76,
                                108,
                                ctor.newInstance(84f, 148f, 16f)));
    }

    @Test
    public void shadedPaperDoesNotBorrowTheWhitePageThreshold() throws Exception {
        setup(230, 224, 2);
        assertFalse(finite());
    }

    @Test
    public void isolatedWhitePixelsDoNotSupplyPaperContrast() throws Exception {
        setup(230, 224, 2);
        rectangle(0, W - 1, 92, 92, 255);
        assertFalse(finite());
    }

    @Test
    public void disconnectedRuleDoesNotProveAnEndpoint() throws Exception {
        setup(250, 224, 2);
        rectangle(625, 632, 80, 88, 250);
        assertFalse(finite());
    }

    @Test
    public void unrelatedFarRuleDoesNotProveAnEndpoint() throws Exception {
        Arrays.fill(gray, (byte) 250);
        rectangle(0, W - 1, 91, 92, 224);
        rectangle(96, 624, 80, 86, 40);
        assertFalse(finite());
    }
}
