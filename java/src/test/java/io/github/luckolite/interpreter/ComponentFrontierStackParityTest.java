// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.junit.Test;

/** Original labeled rasters covering DFS frontiers and eight-neighbor ownership. */
public final class ComponentFrontierStackParityTest {
    record Drawing(byte[] labels, int width, int height, byte target) {}

    static final Method DETECT;

    static {
        try {
            DETECT =
                    OmrScoreInterpreter.class.getDeclaredMethod(
                            "findComponents", byte[].class, int.class, int.class, byte.class);
            DETECT.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static Drawing drawing(int id) {
        int width = new int[] {1, 2, 3, 17, 64, 129, 257}[id % 7],
                height = new int[] {1, 2, 19, 65, 130}[id / 7 % 5];
        if (id >= 245) {
            width = 513;
            height = 257;
        }
        byte target = (byte) (1 + id % 7);
        byte[] labels = new byte[width * height];
        Random random = new Random(0x281f3e07L + id);
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int at = y * width + x;
                labels[at] =
                        switch (id / 35 % 7) {
                            case 0 -> target;
                            case 1 -> (x + y) % 2 == 0 ? target : 0;
                            case 2 -> x % 4 == 0 || y % 4 == 0 ? target : 0;
                            case 3 -> x == width / 2 || y == height / 2 ? target : 0;
                            case 4 -> random.nextInt(5) == 0 ? target : 0;
                            case 5 -> (byte) random.nextInt(8);
                            default -> 0;
                        };
            }
        if (id >= 245)
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    labels[y * width + x] = (id % 2 == 0 || (x + y) % 2 == 0) ? target : 0;
        return new Drawing(labels, width, height, target);
    }

    static List<?> detect(Drawing d) throws Exception {
        return (List<?>) DETECT.invoke(null, d.labels, d.width, d.height, d.target);
    }

    static String signature(List<?> parts) throws Exception {
        StringBuilder out = new StringBuilder();
        for (Object part : parts) {
            for (String name :
                    new String[] {"area", "minX", "maxX", "minY", "maxY", "centerX", "centerY"}) {
                Method getter = part.getClass().getDeclaredMethod(name);
                getter.setAccessible(true);
                Object value = getter.invoke(part);
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

    static String hex(byte[] bytes) {
        StringBuilder s = new StringBuilder();
        for (byte b : bytes) s.append(String.format(Locale.ROOT, "%02x", b & 255));
        return s.toString();
    }

    @Test
    public void generatedFrontiersKeepComponentOrderCentersAndCallerLabels() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int components = 0;
        for (int id = 0; id < 252; id++) {
            Drawing d = drawing(id);
            byte[] before = d.labels.clone();
            List<?> parts = detect(d);
            components += parts.size();
            String value = signature(parts);
            digest.update((id + ":" + value + "\n").getBytes(StandardCharsets.UTF_8));
            assertArrayEquals(before, d.labels);
            assertEquals(value, signature(detect(d)));
        }
        String sha256 = hex(digest.digest());
        System.out.println("COMPONENT_STACK_GOLDEN components=" + components + " sha256=" + sha256);
        assertEquals(3893, components);
        assertEquals("604ecc3f2d6eee30a311fe29c4b6ae4a37659167040b74cae78bb9de02344dcf", sha256);
    }

    @Test
    public void largeAndSmallFrontiersRemainIndependent() throws Exception {
        Drawing small = drawing(73);
        String expected = signature(detect(small));
        detect(drawing(246));
        assertEquals(expected, signature(detect(small)));
        Drawing empty = new Drawing(new byte[0], 0, 0, (byte) 1);
        assertTrue(detect(empty).isEmpty());
    }

    @Test
    public void largeBlankAndSparseScansKeepExactEightNeighborComponents() throws Exception {
        int width = 1024, height = 128;
        byte[] labels = new byte[width * height];
        Drawing d = new Drawing(labels, width, height, (byte) 5);
        assertTrue(detect(d).isEmpty());
        labels[0] = 5;
        labels[1] = 5;
        labels[2] = 5;
        labels[25 * width + 20] = 5;
        labels[26 * width + 21] = 5;
        labels[27 * width + 22] = 5;
        byte[] before = labels.clone();
        assertEquals(
                "3,0,2,0,0,3f800000,0,;3,20,22,25,27,41a80000,41d00000,;", signature(detect(d)));
        assertArrayEquals(before, labels);
    }

    @Test
    public void largeDenseFrontierKeepsOneExactComponent() throws Exception {
        int width = 1024, height = 513;
        byte[] labels = new byte[width * height];
        Arrays.fill(labels, (byte) 5);
        Drawing d = new Drawing(labels, width, height, (byte) 5);
        byte[] before = labels.clone();
        assertEquals("525312,0,1023,0,512,43ffc000,43800000,;", signature(detect(d)));
        assertArrayEquals(before, labels);
    }

    @Test
    public void separatedLargeFrontiersKeepScanOrderAndCentersOnReentry() throws Exception {
        int width = 1024, height = 1049;
        byte[] labels = new byte[width * height];
        Arrays.fill(labels, 0, width * 513, (byte) 5);
        Arrays.fill(labels, width * 536, labels.length, (byte) 5);
        Drawing d = new Drawing(labels, width, height, (byte) 5);
        byte[] before = labels.clone();
        String expected =
                "525312,0,1023,0,512,43ffc000,43800000,;525312,0,1023,536,1048,43ffc000,44460000,;";
        assertEquals(expected, signature(detect(d)));
        assertEquals(expected, signature(detect(d)));
        assertArrayEquals(before, labels);
    }
}
