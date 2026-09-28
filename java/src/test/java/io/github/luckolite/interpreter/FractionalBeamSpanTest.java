// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original beam pair with a short connecting neck below the required staff-gap fraction. */
public class FractionalBeamSpanTest {
    static final int W = 260, H = 220;
    final byte[] gray = new byte[W * H], labels = new byte[W * H];

    void rect(int x0, int x1, int y0, int y1, int ink) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) gray[y * W + x] = (byte) ink;
    }

    void setup(int neck) {
        Arrays.fill(gray, (byte) 255);
        rect(109, 111, 90, 150, 246);
        for (int y = 90; y <= 150; y++) labels[y * W + 110] = 1;
        rect(110, 180, 90, 94, 35);
        rect(110, 180, 96, 100, 35);
        if (neck > 0) rect(110, 109 + neck, 95, 95, 35);
    }

    int beams() throws Exception {
        var hc = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = hc.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        var head = ctor.newInstance(180, 90, 110, 144, 156, 100f, 150f);
        var sc = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var st = sc.getDeclaredConstructor(float.class, float.class, float.class);
        st.setAccessible(true);
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "detectBeamCount",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        hc,
                        sc);
        m.setAccessible(true);
        return (int) m.invoke(null, labels, gray, W, H, head, st.newInstance(30f, 88f, 14.5f));
    }

    @Test
    public void subMinimumNeckDoesNotMergeTwoBeams() throws Exception {
        setup(12);
        assertEquals(2, beams());
    }

    @Test
    public void fullWidthBridgeIsOneBand() throws Exception {
        setup(30);
        assertEquals(1, beams());
    }

    @Test
    public void whiteGapPreservesPair() throws Exception {
        setup(0);
        assertEquals(2, beams());
    }

    @Test
    public void pixelsAreUnchanged() throws Exception {
        setup(12);
        var g = gray.clone();
        var l = labels.clone();
        beams();
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }
}
