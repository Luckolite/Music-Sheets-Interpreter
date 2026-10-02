// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original grayscale ovals on uniform and graded paper; no score pixels or model fixtures. */
public class ShadedPaperAugmentationDotTest {
    private static final class Drawing {
        final int scale, width, height;
        final byte[] gray;

        Drawing(int scale, int paper) {
            this.scale = scale;
            width = 240 * scale;
            height = 160 * scale;
            gray = new byte[width * height];
            Arrays.fill(gray, (byte) paper);
            for (int y = 73 * scale; y <= 87 * scale; y++)
                for (int x = 90 * scale; x <= 110 * scale; x++)
                    if (Math.pow((x - 100.0 * scale) / (10 * scale), 2)
                                    + Math.pow((y - 80.0 * scale) / (7 * scale), 2)
                            <= 1) gray[y * width + x] = 0;
        }

        void spot(int cx, int cy, int radius, int tone) {
            for (int y = (cy - radius) * scale; y <= (cy + radius) * scale; y++)
                for (int x = (cx - radius) * scale; x <= (cx + radius) * scale; x++)
                    if (Math.pow(x - cx * scale, 2) + Math.pow(y - cy * scale, 2)
                            <= Math.pow(radius * scale, 2)) gray[y * width + x] = (byte) tone;
        }

        int count(boolean hollow) throws Exception {
            var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
            var ctor = type.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            var head =
                    ctor.newInstance(
                            180 * scale * scale,
                            90 * scale,
                            110 * scale,
                            73 * scale,
                            87 * scale,
                            100f * scale,
                            80f * scale);
            var method =
                    OmrScoreInterpreter.class.getDeclaredMethod(
                            "countAugmentationDots",
                            List.class,
                            type,
                            float.class,
                            byte[].class,
                            int.class,
                            int.class,
                            boolean.class);
            method.setAccessible(true);
            return (int)
                    method.invoke(null, List.of(), head, 16f * scale, gray, width, height, hollow);
        }
    }

    @Test
    public void smallPaperGrainDoesNotLengthenFilledNote() throws Exception {
        var d = new Drawing(1, 148);
        d.spot(126, 80, 2, 128);
        assertEquals(0, d.count(false));
    }

    @Test
    public void smallPaperGrainDoesNotLengthenHollowNote() throws Exception {
        var d = new Drawing(1, 148);
        d.spot(126, 80, 2, 128);
        assertEquals(0, d.count(true));
    }

    @Test
    public void twoPaperGrainsDoNotMakeDoubleDot() throws Exception {
        var d = new Drawing(1, 148);
        d.spot(124, 80, 2, 128);
        d.spot(136, 80, 2, 128);
        assertEquals(0, d.count(false));
    }

    @Test
    public void paperGrainRejectionScalesWithStaffSpacing() throws Exception {
        var d = new Drawing(2, 148);
        d.spot(126, 80, 2, 128);
        assertEquals(0, d.count(false));
    }

    @Test
    public void gradedPaperGrainStillNeedsPrintedCore() throws Exception {
        var d = new Drawing(1, 148);
        for (int y = 65; y <= 95; y++)
            for (int x = 112; x <= 150; x++) d.gray[y * d.width + x] = (byte) (146 + (x - 112) / 6);
        d.spot(126, 80, 2, 128);
        assertEquals(0, d.count(false));
    }

    @Test
    public void compactPrintedDotOnShadedPaperIsRetained() throws Exception {
        var d = new Drawing(1, 148);
        d.spot(126, 80, 2, 95);
        assertEquals(1, d.count(false));
    }

    @Test
    public void shadedHollowHeadKeepsItsPrintedDot() throws Exception {
        var d = new Drawing(1, 148);
        d.spot(126, 80, 3, 95);
        assertEquals(1, d.count(true));
    }

    @Test
    public void genuineDoubleDotOnShadedPaperIsRetained() throws Exception {
        var d = new Drawing(1, 148);
        d.spot(124, 80, 2, 95);
        d.spot(136, 80, 2, 95);
        assertEquals(2, d.count(false));
    }

    @Test
    public void genuineDotScalesOnShadedPaper() throws Exception {
        var d = new Drawing(2, 148);
        d.spot(126, 80, 2, 95);
        assertEquals(1, d.count(false));
    }

    @Test
    public void faintPrintedDotOnBrightPaperKeepsDarkCore() throws Exception {
        var d = new Drawing(1, 250);
        d.spot(126, 80, 3, 195);
        d.spot(126, 80, 2, 165);
        assertEquals(1, d.count(false));
    }

    @Test
    public void aPaperGrainAfterRealDotDoesNotMakeDoubleDot() throws Exception {
        var d = new Drawing(1, 148);
        d.spot(124, 80, 2, 95);
        d.spot(136, 80, 2, 128);
        assertEquals(1, d.count(false));
    }

    @Test
    public void grayscaleAndSemanticCandidatesRemainUnchanged() throws Exception {
        var d = new Drawing(1, 148);
        d.spot(126, 80, 2, 128);
        var before = d.gray.clone();
        d.count(false);
        assertArrayEquals(before, d.gray);
    }
}
