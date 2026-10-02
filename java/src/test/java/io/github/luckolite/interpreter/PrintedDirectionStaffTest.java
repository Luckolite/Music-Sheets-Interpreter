// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original local direction examples require a written-head phase and bilateral five-rule ink. */
public class PrintedDirectionStaffTest {
    private static final int W = 1280, H = 900;

    private PlayingTechniqueDetector.Staff find(
            int rails,
            int shift,
            float slope,
            int tone,
            boolean broad,
            boolean missingWitness,
            boolean crossStaff,
            float x) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) tone);
        for (int line = 0; line < rails; line++)
            for (int xx = 200; xx < 1000; xx++) {
                int y = 260 - line * 14 + shift + Math.round((xx - 640) * slope);
                for (int dy = broad ? -6 : 0; dy <= (broad ? 6 : 0); dy++)
                    gray[(y + dy) * W + xx] = 30;
            }
        var staff = new PlayingTechniqueDetector.Staff(230, 286, 14, 0, 1);
        var region = new MeasureRegion(.2f, .8f, .18f, .36f);
        var note = new ScoreNoteEvent(0, .5f, 4, 0, 1, 232f / H, false, 0, 0, 2, 1, 1, 0, 0);
        if (crossStaff) note = note.withCrossStaffBeam();
        byte[] before = gray.clone();
        var result =
                PrintedDirectionStaff.at(
                        List.of(staff),
                        List.of(region),
                        missingWitness ? List.of() : List.of(note),
                        gray,
                        W,
                        H,
                        x,
                        275,
                        288);
        assertArrayEquals(before, gray);
        return result;
    }

    @Test
    public void completeLocalStaffOwnsDirectionInsideGlobalFrame() {
        var s = find(5, 0, 0, 255, false, false, false, 640);
        assertNotNull(s);
        assertEquals(260, s.bottom(), 1);
    }

    @Test
    public void fiveTiltedRailsKeepIndependentOwnership() {
        assertNotNull(find(5, 0, .08f, 255, false, false, false, 640));
    }

    @Test
    public void oppositeTiltKeepsIndependentOwnership() {
        assertNotNull(find(5, 0, -.08f, 255, false, false, false, 640));
    }

    @Test
    public void shadedPaperStillNeedsPrintedRails() {
        assertNotNull(find(5, 0, 0, 145, false, false, false, 640));
    }

    @Test
    public void fourRailsCannotSupplyMissingStaff() {
        assertNull(find(4, 0, 0, 255, false, false, false, 640));
    }

    @Test
    public void threeRailsCannotSupplyMissingStaff() {
        assertNull(find(3, 0, 0, 255, false, false, false, 640));
    }

    @Test
    public void noRailsCannotSupplyMissingStaff() {
        assertNull(find(0, 0, 0, 255, false, false, false, 640));
    }

    @Test
    public void broadShadeCannotSupplyStaff() {
        assertNull(find(5, 0, 0, 255, true, false, false, 640));
    }

    @Test
    public void wrongHeadPhaseCannotChooseStaff() {
        assertNull(find(5, 14, 0, 255, false, false, false, 640));
    }

    @Test
    public void noWrittenHeadCannotChooseStaff() {
        assertNull(find(5, 0, 0, 255, false, true, false, 640));
    }

    @Test
    public void crossStaffHeadCannotChooseStaff() {
        assertNull(find(5, 0, 0, 255, false, false, true, 640));
    }

    @Test
    public void distantHeadCannotChooseStaff() {
        assertNull(find(5, 0, 0, 255, false, false, false, 900));
    }

    @Test
    public void nonfiniteColumnCannotChooseStaff() {
        assertNull(find(5, 0, 0, 255, false, false, false, Float.NaN));
    }

    @Test
    public void clippedLocalWindowCannotChooseStaff() {
        assertNull(find(5, 0, 0, 255, false, false, false, 3));
    }
}
