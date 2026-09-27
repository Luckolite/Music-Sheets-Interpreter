// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original naturals whose boxes overlap but whose actual ink is disconnected. */
public class OverlappingAccidentalBoxesTest {
    static final int W = 150, H = 150;
    final byte[] labels = new byte[W * H];

    void rect(int l, int r, int t, int b) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) labels[y * W + x] = 3;
    }

    void natural(int x, int y) {
        rect(x, x + 2, y, y + 32);
        rect(x + 10, x + 12, y + 9, y + 43);
        rect(x, x + 12, y + 9, y + 12);
        rect(x, x + 12, y + 29, y + 32);
    }

    Object original() throws Exception {
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "findComponents", byte[].class, int.class, int.class, byte.class);
        m.setAccessible(true);
        Object component = ((List<?>) m.invoke(null, labels, W, H, (byte) 3)).get(0);
        var c =
                Class.forName(OmrScoreInterpreter.class.getName() + "$AccidentalCandidate")
                        .getDeclaredConstructors()[0];
        c.setAccessible(true);
        return c.newInstance(component, (byte) 3);
    }

    boolean natural(Object candidate) throws Exception {
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "isNaturalGlyph",
                        byte[].class,
                        int.class,
                        int.class,
                        candidate.getClass(),
                        float.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, labels, W, H, candidate, 18f);
    }

    @Test
    public void neighboringNaturalCannotExtendOriginalSpine() throws Exception {
        natural(60, 30);
        Object first = original();
        assertTrue(natural(first));
        natural(54, 63);
        assertTrue(natural(first));
    }

    @Test
    public void unrelatedDisconnectedInkDoesNotChangeACompleteNatural() throws Exception {
        natural(60, 30);
        Object first = original();
        rect(64, 65, 48, 50);
        assertTrue(natural(first));
    }

    @Test
    public void realConnectedSpineExtensionsAreNotDiscarded() throws Exception {
        natural(60, 30);
        rect(70, 72, 30, 39);
        rect(60, 62, 62, 73);
        assertFalse(natural(original()));
    }

    @Test
    public void classificationDoesNotEraseNeighborPixels() throws Exception {
        natural(60, 30);
        Object first = original();
        natural(54, 63);
        var before = labels.clone();
        natural(first);
        assertArrayEquals(before, labels);
    }

    @Test
    public void neighboringNaturalCannotFillTheUpperRightOfAFlat() throws Exception {
        rect(60, 62, 30, 73);
        rect(62, 70, 54, 57);
        rect(69, 72, 56, 65);
        rect(65, 70, 65, 69);
        rect(62, 66, 69, 72);
        Object flat = original();
        natural(70, 3);
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "isFlatGlyph",
                        byte[].class,
                        int.class,
                        int.class,
                        flat.getClass(),
                        float.class);
        m.setAccessible(true);
        assertTrue((boolean) m.invoke(null, labels, W, H, flat, 18f));
    }

    private boolean rawNatural(boolean lowerBar) throws Exception {
        natural(60, 30);
        Object seed = original();
        if (!lowerBar)
            for (int y = 59; y <= 62; y++) for (int x = 63; x < 70; x++) labels[y * W + x] = 0;
        natural(54, 63);
        byte[] gray = new byte[labels.length];
        for (int i = 0; i < gray.length; i++) gray[i] = labels[i] == 0 ? (byte) 255 : 0;
        var cc =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Component")
                        .getDeclaredConstructors()[0];
        cc.setAccessible(true);
        Object head = cc.newInstance(180, 85, 105, 44, 60, 95f, 52f);
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "rawNaturalFromCrossbars",
                        byte[].class,
                        int.class,
                        int.class,
                        List.class,
                        head.getClass(),
                        float.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, gray, W, H, List.of(seed), head, 18f);
    }

    @Test
    public void rawNeighborCannotExtendACompleteNaturalPastTheCrop() throws Exception {
        assertTrue(rawNatural(true));
    }

    @Test
    public void isolatedRawGlyphStillNeedsBothConnectors() throws Exception {
        assertFalse(rawNatural(false));
    }
}
