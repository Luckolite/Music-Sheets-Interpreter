// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original tiny closed pockets separated by a complete staff rule. */
public class CompactRuledHollowTest {
    boolean read(boolean hole, boolean rule, boolean leak) throws Exception {
        int w = 60, h = 40;
        byte[] g = new byte[w * h], l = new byte[w * h];
        Arrays.fill(g, (byte) 255);
        for (int y = 12; y <= 20; y++)
            for (int x = 20; x <= 32; x++) {
                g[y * w + x] = 0;
                l[y * w + x] = 2;
                if (hole && x >= 25 && x <= 27 && (y == 14 || y == 18)) g[y * w + x] = (byte) 255;
            }
        if (rule) for (int x = 10; x <= 42; x++) g[16 * w + x] = 0;
        if (leak) for (int y = 12; y <= 14; y++) g[y * w + 26] = (byte) 255;
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = type.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        var c = ctor.newInstance(110, 20, 32, 12, 20, 26f, 16f);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "hasOpenCenter",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        type,
                        float.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, l, g, w, h, c, 13f);
    }

    @Test
    public void twoClosedPocketsRestoreSustainedDuration() throws Exception {
        assertTrue(read(true, true, false));
    }

    @Test
    public void filledHeadRemainsQuarter() throws Exception {
        assertFalse(read(false, true, false));
    }

    @Test
    public void undersizedHeadWithoutRuleIsNotPromoted() throws Exception {
        assertFalse(read(true, false, false));
    }

    @Test
    public void openExteriorNotchIsNotAnEnclosedPocket() throws Exception {
        assertFalse(read(true, true, true));
    }
}
