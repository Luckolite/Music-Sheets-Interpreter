// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original curved rules and an independent short cue above their right-hand end. */
public class CurvedInsetCuePreservationTest {
    private int staffs(int paper, int ink) throws Exception {
        int width = 1200, height = 400;
        byte[] labels = new byte[width * height], gray = new byte[labels.length];
        Arrays.fill(gray, (byte) paper);
        for (int x = 30; x <= 1170; x++) {
            float bottom = 210 + 40 * Math.max(0, Math.min(1, (x - 400) / 400f));
            for (int line = 0; line < 5; line++) {
                int y = Math.round(bottom - line * 14);
                labels[(250 - line * 14) * width + x] = 4;
                gray[y * width + x] = (byte) ink;
            }
        }
        for (int x = 840; x <= 1170; x++)
            for (int line = 0; line < 5; line++) {
                int y = 145 + line * 9;
                labels[y * width + x] = 4;
                gray[y * width + x] = (byte) ink;
            }
        for (int x : new int[] {30, 400, 800, 1170}) {
            int bottom = Math.round(210 + 40 * Math.max(0, Math.min(1, (x - 400) / 400f)));
            for (int y = bottom - 56; y <= bottom; y++) {
                labels[y * width + x] = 1;
                gray[y * width + x] = (byte) ink;
            }
        }
        for (int x : new int[] {840, 1170})
            for (int y = 145; y <= 181; y++) {
                labels[y * width + x] = 1;
                gray[y * width + x] = (byte) ink;
            }
        var method =
                OmrMeasurePostProcessor.class.getDeclaredMethod(
                        "findStaffs", byte[].class, byte[].class, int.class, int.class);
        method.setAccessible(true);
        return ((List<?>) method.invoke(null, labels, gray, width, height)).size();
    }

    @Test
    public void whiteCueRemainsIndependentOfCurvedFrameEnvelope() throws Exception {
        assertEquals(2, staffs(255, 40));
    }

    @Test
    public void shadedCueRemainsIndependentOfCurvedFrameEnvelope() throws Exception {
        assertEquals(2, staffs(180, 50));
    }
}
