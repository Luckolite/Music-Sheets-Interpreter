// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original touching-second geometry, with a stem inside the fused head bounds. */
public final class InteriorChordStemTest {
    private static final int W = 260, H = 240, G = 16;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    public InteriorChordStemTest() {
        Arrays.fill(gray, (byte) 255);
    }

    private void pixel(int x, int y, int label) {
        gray[y * W + x] = 0;
        labels[y * W + x] = (byte) label;
    }

    private void oval(int cx, int cy) {
        for (int y = cy - 8; y <= cy + 8; y++)
            for (int x = cx - 11; x <= cx + 11; x++)
                if (Math.pow((x - cx) / 11d, 2) + Math.pow((y - cy) / 8d, 2) <= 1) pixel(x, y, 2);
    }

    private void draw(int direction, int beams) {
        oval(101, 79);
        oval(119, 71);
        int endpoint = direction > 0 ? 166 : 12;
        for (int y = Math.min(71, endpoint); y <= Math.max(79, endpoint); y++) pixel(110, y, 1);
        for (int b = 0; b < beams; b++)
            for (int x = 110; x <= 185; x++)
                for (int dy = 0; dy < 6; dy++) pixel(x, endpoint - direction * (b * 12 + dy), 5);
    }

    private Object head() throws Exception {
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return constructor.newInstance(560, 90, 130, 63, 87, 110f, 75f);
    }

    private int[] stem() throws Exception {
        Object head = head();
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "attachedRawStem",
                        byte[].class,
                        int.class,
                        int.class,
                        head.getClass(),
                        float.class);
        method.setAccessible(true);
        return (int[]) method.invoke(null, gray, W, H, head, (float) G);
    }

    private int beams() throws Exception {
        Object head = head();
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object staff = constructor.newInstance(80f, 144f, 16f);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "detectBeamCount",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        head.getClass(),
                        type);
        method.setAccessible(true);
        return (int) method.invoke(null, labels, gray, W, H, head, staff);
    }

    @Test
    public void findsInternalDownStem() throws Exception {
        draw(1, 0);
        assertArrayEquals(new int[] {110, 166, 1}, stem());
    }

    @Test
    public void findsInternalUpStem() throws Exception {
        draw(-1, 0);
        assertArrayEquals(new int[] {110, 12, -1}, stem());
    }

    @Test
    public void quarterChordDoesNotBorrowHeadInkAsBeams() throws Exception {
        draw(1, 0);
        assertEquals(0, beams());
    }

    @Test
    public void realSingleBeamIsRetained() throws Exception {
        draw(1, 1);
        assertEquals(1, beams());
    }

    @Test
    public void realDoubleBeamIsRetained() throws Exception {
        draw(1, 2);
        assertEquals(2, beams());
    }

    @Test
    public void headInkAloneCannotProveStem() throws Exception {
        oval(101, 79);
        oval(119, 71);
        assertNull(stem());
    }

    @Test
    public void shortInternalStrokeIsNotAStem() throws Exception {
        draw(1, 0);
        for (int y = 109; y <= 166; y++) {
            gray[y * W + 110] = (byte) 255;
            labels[y * W + 110] = 0;
        }
        assertNull(stem());
    }

    @Test
    public void sourcePixelsRemainUnchanged() throws Exception {
        draw(1, 2);
        byte[] g = gray.clone(), l = labels.clone();
        stem();
        beams();
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }

    @Test
    public void twoOppositeInteriorContinuationsAreAmbiguous() throws Exception {
        draw(-1, 1);
        for (int y = 79; y <= 166; y++) pixel(110, y, 1);
        assertNull(stem());
    }
}
