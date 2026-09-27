// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic extender and independent note-onset examples. */
public class ContinuedArticulationRowTest {
    private static final int W = 1500, H = 2000;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];

    public ContinuedArticulationRowTest() {
        Arrays.fill(gray, (byte) 255);
    }

    private void box(int l, int t, int r, int b) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) gray[y * W + x] = 0;
    }

    private void row(int count, int step) {
        for (int i = 0; i < count; i++) box(300 + i * step, 500, 313 + i * step, 501);
    }

    private NoteArticulationDetector.Anchor note(int x) {
        return new NoteArticulationDetector.Anchor(x, 460, 20, 0);
    }

    private int[] detect(List<NoteArticulationDetector.Anchor> notes) {
        return NoteArticulationDetector.detect(labels, gray, W, H, notes);
    }

    @Test
    public void wordlessContinuedRowDoesNotFollowOnsets() {
        row(6, 45);
        assertEquals(0, detect(List.of(note(307)))[0]);
    }

    @Test
    public void closeSpacedPedalDashesAreNotTenuto() {
        row(10, 25);
        box(538, 490, 540, 501);
        assertEquals(0, detect(List.of(note(432)))[0]);
    }

    @Test
    public void fiveGenuineRepeatedTenutosRemain() {
        row(5, 45);
        List<NoteArticulationDetector.Anchor> notes = new ArrayList<>();
        for (int i = 0; i < 5; i++) notes.add(note(307 + i * 45));
        for (int mark : detect(notes)) assertEquals(NoteArticulation.TENUTO, mark);
    }

    @Test
    public void twoMissedOwnersDoNotEraseRepeatedTenutos() {
        row(5, 45);
        for (int mark : detect(List.of(note(307), note(352), note(397))))
            assertEquals(NoteArticulation.TENUTO, mark);
    }

    @Test
    public void irregularSpacedMarksDoNotBecomeExtender() {
        for (int x : new int[] {300, 345, 402, 460, 507, 571}) box(x, 500, x + 13, 501);
        assertEquals(NoteArticulation.TENUTO, detect(List.of(note(307)))[0]);
    }

    @Test
    public void realStaccatoUnderContinuedLineSurvives() {
        row(6, 45);
        box(304, 477, 309, 482);
        assertEquals(NoteArticulation.STACCATO, detect(List.of(note(307)))[0]);
    }

    @Test
    public void shortRowWithoutWordIsNotEnough() {
        row(4, 45);
        assertEquals(NoteArticulation.TENUTO, detect(List.of(note(307)))[0]);
    }

    @Test
    public void otherStaffAttacksDoNotOwnAnExtender() {
        row(6, 45);
        List<NoteArticulationDetector.Anchor> notes = new ArrayList<>();
        notes.add(note(307));
        for (int i = 0; i < 6; i++)
            notes.add(new NoteArticulationDetector.Anchor(307 + i * 45, 620, 20, 1));
        assertEquals(0, detect(notes)[0]);
    }
}
