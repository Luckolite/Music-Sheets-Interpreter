// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original filled triangles and semantic false-head controls. */
public class MislabeledWedgeArticulationTest {
    private int detect(boolean below, boolean acceptedHead, int shape, boolean raw) {
        int w = 2048, h = 1600;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int y = 0; y < 20; y++) {
            int radius =
                    shape == 1
                            ? 5
                            : shape == 2
                                    ? (int)
                                            Math.round(
                                                    5
                                                            * Math.sqrt(
                                                                    Math.max(
                                                                            0,
                                                                            1
                                                                                    - Math.pow(
                                                                                            (y
                                                                                                            - 9.5)
                                                                                                    / 10,
                                                                                            2))))
                                    : (int) Math.round(5 * (19 - y) / 19d);
            for (int x = 305 - radius; x <= 305 + radius; x++) {
                int yy = 80 + (below ? 19 - y : y);
                gray[yy * w + x] = 0;
                labels[yy * w + x] = OmrMeasurePostProcessor.NOTEHEAD;
            }
        }
        List<NoteArticulationDetector.Anchor> anchors = new ArrayList<>();
        anchors.add(new NoteArticulationDetector.Anchor(305, below ? 40 : 140, 20, 0));
        if (acceptedHead) anchors.add(new NoteArticulationDetector.Anchor(305, 90, 20, 0));
        return NoteArticulationDetector.detect(labels, raw ? gray : null, w, h, anchors)[0];
    }

    @Test
    public void rawFilledWedgeSurvivesFalseHeadLabel() {
        assertEquals(NoteArticulation.STACCATISSIMO, detect(false, false, 0, true));
    }

    @Test
    public void lowerWedgeTapersTowardNote() {
        assertEquals(NoteArticulation.STACCATISSIMO, detect(true, false, 0, true));
    }

    @Test
    public void acceptedHeadStillVetoesWedge() {
        assertEquals(0, detect(false, true, 0, true));
    }

    @Test
    public void labelOnlyMaskCannotRecoverWedge() {
        assertEquals(0, detect(false, false, 0, false));
    }

    @Test
    public void solidUprightIsNotWedge() {
        assertEquals(0, detect(false, false, 1, true));
    }

    @Test
    public void elongatedOvalIsNotWedge() {
        assertEquals(0, detect(false, false, 2, true));
    }

    private int roundedPixelBounds(boolean labeled, boolean oval) {
        int w = 2048, h = 1600;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int row = 0; row < 16; row++) {
            int span =
                    oval
                            ? Math.max(
                                    1,
                                    (int)
                                            Math.round(
                                                    10
                                                            * Math.sqrt(
                                                                    Math.max(
                                                                            0,
                                                                            1
                                                                                    - Math.pow(
                                                                                            (row
                                                                                                            - 7.5)
                                                                                                    / 8,
                                                                                            2)))))
                            : 1 + (int) Math.round(9 * (15 - row) / 15d);
            int left = 310 + (10 - span) / 2;
            for (int x = left; x < left + span; x++) {
                gray[(80 + row) * w + x] = 0;
                if (labeled) labels[(80 + row) * w + x] = OmrMeasurePostProcessor.NOTEHEAD;
            }
        }
        return NoteArticulationDetector.detect(
                labels, gray, w, h, List.of(new NoteArticulationDetector.Anchor(315, 130, 17, 0)))[
                0];
    }

    @Test
    public void independentRasterEdgesCanWidenFilledWedge() {
        assertEquals(NoteArticulation.STACCATISSIMO, roundedPixelBounds(false, false));
    }

    @Test
    public void roundedWedgeBoundsStillNeedRawTaperForFalseMask() {
        assertEquals(NoteArticulation.STACCATISSIMO, roundedPixelBounds(true, false));
    }

    @Test
    public void rasterAllowanceDoesNotAdmitOval() {
        assertEquals(0, roundedPixelBounds(false, true));
    }

    private int outerCornerWedge(boolean twoRows) {
        return outerCornerWedge(twoRows, false);
    }

    private int outerCornerWedge(boolean twoRows, boolean partialFace) {
        int w = 2048, h = 1600;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        int[] spans = {1, 10, 9, 8, 8, 7, 6, 5, 5, 5, 4, 3, 3, 3, 3, 2, 1};
        if (partialFace) spans[0] = 5;
        if (twoRows) {
            spans[1] = 1;
            spans[2] = 10;
        }
        for (int row = 0; row < spans.length; row++) {
            if (row == 0 && partialFace) {
                for (int x = 310; x < 320; x++) if (x < 313 || x > 317) gray[80 * w + x] = 0;
                continue;
            }
            int left = row == 0 && !twoRows && !partialFace ? 311 : 310 + (10 - spans[row]) / 2;
            for (int x = left; x < left + spans[row]; x++) gray[(80 + row) * w + x] = 0;
        }
        return NoteArticulationDetector.detect(
                labels, gray, w, h, List.of(new NoteArticulationDetector.Anchor(315, 130, 17, 0)))[
                0];
    }

    @Test
    public void singleOuterRasterCornerPreservesFilledWedge() {
        assertEquals(NoteArticulation.STACCATISSIMO, outerCornerWedge(false));
    }

    @Test
    public void narrowNeckBeforeBroadBodyIsNotWedge() {
        assertEquals(0, outerCornerWedge(true));
    }

    @Test
    public void concaveOuterRasterRowPreservesFilledWedge() {
        assertEquals(NoteArticulation.STACCATISSIMO, outerCornerWedge(false, true));
    }

    private int substantialPartialFace(boolean below) {
        int w = 2048, h = 1600, gw = below ? 10 : 8, gh = below ? 18 : 12;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int row = 0; row < gh; row++) {
            int span =
                    row == 0
                            ? gw
                            : 1 + (int) Math.round((gw - 1) * (gh - 1 - row) / (double) (gh - 2));
            int left = 310 + (gw - span) / 2;
            for (int x = left; x < left + span; x++) {
                if (row == 0 && (below ? x >= 314 && x <= 316 : x == left || x == left + span - 1))
                    continue;
                int yy = 80 + (below ? gh - 1 - row : row);
                gray[yy * w + x] = 0;
                labels[yy * w + x] = OmrMeasurePostProcessor.NOTEHEAD;
            }
        }
        return NoteArticulationDetector.detect(
                labels,
                gray,
                w,
                h,
                List.of(new NoteArticulationDetector.Anchor(314, below ? 40 : 125, 17, 0)))[0];
    }

    @Test
    public void mostlyFilledConcaveOuterFacePreservesLowerWedge() {
        assertEquals(NoteArticulation.STACCATISSIMO, substantialPartialFace(true));
    }

    @Test
    public void mostlyFilledOuterFacePreservesRoundedWedgeAspect() {
        assertEquals(NoteArticulation.STACCATISSIMO, substantialPartialFace(false));
    }
}
