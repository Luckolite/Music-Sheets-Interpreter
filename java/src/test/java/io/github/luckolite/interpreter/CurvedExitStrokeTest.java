// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Independently drawn departure curves, ordinary small notes, and staff interruptions. */
public class CurvedExitStrokeTest {
    static final int W = 420, H = 240;
    static final float G = 16, BOTTOM = 162;

    record Box(int l, int t, int r, int b, float x, float y) {}

    static final Box MAIN = new Box(84, 106, 112, 122, 98, 114);
    static final Box TIP = new Box(120, 98, 133, 110, 126.5f, 104);

    static class Page {
        final byte[] gray = new byte[W * H];

        Page(boolean rules, boolean hollow, float slope) {
            Arrays.fill(gray, (byte) 175);
            if (rules)
                for (int x = 0; x < W; x++)
                    for (int line = 0; line < 5; line++)
                        for (int dy = -1; dy <= 1; dy++)
                            dot(
                                    x,
                                    Math.round(BOTTOM - 4 * G + line * G + slope * (x - TIP.x))
                                            + dy,
                                    0);
            for (int y = MAIN.t; y <= MAIN.b; y++)
                for (int x = MAIN.l; x <= MAIN.r; x++) {
                    double radius = Math.pow((x - MAIN.x) / 14, 2) + Math.pow((y - MAIN.y) / 8, 2);
                    if (radius <= 1 && (!hollow || radius >= .42)) dot(x, y, 20);
                }
        }

        void dot(int x, int y, int value) {
            if (x >= 0 && x < W && y >= 0 && y < H) gray[y * W + x] = (byte) value;
        }

        void stroke(int x1, int y1, int x2, int y2, int radius) {
            int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) * 3;
            for (int i = 0; i <= steps; i++) {
                float t = steps == 0 ? 0 : i / (float) steps;
                int x = Math.round(x1 + (x2 - x1) * t), y = Math.round(y1 + (y2 - y1) * t);
                for (int dy = -radius; dy <= radius; dy++)
                    for (int dx = -radius; dx <= radius; dx++)
                        if (dx * dx + dy * dy <= radius * radius) dot(x + dx, y + dy, 20);
            }
        }

        void curve() {
            stroke(120, 106, 128, 104, 2);
            stroke(128, 104, 135, 99, 2);
            stroke(135, 99, 140, 90, 2);
            stroke(140, 90, 141, 84, 2);
        }

        boolean matches(Box tip) throws Exception {
            Class<?> helper;
            try {
                helper =
                        Class.forName(
                                OmrScoreInterpreter.class.getPackageName() + ".CurvedExitInk");
            } catch (ClassNotFoundException absent) {
                return false;
            }
            Class<?> box = Class.forName(helper.getName() + "$Box");
            Constructor<?> ctor =
                    box.getDeclaredConstructor(
                            int.class, int.class, int.class, int.class, float.class, float.class);
            ctor.setAccessible(true);
            Object a = ctor.newInstance(tip.l, tip.t, tip.r, tip.b, tip.x, tip.y),
                    b = ctor.newInstance(MAIN.l, MAIN.t, MAIN.r, MAIN.b, MAIN.x, MAIN.y);
            Method m =
                    helper.getDeclaredMethod(
                            "matches",
                            byte[].class,
                            int.class,
                            int.class,
                            box,
                            box,
                            float.class,
                            float.class);
            m.setAccessible(true);
            return (boolean) m.invoke(null, gray, W, H, a, b, BOTTOM, G);
        }
    }

    @Test
    public void completeCurveAfterHollowHeadIsRecognized() throws Exception {
        Page p = new Page(true, true, 0);
        p.curve();
        assertTrue(p.matches(TIP));
    }

    @Test
    public void slopingRulesDoNotBecomeCurveBranches() throws Exception {
        Page p = new Page(true, true, -.06f);
        p.curve();
        assertTrue(p.matches(TIP));
    }

    @Test
    public void oppositeRuleSlopeKeepsTheSameDeparture() throws Exception {
        Page p = new Page(true, true, .06f);
        p.curve();
        assertTrue(p.matches(TIP));
    }

    @Test
    public void noContinuingStaffFrameKeepsTheCandidate() throws Exception {
        Page p = new Page(false, true, 0);
        p.curve();
        assertFalse(p.matches(TIP));
    }

    @Test
    public void aFilledPrincipalDoesNotSupplyHollowOwnership() throws Exception {
        Page p = new Page(true, false, 0);
        p.curve();
        assertFalse(p.matches(TIP));
    }

    @Test
    public void ordinarySmallNoteOvalRemainsANote() throws Exception {
        Page p = new Page(true, true, 0);
        for (int y = 98; y <= 110; y++)
            for (int x = 120; x <= 133; x++)
                if (Math.pow((x - 126.5) / 7, 2) + Math.pow((y - 104.) / 6, 2) <= 1)
                    p.dot(x, y, 20);
        assertFalse(p.matches(TIP));
    }

    @Test
    public void aSmallHeadWithStraightStemIsPreserved() throws Exception {
        Page p = new Page(true, true, 0);
        p.curve();
        p.stroke(133, 98, 133, 65, 0);
        assertFalse(p.matches(TIP));
    }

    @Test
    public void aStraightDepartureIsNotTheCurvedGlyph() throws Exception {
        Page p = new Page(true, true, 0);
        p.stroke(120, 106, 141, 84, 2);
        assertFalse(p.matches(TIP));
    }

    @Test
    public void aShortHookIsNotACompleteDeparture() throws Exception {
        Page p = new Page(true, true, 0);
        p.stroke(120, 106, 128, 104, 2);
        assertFalse(p.matches(TIP));
    }

    @Test
    public void aSecondBranchIsNotAThinSingleCurve() throws Exception {
        Page p = new Page(true, true, 0);
        p.curve();
        p.stroke(124, 108, 142, 108, 1);
        assertFalse(p.matches(TIP));
    }

    @Test
    public void curveWithoutSeparatedOwnerRetainsTheCandidate() throws Exception {
        Page p = new Page(true, true, 0);
        p.curve();
        assertFalse(p.matches(new Box(108, 98, 121, 110, 114.5f, 104)));
    }

    @Test
    public void originalPixelsAreNotRewritten() throws Exception {
        Page p = new Page(true, true, 0);
        p.curve();
        byte[] before = p.gray.clone();
        p.matches(TIP);
        assertArrayEquals(before, p.gray);
    }

    @Test
    public void tieInkExcludesOnlyTheOwnedCurveAndPreservesRealArcs() throws Exception {
        Page p = new Page(true, true, 0);
        p.curve();
        byte[] labels = new byte[W * H];
        // Independent tie ink elsewhere on the same raster remains untouched.
        p.stroke(210, 70, 240, 67, 1);
        p.stroke(240, 67, 270, 70, 1);
        for (int y = 65; y <= 72; y++)
            for (int x = 210; x <= 270; x++)
                if ((p.gray[y * W + x] & 255) < 100) labels[y * W + x] = 2;
        byte[] originalGray = p.gray.clone(), originalLabels = labels.clone();
        Class<?> helper;
        try {
            helper = Class.forName(OmrScoreInterpreter.class.getPackageName() + ".CurvedExitInk");
        } catch (ClassNotFoundException absent) {
            fail("No owned-curve tie exclusion in parent");
            return;
        }
        Class<?> box = Class.forName(helper.getName() + "$Box");
        Constructor<?> ctor =
                box.getDeclaredConstructor(
                        int.class, int.class, int.class, int.class, float.class, float.class);
        ctor.setAccessible(true);
        Object tip = ctor.newInstance(TIP.l, TIP.t, TIP.r, TIP.b, TIP.x, TIP.y);
        Object main = ctor.newInstance(MAIN.l, MAIN.t, MAIN.r, MAIN.b, MAIN.x, MAIN.y);
        Method find =
                helper.getDeclaredMethod(
                        "find",
                        byte[].class,
                        int.class,
                        int.class,
                        box,
                        box,
                        float.class,
                        float.class);
        find.setAccessible(true);
        Object mark = find.invoke(null, p.gray, W, H, tip, main, BOTTOM, G);
        assertNotNull(mark);
        Method pixels = mark.getClass().getDeclaredMethod("pixels");
        pixels.setAccessible(true);
        int[] owned = (int[]) pixels.invoke(mark);
        assertTrue(owned.length > 20);
        for (int at : owned) labels[at] = 1;
        originalLabels = labels.clone();
        Method exclude =
                helper.getDeclaredMethod(
                        "withoutOwnedCurves", byte[].class, byte[].class, List.class);
        exclude.setAccessible(true);
        Object masked = exclude.invoke(null, labels, p.gray, List.of(mark));
        Method getGray = masked.getClass().getDeclaredMethod("gray"),
                getLabels = masked.getClass().getDeclaredMethod("labels");
        getGray.setAccessible(true);
        getLabels.setAccessible(true);
        byte[] gray = (byte[]) getGray.invoke(masked), clean = (byte[]) getLabels.invoke(masked);
        for (int at : owned) {
            assertEquals(255, gray[at] & 255);
            assertEquals(0, clean[at]);
        }
        for (int y = 65; y <= 72; y++)
            for (int x = 210; x <= 270; x++) {
                int at = y * W + x;
                assertEquals(originalGray[at], gray[at]);
                assertEquals(originalLabels[at], clean[at]);
            }
        assertEquals(originalGray[98 * W + 250], gray[98 * W + 250]);
        assertArrayEquals(originalGray, p.gray);
        assertArrayEquals(originalLabels, labels);
    }
}
