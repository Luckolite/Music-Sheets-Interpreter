// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** A letter's arch continues into outward stems; a tie's interior has a thin detached contour. */
final class TieArcBranchInk {
    private TieArcBranchInk() {}

    static boolean outwardStems(
            byte[] gray,
            int width,
            int height,
            int left,
            int right,
            float[] centers,
            int side,
            float gap,
            int limit) {
        int lastX = -1, branches = 0;
        for (int sample = centers.length / 5; sample < centers.length * 4 / 5; sample++) {
            if (!Float.isFinite(centers[sample])) continue;
            int x = Math.round(left + (sample + .5f) * (right - left) / centers.length);
            if (x == lastX || x < 0 || x >= width) continue;
            lastX = x;
            int start = Math.round(centers[sample]), ink = 0, blanks = 0;
            int radius = Math.max(1, Math.round(gap * .15f));
            boolean rule = false;
            for (int xx = Math.max(0, x - radius); xx <= Math.min(width - 1, x + radius); xx++)
                if (verticalSpan(gray, width, height, xx, start, gap, limit) >= gap * 3.5f) {
                    rule = true;
                    break;
                }
            if (rule) continue;
            for (int distance = 1; distance <= gap; distance++) {
                int y = start + side * distance;
                if (y < 0 || y >= height) break;
                boolean present = false;
                for (int xx = Math.max(0, x - radius); xx <= Math.min(width - 1, x + radius); xx++)
                    if ((gray[y * width + xx] & 255) <= limit) {
                        present = true;
                        break;
                    }
                if (present) {
                    ink++;
                    blanks = 0;
                } else if (++blanks > 1) break;
            }
            if (ink >= gap * .75f && ++branches >= 3) return true;
        }
        return false;
    }

    private static int verticalSpan(
            byte[] gray, int width, int height, int x, int start, float gap, int limit) {
        int span = 0;
        for (int side : new int[] {-1, 1}) {
            int blanks = 0;
            for (int distance = 0; distance <= gap * 5; distance++) {
                int y = start + side * distance;
                if (y < 0 || y >= height) break;
                if ((gray[y * width + x] & 255) <= limit) {
                    span++;
                    blanks = 0;
                } else if (++blanks > 1) break;
            }
        }
        return span;
    }
}
