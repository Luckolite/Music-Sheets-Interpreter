// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original bass-clef header with a nearby note outside the signature's altered pitches. */
public class BassSignatureOwnershipTest {
    private JoinedSignatureSharpTest page(boolean dots) {
        var f = new JoinedSignatureSharpTest();
        f.row(100, 0, 0, false);
        for (int y = 70; y <= 180; y++)
            for (int x = 30; x <= 55; x++) {
                f.labels[y * f.W + x] = 0;
                f.gray[y * f.W + x] = (byte) 255;
            }
        for (int y = 100; y <= 152; y++)
            for (int x = 35; x <= 58; x++)
                if (x >= 43 - (y - 100) / 8 && x <= 58 - (y - 100) / 3) f.ink(x, y, 3);
        if (dots)
            for (int cy : new int[] {108, 124})
                for (int y = cy - 3; y <= cy + 3; y++)
                    for (int x = 63; x <= 69; x++)
                        if ((x - 66) * (x - 66) + (y - cy) * (y - cy) <= 9) f.ink(x, y, 3);
        int[] ys = {116, 140, 108, 132};
        for (int i = 0; i < 4; i++) f.sharp(91 + 21 * i, ys[i]);
        return f;
    }

    @Test
    public void bassClefIntroducesFourSharpHeader() {
        assertEquals(List.of(4), page(true).keys());
    }

    @Test
    public void headerGlyphsAreExcludedFromLocalAccidentals() {
        var f = page(true);
        // Move the synthetic note close enough to expose signature ownership mistakes.
        for (int y = 84; y <= 138; y++)
            for (int x = 340; x <= 360; x++) {
                f.labels[y * f.W + x] = 0;
                f.gray[y * f.W + x] = (byte) 255;
            }
        for (int y = 93; y <= 107; y++)
            for (int x = 188; x <= 208; x++)
                if (Math.pow((x - 198) / 10d, 2) + Math.pow((y - 100) / 7d, 2) <= 1) f.ink(x, y, 2);
        for (int y = 100; y <= 152; y++) f.ink(188, y, 1);
        var result = OmrScoreInterpreter.analyze(f.labels, f.gray, f.W, f.H, f.measures);
        assertFalse(result.notes().isEmpty());
        for (var n : result.notes())
            assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY, n.writtenAccidental());
    }

    @Test
    public void bassHeaderAnalysisPreservesInput() {
        var f = page(true);
        var labels = f.labels.clone();
        var gray = f.gray.clone();
        f.keys();
        assertArrayEquals(labels, f.labels);
        assertArrayEquals(gray, f.gray);
    }
}
