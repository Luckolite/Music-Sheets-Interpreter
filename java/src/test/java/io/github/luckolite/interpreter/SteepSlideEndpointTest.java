// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original steep line and independent endpoint controls. */
public class SteepSlideEndpointTest {
    static final int W = 300, H = 240;

    private List<NoteSlideDetector.Stroke> detect(
            boolean prior, int staff, int measure, boolean curved) {
        return detect(prior, staff, measure, curved, 3);
    }

    private List<NoteSlideDetector.Stroke> detect(
            boolean prior, int staff, int measure, boolean curved, double slope) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int y = 70; y <= 130; y++) {
            int x =
                    (int)
                            Math.round(
                                    140
                                            + (130 - y) / slope
                                            + (curved ? 5 * Math.sin((y - 70) * Math.PI / 60) : 0));
            gray[y * W + x] = 0;
            gray[y * W + x + 1] = 0;
        }
        var heads = new ArrayList<NoteSlideDetector.Head>();
        if (prior) heads.add(new NoteSlideDetector.Head(128, 136, 12, staff, measure));
        heads.add(new NoteSlideDetector.Head(172, 64, 12, 0, 0));
        return NoteSlideDetector.detect(gray, W, H, List.of(), heads);
    }

    @Test
    public void steepGlideHasMatchingSourceAndTarget() {
        var strokes = detect(true, 0, 0, false);
        assertEquals(1, strokes.size());
        assertEquals(1, strokes.get(0).direction());
        assertEquals(1, strokes.get(0).noteIndex());
    }

    @Test
    public void isolatedSteepSlashNeedsPriorEndpoint() {
        assertTrue(detect(false, 0, 0, false).isEmpty());
    }

    @Test
    public void differentStaffCannotSupplyEndpoint() {
        assertTrue(detect(true, 1, 0, false).isEmpty());
    }

    @Test
    public void differentMeasureCannotSupplyEndpoint() {
        assertTrue(detect(true, 0, 1, false).isEmpty());
    }

    @Test
    public void curvedStrokeIsRejected() {
        assertTrue(detect(true, 0, 0, true).isEmpty());
    }

    @Test
    public void nearlyVerticalGlideStillRequiresBothEndpoints() {
        assertEquals(1, detect(true, 0, 0, false, 4.5).size());
        assertTrue(detect(false, 0, 0, false, 4.5).isEmpty());
    }

    @Test
    public void thickSteepRasterCrossesStaffLines() {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int y = 70; y <= 130; y++) {
            int x = (int) Math.round(140 + (130 - y) / 4.5);
            for (int dx = -1; dx <= 2; dx++) gray[y * W + x + dx] = 0;
        }
        for (int y : new int[] {76, 88, 100, 112, 124})
            for (int x = 0; x < W; x++) gray[y * W + x] = 0;
        var staffs = List.of(new NoteSlideDetector.Staff(76, 124, 12));
        var from = new NoteSlideDetector.Head(128, 136, 12, 0, 0);
        var to = new NoteSlideDetector.Head(172, 64, 12, 0, 0);
        assertEquals(1, NoteSlideDetector.detect(gray, W, H, staffs, List.of(from, to)).size());
        assertTrue(NoteSlideDetector.detect(gray, W, H, staffs, List.of(to)).isEmpty());
    }
}
