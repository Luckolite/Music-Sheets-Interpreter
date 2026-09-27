// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original overlapping letter/dot constructions, not extracted score pixels. */
public class TextJoinedStaccatoTest {
    private static final int W = 1500, H = 2400;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    public TextJoinedStaccatoTest() {
        Arrays.fill(gray, (byte) 255);
    }

    private void box(int l, int t, int r, int b) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) gray[y * W + x] = 0;
    }

    private void letter(int x) {
        box(x, 453, x + 3, 490);
        box(x, 453, x + 17, 456);
        box(x, 487, x + 17, 490);
    }

    private void word() {
        letter(620);
        letter(647);
        box(701, 464, 706, 496);
    }

    private void dot() {
        for (int y = 474; y <= 486; y++)
            for (int x = 692; x <= 706; x++)
                if ((x - 700) * (x - 700) + (y - 480) * (y - 480) <= 30) gray[y * W + x] = 0;
    }

    private int mark() {
        return NoteArticulationDetector.detect(
                labels, gray, W, H, List.of(new NoteArticulationDetector.Anchor(700, 500, 20, 0)))[
                0];
    }

    @Test
    public void roundDotJoinedToLetterUprightIsRecovered() {
        word();
        dot();
        assertEquals(NoteArticulation.STACCATO, mark());
    }

    @Test
    public void plainLetterUprightDoesNotInventDot() {
        word();
        assertEquals(0, mark());
    }

    @Test
    public void rectangularLetterCrossbarDoesNotBecomeDot() {
        word();
        box(695, 477, 705, 483);
        assertEquals(0, mark());
    }

    @Test
    public void hollowLetterBowlDoesNotBecomeDot() {
        word();
        box(694, 474, 701, 475);
        box(694, 484, 701, 485);
        box(694, 474, 695, 485);
        assertEquals(0, mark());
    }

    @Test
    public void musicalStemWithoutTextDoesNotAuthorizeRecovery() {
        box(701, 464, 706, 496);
        dot();
        assertEquals(0, mark());
    }

    @Test
    public void oneNearbyLetterIsNotEnough() {
        letter(647);
        box(701, 464, 706, 496);
        dot();
        assertEquals(0, mark());
    }

    @Test
    public void semanticNotationDoesNotEstablishText() {
        word();
        dot();
        for (int p = 0; p < labels.length; p++)
            if (p % W < 680 && (gray[p] & 255) < 155)
                labels[p] = OmrMeasurePostProcessor.CLEF_OR_KEY;
        assertEquals(0, mark());
    }

    @Test
    public void sharedChordDotAppliesToBothHeads() {
        word();
        dot();
        int[] marks =
                NoteArticulationDetector.detect(
                        labels,
                        gray,
                        W,
                        H,
                        List.of(
                                new NoteArticulationDetector.Anchor(700, 500, 20, 0),
                                new NoteArticulationDetector.Anchor(701, 520, 20, 0)));
        assertArrayEquals(new int[] {4, 4}, marks);
    }
}
