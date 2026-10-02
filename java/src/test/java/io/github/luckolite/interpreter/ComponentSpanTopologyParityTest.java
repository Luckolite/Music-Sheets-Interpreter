// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import java.lang.reflect.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

public final class ComponentSpanTopologyParityTest {
    static Method method(Class<?> type) throws Exception {
        var m =
                type.getDeclaredMethod(
                        "findComponents", byte[].class, int.class, int.class, byte.class);
        m.setAccessible(true);
        return m;
    }

    static final Method DETECT;

    static {
        try {
            DETECT = method(OmrScoreInterpreter.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static String signature(List<?> parts) throws Exception {
        StringBuilder out = new StringBuilder();
        for (Object part : parts) {
            for (String name :
                    new String[] {"area", "minX", "maxX", "minY", "maxY", "centerX", "centerY"}) {
                Method m = part.getClass().getDeclaredMethod(name);
                m.setAccessible(true);
                Object value = m.invoke(part);
                out.append(
                                value instanceof Float f
                                        ? Integer.toHexString(Float.floatToRawIntBits(f))
                                        : value)
                        .append(',');
            }
            out.append(';');
        }
        return out.toString();
    }

    static MessageDigest digest;
    static int cases, components;

    static String invoke(Method m, byte[] labels, int width, int height, byte target)
            throws Exception {
        try {
            var parts = (List<?>) m.invoke(null, labels, width, height, target);
            components += parts.size();
            return signature(parts);
        } catch (InvocationTargetException e) {
            return "exception:" + e.getCause().getClass().getName();
        }
    }

    static void check(byte[] labels, int width, int height, byte target) throws Exception {
        byte[] before = labels.clone();
        String expected = invoke(DETECT, labels, width, height, target);
        if (!Arrays.equals(before, labels))
            throw new AssertionError("Caller labels changed in case=" + cases);
        digest.update((cases + ":" + expected + "\n").getBytes(StandardCharsets.UTF_8));
        cases++;
    }

    @org.junit.Test
    public void exhaustiveAndRandomTopologyKeepsGoldenComponentFields() throws Exception {
        digest = MessageDigest.getInstance("SHA-256");
        cases = components = 0;
        for (int bits = 0; bits < 65536; bits++) {
            byte[] labels = new byte[16];
            for (int at = 0; at < 16; at++) if ((bits & (1 << at)) != 0) labels[at] = 5;
            check(labels, 4, 4, (byte) 5);
        }
        Random random = new Random(0x704dde13L);
        for (int id = 0; id < 8192; id++) {
            int width = new int[] {1, 2, 3, 17, 64, 129, 257}[id % 7],
                    height = new int[] {1, 2, 19, 65, 130}[id / 7 % 5];
            byte target = (byte) new int[] {0, 1, 5, 127, -1}[id % 5];
            byte[] labels = new byte[width * height];
            for (int at = 0; at < labels.length; at++)
                labels[at] = (byte) (random.nextInt(4) == 0 ? target : random.nextInt(8));
            check(labels, width, height, target);
        }
        for (int kind = 0; kind < 8; kind++) {
            int width = 1024, height = 1049;
            byte[] labels = new byte[width * height];
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++) {
                    boolean ink =
                            switch (kind) {
                                case 0 -> true;
                                case 1 -> (x + y) % 2 == 0;
                                case 2 -> y < 513 || y >= 536;
                                case 3 -> x % 4 == 0 || y % 4 == 0;
                                case 4 -> random.nextInt(10) < 7;
                                case 5 -> x == 512 || y == 524;
                                case 6 -> false;
                                default -> random.nextBoolean();
                            };
                    if (ink) labels[y * width + x] = 5;
                }
            check(labels, width, height, (byte) 5);
        }
        for (int[] size :
                new int[][] {
                    {0, 0, 0}, {0, 0, 1}, {2, 3, 9}, {2, 4, 7}, {3, 1, 6}, {1, 1, 2}, {-1, 2, 3}
                }) {
            byte[] labels = new byte[size[2]];
            Arrays.fill(labels, (byte) 5);
            check(labels, size[0], size[1], (byte) 5);
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        org.junit.Assert.assertEquals(73743, cases);
        org.junit.Assert.assertEquals(620938, components);
        org.junit.Assert.assertEquals(
                "907eb03ddf36a614886c1e23fd0eb9ce2aab630728d3f2938e98cd81d2b7f8a8", hash);
    }
}
