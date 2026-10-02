// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural quarter ink, shaded paper and independently printed staff phase. */
public class LocalQuarterRestPhaseTest {
    private byte[] page(boolean hook, int missingRule) {
        byte[] gray = new byte[600 * 280];
        for (int y = 0; y < 280; y++)
            for (int x = 0; x < 600; x++) {
                int at = y * 600 + x;
                gray[at] = (byte) (145 + (x * 37 + y * 53) % 21);
            }
        for (int rule = 100; rule <= 164; rule += 16)
            if (rule != missingRule) for (int x = 20; x < 580; x++) gray[rule * 600 + x] = 95;
        double[] stops = {0, .28, .50, .67, .85, 1},
                centers = {3, 9, 4, 9, 1, 5},
                radii = {1, 6, 2, 5, 3, 2};
        for (int i = 0; i < 44; i++) {
            double t = i / 43.0;
            int segment = 0;
            while (segment + 1 < stops.length - 1 && t > stops[segment + 1]) segment++;
            double fraction = (t - stops[segment]) / (stops[segment + 1] - stops[segment]);
            double center = centers[segment] + (centers[segment + 1] - centers[segment]) * fraction;
            double radius = radii[segment] + (radii[segment + 1] - radii[segment]) * fraction;
            if (!hook && t > .58) {
                center = 7;
                radius = 2;
            }
            for (int x = 154 + (int) Math.ceil(center - radius);
                    x <= 154 + (int) Math.floor(center + radius);
                    x++) gray[(107 + i) * 600 + x] = 0;
        }
        return gray;
    }

    private List<ScoreRestEvent> read(byte[] gray, int phase, List<ScoreNoteEvent> notes) {
        return SixteenthRestDetector.detect(
                gray,
                600,
                280,
                List.of(new MeasureRegion(.04f, .95f, .2f, .9f)),
                List.of(new SixteenthRestDetector.Staff(100 + phase, 164 + phase, 16, 0, 1)),
                notes);
    }

    @Test
    public void completeLocalStaffRecoversQuarterAfterPositiveSeedDrift() {
        var rests = read(page(true, -1), 5, List.of());
        assertEquals(rests.toString(), 1, rests.size());
        assertEquals(1, rests.get(0).durationBeats(), 0);
    }

    @Test
    public void completeLocalStaffRecoversQuarterAfterNegativeSeedDrift() {
        var rests = read(page(true, -1), -5, List.of());
        assertEquals(rests.toString(), 1, rests.size());
        assertEquals(1, rests.get(0).durationBeats(), 0);
    }

    @Test
    public void fourRulesCannotSupplyNewQuarterPhase() {
        assertTrue(read(page(true, 132), 5, List.of()).isEmpty());
    }

    @Test
    public void anUpperZigzagWithoutHookStillFails() {
        assertTrue(read(page(false, -1), 5, List.of()).isEmpty());
    }

    @Test
    public void anOwnedPrintedHeadCannotTurnIntoRest() {
        var note =
                new ScoreNoteEvent(
                        0, .249f, 2, 0, 1, .5f, false, 0, 0, 2, 1, 1, 0, 0, 30, false, 0, false, 0,
                        0);
        assertTrue(read(page(true, -1), 5, List.of(note)).isEmpty());
    }

    @Test
    public void recoveryPreservesTheOriginalPhotoRaster() {
        byte[] gray = page(true, -1), before = gray.clone();
        read(gray, 5, List.of());
        assertArrayEquals(before, gray);
    }
}
