// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic arpeggio beside an independent stemmed note. */
public class ArpeggioAccidentalOwnershipTest {
    @Test
    public void fragmentedLongWaveDoesNotAlterFollowingPitch() {
        var f = new JoinedSignatureSharpTest();
        f.row(100, 0, 0, false);
        for (int y = 110; y <= 215; y++) {
            int x = 320 + Math.round(5 * (float) Math.sin((y - 110) * Math.PI / 10));
            for (int dx = -2; dx <= 2; dx++) {
                f.gray[y * f.W + x + dx] = 0;
                if (y < 151 || y > 153 && y < 165 || y > 168) f.labels[y * f.W + x + dx] = 5;
            }
        }
        var result = OmrScoreInterpreter.analyze(f.labels, f.gray, f.W, f.H, f.measures);
        assertFalse(result.notes().isEmpty());
        for (var note : result.notes())
            assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY, note.writtenAccidental());
    }

    @Test
    public void realSharpBesideNoteIsPreserved() {
        var f = new JoinedSignatureSharpTest();
        f.row(100, 0, 0, false);
        f.sharp(325, 132);
        var result = OmrScoreInterpreter.analyze(f.labels, f.gray, f.W, f.H, f.measures);
        assertTrue(
                result.notes().stream()
                        .anyMatch(n -> n.writtenAccidental() == ScoreNoteEvent.ACCIDENTAL_SHARP));
    }
}
