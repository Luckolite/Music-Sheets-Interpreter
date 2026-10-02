// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Estimates photographed paper tone without changing the supplied image. */
final class RestPaperTone {
    private RestPaperTone() {}

    static byte[] normalize(byte[] gray, int width, int height, float gap) {
        if (gray == null
                || width <= 0
                || height <= 0
                || gray.length != (long) width * height
                || !Float.isFinite(gap)
                || gap <= 0) return gray;
        int tile = Math.max(32, Math.min(128, Math.round(gap * 4)));
        int columns = (width + tile - 1) / tile, rows = (height + tile - 1) / tile;
        int[] paper = new int[columns * rows];
        boolean shaded = false;
        for (int row = 0; row < rows; row++)
            for (int col = 0; col < columns; col++) {
                int[] histogram = new int[256];
                int count = 0;
                for (int y = row * tile; y < Math.min(height, (row + 1) * tile); y++)
                    for (int x = col * tile; x < Math.min(width, (col + 1) * tile); x++) {
                        histogram[gray[y * width + x] & 255]++;
                        count++;
                    }
                int target = (count * 3 + 3) / 4, total = 0, tone = 0;
                while (tone < 255 && (total += histogram[tone]) < target) tone++;
                // Very dark tiles cannot distinguish a dark symbol from the paper.
                paper[row * columns + col] = tone >= 96 ? tone : 255;
                shaded |= tone >= 96 && tone < 240;
            }
        if (!shaded) return gray;
        byte[] result = gray.clone();
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                float gx = Math.max(0, Math.min(columns - 1, (x + .5f) / tile - .5f));
                float gy = Math.max(0, Math.min(rows - 1, (y + .5f) / tile - .5f));
                int x0 = (int) gx,
                        y0 = (int) gy,
                        x1 = Math.min(columns - 1, x0 + 1),
                        y1 = Math.min(rows - 1, y0 + 1);
                float fx = gx - x0, fy = gy - y0;
                float upper = paper[y0 * columns + x0] * (1 - fx) + paper[y0 * columns + x1] * fx;
                float lower = paper[y1 * columns + x0] * (1 - fx) + paper[y1 * columns + x1] * fx;
                float tone = upper * (1 - fy) + lower * fy;
                if (tone < 240)
                    result[y * width + x] =
                            (byte)
                                    Math.min(
                                            255,
                                            Math.round((gray[y * width + x] & 255) * 340f / tone));
            }
        return result;
    }
}
