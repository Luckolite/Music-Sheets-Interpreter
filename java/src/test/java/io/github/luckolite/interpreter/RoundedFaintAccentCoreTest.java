// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two-tone raster chevrons; no traced notation or score pixels. */
public class RoundedFaintAccentCoreTest {
    private static final int W = 1200, H = 1000;

    private static byte[] shape(
            int span,
            int tall,
            double darkAfter,
            boolean reverse,
            boolean parallel,
            boolean oneArm) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int x = 0; x < span; x++)
            for (int y = 0; y < tall; y++) {
                int axis = reverse ? span - 1 - x : x;
                double t = axis / (double) (span - 1);
                double upper = 1 + (parallel ? 0 : (tall - 3) * .5 * t),
                        lower = tall - 2 - (parallel ? 0 : (tall - 3) * .5 * t);
                if (Math.abs(y - upper) > .8 && (oneArm || Math.abs(y - lower) > .8)) continue;
                gray[(100 + y) * W + 100 + x] = (byte) (t > darkAfter ? 120 : 175);
            }
        return gray;
    }

    private static int[] detect(
            byte[] gray, int span, float gap, boolean far, boolean semantic, boolean otherStaff) {
        byte[] labels = new byte[gray.length];
        if (semantic)
            for (int p = 0; p < gray.length; p++)
                if ((gray[p] & 255) < 185) labels[p] = OmrMeasurePostProcessor.NOTEHEAD;
        float x = 100 + (span - 1) * .5f, y = far ? 230 : 100 + gap * 2.5f;
        var anchors = new ArrayList<NoteArticulationDetector.Anchor>();
        anchors.add(new NoteArticulationDetector.Anchor(x, y, gap, 0));
        if (otherStaff) anchors.add(new NoteArticulationDetector.Anchor(x, 220, gap, 1));
        byte[] before = gray.clone(), beforeLabels = labels.clone();
        int[] found = NoteArticulationDetector.detect(labels, gray, W, H, anchors);
        assertArrayEquals(before, gray);
        assertArrayEquals(beforeLabels, labels);
        return found;
    }

    @Test
    public void smallAntialiasedTipKeepsCompleteAccent() {
        assertEquals(
                1, detect(shape(17, 9, .8, false, false, false), 17, 12, false, false, false)[0]);
    }

    @Test
    public void anotherRasterScaleKeepsCompleteAccent() {
        assertEquals(
                1, detect(shape(19, 11, .8, false, false, false), 19, 14, false, false, false)[0]);
    }

    @Test
    public void darkAccentRemains() {
        assertEquals(
                1, detect(shape(19, 11, -1, false, false, false), 19, 14, false, false, false)[0]);
    }

    @Test
    public void allPaleStrokeStillHasNoDarkCore() {
        assertEquals(
                0, detect(shape(19, 11, 1, false, false, false), 19, 14, false, false, false)[0]);
    }

    @Test
    public void insufficientTipCoreRemainsExcluded() {
        assertEquals(
                0,
                detect(shape(21, 11, .9, false, false, false), 21, 14, false, false, false)[0] & 1);
    }

    @Test
    public void oneArmCannotSupplyCompleteAccent() {
        assertEquals(
                0,
                detect(shape(19, 11, .8, false, false, true), 19, 14, false, false, false)[0] & 1);
    }

    @Test
    public void reverseChevronRemainsExcluded() {
        assertEquals(
                0,
                detect(shape(19, 11, .8, true, false, false), 19, 14, false, false, false)[0] & 1);
    }

    @Test
    public void parallelStrokesRemainExcluded() {
        assertEquals(
                0,
                detect(shape(19, 11, .8, false, true, false), 19, 14, false, false, false)[0] & 1);
    }

    @Test
    public void semanticHeadRemainsVetoed() {
        assertEquals(
                0,
                detect(shape(19, 11, .8, false, false, false), 19, 14, false, true, false)[0] & 1);
    }

    @Test
    public void distantDirectionIsNotAttached() {
        assertEquals(
                0,
                detect(shape(19, 11, .8, false, false, false), 19, 14, true, false, false)[0] & 1);
    }

    @Test
    public void sameXOnAnotherPhysicalStaffDoesNotInherit() {
        int[] found = detect(shape(19, 11, .8, false, false, false), 19, 14, false, false, true);
        assertEquals(0, found[1]);
    }

    @Test
    public void whitePageRemainsEmpty() {
        byte[] p = new byte[W * H];
        Arrays.fill(p, (byte) 255);
        assertEquals(0, detect(p, 19, 14, false, false, false)[0]);
    }
}
