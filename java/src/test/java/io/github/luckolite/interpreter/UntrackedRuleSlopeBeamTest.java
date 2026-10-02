// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original tilted rules with a deliberately flat pitch frame and separate beams. */
public class UntrackedRuleSlopeBeamTest {
    static final int W = 480, H = 240, X = 240;
    static final float GAP = 16, TOP = 70;

    static class Page {
        final byte[] gray = new byte[W * H], labels = new byte[W * H];
        final float slope;

        Page(float slope, int rules, int thickness, boolean both) {
            this(slope, rules, thickness, both, 0);
        }

        Page(float slope, int rules, int thickness, boolean both, float phase) {
            this.slope = slope;
            Arrays.fill(gray, (byte) 175);
            for (int x = 0; x < W; x++)
                for (int line = 0; line < rules; line++) {
                    if (!both && x > X + 16 && line > 0) continue;
                    int center = Math.round(TOP + line * GAP + slope * (x - X) + phase);
                    for (int dy = -1; dy < thickness - 1; dy++) gray[(center + dy) * W + x] = 82;
                }
        }

        void beam(int line, int left, int right, int thickness) {
            for (int x = left; x <= right; x++) {
                int center = Math.round(TOP + line * GAP + slope * (x - X));
                for (int dy = -1; dy < thickness - 1; dy++) gray[(center + dy) * W + x] = 24;
            }
        }

        int count(int first, int last) throws Exception {
            Class<?> st = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
            Constructor<?> c = st.getDeclaredConstructor(float.class, float.class, float.class);
            c.setAccessible(true);
            Object staff = c.newInstance(TOP, TOP + 4 * GAP, GAP);
            Constructor<?> tc = StaffPitchTrack.class.getDeclaredConstructor(List.class);
            tc.setAccessible(true);
            Field f = st.getDeclaredField("pitchTrack");
            f.setAccessible(true);
            f.set(
                    staff,
                    tc.newInstance(
                            List.of(
                                    new float[] {0, TOP + 4 * GAP, GAP},
                                    new float[] {W, TOP + 4 * GAP, GAP})));
            Method m =
                    OmrScoreInterpreter.class.getDeclaredMethod(
                            "thickNonHeadBandsAtThreshold",
                            byte[].class,
                            byte[].class,
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            st,
                            int.class,
                            int.class);
            m.setAccessible(true);
            return (int) m.invoke(null, gray, labels, W, H, X, first, last, staff, 119, X + 2);
        }
    }

    @Test
    public void risingRuleWithFlatTrackIsNotABeam() throws Exception {
        assertEquals(0, new Page(-.1f, 5, 5, true).count(64, 78));
    }

    @Test
    public void fallingRuleWithFlatTrackIsNotABeam() throws Exception {
        assertEquals(0, new Page(.1f, 5, 5, true).count(64, 78));
    }

    @Test
    public void risingRuleRetainsWholeBodyWithShiftedFrame() throws Exception {
        assertEquals(0, new Page(-.1f, 5, 5, true, -3.5f).count(62, 78));
    }

    @Test
    public void fallingRuleRetainsWholeBodyWithShiftedFrame() throws Exception {
        assertEquals(0, new Page(.1f, 5, 5, true, -3.5f).count(62, 78));
    }

    @Test
    public void threeOtherRulesAreRequired() throws Exception {
        assertEquals(1, new Page(-.1f, 3, 5, true).count(64, 78));
    }

    @Test
    public void bothContinuingFlanksAreRequired() throws Exception {
        assertEquals(1, new Page(-.1f, 5, 5, false).count(64, 78));
    }

    @Test
    public void finiteThinBeamOnTiltedRuleIsRetained() throws Exception {
        Page p = new Page(-.08f, 5, 2, true);
        p.beam(0, 200, 280, 5);
        assertEquals(1, p.count(64, 78));
    }

    @Test
    public void thickerBeamOverTiltedRuleIsRetained() throws Exception {
        Page p = new Page(.1f, 5, 3, true);
        p.beam(0, 160, 320, 9);
        assertEquals(1, p.count(64, 82));
    }

    @Test
    public void beamBetweenRulesIsRetained() throws Exception {
        Page p = new Page(-.1f, 5, 5, true);
        for (int x = 170; x <= 310; x++) for (int y = 78; y <= 82; y++) p.gray[y * W + x] = 24;
        assertEquals(1, p.count(76, 85));
    }

    @Test
    public void sourceArraysArePreserved() throws Exception {
        Page p = new Page(-.1f, 5, 5, true);
        byte[] g = p.gray.clone(), l = p.labels.clone();
        p.count(64, 78);
        assertArrayEquals(g, p.gray);
        assertArrayEquals(l, p.labels);
    }

    @Test
    public void ruleSlopeFitIncludesTheWholeShiftedBlurBody() throws Exception {
        Page p = new Page(-.1f, 5, 5, true, -3.5f);
        Float slope = null;
        try {
            Class<?> helper =
                    Class.forName(
                            OmrScoreInterpreter.class.getPackageName() + ".BeamRuleLocalSlope");
            Method m =
                    helper.getDeclaredMethod(
                            "find",
                            byte[].class,
                            int.class,
                            int.class,
                            int.class,
                            float.class,
                            float.class,
                            int.class,
                            float.class);
            m.setAccessible(true);
            slope = (Float) m.invoke(null, p.gray, W, H, X, TOP, GAP, 119, TOP);
        } catch (ClassNotFoundException absent) {
            /* The parent has no independent witness. */
        }
        assertNotNull(slope);
        assertEquals(-.1f, slope, .02f);
    }

    @Test
    public void neighboringHeadsDoNotDemandEmptyStaffSpaces() throws Exception {
        Page p = new Page(-.1f, 5, 5, true);
        for (int line = 1; line <= 2; line++)
            for (int x = X + 16; x < X + 48; x++) {
                int center = Math.round(TOP + line * GAP + p.slope * (x - X));
                for (int dy = 7; dy <= 10; dy++) p.gray[(center + dy) * W + x] = 40;
            }
        Class<?> helper =
                Class.forName(OmrScoreInterpreter.class.getPackageName() + ".BeamRuleLocalSlope");
        Method m =
                helper.getDeclaredMethod(
                        "find",
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        float.class,
                        float.class,
                        int.class,
                        float.class);
        m.setAccessible(true);
        Float slope = (Float) m.invoke(null, p.gray, W, H, X, TOP, GAP, 119, TOP);
        assertNotNull(slope);
        assertEquals(-.1f, slope, .02f);
    }
}
