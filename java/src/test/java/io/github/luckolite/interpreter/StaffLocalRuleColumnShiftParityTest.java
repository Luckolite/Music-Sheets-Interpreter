// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import org.junit.Test;

/** Original generated printed-rule rasters, with no score or phone evidence. */
public final class StaffLocalRuleColumnShiftParityTest {
    record Drawing(
            byte[] labels,
            byte[] gray,
            int width,
            int height,
            float x,
            int headLeft,
            int headRight,
            float bottom,
            float gap,
            float slope,
            boolean bilateral,
            boolean occluded,
            int threshold) {}

    static final Method DETECT;

    static {
        try {
            DETECT =
                    StaffPitchTrack.class.getDeclaredMethod(
                            "localRulesWithSlope",
                            byte[].class,
                            byte[].class,
                            int.class,
                            int.class,
                            float.class,
                            int.class,
                            int.class,
                            float.class,
                            float.class,
                            float.class,
                            boolean.class,
                            boolean.class,
                            int.class);
            DETECT.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static Drawing drawing(int id) {
        int gap = new int[] {8, 12, 16, 20}[id % 4];
        int width = new int[] {96, 161, 240}[id / 4 % 3], height = gap * 12 + 24;
        float x = width * .5f + new float[] {0, .25f, -.5f}[id / 12 % 3];
        float slope = new float[] {0, .03f, -.03f, .08f, -.08f, 1f / 7, -1f / 7, .5f}[id / 36 % 8];
        float bottom = gap * 8,
                reference = bottom + new float[] {0, 1, -1, gap * .35f}[id / 288 % 4];
        byte[] labels = new byte[width * height], gray = new byte[width * height];
        Arrays.fill(gray, (byte) 245);
        int ink = new int[] {45, 164, 190, 210}[id / 72 % 4];
        int thickness = id / 144 % 2;
        for (int line = 0; line < 5; line++)
            for (int xx = 0; xx < width; xx++) {
                int center = Math.round(bottom - line * gap) + Math.round((xx - x) * slope);
                for (int yy = center; yy <= center + thickness; yy++)
                    if (yy >= 0 && yy < height) {
                        labels[yy * width + xx] = 4;
                        gray[yy * width + xx] = (byte) ink;
                    }
            }
        int left = Math.round(x - gap * .35f), right = Math.round(x + gap * .35f);
        if (id / 24 % 4 == 1) {
            for (int yy = Math.round(bottom - 3 * gap); yy <= Math.round(bottom - gap); yy++)
                for (int xx = left - gap; xx <= right + gap; xx++) gray[yy * width + xx] = 50;
        } else if (id / 24 % 4 == 2) {
            for (int xx = 0; xx < width / 3; xx++)
                for (int yy = 0; yy < height; yy++) {
                    gray[yy * width + xx] = (byte) 245;
                    labels[yy * width + xx] = 0;
                }
        } else if (id / 24 % 4 == 3) {
            for (int xx = 0; xx < width; xx++)
                for (int yy = 0; yy < height; yy++)
                    if ((xx * 17 + yy * 31 + id) % 73 == 0) gray[yy * width + xx] = 80;
        }
        return new Drawing(
                labels,
                gray,
                width,
                height,
                x,
                left,
                right,
                reference,
                gap,
                slope,
                id % 2 == 0,
                id / 2 % 2 == 0,
                new int[] {190, 205, 225}[id / 8 % 3]);
    }

    static float[] detect(Drawing d) throws Exception {
        return (float[])
                DETECT.invoke(
                        null,
                        d.labels,
                        d.gray,
                        d.width,
                        d.height,
                        d.x,
                        d.headLeft,
                        d.headRight,
                        d.bottom,
                        d.gap,
                        d.slope,
                        d.bilateral,
                        d.occluded,
                        d.threshold);
    }

    static String bits(float[] result) {
        if (result == null) return "null";
        StringBuilder text = new StringBuilder();
        for (float value : result)
            text.append(Integer.toHexString(Float.floatToRawIntBits(value))).append(',');
        return text.toString();
    }

    static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (byte value : bytes)
            text.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return text.toString();
    }

    @Test
    public void generatedRulesKeepExactFloatBitsAndCallerRasters() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int positives = 0;
        for (int id = 0; id < 1152; id++) {
            Drawing d = drawing(id);
            byte[] labels = d.labels.clone(), gray = d.gray.clone();
            float[] result = detect(d);
            if (result != null) positives++;
            String value = bits(result);
            digest.update((id + ":" + value + "\n").getBytes(StandardCharsets.UTF_8));
            assertEquals(value, bits(detect(d)));
            assertArrayEquals(labels, d.labels);
            assertArrayEquals(gray, d.gray);
        }
        String sha256 = hex(digest.digest());
        System.out.println("STAFF_RULE_GOLDEN positives=" + positives + " sha256=" + sha256);
        assertEquals(924, positives);
        assertEquals("69b9301427d3c89f6b1f634274ccca5c9386878cf08ecc56d4a5fe2bd03632ae", sha256);
    }

    @Test
    public void interleavedRuleInputsRemainIndependent() throws Exception {
        for (int id = 0; id < 96; id++) {
            Drawing first = drawing(id);
            String expected = bits(detect(first));
            detect(drawing(1151 - id));
            assertEquals(expected, bits(detect(first)));
        }
    }
}
