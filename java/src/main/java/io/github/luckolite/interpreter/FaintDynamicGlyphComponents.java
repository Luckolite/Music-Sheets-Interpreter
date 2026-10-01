// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Propose predominantly faded glyphs; ordinary dark candidates keep their existing bounds. */
final class FaintDynamicGlyphComponents {
    record Bounds(int left, int top, int right, int bottom, int pixels) {}

    static List<Bounds> find(byte[] gray, int width, int height, boolean[] seen, int[] queue) {
        Arrays.fill(seen, false);
        List<Bounds> result = new ArrayList<>();
        for (int p = 0; p < gray.length; p++) {
            if (seen[p] || (gray[p] & 255) >= 205) continue;
            int read = 0, count = 1;
            queue[0] = p;
            seen[p] = true;
            int left = p % width, right = left, top = p / width, bottom = top;
            int dark = 0;
            while (read < count) {
                int at = queue[read++], x = at % width, y = at / width;
                left = Math.min(left, x);
                right = Math.max(right, x);
                top = Math.min(top, y);
                bottom = Math.max(bottom, y);
                if ((gray[at] & 255) < 145) dark++;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                        int next = ny * width + nx;
                        if (!seen[next] && (gray[next] & 255) < 205) {
                            seen[next] = true;
                            queue[count++] = next;
                        }
                    }
            }
            if (dark < count * .2f && count >= 3)
                result.add(new Bounds(left, top, right + 1, bottom + 1, count));
        }
        return result;
    }
}
