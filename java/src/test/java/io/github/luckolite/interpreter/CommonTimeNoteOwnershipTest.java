// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original C-shaped ink with explicit independently recognized note ownership. */
public class CommonTimeNoteOwnershipTest {
    private List<ScoreMeterChange> read(float x, float y) {
        var p = new HeaderSymbolNormalizationTest.Page(false, true);
        var measures = List.of(new MeasureRegion(100f / p.w, 450f / p.w, 60f / p.h, 170f / p.h));
        var note = new ScoreNoteEvent(0, (x - 100) / 350, 4, 0, 1, y / p.h, false, 0, 0);
        return MeterChangeDetector.commonTimeReadings(
                p.labels, p.gray, p.w, p.h, measures, List.of(note));
    }

    @Test
    public void knownChordHeadInsideShapeCannotBecomeCommonTime() {
        assertTrue(read(126, 104).isEmpty());
    }

    @Test
    public void followingNoteDoesNotInvalidatePrintedCommonTime() {
        assertEquals(List.of(new ScoreMeterChange(0, 4, 4)), read(210, 112));
    }

    @Test
    public void noteOnAnotherStaffDoesNotOwnTheSign() {
        assertEquals(List.of(new ScoreMeterChange(0, 4, 4)), read(126, 205));
    }
}
