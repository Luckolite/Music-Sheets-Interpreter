// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ending-number punctuation and neighboring isolated performance marks. */
public class EndingPunctuationArticulationTest {
    private static final int W = 1500, H = 2000;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    public EndingPunctuationArticulationTest() {
        Arrays.fill(gray, (byte) 255);
        box(498, 498, 502, 502);
    }

    private void box(int l, int t, int r, int b) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) gray[y * W + x] = 0;
    }

    private void number() {
        box(482, 478, 485, 502);
        box(476, 500, 490, 502);
        box(478, 478, 485, 480);
    }

    private void roof() {
        box(464, 468, 570, 469);
        box(464, 468, 465, 491);
    }

    private int marks() {
        return NoteArticulationDetector.detect(
                        labels,
                        gray,
                        W,
                        H,
                        List.of(new NoteArticulationDetector.Anchor(500, 550, 16, 0)))[0]
                & NoteArticulation.STACCATO;
    }

    @Test
    public void endingNumberPeriodIsNotStaccato() {
        number();
        roof();
        assertEquals(0, marks());
    }

    @Test
    public void isolatedDotBelowBracketRemains() {
        roof();
        assertEquals(NoteArticulation.STACCATO, marks());
    }

    @Test
    public void nearbyDigitWithoutBracketDoesNotVetoDot() {
        number();
        assertEquals(NoteArticulation.STACCATO, marks());
    }

    @Test
    public void notationCannotSupplyEndingNumber() {
        number();
        roof();
        for (int y = 478; y <= 502; y++)
            for (int x = 476; x <= 490; x++)
                if ((gray[y * W + x] & 255) < 155)
                    labels[y * W + x] = OmrMeasurePostProcessor.NOTEHEAD;
        assertEquals(NoteArticulation.STACCATO, marks());
    }
}
