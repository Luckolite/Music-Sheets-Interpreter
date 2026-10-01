// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;

/** Isolated approach strokes: remove staff lines, then verify the entire stroke is straight. */
final class NoteSlideDetector {
    record Head(float x, float y, float gap, int staff, int measure) {}

    record Staff(float top, float bottom, float gap) {}

    record Stroke(
            float left,
            float leftY,
            float right,
            float rightY,
            int direction,
            int noteIndex,
            boolean connected) {}

    private NoteSlideDetector() {}

    static byte[] removeStaffLines(byte[] gray, int w, int h, List<Staff> staffs) {
        byte[] clean = gray.clone();
        // A diagonal survives a staff crossing when ink continues immediately above and below.
        for (var s : staffs)
            for (int line = 0; line < 5; line++) {
                int center = Math.round(s.top + line * s.gap), top = h, bottom = -1;
                for (int y = Math.max(2, center - 3); y <= Math.min(h - 3, center + 3); y++) {
                    int count = 0;
                    for (int x = 0; x < w; x++) if ((gray[y * w + x] & 255) < 150) count++;
                    if (count > w * .45) {
                        top = Math.min(top, y);
                        bottom = Math.max(bottom, y);
                    }
                }
                if (bottom < top) continue;
                for (int x = 0; x < w; x++) {
                    boolean above = false, below = false;
                    for (int dx = -2; dx <= 2; dx++)
                        if (x + dx >= 0 && x + dx < w)
                            for (int dy = 1; dy <= 2; dy++) {
                                above |= (gray[(top - dy) * w + x + dx] & 255) < 150;
                                below |= (gray[(bottom + dy) * w + x + dx] & 255) < 150;
                            }
                    if (!above || !below)
                        for (int y = top; y <= bottom; y++) clean[y * w + x] = (byte) 255;
                }
            }
        return clean;
    }

    static List<Stroke> detect(byte[] gray, int w, int h, List<Staff> staffs, List<Head> heads) {
        byte[] clean = removeStaffLines(gray, w, h, staffs);
        boolean[] ink = new boolean[gray.length];
        for (int i = 0; i < ink.length; i++) ink[i] = (clean[i] & 255) < 150;
        int[] queue = new int[ink.length];
        List<Stroke> result = new ArrayList<>();
        for (int pixel = 0; pixel < ink.length; pixel++) {
            if (!ink[pixel]) continue;
            int start = 0, end = 1;
            queue[0] = pixel;
            ink[pixel] = false;
            int l = w, r = 0, t = h, b = 0;
            double sx = 0, sy = 0, sxx = 0, syy = 0, sxy = 0;
            while (start < end) {
                int at = queue[start++], x = at % w, y = at / w;
                l = Math.min(l, x);
                r = Math.max(r, x);
                t = Math.min(t, y);
                b = Math.max(b, y);
                sx += x;
                sy += y;
                sxx += (double) x * x;
                syy += (double) y * y;
                sxy += (double) x * y;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                        int next = ny * w + nx;
                        if (ink[next]) {
                            ink[next] = false;
                            queue[end++] = next;
                        }
                    }
            }
            if (end < 8 || r - l < 6 || b - t < 5 || b - t > (r - l) * 5) continue;
            double cx = sx / end,
                    cy = sy / end,
                    vx = sxx / end - cx * cx,
                    vy = syy / end - cy * cy,
                    cov = sxy / end - cx * cy;
            if (vx <= 0 || vy <= 0) continue;
            double slope = cov / vx, correlation = cov / Math.sqrt(vx * vy);
            // Absolute residual accounts for the raster thickness of a very small slash.
            double residual =
                    Math.sqrt(
                            Math.max(
                                    0,
                                    (vx + vy - Math.sqrt((vx - vy) * (vx - vy) + 4 * cov * cov))
                                            / 2));
            // A nearly vertical raster has little horizontal variance, so its
            // stroke thickness lowers Pearson correlation. Both printed heads
            // are mandatory below for these steep strokes.
            boolean steep = Math.abs(slope) > 1.5;
            if (Math.abs(correlation) < (steep ? .85 : .90)
                    || residual > (steep ? 1.4 : 1.3)
                    || Math.abs(slope) < .08
                    || Math.abs(slope) > 5) continue;
            double rightY = cy + (r - cx) * slope;
            int owner = -1;
            double best = Double.MAX_VALUE;
            for (int i = 0; i < heads.size(); i++) {
                var n = heads.get(i);
                float g = n.gap;
                double dx = (n.x - r) / g, dy = (n.y - rightY) / g;
                // Long or very shallow ink is meaningful only between two uniquely
                // proved consecutive attacks. Keep isolated approach limits unchanged.
                boolean extended = r - l > g * 6 || Math.abs(slope) < .2;
                if (n.staff < 0
                        || dx < .5
                        || dx > 3
                        || Math.abs(dy) > 1
                        || r - l < g * .65
                        || r - l > g * 12
                        || (b - t < (r - l) * .2 && !extended)
                        || (extended
                                && !immediateSource(heads, n, l, cy + (l - cx) * slope, slope)))
                    continue;
                boolean touchesHead = false;
                for (int j = 0; j < heads.size(); j++)
                    if (j != i) {
                        var other = heads.get(j);
                        if (other.x > l - g * .6f
                                && other.x < r + g * .6f
                                && Math.abs(other.y - (cy + (other.x - cx) * slope)) < g * .6f) {
                            touchesHead = true;
                            break;
                        }
                    }
                if (touchesHead) continue;
                // Small note-to-note wavy glissandi may have a shallower overall
                // slope than an isolated approach slash. Their preceding printed
                // endpoint must agree with the line; a slur still fails residuals.
                if (Math.abs(slope) < .55 || Math.abs(slope) > 1.5 || r - l > g * 3.5) {
                    boolean prior = false;
                    double leftY = cy + (l - cx) * slope;
                    for (Head other : heads) {
                        if (other == n || other.staff != n.staff || other.measure != n.measure)
                            continue;
                        double distance = (l - other.x) / g;
                        if (distance >= .5
                                && distance <= 3
                                && Math.abs(other.y - leftY) < g
                                && (n.y - other.y) * slope > 0) prior = true;
                    }
                    if (!prior) continue;
                }
                double distance = dx + Math.abs(dy) * 2;
                if (distance < best) {
                    best = distance;
                    owner = i;
                }
            }
            if (owner >= 0)
                result.add(
                        new Stroke(
                                l,
                                (float) (cy + (l - cx) * slope),
                                r,
                                (float) rightY,
                                slope < 0 ? 1 : -1,
                                owner,
                                immediateSource(
                                        heads, heads.get(owner), l, cy + (l - cx) * slope, slope)));
        }
        return List.copyOf(result);
    }

    /** A printed two-note glide starts at its unique immediate attack, not a scale neighbor. */
    private static boolean immediateSource(
            List<Head> heads, Head target, float left, double leftY, double slope) {
        float gap = target.gap, latest = -Float.MAX_VALUE;
        for (var head : heads)
            if (head.staff == target.staff
                    && head.measure == target.measure
                    && head.x < target.x - gap * .35f) latest = Math.max(latest, head.x);
        Head source = null;
        for (var head : heads)
            if (head.staff == target.staff
                    && head.measure == target.measure
                    && head.x < target.x - gap * .35f
                    && latest - head.x <= gap * .35f) {
                // The existing wire stores "previous attack", not one selected head of a chord.
                if (source != null) return false;
                source = head;
            }
        if (source == null) return false;
        double distance = (left - source.x) / gap;
        return distance >= .5
                && distance <= 3
                && Math.abs(source.y - leftY) < gap
                && (target.y - source.y) * slope > 0;
    }

    /** Verify the connecting line before accepting an OCR portamento word (or a clipped p/t). */
    static boolean connection(byte[] gray, int w, int h, Head from, Head to) {
        float g = to.gap, span = to.x - from.x;
        if (span < g * 4 || span > g * 30 || Math.abs(to.y - from.y) > g * 4) return false;
        for (float li = 1; li <= 2; li += .5f)
            for (float ri = 1; ri <= 2; ri += .5f) {
                int left = Math.round(from.x + g * li),
                        right = Math.round(to.x - g * ri),
                        samples = right - left;
                if (samples < g * 2) continue;
                for (float a = -.6f; a <= .61f; a += .2f)
                    for (float b = -.6f; b <= .61f; b += .2f) {
                        int hits = 0;
                        for (int x = left; x < right; x++) {
                            float t = (x - from.x) / span;
                            int y = Math.round((from.y + g * a) * (1 - t) + (to.y + g * b) * t);
                            if (x < 0 || x >= w || y < 1 || y >= h - 1) continue;
                            if ((gray[(y - 1) * w + x] & 255) < 150
                                    || (gray[y * w + x] & 255) < 150
                                    || (gray[(y + 1) * w + x] & 255) < 150) hits++;
                        }
                        if (hits >= samples * .90f) return true;
                    }
            }
        return false;
    }
}
