// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original pairs of filled dyads and independent shafts; no score pixels. */
public class JoinedChordAttacksTest {
    final int w = 240, h = 220;
    final byte[] gray = new byte[w * h], labels = new byte[w * h];

    public JoinedChordAttacksTest() {
        Arrays.fill(gray, (byte) 255);
        for (int cx : new int[] {70, 118})
            for (int cy : new int[] {100, 116})
                for (int y = cy - 8; y <= cy + 8; y++)
                    for (int x = cx - 11; x <= cx + 11; x++)
                        if (Math.pow((x - cx) / 11d, 2) + Math.pow((y - cy) / 8d, 2) <= 1) {
                            gray[y * w + x] = 0;
                            labels[y * w + x] = 2;
                        }
        for (int x : new int[] {80, 128}) for (int y = 45; y <= 116; y++) gray[y * w + x] = 0;
    }

    List<?> split() throws Exception {
        var ct = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var c = ct.getDeclaredConstructors()[0];
        c.setAccessible(true);
        var st = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var s = st.getDeclaredConstructor(float.class, float.class, float.class);
        s.setAccessible(true);
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "splitJoinedChordAttacks",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        ct,
                        st);
        m.setAccessible(true);
        return (List<?>)
                m.invoke(
                        null,
                        labels,
                        gray,
                        w,
                        h,
                        c.newInstance(1100, 59, 129, 92, 124, 94f, 108f),
                        s.newInstance(50f, 114f, 16f));
    }

    @Test
    public void independentShaftsSeparateTwoAttacks() throws Exception {
        assertEquals(4, split().size());
    }

    @Test
    public void noPrintedShaftCannotInventAnotherAttack() throws Exception {
        for (int y = 45; y < 92; y++) gray[y * w + 128] = (byte) 255;
        assertTrue(split().isEmpty());
    }

    @Test
    public void solidBlocksAreNotChordOvals() throws Exception {
        for (int y = 92; y <= 124; y++) for (int x = 59; x <= 129; x++) gray[y * w + x] = 0;
        assertTrue(split().isEmpty());
    }

    @Test
    public void inputArraysStayUnchanged() throws Exception {
        var a = gray.clone();
        var b = labels.clone();
        split();
        assertArrayEquals(a, gray);
        assertArrayEquals(b, labels);
    }
}
