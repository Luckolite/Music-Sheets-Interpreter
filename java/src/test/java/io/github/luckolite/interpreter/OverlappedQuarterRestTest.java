// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original tapered quarter silhouette with an additive wave on its right edge. */
public final class OverlappedQuarterRestTest {
    private double[][] edges(boolean waveOnly) {
        int n = 67;
        double[] left = new double[n], right = new double[n];
        double[] time = {0, .23, .39, .54, .65, 1}, value = {5, 10, -1, 6, -2, 16};
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            int part = 0;
            while (part < time.length - 2 && t > time[part + 1]) part++;
            left[i] =
                    waveOnly
                            ? 5 + 5 * Math.sin(t * 6 * Math.PI)
                            : value[part]
                                    + (value[part + 1] - value[part])
                                            * (t - time[part])
                                            / (time[part + 1] - time[part]);
            right[i] = left[i] + (i < 3 || i > n - 4 ? 2 : 14 + 4 * Math.sin(t * 8 * Math.PI));
        }
        return new double[][] {left, right};
    }

    @Test
    public void fullTaperedSilhouetteSurvivesRightSideOverlap() {
        var e = edges(false);
        assertTrue(CompactQuarterRestContour.leftSilhouette(e[0], e[1], 20));
    }

    @Test
    public void repeatingWaveDoesNotSupplyQuarterHook() {
        var e = edges(true);
        assertFalse(CompactQuarterRestContour.leftSilhouette(e[0], e[1], 20));
    }

    @Test
    public void untaperedTopCannotStartQuarter() {
        var e = edges(false);
        e[1][0] = e[0][0] + 14;
        assertFalse(CompactQuarterRestContour.leftSilhouette(e[0], e[1], 20));
    }

    @Test
    public void repeatedTurnInFootCannotFinishQuarter() {
        var e = edges(false);
        for (int i = 52; i < 57; i++) e[0][i] -= 12;
        assertFalse(CompactQuarterRestContour.leftSilhouette(e[0], e[1], 20));
    }

    @Test
    public void shortFragmentCannotBeCompletedByOverlap() {
        var e = edges(false);
        assertFalse(CompactQuarterRestContour.leftSilhouette(e[0], e[1], 30));
    }
}
