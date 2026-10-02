// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import org.junit.Test;

/** Original shading rasters compare the prior arithmetic and array ownership. */
public final class PaperToneCoordinateParityTest {
    @Test
    public void originalShadingBoundariesKeepBytesAndInputIdentity() throws Exception {
        Random random = new Random(0x6b91f37aL);
        int changed = 0, identity = 0;
        var golden = MessageDigest.getInstance("SHA-256");
        for (int id = 0; id < 1024; id++) {
            int width = 1 + random.nextInt(275), height = 1 + random.nextInt(241);
            byte[] gray = new byte[width * height];
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++) {
                    int value =
                            switch (id % 8) {
                                case 0 -> 255;
                                case 1 -> 0;
                                case 2 -> 95;
                                case 3 -> 96;
                                case 4 -> 239;
                                case 5 -> 240;
                                case 6 -> 130 + (x * 3 + y * 7) % 90;
                                default -> random.nextInt(256);
                            };
                    gray[y * width + x] = (byte) value;
                }
            byte[] before = gray.clone();
            float gap =
                    switch (id % 11) {
                        case 0 -> Float.NaN;
                        case 1 -> -1f;
                        case 2 -> 0f;
                        case 3 -> Float.POSITIVE_INFINITY;
                        case 4 -> Float.MIN_VALUE;
                        default -> Float.intBitsToFloat(0x40800000 + random.nextInt(0x02000000));
                    };
            byte[] reference = Reference.normalize(gray, width, height, gap),
                    candidate = RestPaperTone.normalize(gray, width, height, gap);
            if (!Arrays.equals(reference, candidate)
                    || !Arrays.equals(before, gray)
                    || (reference == gray) != (candidate == gray))
                throw new AssertionError("Normalization changed " + id);
            if (reference == gray) identity++;
            else changed++;
            golden.update(reference);
        }
        org.junit.Assert.assertEquals(324, changed);
        org.junit.Assert.assertEquals(700, identity);
        org.junit.Assert.assertEquals(
                "0bcd2751fc754e4818970952b57cdf87bf44c223ad000033a2c51e792319ac76",
                HexFormat.of().formatHex(golden.digest()));
    }

    @Test
    public void invalidShapeAndNullKeepOriginalArrayContract() {
        org.junit.Assert.assertNull(RestPaperTone.normalize(null, 1, 1, 1));
        byte[] malformed = {1, 2, 3};
        for (int[] shape :
                new int[][] {{0, 1}, {1, 0}, {-1, 1}, {1, -1}, {2, 2}, {Integer.MAX_VALUE, 2}}) {
            org.junit.Assert.assertSame(
                    malformed, Reference.normalize(malformed, shape[0], shape[1], 1));
            org.junit.Assert.assertSame(
                    malformed, RestPaperTone.normalize(malformed, shape[0], shape[1], 1));
        }
    }

    @Test
    public void originalWideImagesKeepExactFallbackAndArrayOwnership() {
        Random random = new Random(0x8234bfL);
        int cases = 0;
        for (int width : new int[] {16383, 16384, 16385, 32767, 65537})
            for (int height : new int[] {1, 5, 33})
                for (int style = 0; style < 3; style++) {
                    byte[] gray = new byte[width * height];
                    for (int i = 0; i < gray.length; i++)
                        gray[i] =
                                (byte)
                                        (style == 0
                                                ? 255
                                                : style == 1 ? 239 : 96 + random.nextInt(144));
                    byte[] before = gray.clone();
                    byte[] expected = Reference.normalize(gray, width, height, 11.375f),
                            actual = RestPaperTone.normalize(gray, width, height, 11.375f);
                    if (!Arrays.equals(expected, actual)
                            || !Arrays.equals(before, gray)
                            || (expected == gray) != (actual == gray))
                        throw new AssertionError(
                                "Wide shape changed " + width + "x" + height + " style" + style);
                    cases++;
                }
        org.junit.Assert.assertEquals(45, cases);
    }

    private static final class Reference {
        private Reference() {}

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
                    float upper =
                            paper[y0 * columns + x0] * (1 - fx) + paper[y0 * columns + x1] * fx;
                    float lower =
                            paper[y1 * columns + x0] * (1 - fx) + paper[y1 * columns + x1] * fx;
                    float tone = upper * (1 - fy) + lower * fy;
                    if (tone < 240)
                        result[y * width + x] =
                                (byte)
                                        Math.min(
                                                255,
                                                Math.round(
                                                        (gray[y * width + x] & 255) * 340f / tone));
                }
            return result;
        }
    }
}
