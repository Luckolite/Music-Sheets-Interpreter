// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original antialiased ellipse with a lighter connection to surrounding printing. */
public class IntermediateFadedDotTest {
    private static final int W = 240, H = 160;
    private final byte[] gray = new byte[W * H];

    private void setup(int body, int core, int bridge) {
        Arrays.fill(gray, (byte) 250);
        for (int y = -2; y <= 2; y++)
            for (int x = -3; x <= 3; x++)
                if (x * x / 9.0 + y * y / 4.0 <= 1.2) gray[(80 + y) * W + 126 + x] = (byte) body;
        for (int y = -1; y <= 1; y++)
            for (int x = -2; x <= 2; x++)
                if (x * x / 4.0 + y * y <= 1.3) gray[(80 + y) * W + 126 + x] = (byte) core;
        for (int x = 130; x <= 165; x++) gray[80 * W + x] = (byte) bridge;
    }

    private int count() throws Exception {
        var hc = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = hc.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object head = ctor.newInstance(180, 90, 110, 73, 87, 100f, 80f);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "countAugmentationDots",
                        List.class,
                        hc,
                        float.class,
                        byte[].class,
                        int.class,
                        int.class,
                        boolean.class);
        method.setAccessible(true);
        return (int) method.invoke(null, List.of(), head, 14.25f, gray, W, H, false);
    }

    @Test
    public void lowerIntermediateContrastSeparatesDotFromFringe() throws Exception {
        setup(170, 145, 178);
        assertEquals(1, count());
    }

    @Test
    public void upperIntermediateContrastSeparatesDotFromFringe() throws Exception {
        setup(190, 165, 200);
        assertEquals(1, count());
    }

    @Test
    public void sameShadeWithoutNestedCoreIsNotEnough() throws Exception {
        setup(190, 190, 200);
        assertEquals(0, count());
    }

    @Test
    public void elongatedCoreStillFails() throws Exception {
        setup(190, 165, 200);
        for (int x = 123; x <= 129; x++) gray[80 * W + x] = (byte) 165;
        assertEquals(0, count());
    }

    @Test
    public void equallyDarkBridgeCannotBecomeDot() throws Exception {
        setup(170, 145, 145);
        assertEquals(0, count());
    }

    @Test
    public void sourcePixelsArePreserved() throws Exception {
        setup(190, 165, 200);
        var copy = gray.clone();
        count();
        assertArrayEquals(copy, gray);
    }
}
