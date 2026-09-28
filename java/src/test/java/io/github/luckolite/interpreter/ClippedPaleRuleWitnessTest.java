// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/** Original finite beam whose dark core clips a pale staff rule's center. */
public class ClippedPaleRuleWitnessTest {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 200;
    private final byte[] gray = new byte[WIDTH * HEIGHT];

    private void rectangle(int left, int right, int top, int bottom, int shade) {
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                gray[y * WIDTH + x] = (byte) shade;
            }
        }
    }

    private void setup(int ruleY, int shade) {
        Arrays.fill(gray, (byte) 250);
        rectangle(0, WIDTH - 1, ruleY, ruleY, shade);
        rectangle(100, 700, 80, 86, 35);
    }

    private boolean finite() throws Exception {
        var staffType = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var constructor = staffType.getDeclaredConstructor(float.class, float.class, float.class);
        constructor.setAccessible(true);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "finiteBeamOverRule",
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        staffType,
                        int.class);
        method.setAccessible(true);
        return (boolean)
                method.invoke(
                        null,
                        gray,
                        WIDTH,
                        HEIGHT,
                        400,
                        80,
                        86,
                        constructor.newInstance(88f, 152f, 16f),
                        165);
    }

    @Test
    public void paleRuleJustOutsideCoreProvesBothEndpoints() throws Exception {
        setup(88, 205);
        assertTrue(finite());
    }

    @Test
    public void fartherUnrelatedRuleDoesNotProveEndpoints() throws Exception {
        setup(91, 205);
        assertFalse(finite());
    }

    @Test
    public void nearWhitePaperDoesNotProveEndpoints() throws Exception {
        setup(88, 230);
        assertFalse(finite());
    }

    @Test
    public void unboundedBeamDoesNotBecomeFinite() throws Exception {
        setup(88, 205);
        rectangle(0, WIDTH - 1, 80, 86, 35);
        assertFalse(finite());
    }
}
