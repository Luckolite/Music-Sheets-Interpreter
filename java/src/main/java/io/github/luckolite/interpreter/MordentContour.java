// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Supplemental evidence for a compact unslashed two-cycle ornament. */
final class MordentContour {
    static boolean matches(byte[] gray, int width, PortableNoteOrnaments.Bounds b) {
        if (gray == null
                || width < 1
                || gray.length % width != 0
                || b == null
                || b.left < 0
                || b.top < 0
                || b.right > width
                || b.bottom > gray.length / width
                || b.width() < 12
                || b.height() < 6
                || b.width() < b.height() * 1.4
                || b.width() > b.height() * 3.5) return false;
        int w = b.width(), h = b.height();
        double[] center = new double[w];
        int centralTop = h, centralBottom = -1, outerTop = h, outerBottom = -1;
        for (int x = 0; x < w; x++) {
            double mass = 0, moment = 0;
            int runs = 0, first = h, last = -1;
            boolean inkBefore = false;
            for (int y = 0; y < h; y++) {
                int ink = Math.max(0, 180 - (gray[(b.top + y) * width + b.left + x] & 255));
                if (ink > 0) {
                    if (!inkBefore) runs++;
                    mass += ink;
                    moment += ink * y;
                    first = Math.min(first, y);
                    last = y;
                }
                inkBefore = ink > 0;
            }
            if (mass == 0 || runs != 1) return false;
            if (x >= w * .4 && x <= w * .6) {
                centralTop = Math.min(centralTop, first);
                centralBottom = Math.max(centralBottom, last);
            } else if (x >= w * .1 && x <= w * .9) {
                outerTop = Math.min(outerTop, first);
                outerBottom = Math.max(outerBottom, last);
            }
            center[x] = moment / mass;
        }
        if (centralTop < outerTop - h * .12 || centralBottom > outerBottom + h * .12) return false;
        int a = extremum(center, .05, .25, false);
        int b1 = extremum(center, .25, .50, true);
        int c = extremum(center, .50, .75, false);
        int d = extremum(center, .75, .96, true);
        double minimum = h * .13;
        if (center[b1] - center[a] < minimum
                || center[b1] - center[c] < minimum
                || center[d] - center[c] < minimum) return false;
        if (Math.abs(center[a] - center[c]) > h * .25 || Math.abs(center[b1] - center[d]) > h * .25)
            return false;
        double variation = 0;
        for (int x = a + 1; x <= d; x++) variation += Math.abs(center[x] - center[x - 1]);
        double travel = center[b1] - center[a] + center[b1] - center[c] + center[d] - center[c];
        return variation <= travel * 1.25;
    }

    private static int extremum(double[] values, double from, double to, boolean maximum) {
        int first = (int) Math.ceil(from * (values.length - 1));
        int last = (int) Math.floor(to * (values.length - 1)), best = first;
        for (int i = first + 1; i <= last; i++)
            if (maximum ? values[i] > values[best] : values[i] < values[best]) best = i;
        return best;
    }
}
