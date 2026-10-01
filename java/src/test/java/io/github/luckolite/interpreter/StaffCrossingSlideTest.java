// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original shallow straight strokes broken by removal of crossed staff rules. */
public class StaffCrossingSlideTest {
    static final int W = 360, H = 220;

    byte[] page(boolean rules, boolean curved, boolean up) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        if (rules)
            for (int y : new int[] {100, 112, 124, 136, 148})
                for (int x = 0; x < W; x++) {
                    gray[y * W + x] = 0;
                    gray[(y + 1) * W + x] = 0;
                }
        for (int x = 92; x <= 188; x++) {
            int y =
                    (int)
                            Math.round(
                                    (up ? 124 : 104)
                                            + (x - 80) * (up ? -1 : 1) / 6d
                                            + (curved ? 8 * Math.sin((x - 92) * Math.PI / 96) : 0));
            gray[y * W + x] = 0;
            gray[(y + 1) * W + x] = 0;
        }
        return gray;
    }

    List<NoteSlideDetector.Stroke> detect(
            boolean rules, boolean curved, boolean up, int sourceStaff) {
        return NoteSlideDetector.detect(
                page(rules, curved, up),
                W,
                H,
                rules ? List.of(new NoteSlideDetector.Staff(100, 148, 12)) : List.of(),
                List.of(
                        new NoteSlideDetector.Head(80, up ? 124 : 104, 12, sourceStaff, 0),
                        new NoteSlideDetector.Head(200, up ? 104 : 124, 12, 0, 0)));
    }

    @Test
    public void shallowDescendingLineSurvivesRuleCrossing() {
        var found = detect(true, false, false, 0);
        assertEquals(1, found.size());
        assertTrue(found.get(0).connected());
    }

    @Test
    public void shallowAscendingLineSurvivesRuleCrossing() {
        var found = detect(true, false, true, 0);
        assertEquals(1, found.size());
        assertTrue(found.get(0).connected());
    }

    @Test
    public void intactStrokeRemainsRecognized() {
        assertEquals(1, detect(false, false, false, 0).size());
    }

    @Test
    public void curvedSlurIsNotRepairedAsStraightInk() {
        assertTrue(detect(true, true, false, 0).isEmpty());
    }

    @Test
    public void otherStaffCannotSupplyConnectingSource() {
        assertTrue(detect(true, false, false, 1).isEmpty());
    }

    @Test
    public void sourceChordIsAmbiguous() {
        assertTrue(
                NoteSlideDetector.detect(
                                page(true, false, false),
                                W,
                                H,
                                List.of(new NoteSlideDetector.Staff(100, 148, 12)),
                                List.of(
                                        new NoteSlideDetector.Head(80, 104, 12, 0, 0),
                                        new NoteSlideDetector.Head(80, 92, 12, 0, 0),
                                        new NoteSlideDetector.Head(200, 124, 12, 0, 0)))
                        .isEmpty());
    }

    @Test
    public void anotherMeasureCannotSupplySource() {
        assertTrue(
                NoteSlideDetector.detect(
                                page(true, false, false),
                                W,
                                H,
                                List.of(new NoteSlideDetector.Staff(100, 148, 12)),
                                List.of(
                                        new NoteSlideDetector.Head(80, 104, 12, 0, 1),
                                        new NoteSlideDetector.Head(200, 124, 12, 0, 0)))
                        .isEmpty());
    }

    @Test
    public void interveningAttackCannotBeSkipped() {
        assertTrue(
                NoteSlideDetector.detect(
                                page(true, false, false),
                                W,
                                H,
                                List.of(new NoteSlideDetector.Staff(100, 148, 12)),
                                List.of(
                                        new NoteSlideDetector.Head(80, 104, 12, 0, 0),
                                        new NoteSlideDetector.Head(156, 146, 12, 0, 0),
                                        new NoteSlideDetector.Head(200, 124, 12, 0, 0)))
                        .isEmpty());
    }

    @Test
    public void genuineMissingInkAwayFromRuleIsNotInvented() {
        byte[] gray = page(true, false, false);
        for (int x = 149; x <= 163; x++)
            for (int y = 114; y <= 121; y++) gray[y * W + x] = (byte) 255;
        assertTrue(
                NoteSlideDetector.detect(
                                gray,
                                W,
                                H,
                                List.of(new NoteSlideDetector.Staff(100, 148, 12)),
                                List.of(
                                        new NoteSlideDetector.Head(80, 104, 12, 0, 0),
                                        new NoteSlideDetector.Head(200, 124, 12, 0, 0)))
                        .isEmpty());
    }
}
