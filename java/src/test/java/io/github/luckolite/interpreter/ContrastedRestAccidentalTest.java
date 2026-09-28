// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original paired-shaft raster controls, without any commercial score pixels. */
public class ContrastedRestAccidentalTest {
    static final int W = 80, H = 80;
    final byte[] gray = new byte[W * H];

    void line(int x, int first, int last, int shade) {
        for (int y = first; y <= last; y++) gray[y * W + x] = (byte) shade;
    }

    boolean pair(boolean paired) throws Exception {
        var m =
                SixteenthRestDetector.class.getDeclaredMethod(
                        "straightShafts",
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        boolean[].class,
                        int.class,
                        float.class,
                        boolean.class,
                        boolean.class);
        m.setAccessible(true);
        return (boolean)
                m.invoke(null, gray, W, 20, 45, 10, 50, new boolean[41], 10, 14.25f, paired, true);
    }

    void setup(int paper, int shade) {
        Arrays.fill(gray, (byte) paper);
        line(28, 11, 49, shade);
        line(35, 10, 46, shade);
    }

    @Test
    public void fadedContrastedPairIsNotRest() throws Exception {
        setup(245, 138);
        assertTrue(pair(true));
    }

    @Test
    public void darkPairRetainsOriginalProof() throws Exception {
        setup(245, 90);
        assertTrue(pair(true));
    }

    @Test
    public void uniformShadowCannotSupplyShafts() throws Exception {
        Arrays.fill(gray, (byte) 150);
        assertFalse(pair(true));
    }

    @Test
    public void weakContrastIsNotProof() throws Exception {
        setup(155, 140);
        assertFalse(pair(true));
    }

    @Test
    public void singleFadedShaftDoesNotBecomePair() throws Exception {
        setup(245, 138);
        line(35, 10, 46, 245);
        assertFalse(pair(true));
    }

    @Test
    public void shortPairsDoNotSupplyLongShafts() throws Exception {
        Arrays.fill(gray, (byte) 245);
        line(28, 20, 30, 138);
        line(35, 20, 30, 138);
        assertFalse(pair(true));
    }

    @Test
    public void singleShaftModeIsNotRelaxed() throws Exception {
        setup(245, 138);
        assertFalse(pair(false));
    }

    @Test
    public void inputPixelsRemainUnchanged() throws Exception {
        setup(245, 138);
        byte[] before = gray.clone();
        pair(true);
        assertArrayEquals(before, gray);
    }

    java.util.List<ScoreRestEvent> printedSharp(int accidental) {
        int w = 400, h = 200;
        byte[] pixels = new byte[w * h];
        Arrays.fill(pixels, (byte) 245);
        for (int row = 0; row < 5; row++) {
            int y = Math.round(60 + row * 14.25f);
            for (int x = 0; x < w; x++) pixels[y * w + x] = 80;
        }
        for (int y = 77; y <= 115; y++) {
            pixels[y * w + 183] = (byte) 138;
            pixels[y * w + 190] = 110;
        }
        for (int y : new int[] {87, 88, 89, 90, 101, 102, 103, 104})
            for (int x = 180; x <= 193; x++) pixels[y * w + x] = 75;
        var note =
                new ScoreNoteEvent(0, 212 / 400f, 3, 0, 1, 101 / 200f, false, 0, 1, accidental, 0);
        return SixteenthRestDetector.detect(
                pixels,
                w,
                h,
                java.util.List.of(new MeasureRegion(0, 1, 0, 1)),
                java.util.List.of(new SixteenthRestDetector.Staff(60, 117, 14.25f, 0, 1)),
                java.util.List.of(note));
    }

    @Test
    public void recognizedSharpOwnsItsFadedPairedStroke() {
        assertTrue(printedSharp(ScoreNoteEvent.ACCIDENTAL_SHARP).isEmpty());
    }

    @Test
    public void unrecognizedNeighborDoesNotAuthorizeFadedVeto() {
        assertEquals(1, printedSharp(ScoreNoteEvent.ACCIDENTAL_FROM_KEY).size());
    }
}
