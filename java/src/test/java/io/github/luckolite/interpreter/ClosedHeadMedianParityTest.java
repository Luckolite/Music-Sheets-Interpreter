// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/** Procedural printed-head evidence captured from the original sorted-median implementation. */
public final class ClosedHeadMedianParityTest {
    private static final int WIDTH = 220, HEIGHT = 180;

    private static byte[] page(int fill, int paper, int salt) {
        byte[] gray = new byte[WIDTH * HEIGHT];
        Arrays.fill(gray, (byte) paper);
        Random texture = new Random(salt);
        for (int y = 120; y <= 140; y++) {
            for (int x = 65; x <= 95; x++) {
                if ((x - 80) * (x - 80) / 169.0 + (y - 130) * (y - 130) / 100.0 <= 1)
                    gray[y * WIDTH + x] = 45;
                if ((x - 80) * (x - 80) / 121.0 + (y - 130) * (y - 130) / 49.0 <= 1)
                    gray[y * WIDTH + x] =
                            (byte)
                                    Math.max(
                                            0,
                                            Math.min(
                                                    255,
                                                    fill
                                                            + (salt == 0
                                                                    ? 0
                                                                    : texture.nextInt(31) - 15)));
            }
        }
        for (int y = 60; y <= 140; y++) for (int x = 93; x <= 95; x++) gray[y * WIDTH + x] = 45;
        return gray;
    }

    private static boolean attached(int fill, int paper, int salt) {
        return ClosedHeadBarlineGuard.attached(
                page(fill, paper, salt), WIDTH, HEIGHT, 94, 60, 140, 20);
    }

    @Test
    public void allGrayLevelsAcrossPaperAndTexturePreserveOriginalOwnership() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int cases = 0, owned = 0;
        for (int paper : new int[] {180, 196, 205, 220, 229, 230, 238, 255}) {
            for (int fill = 0; fill < 256; fill++) {
                for (int salt : new int[] {0, 776}) {
                    boolean result = attached(fill, paper, salt);
                    digest.update((byte) (result ? 1 : 0));
                    cases++;
                    if (result) owned++;
                }
            }
        }
        assertEquals(4096, cases);
        assertEquals(3058, owned);
        assertEquals(
                "032e4a088bcd992215d588c8daaf4c20f11ee5fcd0339f1d7f0e0d4db105afc7",
                HexFormat.of().formatHex(digest.digest()));
    }

    @Test
    public void paperContrastAndMaximumGrayCutoffsRetainTheirInclusiveEdges() {
        assertTrue(attached(204, 229, 0));
        assertFalse(attached(205, 229, 0));
        assertTrue(attached(205, 230, 0));
        assertFalse(attached(206, 230, 0));
    }
}
