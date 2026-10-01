// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

/** Original five-rule geometry with a middle rule covered by a thick beam. */
public class InteriorBeamStaffPhaseTest {
    private static final int W = 240, H = 200, LEFT = 110, RIGHT = 130;
    private static final float X = 120, GAP = 14, BOTTOM = 150;

    private record Page(byte[] labels, byte[] gray) {}

    private static Page page(int covered, float slope, boolean thick) {
        byte[] labels = new byte[W * H], gray = new byte[W * H];
        Arrays.fill(gray, (byte) 176);
        for (int line = 0; line < 5; line++)
            for (int x = 40; x < 200; x++) {
                int y = Math.round(BOTTOM - line * GAP + slope * (x - X));
                if (line == covered && thick)
                    for (int dy = -4; dy <= 4; dy++) gray[(y + dy) * W + x] = 20;
                else if (line != covered) {
                    gray[y * W + x] = 70;
                    labels[y * W + x] = 4;
                }
            }
        return new Page(labels, gray);
    }

    private static float[] resolve(Page p) {
        return BeamOccludedStaffPhase.resolve(
                p.labels, p.gray, W, H, X, LEFT, RIGHT, BOTTOM + 8, GAP);
    }

    private static void exact(Page p) {
        float[] found = resolve(p);
        assertNotNull(found);
        assertEquals(BOTTOM, found[0], 1f);
        assertEquals(GAP, found[1], .35f);
    }

    private static float[] localFlatPitch(Page p) throws Exception {
        var staffType = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var staffConstructor =
                staffType.getDeclaredConstructor(float.class, float.class, float.class);
        staffConstructor.setAccessible(true);
        var staff = staffConstructor.newInstance(BOTTOM + 10 - GAP * 4, BOTTOM + 10, GAP);
        var headType = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var headConstructor = headType.getDeclaredConstructors()[0];
        headConstructor.setAccessible(true);
        var head = headConstructor.newInstance(200, LEFT, RIGHT, 61, 71, X, 66f);
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
        return (float[]) method.invoke(null, p.labels, p.gray, W, H, staff, head);
    }

    @Test
    public void flatShadedStaffWithCoveredMiddleRuleKeepsLedgerPitch() throws Exception {
        float[] local = localFlatPitch(page(2, 0, true));
        assertEquals(BOTTOM, local[0], 1f);
        assertEquals(12, Math.round((local[0] - 66f) * 2 / local[1]));
    }

    @Test
    public void thickBeamCanCoverAnyInteriorRule() {
        for (int line = 1; line < 4; line++) exact(page(line, 0, true));
    }

    @Test
    public void slantedInteriorCoverageKeepsBothOuterRules() {
        for (float slope : new float[] {-.08f, -.04f, .04f, .08f}) exact(page(2, slope, true));
    }

    @Test
    public void missingMiddleWithoutThickBeamCannotRephase() {
        assertNull(resolve(page(2, 0, false)));
    }

    @Test
    public void noSemanticRulesCannotRephase() {
        Page p = page(2, 0, true);
        Arrays.fill(p.labels, (byte) 0);
        assertNull(resolve(p));
    }

    @Test
    public void sixthRuleMakesStaffPhaseAmbiguous() {
        Page p = page(2, 0, true);
        for (int x = 40; x < 200; x++) {
            p.gray[(int) (BOTTOM + GAP) * W + x] = 70;
            p.labels[(int) (BOTTOM + GAP) * W + x] = 4;
        }
        assertNull(resolve(p));
    }

    @Test
    public void inputPixelsAndLabelsArePreserved() {
        Page p = page(2, 0, true);
        byte[] labels = p.labels.clone(), gray = p.gray.clone();
        exact(p);
        assertArrayEquals(labels, p.labels);
        assertArrayEquals(gray, p.gray);
    }
}
