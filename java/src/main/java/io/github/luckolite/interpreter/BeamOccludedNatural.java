// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Reads the asymmetric lower natural when a verified beam obscures its upper crossbar. */
final class BeamOccludedNatural {
    private BeamOccludedNatural() {}

    static boolean matches(
            byte[] gray,
            int width,
            int height,
            int seedLeft,
            int seedRight,
            float pitchY,
            float gap) {
        if (gray == null || gray.length != width * height || gap < 6) return false;
        for (int left = Math.max(0, seedLeft - 2); left <= seedLeft + 2; left++)
            for (int right = seedRight - 2; right <= Math.min(width - 1, seedRight + 2); right++) {
                if (right - left < gap * .3f || right - left > gap * .85f) continue;
                for (int bottom = Math.round(pitchY + gap * .85f);
                        bottom <= Math.round(pitchY + gap * 1.5f);
                        bottom++)
                    for (int bridge = Math.round(pitchY + gap * .3f);
                            bridge <= Math.round(pitchY + gap * .8f);
                            bridge++) {
                        if (bottom - bridge < gap * .45f
                                || bottom - bridge > gap * .95f
                                || bridge - gap * 2 < 0
                                || bottom + gap * .35f >= height) continue;
                        if (!vertical(gray, width, left, Math.round(bridge - gap * 2), bridge, .9f)
                                || !vertical(
                                        gray,
                                        width,
                                        right,
                                        Math.round(bridge - gap * 1.3f),
                                        bottom,
                                        .9f)) continue;
                        if (!paper(gray, width, left, bridge + 3, bottom, .7f)
                                || !paper(
                                        gray,
                                        width,
                                        right,
                                        bottom + 2,
                                        Math.round(bottom + gap * .3f),
                                        .7f)
                                || !paper(
                                        gray,
                                        width,
                                        (left + right) / 2,
                                        Math.round(bridge - gap * .6f),
                                        Math.round(bridge - gap * .25f),
                                        .7f)) continue;
                        boolean joined = false;
                        for (int rise = 0; rise <= Math.round(gap * .3f); rise++) {
                            int ink = 0;
                            for (int x = left; x <= right; x++) {
                                int y =
                                        Math.round(
                                                bridge
                                                        - rise
                                                                * (x - left)
                                                                / (float) (right - left));
                                if (dark(gray, width, x, y) || dark(gray, width, x, y - 1)) ink++;
                            }
                            if (ink >= (right - left + 1) * .95f) {
                                joined = true;
                                break;
                            }
                        }
                        if (joined && beam(gray, width, height, left, right, bridge, gap))
                            return true;
                    }
            }
        return false;
    }

    private static boolean vertical(
            byte[] gray, int width, int x, int top, int bottom, float fraction) {
        int ink = 0;
        for (int y = top; y <= bottom; y++) if (dark(gray, width, x, y)) ink++;
        return ink >= (bottom - top + 1) * fraction;
    }

    private static boolean paper(
            byte[] gray, int width, int x, int top, int bottom, float fraction) {
        if (bottom < top) return false;
        int clear = 0;
        for (int y = top; y <= bottom; y++) if (!dark(gray, width, x, y)) clear++;
        return clear >= (bottom - top + 1) * fraction;
    }

    private static boolean dark(byte[] gray, int width, int x, int y) {
        return (gray[y * width + x] & 255) < 170;
    }

    private static boolean beam(
            byte[] gray, int width, int height, int left, int right, int bridge, float gap) {
        int first = Math.round(left - gap * 2),
                last = Math.round(right + gap * 2),
                thickness = Math.max(2, Math.round(gap * .16f));
        if (first < 0 || last >= width) return false;
        for (int y = Math.round(bridge - gap * 1.6f); y <= Math.round(bridge - gap * .8f); y++)
            for (int slope = -5; slope <= 5; slope++) {
                int ink = 0;
                for (int x = first; x <= last; x++) {
                    int row = Math.round(y + (x - left) * slope * .1f);
                    if (row < 0 || row + thickness >= height) continue;
                    boolean full = true;
                    for (int dy = 0; dy < thickness; dy++)
                        if (!dark(gray, width, x, row + dy)) {
                            full = false;
                            break;
                        }
                    if (full) ink++;
                }
                if (ink >= (last - first + 1) * .95f) return true;
            }
        return false;
    }
}
