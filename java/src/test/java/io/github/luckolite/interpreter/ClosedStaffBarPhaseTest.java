// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

/** Original synthetic raw staff geometry; contains no score pixels. */
public class ClosedStaffBarPhaseTest {
    private static final int W = 340, H = 260, BAR = 260;
    private static final float GAP = 14, BOTTOM = 180, X = 190;

    private static byte[] page(float slope, boolean bar, int missing, boolean extra) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 176);
        for (int line = 0; line < 5; line++)
            if (line != missing)
                for (int x = 70; x <= BAR; x++) {
                    int y = Math.round(BOTTOM - line * GAP + slope * (x - X));
                    gray[y * W + x] = 70;
                    gray[(y + 1) * W + x] = 70;
                }
        if (extra)
            for (int x = 70; x < BAR - 25; x++) {
                int y = Math.round(BOTTOM - 5 * GAP + slope * (x - X));
                gray[y * W + x] = 70;
                gray[(y + 1) * W + x] = 70;
            }
        if (bar) {
            int b = Math.round(BOTTOM + slope * (BAR - X));
            for (int y = b - 56; y <= b + 1; y++)
                for (int dx = 0; dx <= 1; dx++) gray[y * W + BAR + dx] = 30;
        }
        return gray;
    }

    private static float[] resolve(byte[] gray, float x, float step, float slope) {
        float bottom = BOTTOM + slope * (x - X), y = bottom - step * GAP / 2;
        return ClosedStaffBarPhase.resolve(
                gray, W, H, x, y, Math.round(x) - 8, Math.round(x) + 8, bottom + GAP, GAP);
    }

    private static void exact(byte[] gray, float x, float step, float slope) {
        float[] fit = resolve(gray, x, step, slope);
        assertNotNull(fit);
        float bottom = BOTTOM + slope * (x - X), y = bottom - step * GAP / 2;
        assertEquals(bottom, fit[0], 1.5f);
        assertEquals(GAP, fit[1], .4f);
        assertEquals(Math.round(step), Math.round((fit[0] - y) * 2 / fit[1]));
    }

    @Test
    public void closedSlopingStaffProvesAllFiveRulesOnBothSides() {
        for (float slope : new float[] {-.16f, -.10f, 0, .10f, .16f}) {
            byte[] gray = page(slope, true, -1, false);
            exact(gray, 190, 8, slope);
            exact(gray, 230, 7, slope);
        }
    }

    @Test
    public void earlierEndingParallelSlurCannotReplaceJoinedRules() {
        byte[] gray = page(-.10f, true, -1, true);
        exact(gray, 190, 8, -.10f);
        exact(gray, 230, 7, -.10f);
    }

    @Test
    public void realPitchPathUsesClosedRawRulesDespiteMissingSemanticStripes() throws Exception {
        float slope = -.10f, x = 190, y = 124, bottom = 180;
        byte[] gray = page(slope, true, -1, true), labels = new byte[W * H];
        var staffType = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var constructor = staffType.getDeclaredConstructor(float.class, float.class, float.class);
        constructor.setAccessible(true);
        var staff = constructor.newInstance(bottom + GAP - 4 * GAP, bottom + GAP, GAP);
        var headType = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var headConstructor = headType.getDeclaredConstructors()[0];
        headConstructor.setAccessible(true);
        var head = headConstructor.newInstance(200, 182, 198, 119, 129, x, y);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "localStaffPitch",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        staffType,
                        headType);
        method.setAccessible(true);
        float[] fit = (float[]) method.invoke(null, labels, gray, W, H, staff, head);
        assertEquals(8, Math.round((fit[0] - y) * 2 / fit[1]));
    }

    @Test
    public void openStaffCannotEstablishClosingPhase() {
        assertNull(resolve(page(-.10f, false, -1, true), 190, 8, -.10f));
    }

    @Test
    public void fourRulesAndAnIsolatedBarCannotSupplyMissingRule() {
        for (int line = 0; line < 5; line++)
            assertNull(resolve(page(-.10f, true, line, false), 190, 8, -.10f));
    }

    @Test
    public void extendedVerticalStemCannotSupplyClosingBar() {
        byte[] gray = page(0, true, -1, false);
        for (int y = 100; y <= 200; y++)
            for (int dx = 0; dx <= 1; dx++) gray[y * W + BAR + dx] = 30;
        assertNull(resolve(gray, 190, 8, 0));
    }

    @Test
    public void staffNeedsPhysicalRulesOnBothSidesOfHead() {
        byte[] gray = page(0, true, -1, false);
        for (int y = 100; y < 200; y++)
            for (int x = 200; x < 250; x++) gray[y * W + x] = (byte) 176;
        assertNull(resolve(gray, 190, 8, 0));
    }

    @Test
    public void sixthJoinedRuleWithExtendedBarIsAmbiguous() {
        byte[] gray = page(0, true, -1, false);
        for (int x = 70; x <= BAR; x++) gray[110 * W + x] = 70;
        for (int y = 110; y <= 181; y++)
            for (int dx = 0; dx <= 1; dx++) gray[y * W + BAR + dx] = 30;
        assertNull(resolve(gray, 190, 8, 0));
    }

    @Test
    public void lowContrastPaperTextureCannotSupplyPhysicalRules() {
        byte[] gray = page(0, true, -1, false);
        for (int i = 0; i < gray.length; i++) if ((gray[i] & 255) == 70) gray[i] = (byte) 163;
        assertNull(resolve(gray, 190, 8, 0));
    }

    @Test
    public void invalidInputIsRejectedAndCallerPixelsArePreserved() {
        byte[] gray = page(-.10f, true, -1, true), before = gray.clone();
        exact(gray, 190, 8, -.10f);
        assertArrayEquals(before, gray);
        assertNull(ClosedStaffBarPhase.resolve(null, W, H, 190, 124, 182, 198, 194, 14));
        assertNull(ClosedStaffBarPhase.resolve(gray, W, H, Float.NaN, 124, 182, 198, 194, 14));
        assertNull(ClosedStaffBarPhase.resolve(gray, W, H, 190, 124, 182, 198, 194, Float.NaN));
        assertNull(ClosedStaffBarPhase.resolve(new byte[1], W, H, 190, 124, 182, 198, 194, 14));
        assertNull(ClosedStaffBarPhase.resolve(gray, W, H, 190, 124, -1, 198, 194, 14));
        assertNull(ClosedStaffBarPhase.resolve(gray, W, H, 190, 124, 182, W, 194, 14));
        assertNull(
                ClosedStaffBarPhase.resolve(gray, W, H, Float.MAX_VALUE, 124, 182, 198, 194, 14));
        assertNull(
                ClosedStaffBarPhase.resolve(gray, W, H, 190, Float.MAX_VALUE, 182, 198, 194, 14));
    }
}
