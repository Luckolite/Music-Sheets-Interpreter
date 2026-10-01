// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original independent full cores; thin raster seams are not extra beams. */
public class FourBeamInkTest {
    static final int W = 200, H = 170;
    byte[] pixels = new byte[W * H];

    public FourBeamInkTest() {
        Arrays.fill(pixels, (byte) 240);
    }

    void beams(int count, int fourthThickness) {
        for (int x = 60; x <= 90; x++)
            for (int b = 0; b < count; b++)
                for (int y = 50 + b * 9; y < 50 + b * 9 + (b == 3 ? fourthThickness : 5); y++)
                    pixels[y * W + x] = 20;
    }

    int count() {
        return WideTripleBeamInk.countFour(
                pixels, W, H, new int[] {60, 50, -1}, new int[] {90, 50, -1}, 12);
    }

    @Test
    public void fourCoresNeedThreeMatchingColumns() {
        beams(4, 5);
        assertEquals(4, count());
    }

    @Test
    public void threeCoresRemainThree() {
        beams(3, 5);
        assertEquals(0, count());
    }

    @Test
    public void thinFourthLineCannotSupplyFourthCore() {
        beams(4, 1);
        assertEquals(0, count());
    }

    @Test
    public void brokenMiddleColumnRejects() {
        beams(4, 5);
        for (int y = 50; y < 82; y++) for (int x = 74; x <= 76; x++) pixels[y * W + x] = (byte) 240;
        assertEquals(0, count());
    }

    @Test
    public void oppositeShaftsCannotShareFourBeams() {
        beams(4, 5);
        assertEquals(
                0,
                WideTripleBeamInk.countFour(
                        pixels, W, H, new int[] {60, 50, -1}, new int[] {90, 50, 1}, 12));
    }
}
