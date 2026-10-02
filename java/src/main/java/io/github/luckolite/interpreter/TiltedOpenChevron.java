// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Two thin, straight branches join at one right tip despite a bounded photograph tilt. */
final class TiltedOpenChevron {
    private TiltedOpenChevron() {}

    static boolean matches(int[] pixels, int stride, int left, int top, int right, int bottom) {
        int w = right - left + 1, h = bottom - top + 1;
        if (pixels == null || stride <= 0 || w < 10 || h < 6 || w < h * 1.25 || w > h * 4)
            return false;
        boolean[][] ink = new boolean[w][h];
        for (int p : pixels) {
            int x = p % stride - left, y = p / stride - top;
            if (x < 0 || x >= w || y < 0 || y >= h) return false;
            ink[x][y] = true;
        }
        double[] upper = new double[w], lower = new double[w], tip = new double[w];
        int[] runs = new int[w];
        for (int x = 0; x < w; x++) {
            int first = -1, last = -1, firstEnd = -1, second = -1;
            for (int y = 0; y < h; y++)
                if (ink[x][y]) {
                    if (first < 0) first = y;
                    last = y;
                    if (y == 0 || !ink[x][y - 1]) {
                        runs[x]++;
                        if (runs[x] == 2) second = y;
                    }
                    if (runs[x] == 1) firstEnd = y;
                }
            if (runs[x] < 1 || runs[x] > 2) return false;
            if (runs[x] == 2) {
                if (firstEnd - first + 1 > Math.max(3, h * .3)
                        || last - second + 1 > Math.max(3, h * .3)) return false;
                upper[x] = (first + firstEnd) * .5;
                lower[x] = (second + last) * .5;
            } else tip[x] = (first + last) * .5;
        }
        int first = 1, last = (int) Math.floor((w - 1) * .45);
        if (last - first + 1 < 4) return false;
        for (int x = first; x <= last; x++)
            if (runs[x] != 2 || lower[x] - upper[x] < h * .3) return false;
        double[] a = fit(upper, first, last), b = fit(lower, first, last);
        if (a[0] <= 0
                || b[0] >= 0
                || a[0] - b[0] < .35
                || Math.abs((a[0] + b[0]) * .5) > .25
                || a[2] > .65
                || b[2] > .65) return false;
        double intersection = (b[1] - a[1]) / (a[0] - b[0]);
        if (Math.abs(intersection - (w - 1)) > Math.max(1.5, w * .12)) return false;
        double center = a[1] + a[0] * intersection;
        if (center < h * .15 || center > h * .85) return false;
        boolean joined = false;
        for (int x = first; x < w; x++) {
            if (runs[x] == 2) {
                if (joined
                        || Math.abs(upper[x] - (a[1] + a[0] * x)) > .9
                        || Math.abs(lower[x] - (b[1] + b[0] * x)) > .9) return false;
            } else {
                joined = true;
                if (Math.abs((a[1] + a[0] * x) - (b[1] + b[0] * x)) > Math.max(3, h * .4)
                        || Math.abs(tip[x] - (a[1] + a[0] * x + b[1] + b[0] * x) * .5) > .9)
                    return false;
            }
        }
        for (int x = (int) Math.ceil((w - 1) * .8); x < w; x++) {
            if (runs[x] != 1
                    || Math.abs(tip[x] - (a[1] + a[0] * x)) > 1.7
                    || Math.abs(tip[x] - (b[1] + b[0] * x)) > 1.7) return false;
        }
        return true;
    }

    private static double[] fit(double[] values, int first, int last) {
        int n = last - first + 1;
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (int x = first; x <= last; x++) {
            sx += x;
            sy += values[x];
            sxx += (double) x * x;
            sxy += x * values[x];
        }
        double slope = (n * sxy - sx * sy) / (n * sxx - sx * sx),
                intercept = (sy - slope * sx) / n,
                error = 0;
        for (int x = first; x <= last; x++) {
            double residual = values[x] - intercept - slope * x;
            error += residual * residual;
        }
        return new double[] {slope, intercept, Math.sqrt(error / n)};
    }
}
