// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;

/** Isolated continuous pedal rails have two short upward hooks, unlike octave dashes or beams. */
final class PedalBracketDetector {
    record Bracket(
            float left,
            float right,
            float baseline,
            float slope,
            float gap,
            int staffIndex,
            int staffCount) {}

    private PedalBracketDetector() {}

    static List<Bracket> detect(
            byte[] gray, int width, int height, List<PlayingTechniqueDetector.Staff> staffs) {
        if (gray == null
                || width <= 0
                || height <= 0
                || (long) width * height != gray.length
                || staffs == null) return List.of();
        var result = new ArrayList<Bracket>();
        for (var staff : staffs) {
            float gap = staff.gap();
            if (!Float.isFinite(gap)
                    || gap < 5
                    || !Float.isFinite(staff.top())
                    || !Float.isFinite(staff.bottom())
                    || staff.bottom() <= staff.top()
                    || staff.count() < 1
                    || staff.index() < 0
                    || staff.index() >= staff.count()) continue;
            int first = Math.max(0, (int) Math.ceil(staff.bottom() + gap * .3));
            int last = Math.min(height - 1, (int) Math.floor(staff.bottom() + gap * 16));
            for (var next : staffs)
                if (next.top() > staff.bottom() + gap * .5)
                    last = Math.min(last, (int) Math.floor(next.top() - gap * .3));
            if (last <= first) continue;
            int cells = Math.multiplyExact(width, last - first + 1);
            var visited = new boolean[cells];
            var queue = new int[cells];
            for (int local = 0; local < cells; local++) {
                if (visited[local] || (gray[first * width + local] & 255) >= 165) continue;
                visited[local] = true;
                queue[0] = local;
                int count = 1;
                int left = width, right = -1, top = height, bottom = -1;
                for (int at = 0; at < count; at++) {
                    int cell = queue[at], x = cell % width, y = cell / width + first;
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dx = -1; dx <= 1; dx++) {
                            int xx = x + dx, yy = y + dy;
                            if (xx < 0 || xx >= width || yy < first || yy > last) continue;
                            int next = (yy - first) * width + xx;
                            if (!visited[next] && (gray[yy * width + xx] & 255) < 165) {
                                visited[next] = true;
                                queue[count++] = next;
                            }
                        }
                }
                if (top == first
                        || bottom == last
                        || right - left < gap * 6
                        || bottom - top > gap * 2 + (right - left) * .18) continue;
                var bracket = prove(queue, count, width, first, left, right, gap, staff);
                if (bracket != null) result.add(bracket);
            }
        }
        result.sort(
                Comparator.comparingDouble(Bracket::baseline).thenComparingDouble(Bracket::left));
        return List.copyOf(result);
    }

    private static Bracket prove(
            int[] queue,
            int count,
            int width,
            int first,
            int left,
            int right,
            float gap,
            PlayingTechniqueDetector.Staff staff) {
        int span = right - left + 1;
        var floor = new int[span];
        Arrays.fill(floor, -1);
        for (int i = 0; i < count; i++) {
            int cell = queue[i], x = cell % width - left, y = cell / width + first;
            floor[x] = Math.max(floor[x], y);
        }
        int edge = Math.max(3, Math.round(gap * .28f));
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        int samples = 0;
        for (int x = edge; x < span - edge; x++)
            if (floor[x] >= 0) {
                sx += x;
                sy += floor[x];
                sxx += x * (double) x;
                sxy += x * (double) floor[x];
                samples++;
            }
        if (samples < span - edge * 2 - 1) return null;
        double denominator = samples * sxx - sx * sx;
        if (denominator <= 0) return null;
        double slope = (samples * sxy - sx * sy) / denominator,
                intercept = (sy - slope * sx) / samples;
        if (Math.abs(slope) > .18) return null;
        double residual = 0;
        for (int x = edge; x < span - edge; x++)
            if (floor[x] >= 0) {
                double delta = floor[x] - (intercept + slope * x);
                residual += delta * delta;
            }
        if (Math.sqrt(residual / samples) > Math.max(.8, gap * .06)) return null;
        double leftHook = 0, rightHook = 0;
        int covered = 0;
        for (int x = 0; x < span; x++)
            if (floor[x] >= 0
                    && Math.abs(floor[x] - (intercept + slope * x)) <= Math.max(1.5, gap * .1))
                covered++;
        if (covered < span * .985) return null;
        for (int i = 0; i < count; i++) {
            int cell = queue[i], x = cell % width - left, y = cell / width + first;
            double above = intercept + slope * x - y;
            if (above < -Math.max(1.5, gap * .1)) return null;
            if (x < edge) leftHook = Math.max(leftHook, above);
            else if (x >= span - edge) rightHook = Math.max(rightHook, above);
            else if (above > Math.max(2, gap * .22)) return null;
        }
        if (leftHook < gap * .35
                || rightHook < gap * .35
                || leftHook > gap * 1.8
                || rightHook > gap * 1.8
                || Math.min(leftHook, rightHook) < Math.max(leftHook, rightHook) * .4) return null;
        double center = intercept + slope * (span - 1) * .5;
        if (center < staff.bottom() + gap * .75 || center > staff.bottom() + gap * 16) return null;
        return new Bracket(
                left, right, (float) center, (float) slope, gap, staff.index(), staff.count());
    }
}
