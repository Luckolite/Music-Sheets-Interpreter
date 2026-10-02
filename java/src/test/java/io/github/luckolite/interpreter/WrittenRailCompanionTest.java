// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import java.lang.reflect.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original complete straight rails between independently attached written shafts. */
public class WrittenRailCompanionTest {
    private int read(
            int span, int cores, boolean broken, boolean bent, boolean opposite, boolean compact) {
        int width = 200, height = 160, left = 50, right = left + span;
        byte[] gray = new byte[width * height];
        Arrays.fill(gray, (byte) 245);
        for (int x = left; x <= right; x++)
            for (int core = 0; core < cores; core++)
                for (int dy = 0; dy < 6; dy++) {
                    if (broken && core == 1 && Math.abs(x - (left + right) / 2) <= 2) continue;
                    int y = 70 + core * 10 + dy + (bent && x > (left + right) / 2 ? 5 : 0);
                    gray[y * width + x] = 30;
                }
        for (int y = 70; y < 130; y++) {
            gray[y * width + left] = 30;
            gray[y * width + right] = 30;
        }
        byte[] before = gray.clone();
        int[] first = {left, 70, -1}, second = {right, 70, opposite ? 1 : -1};
        int result =
                compact
                        ? PairedGraceBeamInk.countFullSize(gray, width, height, first, second, 14)
                        : PairedGraceBeamInk.countPrintedSize(
                                gray, width, height, first, second, 14);
        assertArrayEquals(before, gray);
        assertArrayEquals(new int[] {left, 70, -1}, first);
        assertArrayEquals(new int[] {right, 70, opposite ? 1 : -1}, second);
        return result;
    }

    @Test
    public void wideWrittenSixteenthsHaveTwoCompleteCores() {
        assertEquals(2, read(63, 2, false, false, false, false));
    }

    @Test
    public void compactClassifierKeepsItsOriginalSpanBoundary() {
        assertEquals(0, read(63, 2, false, false, false, true));
    }

    @Test
    public void exactlyFiveStaffGapsRemainBounded() {
        assertEquals(2, read(70, 2, false, false, false, false));
    }

    @Test
    public void onePixelBeyondFiveGapsIsRejected() {
        assertEquals(0, read(71, 2, false, false, false, false));
    }

    @Test
    public void compactWrittenTriplesHaveThreeCores() {
        assertEquals(3, read(35, 3, false, false, false, false));
    }

    @Test
    public void missingMiddleColumnCannotProveTwoRails() {
        assertEquals(0, read(63, 2, true, false, false, false));
    }

    @Test
    public void bentFlagLikeCoresCannotProveStraightRails() {
        assertEquals(0, read(63, 2, false, true, false, false));
    }

    @Test
    public void oppositeStemsDoNotSupplyTheSameBeam() {
        assertEquals(0, read(63, 2, false, false, true, false));
    }

    @Test
    public void aSingleRailCannotSupplyASecond() {
        assertEquals(0, read(63, 1, false, false, false, false));
    }

    private static int decodedMixedHeads(
            int firstWidth, int secondWidth, int cy, int delta, int cores, boolean broken)
            throws Exception {
        int w = 280, h = 450;
        byte[] g = new byte[w * h], l = new byte[w * h];
        Arrays.fill(g, (byte) 245);
        var headType = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var hc = headType.getDeclaredConstructors()[0];
        hc.setAccessible(true);
        var staffType = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var sc = staffType.getDeclaredConstructor(float.class, float.class, float.class);
        sc.setAccessible(true);
        var heads = new ArrayList<Object>();
        int[] shafts = new int[2];
        for (int i = 0; i < 2; i++) {
            int x = 100 + i * 28,
                    y = cy + (i == 0 ? 0 : delta),
                    wide = i == 0 ? firstWidth : secondWidth,
                    half = wide / 2,
                    area = 0;
            for (int yy = y - 5; yy <= y + 5; yy++)
                for (int xx = x - half; xx <= x + half; xx++)
                    if (Math.pow((xx - x) / (double) half, 2) + Math.pow((yy - y) / 5.0, 2) <= 1) {
                        g[yy * w + xx] = 30;
                        l[yy * w + xx] = 2;
                        area++;
                    }
            int shaft = x - half;
            shafts[i] = shaft;
            for (int yy = y; yy <= 364; yy++) {
                g[yy * w + shaft] = 30;
                l[yy * w + shaft] = 1;
            }
            heads.add(hc.newInstance(area, x - half, x + half, y - 5, y + 5, (float) x, (float) y));
        }
        for (int x = Math.min(shafts[0], shafts[1]); x <= Math.max(shafts[0], shafts[1]); x++)
            for (int beam = 0; beam < cores; beam++)
                for (int dy = 0; dy < 5; dy++) {
                    if (broken && beam == 2 && Math.abs(x - (shafts[0] + shafts[1]) / 2) <= 2)
                        continue;
                    int y = 360 - beam * 12 + dy;
                    g[y * w + x] = 30;
                    l[y * w + x] = 1;
                }
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "detectBeamCount",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        headType,
                        staffType,
                        List.class);
        m.setAccessible(true);
        return (int)
                m.invoke(null, l, g, w, h, heads.get(0), sc.newInstance(280f, 336f, 14f), heads);
    }

    @Test
    public void ordinaryHeadUsesReducedCompanionForThirdRail() throws Exception {
        assertEquals(3, decodedMixedHeads(20, 13, 308, 0, 3, false));
    }

    @Test
    public void reducedHeadUsesOrdinaryCompanionForThirdRail() throws Exception {
        assertEquals(3, decodedMixedHeads(13, 20, 300, 0, 3, false));
    }

    @Test
    public void twoRailsDoNotBecomeThreeInMixedMasks() throws Exception {
        assertEquals(2, decodedMixedHeads(20, 13, 308, 0, 2, false));
    }

    @Test
    public void brokenThirdRailCannotPromoteMixedMasks() throws Exception {
        assertEquals(2, decodedMixedHeads(20, 13, 308, 0, 3, true));
    }
}
