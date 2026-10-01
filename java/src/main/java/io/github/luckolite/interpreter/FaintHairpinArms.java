// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;

/** Recover a faint second arm only beside an already connected, strong straight arm. */
final class FaintHairpinArms {
    record Shape(int top, int bottom, int[] upper, int[] lower, int direction) {}

    static Shape recover(
            byte[] gray,
            int width,
            int height,
            int seed,
            int left,
            int right,
            int top,
            int bottom,
            int[] strongUpper,
            int[] strongLower,
            float gap) {
        int w = right - left + 1;
        if (w < gap * 6 || !straight(strongUpper, gap) && !straight(strongLower, gap)) return null;
        int firstY = Math.max(0, top - (int) Math.ceil(gap)),
                lastY = Math.min(height - 1, bottom + (int) Math.ceil(gap));
        int area = w * (lastY - firstY + 1);
        boolean[] seen = new boolean[area];
        int[] queue = new int[area];
        int start = 0, count = 1;
        queue[0] = seed;
        seen[(seed / width - firstY) * w + seed % width - left] = true;
        int newTop = seed / width, newBottom = newTop;
        int limit = (int) (w * Math.max(8, gap));
        while (start < count) {
            int at = queue[start++], x = at % width, y = at / width;
            newTop = Math.min(newTop, y);
            newBottom = Math.max(newBottom, y);
            if (newBottom - newTop + 1 > gap * 3 || count > limit) return null;
            for (int dy = -2; dy <= 2; dy++)
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < left || nx > right || ny < firstY || ny > lastY) continue;
                    int local = (ny - firstY) * w + nx - left, next = ny * width + nx;
                    if (!seen[local] && (gray[next] & 255) < 185) {
                        seen[local] = true;
                        queue[count++] = next;
                    }
                }
        }
        int[] upper = new int[w], lower = new int[w];
        Arrays.fill(upper, Integer.MAX_VALUE);
        Arrays.fill(lower, Integer.MIN_VALUE);
        for (int i = 0; i < count; i++) {
            int x = queue[i] % width - left, y = queue[i] / width;
            upper[x] = Math.min(upper[x], y);
            lower[x] = Math.max(lower[x], y);
        }
        int direction = ScoreDynamicsDetector.hairpinDirection(upper, lower, gap);
        if (direction == 0) return null;
        // A truncated pair or unrelated neighboring lines cannot supply a real apex.
        int edge = Math.max(1, w / 20);
        double openingLeft = opening(upper, lower, 0, edge),
                openingRight = opening(upper, lower, w - edge, w);
        if (!Double.isFinite(openingLeft)
                || !Double.isFinite(openingRight)
                || Math.min(openingLeft, openingRight) > gap * .25
                || Math.max(openingLeft, openingRight) < gap * .55) return null;
        return new Shape(newTop, newBottom, upper, lower, direction);
    }

    private static double opening(int[] upper, int[] lower, int from, int to) {
        double sum = 0;
        int count = 0;
        for (int x = from; x < to; x++)
            if (upper[x] != Integer.MAX_VALUE) {
                sum += lower[x] - upper[x];
                count++;
            }
        return count < (to - from) * .8 ? Double.NaN : sum / count;
    }

    private static boolean straight(int[] arm, float gap) {
        double[] bins = new double[8];
        int w = arm.length;
        for (int b = 0; b < 8; b++) {
            int count = 0;
            for (int x = b * w / 8; x < (b + 1) * w / 8; x++)
                if (arm[x] != Integer.MAX_VALUE && arm[x] != Integer.MIN_VALUE) {
                    bins[b] += arm[x];
                    count++;
                }
            if (count < Math.max(1, w / 8 * .8)) return false;
            bins[b] /= count;
        }
        for (int b = 0; b < 8; b++)
            if (Math.abs(bins[b] - (bins[0] + (bins[7] - bins[0]) * b / 7))
                    > Math.max(1.4, gap * .14)) return false;
        return true;
    }
}
