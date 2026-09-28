// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original beam-occluded local raster; tests the already-proved frame contract. */
public class ProvedOccludedPhaseTest {
    private static final int W = 400, H = 260;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    private void page(boolean beam) {
        Arrays.fill(gray, (byte) 170);
        for (int x = 35; x <= 337; x++)
            for (int line = 0; line < 5; line++) {
                int row = Math.round(204 + Math.max(0, x - 240) * .065f - line * 16.5f);
                for (int y = row; y <= row + 1; y++) {
                    gray[y * W + x] = 60;
                    labels[y * W + x] = 4;
                }
            }
        if (beam)
            for (int x = 247; x <= 288; x++) {
                int row = Math.round(204 + Math.max(0, x - 240) * .065f - 4 * 16.5f);
                for (int y = row - 12; y <= row - 4; y++) gray[y * W + x] = 10;
            }
    }

    private float[] local(float gap, boolean proved) throws Exception {
        var st = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var sc = st.getDeclaredConstructor(float.class, float.class, float.class);
        sc.setAccessible(true);
        Object staff = sc.newInstance(208 - gap * 4, 208f, gap);
        var flag = st.getDeclaredField("printedPhase");
        flag.setAccessible(true);
        flag.setBoolean(staff, proved);
        var ht = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var hc = ht.getDeclaredConstructors()[0];
        hc.setAccessible(true);
        var head = hc.newInstance(380, 289, 315, 183, 201, 302f, 192.2f);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "localStaffPitch",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        st,
                        ht);
        method.setAccessible(true);
        return (float[]) method.invoke(null, labels, gray, W, H, staff, head);
    }

    @Test
    public void inferredRuleCannotRescaleAlreadyProvedFrame() throws Exception {
        page(true);
        assertArrayEquals(
                new float[] {208.5f, 16.5f},
                StaffPitchTrack.localOccludedRules(labels, gray, W, H, 302, 289, 315, 208, 15.5f),
                .01f);
        assertArrayEquals(new float[] {208, 15.5f}, local(15.5f, true), .01f);
    }

    @Test
    public void unprovedSeedStillAcceptsLocalEvidence() throws Exception {
        page(true);
        assertArrayEquals(new float[] {208.5f, 16.5f}, local(15.5f, false), .01f);
    }

    @Test
    public void smallSpacingDifferenceStillAcceptsLocalEvidence() throws Exception {
        page(true);
        assertArrayEquals(new float[] {208.5f, 16.5f}, local(16, true), .01f);
    }

    @Test
    public void completeLocalRulesAreNotVetoed() throws Exception {
        Arrays.fill(gray, (byte) 255);
        for (int x = 0; x < W; x++)
            for (int line = 0; line < 5; line++) {
                int y = Math.round(208 - line * 16.5f);
                gray[y * W + x] = 20;
                labels[y * W + x] = 4;
            }
        float[] complete =
                StaffPitchTrack.localPrintedRules(labels, gray, W, H, 302, 289, 315, 208, 15.5f);
        assertNotNull("Fixture must reach complete-rule path", complete);
        assertArrayEquals(complete, local(15.5f, true), .01f);
    }

    @Test
    public void noSourceArraysAreModified() throws Exception {
        page(true);
        var g = gray.clone();
        var l = labels.clone();
        local(15.5f, true);
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }
}
