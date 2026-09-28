// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

/** Original dark shaft beside a pale traced fringe; no score-derived pixels. */
public class RecenteredFlagRootTest {
    private static final int W = 260, H = 220;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    private void rect(int l, int r, int t, int b, int ink) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) gray[y * W + x] = (byte) ink;
    }

    private void line(int ax, int ay, int bx, int by) {
        int n = Math.max(Math.abs(bx - ax), Math.abs(by - ay));
        for (int i = 0; i <= n; i++) {
            int x = Math.round(ax + (bx - ax) * i / (float) n),
                    y = Math.round(ay + (by - ay) * i / (float) n);
            rect(x - 1, x + 1, y - 1, y + 1, 35);
        }
    }

    private void setup() {
        Arrays.fill(gray, (byte) 255);
        rect(107, 109, 80, 150, 235);
        rect(110, 110, 80, 150, 35);
        rect(110, 117, 80, 85, 35);
        rect(110, 117, 94, 99, 35);
        line(117, 85, 125, 96);
        line(125, 96, 119, 108);
        line(117, 99, 125, 110);
        line(125, 110, 119, 122);
    }

    private boolean roots(int original, int edge) throws Exception {
        var hc = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = hc.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        var sc = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var st = sc.getDeclaredConstructor(float.class, float.class, float.class);
        st.setAccessible(true);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "recenteredFlagRoots",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        hc,
                        sc,
                        int.class,
                        int.class,
                        boolean.class,
                        int.class,
                        int.class);
        method.setAccessible(true);
        return (boolean)
                method.invoke(
                        null,
                        gray,
                        labels,
                        W,
                        H,
                        ctor.newInstance(180, 90, edge, 144, 156, 100f, 150f),
                        st.newInstance(70f, 134f, 16f),
                        original,
                        80,
                        true,
                        77,
                        114);
    }

    private boolean direct(int axis, int far) throws Exception {
        var sc = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var st = sc.getDeclaredConstructor(float.class, float.class, float.class);
        st.setAccessible(true);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "adjacentFlagRoots",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        sc,
                        boolean.class);
        method.setAccessible(true);
        return (boolean)
                method.invoke(
                        null,
                        gray,
                        labels,
                        W,
                        H,
                        axis,
                        77,
                        far,
                        st.newInstance(70f, 134f, 16f),
                        true);
    }

    @Test
    public void darkShaftProvesShiftedPair() throws Exception {
        setup();
        assertFalse("Prior axis misses both roots", direct(107, 114));
        assertTrue(roots(107, 110));
    }

    @Test
    public void establishedDarkAxisIsNotMoved() throws Exception {
        setup();
        assertFalse(roots(110, 110));
    }

    @Test
    public void noDarkShaftCannotBorrowOuterRoots() throws Exception {
        setup();
        rect(110, 110, 86, 150, 235);
        assertFalse(roots(107, 110));
    }

    @Test
    public void distantShaftCannotBeBorrowed() throws Exception {
        setup();
        assertFalse(roots(103, 110));
    }

    @Test
    public void shaftAwayFromHeadEdgeCannotBeBorrowed() throws Exception {
        setup();
        assertFalse(roots(107, 100));
    }

    @Test
    public void oneFlagCannotProveTwo() throws Exception {
        setup();
        rect(111, 118, 94, 99, 255);
        assertFalse(roots(107, 110));
    }

    @Test
    public void noReturningHookCannotProvePair() throws Exception {
        setup();
        rect(118, 130, 78, 125, 255);
        assertFalse(roots(107, 110));
    }

    @Test
    public void sourceArraysRemainUnchanged() throws Exception {
        setup();
        var g = gray.clone();
        var l = labels.clone();
        roots(107, 110);
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }

    @Test
    public void narrowScanClipsSecondFullThicknessRoot() throws Exception {
        Arrays.fill(gray, (byte) 255);
        rect(111, 115, 94, 99, 35);
        rect(111, 115, 107, 112, 35);
        assertFalse(direct(110, 80 + Math.round(16 * 1.85f)));
        assertTrue(direct(110, 80 + Math.round(16 * 2.1f)));
    }

    @Test
    public void extendedWindowDoesNotPromoteThinSecondRoot() throws Exception {
        Arrays.fill(gray, (byte) 255);
        rect(111, 115, 94, 99, 35);
        rect(111, 115, 110, 112, 35);
        assertFalse(direct(110, 80 + Math.round(16 * 2.1f)));
    }
}
