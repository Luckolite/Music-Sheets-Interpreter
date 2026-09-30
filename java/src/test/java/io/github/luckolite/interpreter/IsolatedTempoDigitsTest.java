// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Isolated OCR digits require a complete printed note-equals-number equation. */
public class IsolatedTempoDigitsTest {
    private PlayingTechniqueDetector.Word word(String text) {
        return new PlayingTechniqueDetector.Word(
                text, 164f / 500, 62f / 260, 176f / 500, 89f / 260);
    }

    private List<MeasureNumberReconciler.NumberToken> candidates(
            TempoUnitNoteTest.Page p, String text) {
        return TempoChangeDetector.withIsolatedDigits(
                List.of(), List.of(word(text)), p.gray, p.w, p.h);
    }

    @Test
    public void isolatedDigitsWithPrintedBeatAndEqualsAreRecovered() {
        var p = new TempoUnitNoteTest.Page();
        assertEquals(1, candidates(p, "96").size());
    }

    @Test
    public void digitsAloneStayRejected() {
        var p = new TempoUnitNoteTest.Page();
        p.erase(100, 40, 55, 55);
        assertTrue(candidates(p, "96").isEmpty());
    }

    @Test
    public void theoryEqualsWithoutBeatNoteStaysRejected() {
        var p = new TempoUnitNoteTest.Page();
        p.erase(100, 40, 25, 55);
        assertTrue(candidates(p, "96").isEmpty());
    }

    @Test
    public void chordLabelsStayRejected() {
        assertTrue(candidates(new TempoUnitNoteTest.Page(), "C96").isEmpty());
    }

    @Test
    public void FingeringDigitsStayRejected() {
        assertTrue(candidates(new TempoUnitNoteTest.Page(), "3").isEmpty());
    }

    @Test
    public void duplicateEquationEvidenceDoesNotDuplicateTempo() {
        var p = new TempoUnitNoteTest.Page();
        var tokens = candidates(p, "96");
        assertEquals(
                1,
                TempoChangeDetector.withIsolatedDigits(
                                tokens, List.of(word("96")), p.gray, p.w, p.h)
                        .size());
    }
}
