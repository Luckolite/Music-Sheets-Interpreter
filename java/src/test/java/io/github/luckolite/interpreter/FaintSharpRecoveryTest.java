// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original sharp and note drawings with independently faded spines. */
public class FaintSharpRecoveryTest {
    private SharpPitchAlignmentTest.Page page(int headY, boolean rightSpine, int shade) {
        var p = new SharpPitchAlignmentTest.Page(headY, 124);
        for (int i = 0; i < p.gray.length; i++)
            if ((p.gray[i] & 255) == 255) p.gray[i] = (byte) 240;
        for (int y = 102; y <= 146; y++)
            for (int x = 248; x <= 263; x++) {
                p.labels[y * 420 + x] = 0;
                p.gray[y * 420 + x] = (byte) 240;
            }
        for (int x : new int[] {251, 252, 259, 260})
            if (rightSpine || x < 255)
                for (int y = 106; y <= 142; y++) p.gray[y * 420 + x] = (byte) shade;
        for (int cy : new int[] {117, 131})
            for (int y = cy - 1; y <= cy + 1; y++)
                for (int x = 248; x <= 263; x++) {
                    p.labels[y * 420 + x] = 3;
                    p.gray[y * 420 + x] = 60;
                }
        return p;
    }

    @Test
    public void twoFaintSpinesRecoverSharp() {
        var p = page(124, true, 239);
        assertEquals(1, p.accidental(p.gray));
    }

    @Test
    public void oneSpineCannotProveSharp() {
        var p = page(124, false, 239);
        assertNotEquals(1, p.accidental(p.gray));
    }

    @Test
    public void flatPaperBetweenBarsIsNotSharp() {
        var p = page(124, true, 240);
        assertNotEquals(1, p.accidental(p.gray));
    }

    @Test
    public void adjacentPitchIsNotChanged() {
        var p = page(132, true, 239);
        assertNotEquals(1, p.accidental(p.gray));
    }

    @Test
    public void rawAndSemanticPixelsStayUnchanged() {
        var p = page(124, true, 239);
        byte[] g = p.gray.clone(), l = p.labels.clone();
        p.accidental(p.gray);
        assertArrayEquals(g, p.gray);
        assertArrayEquals(l, p.labels);
    }
}
