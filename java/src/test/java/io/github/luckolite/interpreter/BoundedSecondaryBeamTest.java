// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

/** Original geometric evidence and rejection cases, independent of a score. */
public class BoundedSecondaryBeamTest {
    private static final int W = 400, H = 240;

    private static byte[] page() {
        byte[] g = new byte[W * H];
        Arrays.fill(g, (byte) 255);
        for (int x : new int[] {94, 119, 144, 169})
            for (int y = 72; y <= 115; y++) g[y * W + x] = 0;
        for (int x = 94; x <= 169; x++) for (int y = 112; y <= 115; y++) g[y * W + x] = 0;
        for (int x = 94; x <= 144; x++) for (int y = 104; y <= 107; y++) g[y * W + x] = 0;
        return g;
    }

    private static boolean proves(byte[] g) {
        return BoundedSecondaryBeam.proves(
                g, W, H, new float[] {100, 125, 150}, new float[] {79, 72, 79}, 14.4f);
    }

    @Test
    public void twoDistinctRailsWithBoundedInnerAndExtendedOuterProveGroup() {
        assertTrue(proves(page()));
    }

    @Test
    public void stemUpGeometryHasTheSameOwnership() {
        byte[] old = page(), g = new byte[old.length];
        for (int y = 0; y < H; y++) System.arraycopy(old, y * W, g, (H - 1 - y) * W, W);
        assertTrue(
                BoundedSecondaryBeam.proves(
                        g,
                        W,
                        H,
                        new float[] {100, 125, 150},
                        new float[] {H - 1 - 79, H - 1 - 72, H - 1 - 79},
                        14.4f));
    }

    @Test
    public void slopedRailsAndStemsStillProveGroup() {
        byte[] old = page(), g = new byte[old.length];
        Arrays.fill(g, (byte) 255);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int ny = y + Math.round((x - 94) * .08f);
                if (ny >= 0 && ny < H) g[ny * W + x] = old[y * W + x];
            }
        assertTrue(
                BoundedSecondaryBeam.proves(
                        g,
                        W,
                        H,
                        new float[] {100, 125, 150},
                        new float[] {79.48f, 74.48f, 83.48f},
                        14.4f));
    }

    @Test
    public void secondaryRailContinuingToFourthAttackDoesNotOwnThree() {
        byte[] g = page();
        for (int x = 145; x <= 169; x++) for (int y = 104; y <= 107; y++) g[y * W + x] = 0;
        assertFalse(proves(g));
    }

    @Test
    public void oneThickJoinedBandCannotSupplyTwoRails() {
        byte[] g = page();
        for (int x = 94; x <= 144; x++) for (int y = 104; y <= 115; y++) g[y * W + x] = 0;
        assertFalse(proves(g));
    }

    @Test
    public void thinStaffRuleCannotBeTheSecondaryBeam() {
        byte[] g = page();
        for (int x = 94; x <= 144; x++) for (int y = 104; y <= 107; y++) g[y * W + x] = (byte) 255;
        for (int x = 20; x < 380; x++) g[105 * W + x] = 0;
        assertFalse(proves(g));
    }

    @Test
    public void missingMiddleStemDoesNotProveThreeAttacks() {
        byte[] g = page();
        for (int y = 72; y < 104; y++) g[y * W + 119] = (byte) 255;
        assertFalse(proves(g));
    }

    @Test
    public void shortSeparateRailsDoNotConnectAllThree() {
        byte[] g = page();
        for (int x = 121; x <= 133; x++) for (int y = 104; y <= 107; y++) g[y * W + x] = (byte) 255;
        assertFalse(proves(g));
    }

    @Test
    public void inputPixelsArePreserved() {
        byte[] g = page(), before = g.clone();
        assertTrue(proves(g));
        assertArrayEquals(before, g);
    }

    @Test
    public void malformedBoundsAndNonfiniteGeometryAreRejected() {
        byte[] g = page();
        assertFalse(
                BoundedSecondaryBeam.proves(
                        g,
                        W,
                        H,
                        new float[] {100, 125, Float.NaN},
                        new float[] {79, 72, 79},
                        14.4f));
        assertFalse(
                BoundedSecondaryBeam.proves(
                        g, W, H, new float[] {100, 125, 401}, new float[] {79, 72, 79}, 14.4f));
        assertFalse(
                BoundedSecondaryBeam.proves(
                        new byte[1],
                        W,
                        H,
                        new float[] {100, 125, 150},
                        new float[] {79, 72, 79},
                        14.4f));
        assertFalse(
                BoundedSecondaryBeam.proves(
                        g,
                        W,
                        H,
                        new float[] {100, 125, 150},
                        new float[] {79, 72, 79},
                        Float.POSITIVE_INFINITY));
    }
}
