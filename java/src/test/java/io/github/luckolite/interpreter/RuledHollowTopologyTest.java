// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original full-size masks with two small enclosed pockets across a staff rule. */
public class RuledHollowTopologyTest {
    private boolean read(boolean pockets, boolean rule, boolean leak, boolean tall)
            throws Exception {
        return read(pockets, rule, leak, tall, false);
    }

    private boolean read(
            boolean pockets, boolean rule, boolean leak, boolean tall, boolean exterior)
            throws Exception {
        int w = 70, h = 60, top = 14, bottom = tall ? 40 : 28;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int y = top; y <= bottom; y++)
            for (int x = 25; x <= 40; x++) {
                gray[y * w + x] = 0;
                labels[y * w + x] = 2;
            }
        if (pockets)
            for (int y : new int[] {18, 24})
                for (int x = 31; x <= 33; x++) gray[y * w + x] = (byte) 255;
        if (exterior)
            for (int y = 25; y <= 27; y++)
                for (int x = 38; x <= 39; x++) gray[y * w + x] = (byte) 255;
        if (rule) for (int x = 15; x <= 50; x++) gray[21 * w + x] = 0;
        if (leak) for (int y = top; y <= 18; y++) gray[y * w + 32] = (byte) 255;
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = type.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object head = ctor.newInstance(200, 25, 40, top, bottom, 32.5f, 21f);
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
        byte[] before = gray.clone();
        boolean result = (boolean) method.invoke(null, labels, gray, w, h, head, 14f);
        assertArrayEquals(before, gray);
        return result;
    }

    @Test
    public void enclosedPocketsRestoreFullSizeRuledHalf() throws Exception {
        assertTrue(read(true, true, false, false));
    }

    @Test
    public void exteriorPocketBetweenFilledHeadsCannotRestoreHalf() throws Exception {
        assertFalse(read(false, true, false, false, true));
    }

    @Test
    public void filledRuledHeadRemainsFilled() throws Exception {
        assertFalse(read(false, true, false, false));
    }

    @Test
    public void leakingPocketCannotRestoreHalf() throws Exception {
        assertFalse(read(true, true, true, false));
    }

    @Test
    public void twoTinyPocketsWithoutRuleRemainRejected() throws Exception {
        assertFalse(read(true, false, false, false));
    }

    @Test
    public void tallGlyphDoesNotUseNoteTopologyFallback() throws Exception {
        assertFalse(read(true, true, false, true));
    }
}
