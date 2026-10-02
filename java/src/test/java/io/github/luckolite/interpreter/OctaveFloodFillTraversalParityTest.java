// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic counter topology, border and threshold parity controls. */
public final class OctaveFloodFillTraversalParityTest {
    record Drawing(byte[] gray, int width, int height, int top, int bottom, float gap) {}

    static final Method DETECT;

    static {
        try {
            DETECT =
                    OctaveMarkDetector.class.getDeclaredMethod(
                            "attachedEightBoxes",
                            byte[].class,
                            int.class,
                            int.class,
                            int.class,
                            int.class,
                            float.class);
            DETECT.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static void rect(Drawing d, int left, int right, int top, int bottom, int ink) {
        for (int y = Math.max(0, top); y <= Math.min(d.height - 1, bottom); y++)
            for (int x = Math.max(0, left); x <= Math.min(d.width - 1, right); x++)
                d.gray[y * d.width + x] = (byte) ink;
    }

    static Drawing drawing(int id) {
        int gap = new int[] {8, 12, 16, 20}[id % 4],
                w = new int[] {96, 160, 256}[id / 4 % 3],
                h = gap * 8 + 20;
        Drawing d = new Drawing(new byte[w * h], w, h, 3, h - 4, gap);
        Arrays.fill(d.gray, (byte) new int[] {164, 165, 166, 230, 250, 255}[id / 12 % 6]);
        int x = gap + id % 9,
                y = gap * 2,
                width = Math.max(5, gap * 3 / 4),
                height = Math.round(gap * 1.6f);
        rect(d, x, x + width, y, y + height, new int[] {35, 164, 165, 166}[id / 72 % 4]);
        int inset = Math.max(1, gap / 7), middle = y + height / 2;
        rect(d, x + inset, x + width - inset, y + inset, middle - inset, 250);
        rect(d, x + inset, x + width - inset, middle + inset, y + height - inset, 250);
        rect(d, 0, x + 1, middle, middle + Math.max(0, gap / 8 - 1), 35);
        switch (id / 24 % 6) {
            case 1 -> rect(d, x + width / 2, x + width / 2, middle - inset, middle + inset, 250);
            case 2 -> rect(d, x, x + inset, y, y + height, 250);
            case 3 -> rect(d, 0, w - 1, y + height + gap / 2, y + height + gap / 2, 35);
            case 4 -> rect(d, w - 3, w - 1, d.top, d.top + gap, 250);
            case 5 -> rect(d, x + width / 2, x + width / 2, y - 1, middle, 250);
        }
        return d;
    }

    static List<?> detect(Drawing d) throws Exception {
        return (List<?>) DETECT.invoke(null, d.gray, d.width, d.height, d.top, d.bottom, d.gap);
    }

    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (byte value : bytes) text.append(String.format(Locale.ROOT, "%02x", value & 255));
        return text.toString();
    }

    @Test
    public void originalCounterAndEdgeTopologiesKeepTheirGoldenOrdering() throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        int positive = 0;
        for (int i = 0; i < 288; i++) {
            Drawing d = drawing(i);
            byte[] before = d.gray.clone();
            String result = detect(d).toString();
            if (!result.equals("[]")) positive++;
            digest.update((i + ":" + result + "\n").getBytes(StandardCharsets.UTF_8));
            assertArrayEquals(before, d.gray);
            assertEquals(result, detect(d).toString());
        }
        assertEquals(48, positive);
        assertEquals(
                "e53f5d26fc7bddc661fb72ded9abaab54e60e90573e70383b982624f14083698",
                hex(digest.digest()));
    }

    @Test
    public void interleavedWidthsAndThresholdsKeepIndependentTraversals() throws Exception {
        for (int i = 0; i < 72; i++) {
            Drawing first = drawing(i), second = drawing(287 - i);
            String expected = detect(first).toString();
            detect(second);
            assertEquals(expected, detect(first).toString());
        }
    }
}
