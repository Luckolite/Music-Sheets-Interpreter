// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;
import org.junit.Test;

/** Original generated rasters; the golden records the previous released slope decisions. */
public final class StaffSlopeProjectionScratchParityTest {
    static byte[] labels(int index) {
        int width = 60 + index % 121, height = 48 + index % 95;
        byte[] labels = new byte[width * height];
        Random random = new Random(0x6759123L + index);
        float slope = (index % 49 - 24) * .006f + (index % 11 - 5) * .0006f;
        if (index % 8 != 0) {
            for (int x = 0; x < width; x++) {
                for (int staff = 0; staff < 2; staff++) {
                    for (int rule = 0; rule < 5; rule++) {
                        int y =
                                Math.round(
                                        12
                                                + staff * height * .45f
                                                + rule * 4
                                                + slope * (x - width * .5f));
                        if (y >= 0 && y < height) labels[y * width + x] = 4;
                    }
                }
            }
        }
        if (index % 3 == 0) {
            for (int at = 0; at < labels.length; at++) if (random.nextInt(211) == 0) labels[at] = 4;
        }
        if (index % 7 == 0) {
            for (int at = 0; at < labels.length; at++) {
                if (random.nextInt(71) == 0) labels[at] = (byte) (1 + random.nextInt(5));
            }
        }
        return labels;
    }

    @Test
    public void everyAngleDecisionAndCallerRasterMatchesReleasedGolden() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer word = ByteBuffer.allocate(4);
        int nonzero = 0;
        for (int index = 0; index < 1024; index++) {
            byte[] labels = labels(index), original = labels.clone();
            float slope =
                    OmrMeasurePostProcessor.estimateStaffSlope(
                            labels, 60 + index % 121, 48 + index % 95);
            if (slope != 0f) nonzero++;
            word.clear();
            word.putInt(Float.floatToRawIntBits(slope));
            digest.update(word.array());
            assertArrayEquals("case=" + index, original, labels);
        }
        assertEquals(878, nonzero);
        assertEquals(
                "6bd6a2ef9b7f7f94f2c026dac647d9565028f1554dec3b8a853373ece3c13da0",
                HexFormat.of().formatHex(digest.digest()));
    }
}
