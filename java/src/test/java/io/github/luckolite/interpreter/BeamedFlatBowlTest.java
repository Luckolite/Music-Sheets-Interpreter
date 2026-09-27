// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original bowl and crossing beams; no source score pixels. */
public class BeamedFlatBowlTest {
    static final int W = 180, H = 130;
    final byte[] gray = new byte[W * H], labels = new byte[W * H];
    final List<Object> candidates = new ArrayList<>();

    public BeamedFlatBowlTest() {
        Arrays.fill(gray, (byte) 255);
    }

    void rect(int l, int r, int t, int b, int label) {
        for (int y = t; y <= b; y++)
            for (int x = l; x <= r; x++) {
                gray[y * W + x] = 0;
                labels[y * W + x] = (byte) label;
            }
    }

    Object make(String name, Object... args) throws Exception {
        var c =
                Class.forName(OmrScoreInterpreter.class.getName() + "$" + name)
                        .getDeclaredConstructors()[0];
        c.setAccessible(true);
        return c.newInstance(args);
    }

    Object candidate(int l, int r, int t, int b, int label) throws Exception {
        return make(
                "AccidentalCandidate",
                make("Component", 80, l, r, t, b, (l + r) * .5f, (t + b) * .5f),
                (byte) label);
    }

    void fixture(boolean spine, boolean beam, boolean bowl) throws Exception {
        if (spine) rect(70, 72, 31, 74, 1);
        if (bowl) {
            rect(72, 80, 55, 58, 3);
            rect(79, 82, 57, 66, 3);
            rect(75, 80, 66, 70, 3);
            rect(72, 76, 70, 73, 3);
        }
        candidates.add(candidate(70, 82, 55, 74, 3));
        if (beam)
            for (int y : new int[] {31, 44}) {
                rect(40, 115, y, y + 5, 5);
                candidates.add(candidate(40, 115, y, y + 5, 5));
            }
    }

    boolean read() throws Exception {
        Object head = make("Component", 180, 88, 108, 58, 74, 98f, 65f);
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "flatBowlUnderBeamedStem",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        List.class,
                        head.getClass(),
                        float.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, labels, gray, W, H, candidates, head, 18f);
    }

    @Test
    public void crossingBeamsDoNotTurnFlatIntoSharp() throws Exception {
        fixture(true, true, true);
        assertTrue(read());
    }

    @Test
    public void beamsCannotSupplyAnAbsentSpine() throws Exception {
        fixture(false, true, true);
        assertFalse(read());
    }

    @Test
    public void stemAndBeamsCannotSupplyAbsentBowl() throws Exception {
        fixture(true, true, false);
        assertFalse(read());
    }

    @Test
    public void recoveryRequiresBeamEvidence() throws Exception {
        fixture(true, false, true);
        assertFalse(read());
    }

    @Test
    public void secondUprightPreventsFlatRecovery() throws Exception {
        fixture(true, true, true);
        rect(80, 82, 31, 74, 1);
        assertFalse(read());
    }

    @Test
    public void sourcePixelsArePreserved() throws Exception {
        fixture(true, true, true);
        var g = gray.clone();
        var l = labels.clone();
        read();
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }
}
