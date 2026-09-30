// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic three-bulb rest geometry; no score pixels are embedded. */
public class ThirtySecondRestTest {
    private static final int W = 480, H = 200;

    private byte[] raster(int bulbs, boolean tail) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int y = 60; y <= 124; y += 16) for (int x = 20; x < 460; x++) gray[y * W + x] = 0;
        for (int cy = 70; cy < 70 + bulbs * 16; cy += 16)
            for (int y = cy - 5; y <= cy + 5; y++)
                for (int x = 164; x <= 182; x++)
                    if (Math.pow((x - (176 - 3 * (cy - 70) / 16)) / 6.0, 2)
                                    + Math.pow((y - cy) / 5.0, 2)
                            <= 1) gray[y * W + x] = 0;
        if (tail)
            for (int y = 67; y <= 123; y++) {
                int x = 185 - (y - 67) * 14 / 56;
                gray[y * W + x] = 0;
                gray[y * W + x + 1] = 0;
            }
        return gray;
    }

    private List<ScoreRestEvent> detect(byte[] gray) {
        return SixteenthRestDetector.detect(
                gray,
                W,
                H,
                List.of(new MeasureRegion(0, 1, 0, 1)),
                List.of(new SixteenthRestDetector.Staff(60, 124, 16, 0, 1)),
                List.of());
    }

    @Test
    public void threeBulbsAreOneThirtySecondRest() {
        var rests = detect(raster(3, true));
        assertEquals(rests.toString(), 1, rests.size());
        assertEquals(.125, rests.get(0).durationBeats(), 0);
    }

    @Test
    public void disconnectedBulbsDoNotCreateThirtySecondSilence() {
        assertTrue(detect(raster(3, false)).stream().noneMatch(r -> r.durationBeats() == .125));
    }

    @Test
    public void detectionPreservesSourcePixels() {
        byte[] gray = raster(3, true), before = gray.clone();
        detect(gray);
        assertArrayEquals(before, gray);
    }

    @Test
    public void completeThreeBulbRestSurvivesWaveSilhouetteGuard() {
        byte[] gray = raster(3, true);
        for (int y = 124; y <= 129; y++) {
            gray[y * W + 170] = 0;
            gray[y * W + 171] = 0;
        }
        assertTrue(RestVerticalWave.crosses(gray, W, H, 164, 186, 65, 129, 16));
        var rests = detect(gray);
        assertEquals(rests.toString(), 1, rests.size());
        assertEquals(.125, rests.get(0).durationBeats(), 0);
    }

    @Test
    public void narrowArpeggioWaveIsNotAThirtySecondRest() {
        byte[] gray = raster(0, false);
        for (int y = 64; y <= 130; y++) {
            int x = 176 + Math.round(4 * (float) Math.sin((y - 64) * Math.PI / 8));
            gray[y * W + x] = 0;
            gray[y * W + x + 1] = 0;
        }
        assertTrue(detect(gray).stream().noneMatch(r -> r.durationBeats() == .125));
    }
}
