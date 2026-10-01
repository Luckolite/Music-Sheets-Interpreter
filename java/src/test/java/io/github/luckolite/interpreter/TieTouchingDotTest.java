// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original dot disks and thin connecting curves, not score-derived pixels. */
public class TieTouchingDotTest {
    private static final int W = 240, H = 160;
    private final byte[] gray = new byte[W * H];

    public TieTouchingDotTest() {
        Arrays.fill(gray, (byte) 250);
    }

    private void disk(int cx, int cy) {
        for (int dy = -3; dy <= 3; dy++)
            for (int dx = -3; dx <= 3; dx++)
                if (dx * dx + dy * dy <= 10) gray[(cy + dy) * W + cx + dx] = 0;
    }

    private void curve() {
        for (int x = 110; x <= 180; x++) {
            int y = 80 - Math.round((x - 126) * .25f);
            gray[y * W + x] = 0;
        }
    }

    private Object head(int left, int right, int top, int bottom, float x, float y)
            throws Exception {
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = type.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return ctor.newInstance(180, left, right, top, bottom, x, y);
    }

    private int count(boolean neighbor, boolean excluded) throws Exception {
        return count(neighbor, excluded, 16f);
    }

    private int count(boolean neighbor, boolean excluded, float gap) throws Exception {
        Object head = head(90, 110, 73, 87, 100, 80);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "countAugmentationDots",
                        List.class,
                        head.getClass(),
                        float.class,
                        byte[].class,
                        int.class,
                        int.class,
                        boolean.class,
                        List.class,
                        List.class);
        method.setAccessible(true);
        Object other = head(122, 132, 74, 86, 127, 80);
        return (int)
                method.invoke(
                        null,
                        List.of(),
                        head,
                        gap,
                        gray,
                        W,
                        H,
                        false,
                        excluded ? List.of(other) : List.of(),
                        neighbor ? List.of(head, other) : List.of(head));
    }

    @Test
    public void solidTieConnectionKeepsRoundDot() throws Exception {
        disk(126, 80);
        curve();
        assertEquals(1, count(false, false));
    }

    @Test
    public void bareThinCurveCannotBecomeDot() throws Exception {
        curve();
        assertEquals(0, count(false, false));
    }

    @Test
    public void neighboringHeadOwnsItsInk() throws Exception {
        disk(126, 80);
        curve();
        assertEquals(0, count(true, false));
    }

    @Test
    public void restExclusionStillWins() throws Exception {
        disk(126, 80);
        curve();
        assertEquals(0, count(false, true));
    }

    @Test
    public void tinySpeckOnCurveIsInsufficient() throws Exception {
        curve();
        gray[81 * W + 126] = 0;
        assertEquals(0, count(false, false));
    }

    @Test
    public void ordinaryDetachedDotIsUnchanged() throws Exception {
        disk(126, 80);
        assertEquals(1, count(false, false));
    }

    @Test
    public void connectedRestBulbIsNotAugmentation() throws Exception {
        disk(126, 80);
        curve();
        for (int y = 80; y <= 105; y++) {
            int x = 126 + Math.round((y - 80) * .2f);
            gray[y * W + x] = 0;
            gray[y * W + x + 1] = 0;
        }
        assertEquals(0, count(false, false));
    }

    @Test
    public void thickSlurAtFractionalSpacingCannotBecomeDot() throws Exception {
        for (int x = 110; x <= 180; x++)
            for (int y = 55; y <= 110; y++)
                if (Math.abs(y - (78 + (x - 126) * .35)) / Math.sqrt(1 + .35 * .35) <= 2.45)
                    gray[y * W + x] = 0;
        assertEquals(0, count(false, false, 16.625f));
    }

    @Test
    public void roundDotOnTieSurvivesFractionalSpacing() throws Exception {
        disk(126, 80);
        curve();
        assertEquals(1, count(false, false, 16.625f));
    }

    @Test
    public void inputRasterIsUnchanged() throws Exception {
        disk(126, 80);
        curve();
        byte[] saved = gray.clone();
        count(false, false);
        assertArrayEquals(saved, gray);
    }
}
