// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original small antialiased ellipse at fractional staff spacing. */
public class FractionalFadedDotTest {
    private int count(boolean elongated) throws Exception {
        int w = 240, h = 160;
        byte[] gray = new byte[w * h];
        Arrays.fill(gray, (byte) 250);
        for (int y = -2; y <= 2; y++)
            for (int x = -3; x <= 3; x++)
                if (x * x / 9.0 + y * y / 4.0 <= 1.2) gray[(80 + y) * w + 126 + x] = (byte) 170;
        for (int y = -1; y <= 1; y++)
            for (int x = -2; x <= 2; x++)
                if (x * x / 4.0 + y * y <= 1.3) gray[(80 + y) * w + 126 + x] = (byte) 145;
        if (elongated) for (int x = -3; x <= 3; x++) gray[80 * w + 126 + x] = (byte) 145;
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = type.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object head = ctor.newInstance(180, 90, 110, 73, 87, 100f, 80f);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "countAugmentationDots",
                        List.class,
                        type,
                        float.class,
                        byte[].class,
                        int.class,
                        int.class,
                        boolean.class);
        method.setAccessible(true);
        return (int) method.invoke(null, List.of(), head, 14.25f, gray, w, h, false);
    }

    @Test
    public void roundedFiveByThreeCoreKeepsFaintDot() throws Exception {
        assertEquals(1, count(false));
    }

    @Test
    public void elongatedCoreStillRejects() throws Exception {
        assertEquals(0, count(true));
    }
}
