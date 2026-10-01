// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two-capital abbreviation geometry with punctuation between the initials. */
public class InitialsPunctuationArticulationTest {
    static final int W = 1500, H = 1600;

    private int detect(int owner, boolean second, boolean notation, boolean extraLetter) {
        return detect(owner, second, notation, extraLetter, false);
    }

    private int detect(
            int owner, boolean second, boolean notation, boolean extraLetter, boolean italic) {
        byte[] gray = new byte[W * H], labels = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int left : extraLetter ? new int[] {300, 325} : new int[] {300}) {
            for (int y = 90; y <= 107; y++)
                for (int x = left; x <= left + 13; x++)
                    if (x <= left + 1 || x >= left + 12 || y <= 91 || y >= 106) {
                        gray[y * W + x] = 0;
                        if (notation) labels[y * W + x] = OmrMeasurePostProcessor.NOTEHEAD;
                    }
        }
        if (italic)
            for (int left : new int[] {300, 325})
                for (int y = 90; y <= 92; y++)
                    for (int x = left + 13; x <= left + 19; x++) gray[y * W + x] = 0;
        for (int left : second ? new int[] {316, 341} : new int[] {316})
            for (int y = 104; y <= 108; y++)
                for (int x = left; x <= left + 4; x++) gray[y * W + x] = 0;
        return NoteArticulationDetector.detect(
                labels,
                gray,
                W,
                H,
                List.of(new NoteArticulationDetector.Anchor(owner, 142, 16, 0)))[0];
    }

    @Test
    public void firstInitialPeriodIsNotStaccato() {
        assertEquals(0, detect(318, true, false, true));
    }

    @Test
    public void secondInitialPeriodIsNotStaccato() {
        assertEquals(0, detect(343, true, false, true));
    }

    @Test
    public void loneLetterDoesNotProveAbbreviation() {
        assertEquals(NoteArticulation.STACCATO, detect(318, false, false, false));
    }

    @Test
    public void acceptedHeadsCannotProveText() {
        assertEquals(NoteArticulation.STACCATO, detect(318, true, true, true));
    }

    @Test
    public void italicOverhangDoesNotOwnFirstPeriod() {
        assertEquals(0, detect(318, true, false, true, true));
    }

    @Test
    public void italicOverhangDoesNotOwnSecondPeriod() {
        assertEquals(0, detect(343, true, false, true, true));
    }
}
