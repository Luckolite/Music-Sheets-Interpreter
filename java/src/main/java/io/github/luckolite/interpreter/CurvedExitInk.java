// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;

/** A complete thin departure curve is not another oval attack. */
final class CurvedExitInk {
    record Box(int left, int top, int right, int bottom, float x, float y) {}

    record Mark(int[] pixels) {}

    record TieInk(byte[] labels, byte[] gray) {}

    private record Frame(float slope, float phase) {}

    private CurvedExitInk() {}

    static boolean matches(
            byte[] gray, int width, int height, Box fragment, Box main, float bottom, float gap) {
        return find(gray, width, height, fragment, main, bottom, gap) != null;
    }

    static TieInk withoutOwnedCurves(byte[] labels, byte[] gray, List<Mark> marks) {
        if (marks.isEmpty()) return new TieInk(labels, gray);
        byte[] cleanLabels = labels.clone(), cleanGray = gray.clone();
        for (Mark mark : marks)
            for (int pixel : mark.pixels) {
                cleanLabels[pixel] = 0;
                cleanGray[pixel] = (byte) 255;
            }
        return new TieInk(cleanLabels, cleanGray);
    }

    static Mark find(
            byte[] gray, int width, int height, Box fragment, Box main, float bottom, float gap) {
        if (gray == null || gray.length != (long) width * height || gap < 5) return null;
        float dx = fragment.x - main.x, dy = main.y - fragment.y;
        if (dx < gap
                || dx > gap * 3
                || dy < gap * .35f
                || dy > gap * 1.4f
                || main.right - main.left + 1 < gap * 1.4f
                || fragment.right - fragment.left + 1 > gap * 1.3f
                || fragment.bottom - fragment.top + 1 > gap * 1.1f) return null;
        int left = main.right + 1, right = fragment.right + Math.round(gap * 1.6f);
        int top = fragment.top - Math.round(gap * 1.6f),
                last = fragment.bottom + Math.round(gap * .5f);
        if (left < 1
                || right >= width - 1
                || top < 1
                || last >= height - 1
                || left >= fragment.left) return null;
        int threshold =
                Math.max(
                        32,
                        BeamInkThreshold.at(
                                        gray, width, height, Math.round(fragment.x), top, last, gap)
                                - 8);
        int white = 0;
        for (int y = main.top + (main.bottom - main.top + 1) / 4;
                y <= main.bottom - (main.bottom - main.top + 1) / 4;
                y++)
            for (int x = main.left + (main.right - main.left + 1) / 4;
                    x <= main.right - (main.right - main.left + 1) / 4;
                    x++)
                if (x >= 0
                        && x < width
                        && y >= 0
                        && y < height
                        && (gray[y * width + x] & 255) >= threshold + 16) white++;
        if (white < Math.max(4, Math.round(gap * gap * .08f))) return null;
        if (straightStem(gray, width, height, fragment, gap, threshold)) return null;
        Frame frame = frame(gray, width, height, fragment, main, bottom, gap, threshold);
        if (frame == null) return null;
        int w = right - left + 1, h = last - top + 1;
        boolean[] raw = new boolean[w * h], ink = new boolean[w * h], rule = new boolean[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int at = y * w + x;
                raw[at] = (gray[(top + y) * width + left + x] & 255) < threshold;
                float shift = frame.phase + frame.slope * (left + x - fragment.x);
                for (int line = 0; line < 5; line++)
                    if (Math.abs(top + y - (bottom - 4 * gap + line * gap + shift)) <= gap * .23f)
                        rule[at] = true;
                ink[at] = raw[at] && !rule[at];
            }
        // A sloping curve needs neighboring columns to witness both sides of
        // a removed horizontal stripe. Restore only original dark pixels.
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; ) {
                if (!rule[y * w + x]) {
                    y++;
                    continue;
                }
                int first = y;
                while (y < h && rule[y * w + x]) y++;
                if (first == 0 || y == h) continue;
                boolean above = false, below = false;
                for (int xx = Math.max(0, x - 2); xx <= Math.min(w - 1, x + 2); xx++) {
                    above |= raw[(first - 1) * w + xx];
                    below |= raw[y * w + xx];
                }
                if (above && below)
                    for (int yy = first; yy < y; yy++) ink[yy * w + x] = raw[yy * w + x];
            }
        int seed = -1;
        for (int y = Math.max(top, fragment.top);
                y <= Math.min(last, fragment.bottom) && seed < 0;
                y++)
            for (int x = Math.max(left, fragment.left); x <= Math.min(right, fragment.right); x++)
                if (ink[(y - top) * w + x - left]) {
                    seed = (y - top) * w + x - left;
                    break;
                }
        if (seed < 0) return null;
        boolean[] component = new boolean[w * h];
        int[] queue = new int[w * h];
        int read = 0, count = 1;
        queue[0] = seed;
        component[seed] = true;
        int minX = w, minY = h, maxX = -1, maxY = -1;
        while (read < count) {
            int at = queue[read++], x = at % w, y = at / w;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            for (int yy = Math.max(0, y - 1); yy <= Math.min(h - 1, y + 1); yy++)
                for (int xx = Math.max(0, x - 1); xx <= Math.min(w - 1, x + 1); xx++) {
                    int next = yy * w + xx;
                    if (ink[next] && !component[next]) {
                        component[next] = true;
                        queue[count++] = next;
                    }
                }
        }
        int span = maxX - minX + 1, rise = maxY - minY + 1;
        if (minX == 0
                || maxX == w - 1
                || minY == 0
                || maxY == h - 1
                || span < gap * 1.2f
                || span > gap * 2.5f
                || rise < gap * .9f
                || rise > gap * 2.2f
                || span < (fragment.right - fragment.left + 1) * 1.3f
                || rise < (fragment.bottom - fragment.top + 1) * 1.4f
                || count > span * rise * .55f) return null;
        float[] centers = new float[span];
        int[] thickness = new int[span];
        float[] bins = new float[3];
        int[] populations = new int[3];
        for (int x = minX; x <= maxX; x++) {
            int pixels = 0, sum = 0, runs = 0;
            boolean previous = false;
            for (int y = minY; y <= maxY; y++) {
                boolean dark = component[y * w + x];
                if (dark) {
                    pixels++;
                    sum += y;
                    if (!previous) runs++;
                }
                previous = dark;
            }
            if (pixels == 0 || runs > 1) return null;
            int i = x - minX;
            centers[i] = sum / (float) pixels;
            thickness[i] = pixels;
            int bin = Math.min(2, i * 3 / span);
            bins[bin] += centers[i];
            populations[bin]++;
        }
        int checked = 0, thin = 0;
        for (int x = 2; x < span - 2; x++) {
            float slope = (centers[x + 2] - centers[x - 2]) * .25f;
            if (thickness[x] / Math.sqrt(1 + slope * slope) <= Math.max(3, gap * .4f)) thin++;
            if (centers[x + 1] > centers[x] + 1.5f) return null;
            checked++;
        }
        if (checked < gap * .7f || thin < checked * .85f) return null;
        for (int i = 0; i < 3; i++) {
            if (populations[i] == 0) return null;
            bins[i] /= populations[i];
        }
        boolean curve =
                bins[0] - bins[1] >= gap * .08f
                        && bins[1] - bins[2] >= gap * .25f
                        && bins[0] - bins[2] >= gap * .6f
                        && bins[1] - (bins[0] + bins[2]) * .5f >= gap * .08f;
        if (!curve) return null;
        int[] pixels = new int[count];
        for (int i = 0; i < count; i++)
            pixels[i] = (top + queue[i] / w) * width + left + queue[i] % w;
        return new Mark(pixels);
    }

    private static boolean straightStem(
            byte[] gray, int width, int height, Box head, float gap, int threshold) {
        for (int x = Math.max(0, head.left - 1); x <= Math.min(width - 1, head.right + 1); x++)
            for (int direction : new int[] {-1, 1}) {
                int first = direction < 0 ? head.top : head.bottom, run = 0;
                for (int distance = 1; distance <= Math.round(gap * 3); distance++) {
                    int y = first + direction * distance;
                    if (y < 0 || y >= height) break;
                    if ((gray[y * width + x] & 255) >= threshold) break;
                    run++;
                }
                if (run >= gap * 1.5f) return true;
            }
        return false;
    }

    private static Frame frame(
            byte[] gray,
            int width,
            int height,
            Box fragment,
            Box main,
            float bottom,
            float gap,
            int threshold) {
        int left = (int) Math.floor(main.left - 2 * gap),
                leftEnd = (int) Math.ceil(main.left - gap * .8f);
        int right = (int) Math.floor(fragment.right + 2 * gap),
                rightEnd = (int) Math.ceil(fragment.right + 3.2f * gap);
        if (left < 0 || rightEnd >= width) return null;
        float top = bottom - 4 * gap;
        int occupied = Math.max(0, Math.min(4, Math.round((fragment.y - top) / gap)));
        float[] slopes = new float[4], phases = new float[4];
        int fits = 0;
        for (int line = 0; line < 5; line++) {
            if (line == occupied) continue;
            float seed = top + line * gap;
            double sx = 0, sy = 0, sxx = 0, sxy = 0;
            int n = 0, l = 0, r = 0;
            List<float[]> points = new ArrayList<>();
            for (int side = 0; side < 2; side++)
                for (int x = side == 0 ? left : right; x <= (side == 0 ? leftEnd : rightEnd); x++) {
                    float row = ruleCenter(gray, width, height, x, seed, gap, threshold);
                    if (!Float.isFinite(row)) continue;
                    float dx = x - fragment.x;
                    points.add(new float[] {dx, row});
                    sx += dx;
                    sy += row;
                    sxx += (double) dx * dx;
                    sxy += dx * row;
                    n++;
                    if (side == 0) l++;
                    else r++;
                }
            if (l < (leftEnd - left + 1) * .8f || r < (rightEnd - right + 1) * .8f) continue;
            double denominator = n * sxx - sx * sx;
            if (denominator <= 0) continue;
            float slope = (float) ((n * sxy - sx * sy) / denominator);
            float intercept = (float) ((sy - slope * sx) / n);
            double residual = 0;
            for (float[] point : points)
                residual += Math.abs(point[1] - intercept - slope * point[0]);
            if (Math.abs(slope) > .12f
                    || Math.abs(intercept - seed) > gap * .3f
                    || residual / n > Math.max(.65f, gap * .06f)) continue;
            slopes[fits] = slope;
            phases[fits++] = intercept - seed;
        }
        if (fits < 3) return null;
        float[] sorted = Arrays.copyOf(slopes, fits);
        Arrays.sort(sorted);
        float median = sorted[fits / 2];
        int agree = 0;
        float slope = 0, phase = 0;
        for (int i = 0; i < fits; i++)
            if (Math.abs(slopes[i] - median) <= .04f) {
                agree++;
                slope += slopes[i];
                phase += phases[i];
            }
        return agree >= 3 ? new Frame(slope / agree, phase / agree) : null;
    }

    private static float ruleCenter(
            byte[] gray, int width, int height, int x, float seed, float gap, int threshold) {
        int lo = Math.max(1, (int) Math.floor(seed - gap * .8f)),
                hi = Math.min(height - 2, (int) Math.ceil(seed + gap * .8f));
        int run = 0;
        float best = Float.NaN, distance = Float.POSITIVE_INFINITY;
        for (int y = lo; y <= hi + 1; y++) {
            if (y <= hi && (gray[y * width + x] & 255) < threshold) {
                run++;
                continue;
            }
            if (run > 0) {
                int first = y - run, last = y - 1;
                float center = (first + last) * .5f;
                int fringe = Math.max(1, Math.round(gap * .15f));
                if (first > lo
                        && last < hi
                        && run <= Math.ceil(gap * .45f)
                        && Math.abs(center - seed) <= gap * .6f
                        && Math.abs(center - seed) < distance
                        && (gray[Math.max(0, first - fringe) * width + x] & 255) >= threshold + 8
                        && (gray[Math.min(height - 1, last + fringe) * width + x] & 255)
                                >= threshold + 8) {
                    best = center;
                    distance = Math.abs(center - seed);
                }
                run = 0;
            }
        }
        return best;
    }
}
