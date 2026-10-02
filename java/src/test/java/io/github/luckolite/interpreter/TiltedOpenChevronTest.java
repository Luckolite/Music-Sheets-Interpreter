// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original complete, broken and connected branch shapes. */
public class TiltedOpenChevronTest {
    private static final int W = 1280;

    private Set<Integer> glyph(float shear) {
        Set<Integer> pixels = new HashSet<>();
        for (int x = 0; x <= 20; x++)
            for (int side : new int[] {-1, 1}) {
                int d = 20 - x, y = 226 + (int) Math.round(-shear * d + side * d * .30);
                for (int dy = -1; dy <= 1; dy++) pixels.add((y + dy) * W + 470 + x);
            }
        return pixels;
    }

    private boolean matches(Set<Integer> pixels) {
        int l = W, r = 0, t = 1280, b = 0;
        for (int p : pixels) {
            l = Math.min(l, p % W);
            r = Math.max(r, p % W);
            t = Math.min(t, p / W);
            b = Math.max(b, p / W);
        }
        return TiltedOpenChevron.matches(
                pixels.stream().mapToInt(Integer::intValue).toArray(), W, l, t, r, b);
    }

    @Test
    public void risingPhotoTiltKeepsStraightOpenBranches() {
        assertTrue(matches(glyph(-.18f)));
    }

    @Test
    public void fallingPhotoTiltKeepsStraightOpenBranches() {
        assertTrue(matches(glyph(.18f)));
    }

    @Test
    public void levelCompleteBranchesRemainValid() {
        assertTrue(matches(glyph(0)));
    }

    @Test
    public void missingInteriorColumnCannotBeFilled() {
        var p = glyph(-.18f);
        p.removeIf(x -> x % W == 475);
        assertFalse(matches(p));
    }

    @Test
    public void joinedInteriorCrossbarCannotReopen() {
        var p = glyph(-.18f);
        for (int y = 222; y <= 231; y++) p.add(y * W + 481);
        assertFalse(matches(p));
    }

    @Test
    public void solidTriangleHasNoIndependentOpenArms() {
        var p = glyph(0);
        for (int x = 470; x <= 480; x++) for (int y = 221; y <= 231; y++) p.add(y * W + x);
        assertFalse(matches(p));
    }

    @Test
    public void leftPointingShapeCannotBecomeRightAccent() {
        Set<Integer> p = new HashSet<>();
        for (int n : glyph(0)) p.add((n / W) * W + 960 - n % W);
        assertFalse(matches(p));
    }

    @Test
    public void parallelDetachedStrokesDoNotConverge() {
        Set<Integer> p = new HashSet<>();
        for (int x = 470; x <= 490; x++)
            for (int y : new int[] {219, 220, 231, 232}) p.add(y * W + x);
        assertFalse(matches(p));
    }

    @Test
    public void excessiveShearAbstains() {
        assertFalse(matches(glyph(.45f)));
    }

    @Test
    public void sourcePixelOrderAndContentStayUnchanged() {
        var p = glyph(-.18f);
        int[] a = p.stream().mapToInt(Integer::intValue).toArray(), copy = a.clone();
        TiltedOpenChevron.matches(a, W, 470, 220, 490, 238);
        assertArrayEquals(copy, a);
    }

    @Test
    public void malformedInputsAbstain() {
        assertFalse(TiltedOpenChevron.matches(null, W, 470, 220, 490, 238));
        assertFalse(TiltedOpenChevron.matches(new int[] {1, 2}, 0, 0, 0, 1, 1));
    }
}
