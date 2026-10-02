// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PaperDynamicProposalTest {
    private static final int W = 1280, H = 900;

    private List<PlayingTechniqueDetector.Word> boxes(
            int tone, int rails, boolean witness, boolean word, boolean compound, boolean clipped) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) tone);
        for (int line = 0; line < rails; line++)
            for (int x = 200; x < 1000; x++) gray[(260 - line * 14) * W + x] = 30;
        if (word) {
            int start = clipped ? 246 : 650;
            for (int y = 290; y < 320; y++)
                for (int x = start; x < start + 15; x++)
                    if (x < start + 3 || y < 293 || y > 316) gray[y * W + x] = 30;
            if (compound)
                for (int y = 290; y < 320; y++)
                    for (int x = start + 19; x < start + 34; x++)
                        if (x < start + 22 || y < 293 || y > 316) gray[y * W + x] = 30;
        }
        var staff = new PlayingTechniqueDetector.Staff(230, 286, 14, 0, 1);
        var region = new MeasureRegion(.2f, .8f, .18f, .36f);
        var note = new ScoreNoteEvent(0, .5f, 4, 0, 1, 232f / H, false, 0, 0, 2, 1, 1, 0, 0);
        byte[] before = gray.clone();
        var found =
                ScoreDynamicsDetector.paperSymbolBoxes(
                        gray,
                        List.of(staff),
                        List.of(region),
                        witness ? List.of(note) : List.of(),
                        W,
                        H);
        assertArrayEquals(before, gray);
        return found;
    }

    @Test
    public void shadedLocalBodyHasNoInventedIdentity() {
        var b = boxes(140, 5, true, true, false, false);
        assertEquals(1, b.size());
        assertEquals("", b.get(0).text());
    }

    @Test
    public void adjacentLettersStayComplete() {
        var b = boxes(140, 5, true, true, true, false);
        assertEquals(1, b.size());
        assertTrue((b.get(0).right() - b.get(0).left()) * W > 30);
    }

    @Test
    public void absentPhysicalStaffRejects() {
        assertTrue(boxes(140, 0, true, true, false, false).isEmpty());
    }

    @Test
    public void fourRailsCannotInventStaff() {
        assertTrue(boxes(140, 4, true, true, false, false).isEmpty());
    }

    @Test
    public void absentWrittenWitnessRejects() {
        assertTrue(boxes(140, 5, false, true, false, false).isEmpty());
    }

    @Test
    public void noLetterBodyReturnsNoWord() {
        assertTrue(boxes(140, 5, true, false, false, false).isEmpty());
    }

    @Test
    public void brightPageKeepsOriginalPath() {
        assertTrue(boxes(255, 5, true, true, false, false).isEmpty());
    }

    @Test
    public void clippedMeasureWordRejects() {
        assertTrue(boxes(140, 5, true, true, false, true).isEmpty());
    }

    @Test
    public void invalidRasterReturnsNoWord() {
        assertTrue(
                ScoreDynamicsDetector.paperSymbolBoxes(
                                new byte[1], List.of(), List.of(), List.of(), W, H)
                        .isEmpty());
    }
}
