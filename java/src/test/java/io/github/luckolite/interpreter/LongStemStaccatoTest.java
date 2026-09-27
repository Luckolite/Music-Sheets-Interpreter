// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original tall-stem beamed-note fixtures with distant isolated articulation dots. */
public class LongStemStaccatoTest {
    private static final int W = 1000, H = 1000;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    public LongStemStaccatoTest() {
        Arrays.fill(gray, (byte) 255);
        box(498, 508, 502, 512);
    }

    private void box(int l, int t, int r, int b) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) gray[y * W + x] = 0;
    }

    private void stem() {
        box(490, 400, 492, 497);
    }

    private void beam() {
        box(420, 494, 492, 497);
    }

    private int marks() {
        return NoteArticulationDetector.detect(
                        labels,
                        gray,
                        W,
                        H,
                        List.of(new NoteArticulationDetector.Anchor(500, 400, 16, 0)))[0]
                & NoteArticulation.STACCATO;
    }

    @Test
    public void tallBeamedStemOwnsDistantDot() {
        stem();
        beam();
        assertEquals(NoteArticulation.STACCATO, marks());
    }

    @Test
    public void distantDotWithoutStemIsNotAdopted() {
        beam();
        assertEquals(0, marks());
    }

    @Test
    public void unconnectedBeamDoesNotAuthorizeDot() {
        box(490, 400, 492, 455);
        beam();
        assertEquals(0, marks());
    }

    @Test
    public void stemWithoutBeamDoesNotAuthorizeDot() {
        stem();
        assertEquals(0, marks());
    }

    @Test
    public void rightStemAboveHeadAlsoWorks() {
        stem();
        beam();
        byte[] old = gray.clone();
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) gray[(H - 1 - y) * W + W - 1 - x] = old[y * W + x];
        assertEquals(
                NoteArticulation.STACCATO,
                NoteArticulationDetector.detect(
                                labels,
                                gray,
                                W,
                                H,
                                List.of(new NoteArticulationDetector.Anchor(499, 599, 16, 0)))[0]
                        & NoteArticulation.STACCATO);
    }
}
