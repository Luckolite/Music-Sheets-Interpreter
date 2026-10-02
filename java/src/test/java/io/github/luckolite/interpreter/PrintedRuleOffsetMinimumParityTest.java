// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import java.lang.reflect.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

public final class PrintedRuleOffsetMinimumParityTest {
    record Drawing(
            byte[] gray, int width, int height, int center, int[] rows, float gap, float shift) {}

    static final Method DETECT;

    static {
        try {
            DETECT =
                    OmrMeasurePostProcessor.class.getDeclaredMethod(
                            "printedRuleOffset",
                            byte[].class,
                            int.class,
                            int.class,
                            int.class,
                            int[].class,
                            float.class,
                            float.class);
            DETECT.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static Drawing drawing(int id) {
        int width = new int[] {17, 64, 129, 257}[id % 4],
                height = 150,
                center = new int[] {0, width - 1, width / 2, width / 4}[id / 4 % 4];
        float gap = new float[] {7f, 11.5f, 16f, 23f}[id / 16 % 4],
                shift = new float[] {0f, -.49f, .51f, 2.75f}[id / 64 % 4];
        int[] rows = new int[5];
        for (int line = 0; line < 5; line++) rows[line] = Math.round(18 + line * gap);
        byte[] gray = new byte[width * height];
        Random random = new Random(0x6530bb4dL + id);
        for (int at = 0; at < gray.length; at++) gray[at] = (byte) (180 + random.nextInt(76));
        int displacement = id / 256 % 9 - 4, kind = id / 64 % 8;
        for (int line = 0; line < 5; line++) {
            int y = Math.round(rows[line] + shift) + displacement;
            if (kind == 0 || kind == 1 && line > 0 || kind == 2 && line == 0) continue;
            for (int x = 0; x < width; x++) {
                if (kind == 3 && x % 3 == 0
                        || kind == 4 && x < width / 2
                        || kind == 5 && random.nextBoolean()) continue;
                int yy = y + (kind == 6 ? x / 35 : 0);
                if (yy >= 0 && yy < height) gray[yy * width + x] = (byte) (30 + id % 105);
            }
        }
        if (id % 17 == 0) rows[0] = -10;
        if (id % 19 == 0) rows[4] = height + 5;
        return new Drawing(gray, width, height, center, rows, gap, shift);
    }

    static int detect(Drawing d) throws Exception {
        return (int)
                DETECT.invoke(null, d.gray, d.width, d.height, d.center, d.rows, d.gap, d.shift);
    }

    @org.junit.Test
    public void generatedRulesKeepGoldenOffsetsAndCallerArrays() throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        int nonzero = 0;
        for (int id = 0; id < 2048; id++) {
            Drawing d = drawing(id);
            byte[] before = d.gray.clone();
            int[] rows = d.rows.clone();
            int value = detect(d);
            if (value != 0) nonzero++;
            digest.update((id + ":" + value + "\n").getBytes(StandardCharsets.UTF_8));
            org.junit.Assert.assertArrayEquals(before, d.gray);
            org.junit.Assert.assertArrayEquals(rows, d.rows);
        }
        org.junit.Assert.assertEquals(1411, nonzero);
        org.junit.Assert.assertEquals(
                "5a5687c93a1d0d06a79ec957e0b479b3e06efda1cac114042c6e0b20dfab576b",
                HexFormat.of().formatHex(digest.digest()));
    }
}
