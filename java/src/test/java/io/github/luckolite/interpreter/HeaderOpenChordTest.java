// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original open-oval pair below a clef, with a stem ending on the top rule. */
public class HeaderOpenChordTest {
    static final int W = 260, H = 240, G = 20;
    final byte[] gray = new byte[W * H], labels = new byte[W * H];

    public HeaderOpenChordTest() {
        Arrays.fill(gray, (byte) 255);
        for (int cy : new int[] {150, 170})
            for (int y = cy - 10; y <= cy + 10; y++)
                for (int x = 87; x <= 113; x++) {
                    double r = Math.pow((x - 100) / 13d, 2) + Math.pow((y - cy) / 10d, 2);
                    if (r <= 1) {
                        labels[y * W + x] = 2;
                        if (r > .42) gray[y * W + x] = 0;
                    }
                }
        for (int y = 80; y <= 170; y++) for (int x = 111; x <= 113; x++) gray[y * W + x] = 0;
        for (int y = 150; y <= 170; y++) labels[y * W + 100] = 2;
    }

    Object component(int left, int right, int top, int bottom, int area) throws Exception {
        var c =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Component")
                        .getDeclaredConstructors()[0];
        c.setAccessible(true);
        return c.newInstance(
                area, left, right, top, bottom, (left + right) / 2f, (top + bottom) / 2f);
    }

    boolean rejected() throws Exception {
        Object head = component(87, 113, 140, 180, 600), clef = component(25, 55, 50, 174, 300);
        var c =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Staff")
                        .getDeclaredConstructors()[0];
        c.setAccessible(true);
        Object staff = c.newInstance(80f, 160f, 20f);
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "isHeaderMeterDigit",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        head.getClass(),
                        List.class,
                        List.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, labels, gray, W, H, head, List.of(staff), List.of(clef));
    }

    @Test
    public void twoOpenHeadsWithTopRuleStemAreNotMeterDigit() throws Exception {
        assertFalse(rejected());
    }

    @Test
    public void twoFilledHeadsWithTopRuleStemAreNotMeterDigit() throws Exception {
        for (int cy : new int[] {150, 170})
            for (int y = cy - 10; y <= cy + 10; y++)
                for (int x = 87; x <= 113; x++)
                    if (Math.pow((x - 100) / 13d, 2) + Math.pow((y - cy) / 10d, 2) <= 1)
                        gray[y * W + x] = 0;
        assertFalse(rejected());
    }

    @Test
    public void inputIsUnchanged() throws Exception {
        var l = labels.clone();
        var g = gray.clone();
        rejected();
        assertArrayEquals(l, labels);
        assertArrayEquals(g, gray);
    }
}
