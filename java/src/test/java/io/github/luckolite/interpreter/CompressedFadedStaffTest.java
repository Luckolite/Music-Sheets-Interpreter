// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original faded five-rule groups and independently corroborated spacing. */
public class CompressedFadedStaffTest {
    static final int W = 800, H = 1100;

    byte[] page(int lines, int shade, int thickness, int right) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int line = 0; line < lines; line++)
            for (int y = 600 + line * 15 - thickness / 2; y <= 600 + line * 15 + thickness / 2; y++)
                for (int x = 40; x < right; x++) gray[y * W + x] = (byte) shade;
        return gray;
    }

    float[] calibrate(
            byte[] gray, int witnesses, float top, float bottom, float gap, boolean established)
            throws Exception {
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var ctor = type.getDeclaredConstructor(float.class, float.class, float.class);
        ctor.setAccessible(true);
        List<Object> staffs = new ArrayList<>();
        for (int i = 0; i < witnesses; i++)
            staffs.add(ctor.newInstance(100f + i * 160, 160f + i * 160, 15f));
        var target = ctor.newInstance(top, bottom, gap);
        var phase = type.getDeclaredField("printedPhase");
        phase.setAccessible(true);
        phase.setBoolean(target, established);
        staffs.add(target);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "calibrateContrastedFadedStaffs",
                        byte[].class,
                        int.class,
                        int.class,
                        List.class,
                        float.class);
        method.setAccessible(true);
        method.invoke(null, gray, W, H, staffs, 0f);
        var pitchBottom = type.getDeclaredField("pitchBottom");
        pitchBottom.setAccessible(true);
        var pitchGap = type.getDeclaredField("pitchGap");
        pitchGap.setAccessible(true);
        return new float[] {pitchBottom.getFloat(target), pitchGap.getFloat(target)};
    }

    float[] compressed(byte[] gray, int witnesses, boolean established) throws Exception {
        return calibrate(gray, witnesses, 601, 641, 10, established);
    }

    @Test
    public void completeRulesRecoverCompressedAlias() throws Exception {
        assertArrayEquals(new float[] {660, 15}, compressed(page(5, 215, 1, 760), 3, false), .1f);
    }

    @Test
    public void missingOuterRuleCannotReassign() throws Exception {
        assertArrayEquals(new float[] {641, 10}, compressed(page(4, 215, 1, 760), 3, false), .1f);
    }

    @Test
    public void sixthRuleMakesGroupAmbiguous() throws Exception {
        assertArrayEquals(new float[] {641, 10}, compressed(page(6, 215, 1, 760), 3, false), .1f);
    }

    @Test
    public void thickBandsCannotReassign() throws Exception {
        assertArrayEquals(new float[] {641, 10}, compressed(page(5, 215, 7, 760), 3, false), .1f);
    }

    @Test
    public void shortStrokesCannotReassign() throws Exception {
        assertArrayEquals(new float[] {641, 10}, compressed(page(5, 215, 1, 300), 3, false), .1f);
    }

    @Test
    public void independentWitnessesAreRequired() throws Exception {
        assertArrayEquals(new float[] {641, 10}, compressed(page(5, 215, 1, 760), 2, false), .1f);
    }

    @Test
    public void nearWhiteRulesCannotReassign() throws Exception {
        assertArrayEquals(new float[] {641, 10}, compressed(page(5, 240, 1, 760), 3, false), .1f);
    }

    @Test
    public void establishedPrintedPhaseIsPreserved() throws Exception {
        assertArrayEquals(new float[] {641, 10}, compressed(page(5, 215, 1, 760), 3, true), .1f);
    }

    @Test
    public void neighboringSeedCannotBorrowStaff() throws Exception {
        assertArrayEquals(
                new float[] {700, 10},
                calibrate(page(5, 215, 1, 760), 3, 660, 700, 10, false),
                .1f);
    }

    @Test
    public void extremeCompressionRemainsAmbiguous() throws Exception {
        assertArrayEquals(
                new float[] {624, 6}, calibrate(page(5, 215, 1, 760), 3, 600, 624, 6, false), .1f);
    }

    @Test
    public void existingModerateCalibrationStillWorks() throws Exception {
        assertArrayEquals(
                new float[] {660, 15},
                calibrate(page(5, 215, 1, 760), 3, 604, 656, 13, false),
                .1f);
    }

    @Test
    public void pixelsRemainUnchanged() throws Exception {
        byte[] p = page(5, 215, 1, 760), before = p.clone();
        compressed(p, 3, false);
        assertArrayEquals(before, p);
    }
}
