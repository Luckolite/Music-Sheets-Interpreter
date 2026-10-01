// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original small-amplitude waviness on a shallow note-to-note glide. */
public class ShallowWavySlideTest {
    static final int W = 400, H = 200;

    byte[] page(boolean curved) {
        byte[] p = new byte[W * H];
        Arrays.fill(p, (byte) 255);
        for (int x = 164; x <= 205; x++) {
            double t = (x - 150) / 68d;
            int y =
                    (int)
                            Math.round(
                                    90
                                            + 30 * t
                                            + (curved
                                                    ? -12 * Math.sin(Math.PI * (x - 164) / 41d)
                                                    : 1.1 * Math.sin((x - 164) * Math.PI / 4)));
            p[y * W + x] = 0;
            p[(y + 1) * W + x] = 0;
        }
        return p;
    }

    List<NoteSlideDetector.Stroke> detect(boolean curved, boolean prior, int staff) {
        var h = new ArrayList<NoteSlideDetector.Head>();
        if (prior) h.add(new NoteSlideDetector.Head(150, 90, 12, staff, 0));
        h.add(new NoteSlideDetector.Head(218, 120, 12, 0, 0));
        return NoteSlideDetector.detect(page(curved), W, H, List.of(), h);
    }

    @Test
    public void shallowWaveWithMatchingEndpointsIsRead() {
        var found = detect(false, true, 0);
        assertEquals(1, found.size());
        assertEquals(1, found.get(0).noteIndex());
        assertEquals(-1, found.get(0).direction());
    }

    @Test
    public void curvedSlurIsRejected() {
        assertTrue(detect(true, true, 0).isEmpty());
    }

    @Test
    public void isolatedShallowInkNeedsPriorNoteProof() {
        assertTrue(detect(false, false, 0).isEmpty());
    }

    @Test
    public void OtherStaffCannotSupplyPriorEndpoint() {
        assertTrue(detect(false, true, 1).isEmpty());
    }

    @Test
    public void longerWaveNeedsBothPrintedEndpoints() {
        byte[] p = new byte[W * H];
        Arrays.fill(p, (byte) 255);
        for (int x = 164; x <= 221; x++) {
            int y = (int) Math.round(95 + (x - 164) * .4 + Math.sin((x - 164) * Math.PI / 4));
            p[y * W + x] = 0;
            p[(y + 1) * W + x] = 0;
        }
        var target = new NoteSlideDetector.Head(236, 124, 12, 0, 0);
        var prior = new NoteSlideDetector.Head(150, 90, 12, 0, 0);
        assertEquals(
                1, NoteSlideDetector.detect(p, W, H, List.of(), List.of(prior, target)).size());
        assertTrue(NoteSlideDetector.detect(p, W, H, List.of(), List.of(target)).isEmpty());
    }
}
