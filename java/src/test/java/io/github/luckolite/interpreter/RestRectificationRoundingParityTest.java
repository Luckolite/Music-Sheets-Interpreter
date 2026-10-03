// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.Test;

/** Original curved quarter-rest drawings around half-pixel staff translations. */
public final class RestRectificationRoundingParityTest {
    private static final int W = CurvedRestRecognitionTest.W, H = CurvedRestRecognitionTest.H;
    private static final List<MeasureRegion> M = CurvedRestRecognitionTest.M;

    private static void rest(DataOutputStream out, ScoreRestEvent rest) throws IOException {
        out.writeInt(rest.measureIndex());
        out.writeInt(Float.floatToRawIntBits(rest.positionInMeasure()));
        out.writeInt(Float.floatToRawIntBits(rest.pageY()));
        out.writeInt(Float.floatToRawIntBits(rest.pageHeight()));
        out.writeInt(rest.staffIndex());
        out.writeInt(rest.staffCount());
        out.writeLong(Double.doubleToRawLongBits(rest.durationBeats()));
    }

    private static byte[] bytes(SixteenthRestDetector.Detection detection) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(detection.rests().size());
            for (var rest : detection.rests()) rest(out, rest);
            out.writeInt(detection.dots().size());
            for (var dot : detection.dots()) {
                out.writeInt(Float.floatToRawIntBits(dot.x()));
                out.writeInt(Float.floatToRawIntBits(dot.y()));
                rest(out, dot.rest());
            }
        }
        return bytes.toByteArray();
    }

    @Test
    public void fractionalFramesKeepExactRestCoordinatesAndCallerLifetime() throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        float[] bottoms = {
            204f, Math.nextDown(204.5f), 204.5f, Math.nextUp(204.5f), 203.5f, Math.nextUp(203.5f)
        };
        int positives = 0, cases = 0;
        for (boolean reverse : new boolean[] {false, true}) {
            for (float bottom : bottoms) {
                for (int dots = 0; dots < 3; dots++) {
                    var page = new CurvedRestRecognitionTest.Page(reverse, 0, false, dots);
                    byte[] originalGray = page.gray.clone(), originalLabels = page.labels.clone();
                    var track = StaffPitchTrack.linear(W, bottom, 16f, reverse ? -.025f : .025f);
                    var staff = new SixteenthRestDetector.Staff(140, 204, 16, 0, 1, track);
                    var staffs = List.of(staff);
                    var detection =
                            SixteenthRestDetector.detectWithDots(
                                    page.gray, W, H, M, staffs, List.of());
                    positives += detection.rests().size();
                    byte[] expected = bytes(detection);
                    digest.update(expected);
                    assertArrayEquals(originalGray, page.gray);
                    assertArrayEquals(originalLabels, page.labels);
                    Arrays.fill(page.gray, (byte) 255);
                    var erased =
                            SixteenthRestDetector.detectWithDots(
                                    page.gray, W, H, M, staffs, List.of());
                    assertTrue(erased.rests().isEmpty());
                    assertTrue(erased.dots().isEmpty());
                    System.arraycopy(originalGray, 0, page.gray, 0, page.gray.length);
                    assertArrayEquals(
                            expected,
                            bytes(
                                    SixteenthRestDetector.detectWithDots(
                                            page.gray, W, H, M, staffs, List.of())));
                    assertArrayEquals(originalGray, page.gray);
                    assertArrayEquals(originalLabels, page.labels);
                    cases++;
                }
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte value : digest.digest())
            hex.append(String.format(Locale.ROOT, "%02x", value & 255));
        String hash = hex.toString();
        System.out.println(
                "RECTIFICATION_ROUNDING_GOLD cases="
                        + cases
                        + " positives="
                        + positives
                        + " sha256="
                        + hash);
        assertEquals(36, cases);
        assertEquals(36, positives);
        assertEquals("fb91c26cdd36d09960f02eda97728e24dafbe13af5353d6e87ec1b7cf688bba1", hash);
    }
}
