// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original hairpin arms on shaded paper; corrections remain supplemental. */
public class PaperHairpinProposalTest {
    private static final int W = 1280, H = 900;

    private List<ScoreDynamicChange> detect(
            boolean dim, boolean tiltedFrame, boolean parallel, boolean wedge, int paper) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) paper);
        for (int line = 0; line < 5; line++)
            for (int x = 200; x < 1000; x++) gray[(260 - line * 14) * W + x] = 30;
        if (wedge)
            for (int dx = 0; dx <= 160; dx++) {
                int open = parallel ? 8 : Math.round((dim ? 160 - dx : dx) * .05f);
                for (int sign : new int[] {-1, 1}) gray[(290 + sign * open) * W + 590 + dx] = 30;
            }
        var staff =
                new PlayingTechniqueDetector.Staff(
                        tiltedFrame ? 230 : 204, tiltedFrame ? 286 : 260, 14, 0, 1);
        var region = new MeasureRegion(.2f, .8f, .18f, .36f);
        var note = new ScoreNoteEvent(0, .5f, 4, 0, 1, 232f / H, false, 0, 0, 2, 1, 1, 0, 0);
        byte[] before = gray.clone();
        var result =
                ScoreDynamicsDetector.detect(
                        List.of(), List.of(staff), List.of(region), List.of(note), gray, W, H);
        assertArrayEquals(before, gray);
        return result;
    }

    @Test
    public void shadedPaperCrescendoHasCompleteArmProof() {
        var c = detect(false, false, false, true, 140);
        assertEquals(1, c.size());
        assertEquals(1, c.get(0).direction());
    }

    @Test
    public void shadedPaperDiminuendoHasCompleteArmProof() {
        var c = detect(true, false, false, true, 140);
        assertEquals(1, c.size());
        assertEquals(-1, c.get(0).direction());
    }

    @Test
    public void tiltedGlobalFrameStillNeedsFivePrintedRails() {
        var c = detect(true, true, false, true, 140);
        assertEquals(1, c.size());
        assertEquals(-1, c.get(0).direction());
    }

    @Test
    public void originalBrightHairpinDoesNotDuplicate() {
        assertEquals(1, detect(false, false, false, true, 255).size());
    }

    @Test
    public void originalBrightDiminuendoDoesNotDuplicate() {
        assertEquals(1, detect(true, false, false, true, 255).size());
    }

    @Test
    public void parallelThinStrokesAreNotHairpin() {
        assertTrue(detect(false, false, true, true, 140).isEmpty());
    }

    @Test
    public void shadedFiveRailsAreNotHairpin() {
        assertTrue(detect(false, false, false, false, 140).isEmpty());
    }
}
