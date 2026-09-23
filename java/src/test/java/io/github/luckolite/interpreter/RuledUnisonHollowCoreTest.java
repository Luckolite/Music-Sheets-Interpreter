// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generated pixels: an open head crossed by a staff rule and a filled impostor. */
public class RuledUnisonHollowCoreTest {
    private static final int WIDTH = 60, HEIGHT = 40;

    private static boolean hollowCore(boolean lowerPocket) throws Exception {
        byte[] gray = new byte[WIDTH * HEIGHT];
        Arrays.fill(gray, (byte) 250);
        for (int y = 10; y <= 26; y++) for (int x = 20; x <= 38; x++) {
            double outer = Math.pow((x - 29) / 9.0, 2) + Math.pow((y - 18) / 8.0, 2);
            double inner = Math.pow((x - 29) / 6.0, 2) + Math.pow((y - 18) / 5.0, 2);
            if (outer <= 1) gray[y * WIDTH + x] = (byte) 25;
            if (inner <= 1 && (lowerPocket || y <= 18))
                gray[y * WIDTH + x] = (byte) 250;
        }
        for (int y = 16; y <= 20; y++) for (int x = 18; x <= 40; x++)
            gray[y * WIDTH + x] = (byte) 25;
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        var head = constructor.newInstance(260, 20, 38, 10, 26, 29f, 18f);
        var method = OmrScoreInterpreter.class.getDeclaredMethod("hasRawUnisonHollowCore",
                byte[].class, int.class, int.class, type);
        method.setAccessible(true);
        return (boolean) method.invoke(null, gray, WIDTH, HEIGHT, head);
    }

    @Test public void aStaffRuleDoesNotEraseBothHollowPockets() throws Exception {
        assertTrue(hollowCore(true));
    }

    @Test public void anUpperWhiteNotchDoesNotMakeAFilledHeadHollow() throws Exception {
        assertFalse(hollowCore(false));
    }
}
