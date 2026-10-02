// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import java.lang.reflect.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original reduced head masks with complete metrical shafts and three separate rails. */
public class ReducedMetricalTripleBeamTest {
    private int count(int beams, boolean broken) throws Exception {
        return count(beams, broken, 304);
    }

    private int count(int beams, boolean broken, int centerY) throws Exception {
        int w = 260, h = 450;
        byte[] g = new byte[w * h], l = new byte[w * h];
        Arrays.fill(g, (byte) 255);
        Class<?> hc = Class.forName(OmrScoreInterpreter.class.getName() + "$Component"),
                sc = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        Constructor<?> head = hc.getDeclaredConstructors()[0],
                staff = sc.getDeclaredConstructors()[0];
        head.setAccessible(true);
        staff.setAccessible(true);
        var heads = new ArrayList<Object>();
        for (int x : new int[] {100, 128}) {
            int area = 0;
            for (int yy = centerY - 5; yy <= centerY + 5; yy++)
                for (int xx = x - 6; xx <= x + 6; xx++)
                    if ((xx - x) * (xx - x) / 36.0 + (yy - centerY) * (yy - centerY) / 25.0 <= 1) {
                        g[yy * w + xx] = 40;
                        l[yy * w + xx] = 2;
                        area++;
                    }
            int shaft = x - 5;
            for (int y = centerY; y <= 364; y++) {
                g[y * w + shaft] = 40;
                l[y * w + shaft] = 1;
            }
            heads.add(
                    head.newInstance(
                            area,
                            x - 6,
                            x + 6,
                            centerY - 5,
                            centerY + 5,
                            (float) x,
                            (float) centerY));
        }
        for (int x = 95; x <= 123; x++)
            for (int b = 0; b < beams; b++)
                for (int dy = 0; dy < 5; dy++) {
                    if (broken && b == 2 && x >= 107 && x <= 112) continue;
                    int y = 360 - b * 12 + dy;
                    g[y * w + x] = 40;
                    l[y * w + x] = 1;
                }
        Method method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "detectBeamCount",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        hc,
                        sc,
                        List.class);
        method.setAccessible(true);
        return (int)
                method.invoke(
                        null, l, g, w, h, heads.get(0), staff.newInstance(280f, 336f, 14f), heads);
    }

    @Test
    public void completeThreeRailsOutrankACompactTwoRailWindow() throws Exception {
        assertEquals(3, count(3, false));
    }

    @Test
    public void genuineTwoRailsRemainTwo() throws Exception {
        assertEquals(2, count(2, false));
    }

    @Test
    public void anInterruptedThirdRailDoesNotPromoteTheRhythm() throws Exception {
        assertEquals(2, count(3, true));
    }

    @Test
    public void completeLedgerShaftsKeepTheirThirdRail() throws Exception {
        assertEquals(3, count(3, false, 234));
    }

    @Test
    public void genuineLedgerTwoRailsRemainTwo() throws Exception {
        assertEquals(2, count(2, false, 234));
    }

    @Test
    public void ledgerRecoveryRequiresAContinuousThirdRail() throws Exception {
        assertEquals(2, count(3, true, 234));
    }
}
