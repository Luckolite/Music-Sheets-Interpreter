// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.Executors;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural crop changes exercise reuse, edge clipping, and the uncached large path. */
public final class ClosedHeadScratchParityTest {
    private static final String EXPECTED =
            "f33b72fe36fe9e6b1f7082995ce8cfdd0fe98e0f136709ed9b46e2cd1ef1cc0e:1311:123";

    private static byte[] image(int width, int height, int dx, int dy, int fill, int paper) {
        byte[] gray = new byte[width * height];
        Arrays.fill(gray, (byte) paper);
        for (int y = 120; y <= 140; y++)
            for (int x = 65; x <= 95; x++) {
                int shade = paper;
                if ((x - 80) * (x - 80) / 169.0 + (y - 130) * (y - 130) / 100.0 <= 1) shade = 45;
                if ((x - 80) * (x - 80) / 121.0 + (y - 130) * (y - 130) / 49.0 <= 1) shade = fill;
                if (x + dx >= 0 && x + dx < width && y + dy >= 0 && y + dy < height)
                    gray[(y + dy) * width + x + dx] = (byte) shade;
            }
        for (int y = 60; y <= 140; y++)
            for (int x = 93; x <= 95; x++)
                if (x + dx >= 0 && x + dx < width && y + dy >= 0 && y + dy < height)
                    gray[(y + dy) * width + x + dx] = 45;
        return gray;
    }

    public static String golden() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int count = 0, owned = 0;
        for (int round = 0; round < 3; round++) {
            for (int paper : new int[] {196, 220, 229, 230, 238, 255}) {
                for (int fill : new int[] {145, 178, 204, 205, 206, 238}) {
                    byte[] gray = image(220, 180, 0, 0, fill, paper);
                    for (int column : new int[] {94, 103, 0, 219}) {
                        for (int gap : new int[] {12, 20, 31}) {
                            int top = round == 0 ? 60 : round == 1 ? 5 : 90;
                            int bottom = round == 2 ? 145 : 175;
                            boolean result =
                                    ClosedHeadBarlineGuard.attached(
                                            gray, 220, 180, column, top, bottom, gap);
                            digest.update((byte) (result ? 1 : 0));
                            count++;
                            if (result) owned++;
                        }
                    }
                }
            }
            // The large crop exceeds the bounded cache, then contracts and shifts to an edge.
            byte[] large = image(800, 1200, 300, 400, 178, 238);
            for (int[] crop :
                    new int[][] {
                        {394, 20, 1100, 20},
                        {394, 460, 540, 20},
                        {0, 20, 1100, 20},
                        {394, 460, 540, 12},
                        {394, 20, 1100, 31}
                    }) {
                boolean result =
                        ClosedHeadBarlineGuard.attached(
                                large, 800, 1200, crop[0], crop[1], crop[2], crop[3]);
                digest.update((byte) (result ? 1 : 0));
                count++;
                if (result) owned++;
            }
        }
        return HexFormat.of().formatHex(digest.digest()) + ":" + count + ":" + owned;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(golden());
    }

    @Test
    public void changingCropSizesAndLargeFallbackPreserveOriginalOwnership() throws Exception {
        assertEquals(EXPECTED, golden());
    }

    @Test
    public void concurrentCallersKeepIndependentTraversalState() throws Exception {
        var executor = Executors.newFixedThreadPool(3);
        try {
            var first = executor.submit(ClosedHeadScratchParityTest::golden);
            var second = executor.submit(ClosedHeadScratchParityTest::golden);
            var third = executor.submit(ClosedHeadScratchParityTest::golden);
            assertEquals(EXPECTED, first.get());
            assertEquals(EXPECTED, second.get());
            assertEquals(EXPECTED, third.get());
        } finally {
            executor.shutdownNow();
        }
    }
}
