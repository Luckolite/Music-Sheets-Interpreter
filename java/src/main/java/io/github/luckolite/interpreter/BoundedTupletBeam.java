// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Two continuous beam levels with an inner-level break at both ends of a printed group. */
final class BoundedTupletBeam {
    private BoundedTupletBeam() {}

    static boolean five(
            byte[] gray,
            int width,
            int height,
            float firstX,
            float lastX,
            float nextX,
            float numeralTop,
            float numeralBottom,
            float gap,
            boolean above) {
        if (gray == null
                || width < 1
                || height < 1
                || gray.length != width * height
                || gap < 4
                || lastX - firstX < gap * 3
                || nextX - lastX < gap) return false;
        int direction = above ? 1 : -1;
        float center = (firstX + lastX) * .5f;
        // A stem sits beside the oval. The numeral is centred over the stem-to-stem beam.
        for (float stemOffset : new float[] {gap * .45f, -gap * .45f}) {
            float left = firstX + stemOffset, right = lastX + stemOffset;
            for (int slopeIndex = -20; slopeIndex <= 20; slopeIndex++) {
                float slope = slopeIndex * .02f;
                for (int distance = Math.round(gap * .3f); distance <= gap * 2; distance++) {
                    float outer = (above ? numeralBottom : numeralTop) + direction * distance;
                    if (coverage(
                                    gray,
                                    width,
                                    height,
                                    left + gap * .15f,
                                    right - gap * .15f,
                                    center + stemOffset,
                                    outer,
                                    slope,
                                    gap)
                            < .94) continue;
                    for (int separation = Math.round(gap * .35f);
                            separation <= gap * .8f;
                            separation++) {
                        float inner = outer + direction * separation;
                        if (coverage(
                                        gray,
                                        width,
                                        height,
                                        left + gap * .15f,
                                        right - gap * .15f,
                                        center + stemOffset,
                                        inner,
                                        slope,
                                        gap)
                                < .94) continue;
                        // The short hook on the following regular note can approach the end;
                        // require the clear interval immediately outside the group's stem.
                        double before =
                                coverage(
                                        gray,
                                        width,
                                        height,
                                        left - gap * .45f,
                                        left - gap * .2f,
                                        center + stemOffset,
                                        inner,
                                        slope,
                                        gap);
                        double after =
                                coverage(
                                        gray,
                                        width,
                                        height,
                                        right + gap * .2f,
                                        right + gap * .45f,
                                        center + stemOffset,
                                        inner,
                                        slope,
                                        gap);
                        if (before <= .2 && after <= .2) return true;
                    }
                }
            }
        }
        return false;
    }

    private static double coverage(
            byte[] gray,
            int width,
            int height,
            float left,
            float right,
            float center,
            float baseline,
            float slope,
            float gap) {
        int total = 0, ink = 0, radius = Math.max(1, Math.round(gap * .08f));
        for (int x = Math.round(left); x <= Math.round(right); x++) {
            int y = Math.round(baseline + slope * (x - center));
            if (x < 0 || x >= width || y - radius < 0 || y + radius >= height) return 1;
            boolean core = true;
            for (int dy = -radius; dy <= radius; dy++)
                if ((gray[(y + dy) * width + x] & 255) >= 165) core = false;
            total++;
            if (core) ink++;
        }
        return total > 0 ? ink / (double) total : 1;
    }
}
