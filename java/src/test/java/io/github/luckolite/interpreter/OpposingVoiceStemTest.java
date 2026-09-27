// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original touching ovals with independent, unequal-length outer shafts. */
public class OpposingVoiceStemTest {
    final int w = 240, h = 220;
    final byte[] gray = new byte[w * h];
    final Class<?> type;
    final Object upper, lower;

    public OpposingVoiceStemTest() throws Exception {
        Arrays.fill(gray, (byte) 255);
        type = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var c = type.getDeclaredConstructors()[0];
        c.setAccessible(true);
        upper = c.newInstance(200, 90, 110, 83, 97, 100f, 90f);
        lower = c.newInstance(200, 90, 110, 99, 113, 100f, 106f);
        for (int cy : new int[] {90, 106})
            for (int y = cy - 7; y <= cy + 7; y++)
                for (int x = 90; x <= 110; x++)
                    if (Math.pow((x - 100) / 10d, 2) + Math.pow((y - cy) / 7d, 2) <= 1)
                        gray[y * w + x] = 0;
        shaft(109, 30, 106);
        shaft(91, 90, 155);
    }

    void shaft(int x, int top, int bottom) {
        for (int y = top; y <= bottom; y++) gray[y * w + x] = 0;
    }

    int[] stem(Object head) throws Exception {
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "opposingVoiceStem",
                        byte[].class,
                        int.class,
                        int.class,
                        type,
                        float.class,
                        List.class);
        m.setAccessible(true);
        return (int[]) m.invoke(null, gray, w, h, head, 16f, List.of(upper, lower));
    }

    @Test
    public void upperVoiceOwnsUpwardShaft() throws Exception {
        assertEquals(-1, stem(upper)[2]);
    }

    @Test
    public void lowerVoiceOwnsDownwardShaftEvenWhenOtherIsLonger() throws Exception {
        assertEquals(1, stem(lower)[2]);
    }

    @Test
    public void oneShaftDoesNotInventAnIndependentVoice() throws Exception {
        for (int y = 114; y <= 155; y++) gray[y * w + 91] = (byte) 255;
        assertNull(stem(upper));
        assertNull(stem(lower));
    }

    @Test
    public void analysisDoesNotChangePixels() throws Exception {
        var before = gray.clone();
        stem(lower);
        assertArrayEquals(before, gray);
    }
}
