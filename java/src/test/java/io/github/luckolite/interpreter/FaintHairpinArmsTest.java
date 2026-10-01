// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic geometry; no score pixels, names, or fingerprints. */
public final class FaintHairpinArmsTest {
    private static final int W = 640, H = 280;

    private static byte[] blank() {
        byte[] p = new byte[W * H];
        Arrays.fill(p, (byte) 255);
        return p;
    }

    private static void stroke(byte[] p, int x, int y, int ink) {
        p[y * W + x] = (byte) ink;
    }

    private static void wedge(byte[] p, boolean opening, boolean faint) {
        int left = 100, right = 540;
        for (int x = left; x <= right; x++) {
            double t = (x - left) / (double) (right - left);
            int upper = (int) Math.round(opening ? 174 - 8 * t : 160 + 14 * t);
            int lower = (int) Math.round(opening ? 174 + 12 * t : 184 - 10 * t);
            stroke(p, x, upper, 0);
            int weak = faint && (x - left) % 48 < 30 && t < .82 ? 175 : 0;
            stroke(p, x, lower, weak);
        }
    }

    private static List<ScoreDynamicChange> detect(byte[] p) {
        var staffs = List.of(new PlayingTechniqueDetector.Staff(80, 128, 12, 0, 1));
        var bars =
                List.of(
                        new MeasureRegion(.08f, .5f, .25f, .48f),
                        new MeasureRegion(.5f, .92f, .25f, .48f));
        return ScoreDynamicsDetector.detect(List.of(), staffs, bars, List.of(), p, W, H);
    }

    @Test
    public void fragmentedLowerArmKeepsPrintedDiminuendo() {
        byte[] p = blank();
        wedge(p, false, true);
        var found = detect(p);
        assertEquals(1, found.size());
        assertEquals(-1, found.get(0).direction());
    }

    @Test
    public void fragmentedLowerArmKeepsPrintedCrescendo() {
        byte[] p = blank();
        wedge(p, true, true);
        var found = detect(p);
        assertEquals(1, found.size());
        assertEquals(1, found.get(0).direction());
    }

    @Test
    public void completeDarkDiminuendoRemains() {
        byte[] p = blank();
        wedge(p, false, false);
        assertEquals(-1, detect(p).get(0).direction());
    }

    @Test
    public void completeDarkCrescendoRemains() {
        byte[] p = blank();
        wedge(p, true, false);
        assertEquals(1, detect(p).get(0).direction());
    }

    @Test
    public void oneDarkLineAndOneFaintParallelLineStayExcluded() {
        byte[] p = blank();
        for (int x = 100; x <= 540; x++) {
            stroke(p, x, 168, 0);
            stroke(p, x, 184, 175);
        }
        assertTrue(detect(p).isEmpty());
    }

    @Test
    public void oneFaintLineNeverInventsItsMissingArm() {
        byte[] p = blank();
        for (int x = 100; x <= 540; x++) stroke(p, x, 168 + x / 50, 175);
        assertTrue(detect(p).isEmpty());
    }

    @Test
    public void darkSingleSlopingLineNeverInventsItsMissingArm() {
        byte[] p = blank();
        for (int x = 100; x <= 540; x++) stroke(p, x, 168 + x / 50, 0);
        assertTrue(detect(p).isEmpty());
    }

    @Test
    public void curvedPairStaysExcluded() {
        byte[] p = blank();
        for (int x = 100; x <= 540; x++) {
            double t = (x - 100) / 440.0;
            int y = 165 + (int) Math.round(14 * Math.sin(t * Math.PI));
            stroke(p, x, y, 0);
            stroke(p, x, y + 9, 175);
        }
        assertTrue(detect(p).isEmpty());
    }

    @Test
    public void longFadingTextBaselineStaysExcluded() {
        byte[] p = blank();
        for (int x = 100; x <= 540; x++) stroke(p, x, 184, 0);
        for (int left = 110; left < 520; left += 28)
            for (int y = 163; y <= 183; y++)
                for (int x = left; x <= left + 10; x++)
                    if (x == left || x == left + 10 || y == 163) stroke(p, x, y, 175);
        assertTrue(detect(p).isEmpty());
    }

    @Test
    public void aPairThatNeverReachesAnApexStaysExcludedInRecovery() {
        byte[] p = blank();
        for (int x = 100; x <= 540; x++) {
            double t = (x - 100) / 440.0;
            stroke(p, x, (int) Math.round(160 + 8 * t), 0);
            stroke(p, x, (int) Math.round(184 - 2 * t), 175);
        }
        assertTrue(detect(p).isEmpty());
    }

    @Test
    public void uniformlyPaleWedgeCannotSupplyADarkAnchor() {
        byte[] p = blank();
        wedge(p, false, false);
        for (int i = 0; i < p.length; i++) if ((p[i] & 255) < 145) p[i] = (byte) 175;
        assertTrue(detect(p).isEmpty());
    }

    @Test
    public void grayPaperCannotBecomeTheRecoveredSecondArm() {
        byte[] p = new byte[W * H];
        Arrays.fill(p, (byte) 180);
        for (int x = 100; x <= 540; x++) stroke(p, x, 160 + x / 40, 0);
        assertTrue(detect(p).isEmpty());
    }

    @Test
    public void recoveringBothDirectionsPreservesCallerPixels() {
        for (boolean opening : new boolean[] {false, true}) {
            byte[] p = blank();
            wedge(p, opening, true);
            byte[] original = p.clone();
            assertEquals(1, detect(p).size());
            assertArrayEquals(original, p);
        }
    }
}
