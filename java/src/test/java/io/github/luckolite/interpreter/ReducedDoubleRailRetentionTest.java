// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two straight rails on a short metrical shaft, with a reduced head mask. */
public class ReducedDoubleRailRetentionTest {
    private int read(int first, int thickness, boolean second) throws Exception {
        int w = 260, h = 240;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 245);
        for (int y = first; y <= 120; y++) {
            gray[y * w + 106] = 25;
            labels[y * w + 106] = 1;
        }
        for (int top : new int[] {first, first + 10}) {
            if (top != first && !second) continue;
            for (int y = top; y < top + thickness; y++)
                for (int x = 55; x <= 165; x++) {
                    gray[y * w + x] = 25;
                    labels[y * w + x] = 5;
                }
        }
        for (int y = 115; y <= 125; y++)
            for (int x = 93; x <= 107; x++)
                if (Math.pow((x - 100) / 7.0, 2) + Math.pow((y - 120) / 5.0, 2) <= 1) {
                    gray[y * w + x] = 25;
                    labels[y * w + x] = 2;
                }
        for (int y = 160; y <= 224; y += 16)
            for (int x = 20; x <= 220; x++) {
                gray[y * w + x] = 40;
                labels[y * w + x] = 4;
            }
        var headType = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var hc = headType.getDeclaredConstructors()[0];
        hc.setAccessible(true);
        Object head = hc.newInstance(110, 93, 107, 115, 125, 100f, 120f);
        var staffType = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var sc = staffType.getDeclaredConstructor(float.class, float.class, float.class);
        sc.setAccessible(true);
        Object staff = sc.newInstance(160f, 224f, 16f);
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
        byte[] originalGray = gray.clone(), originalLabels = labels.clone();
        int result = (int) m.invoke(null, labels, gray, w, h, head, staff, List.of(head));
        assertArrayEquals(originalGray, gray);
        assertArrayEquals(originalLabels, labels);
        return result;
    }

    @Test
    public void shortShaftKeepsItsTwoCompleteRails() throws Exception {
        assertEquals(2, read(68, 6, true));
    }

    @Test
    public void thinnerTwoCompleteRailsAreAlsoRetained() throws Exception {
        assertEquals(2, read(68, 5, true));
    }

    @Test
    public void thinRulesCannotBecomeDoubleBeams() throws Exception {
        assertEquals(0, read(68, 1, true));
    }

    @Test
    public void singleCompleteRailRemainsOne() throws Exception {
        assertEquals(1, read(68, 6, false));
    }

    @Test
    public void longShaftRetainsItsEstablishedTwoRails() throws Exception {
        assertEquals(2, read(64, 6, true));
    }
}
