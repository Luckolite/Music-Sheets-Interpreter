// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original dark beam pair with a short dark endpoint and an independently visible pale shaft. */
public class ClippedDarkBeamEndpointTest {
    static final int W = 300, H = 240;
    final byte[] gray = new byte[W * H], labels = new byte[W * H];
    boolean down;
    int headY = 150;
    boolean requireCurved;

    int y(int row) {
        return down ? H - 1 - row : row;
    }

    int stemX() {
        return down ? 90 : 110;
    }

    void rect(int left, int right, int top, int bottom, int ink) {
        for (int row = top; row <= bottom; row++)
            for (int x = left; x <= right; x++) gray[y(row) * W + x] = (byte) ink;
    }

    void setup(boolean downward) {
        down = downward;
        headY = 150;
        Arrays.fill(gray, (byte) 250);
        rect(stemX() - 1, stemX() + 1, 90, 150, 230);
        rect(stemX(), stemX() + 80, 90, 97, 35);
        rect(stemX(), stemX() + 80, 103, 110, 35);
    }

    int beams(int endpoint, int horizontalOffset) throws Exception {
        return beams(endpoint, horizontalOffset, down ? 1 : -1);
    }

    int beams(int endpoint, int horizontalOffset, int direction) throws Exception {
        var hc = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = hc.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        var head =
                ctor.newInstance(180, 90, 110, y(headY) - 6, y(headY) + 6, 100f, (float) y(headY));
        var sc = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var st = sc.getDeclaredConstructor(float.class, float.class, float.class);
        st.setAccessible(true);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "detectBeamCount",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        hc,
                        sc,
                        boolean.class,
                        boolean.class,
                        int[].class);
        method.setAccessible(true);
        if (requireCurved) {
            var flag =
                    OmrScoreInterpreter.class.getDeclaredMethod(
                            "hasCurvedFlag",
                            byte[].class,
                            byte[].class,
                            int.class,
                            int.class,
                            hc,
                            float.class,
                            int.class,
                            int.class,
                            boolean.class);
            flag.setAccessible(true);
            assertTrue(
                    (boolean)
                            flag.invoke(
                                    null,
                                    labels,
                                    gray,
                                    W,
                                    H,
                                    head,
                                    16f,
                                    stemX() + horizontalOffset,
                                    y(endpoint),
                                    direction < 0));
        }
        return (int)
                method.invoke(
                        null,
                        labels,
                        gray,
                        W,
                        H,
                        head,
                        st.newInstance(40f, 104f, 16f),
                        false,
                        false,
                        new int[] {stemX() + horizontalOffset, y(endpoint), direction});
    }

    @Test
    public void clippedUpEndpointRecoversBothBeams() throws Exception {
        setup(false);
        assertEquals(2, beams(98, 0));
    }

    @Test
    public void clippedDownEndpointRecoversBothBeams() throws Exception {
        setup(true);
        assertEquals(2, beams(98, 0));
    }

    @Test
    public void completeEndpointRetainsPair() throws Exception {
        setup(false);
        assertEquals(2, beams(90, 0));
    }

    @Test
    public void missingSecondBeamDoesNotInventOne() throws Exception {
        setup(false);
        rect(stemX(), stemX() + 80, 90, 97, 250);
        assertTrue(beams(96, 0) < 2);
    }

    @Test
    public void disconnectedPaleShaftCannotExtend() throws Exception {
        setup(false);
        rect(stemX() - 1, stemX() + 1, 120, 135, 250);
        assertTrue(beams(96, 0) < 2);
    }

    @Test
    public void sourceArraysArePreserved() throws Exception {
        setup(true);
        var g = gray.clone();
        var l = labels.clone();
        beams(96, 0);
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }

    @Test
    public void laterallyDifferentShaftCannotUpgrade() throws Exception {
        setup(false);
        assertEquals(1, beams(98, 8));
    }

    void widePair() {
        setup(false);
        headY = 180;
        rect(stemX(), stemX() + 80, 103, 118, 250);
        rect(stemX() - 1, stemX() + 1, 90, 180, 230);
        rect(stemX(), stemX() + 80, 90, 97, 35);
        rect(stemX(), stemX() + 80, 111, 118, 35);
    }

    @Test
    public void boundedExtensionCanRecoverWidePair() throws Exception {
        widePair();
        assertEquals(2, beams(109, 0));
    }

    @Test
    public void excessivelyDistantEndpointCannotUpgrade() throws Exception {
        widePair();
        assertEquals(1, beams(110, 0));
    }

    @Test
    public void oppositeShaftDirectionCannotBorrowPair() throws Exception {
        setup(false);
        assertEquals(1, beams(98, 0, 1));
    }

    @Test
    public void curvedFlagCannotBorrowOppositeBeamPair() throws Exception {
        setup(false);
        rect(111, 190, 90, 110, 250);
        rect(30, 110, 90, 97, 35);
        rect(30, 110, 103, 110, 35);
        int[] xs = {110, 120, 127, 130, 129, 122, 119, 120, 125, 120, 114, 110};
        int[] ys = {98, 107, 112, 120, 128, 138, 139, 125, 121, 117, 113, 110};
        for (int yy = 97; yy <= 140; yy++)
            for (int x = 110; x <= 131; x++) {
                boolean inside = false;
                for (int i = 0, j = xs.length - 1; i < xs.length; j = i++)
                    if ((ys[i] > yy) != (ys[j] > yy)
                            && x
                                    < (xs[j] - xs[i]) * (yy - ys[i]) / (double) (ys[j] - ys[i])
                                            + xs[i]) inside = !inside;
                if (inside) {
                    gray[yy * W + x] = 0;
                    labels[yy * W + x] = 5;
                }
            }
        requireCurved = true;
        assertEquals(1, beams(98, 0));
    }
}
