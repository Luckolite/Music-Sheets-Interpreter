// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic numeral and dash geometry; no source score pixels. */
public class OctaveNumeralBoundsTest {
    @Test
    public void confirmedNumeralBoundsExcludeSoundingNotes() {
        int w = 320, h = 340;
        byte[] gray = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int cy : new int[] {215, 224})
            for (int y = cy - 5; y <= cy + 5; y++)
                for (int x = 55; x <= 65; x++) {
                    double r = Math.pow((x - 60) / 5d, 2) + Math.pow((y - cy) / 5d, 2);
                    if (r <= 1.1 && r >= .3) gray[y * w + x] = 0;
                }
        for (int x = 76; x < 212; x += 8) for (int xx = x; xx < x + 3; xx++) gray[213 * w + xx] = 0;
        var words =
                OctaveMarkDetector.printedWords(
                        gray,
                        w,
                        h,
                        List.of(
                                new PlayingTechniqueDetector.Staff(100, 148, 12, 0, 1),
                                new PlayingTechniqueDetector.Staff(250, 298, 12, 0, 1)));
        assertTrue(OctaveMarkDetector.containsPrintedMark(words, 60, 215, w, h));
        assertFalse(OctaveMarkDetector.containsPrintedMark(words, 100, 125, w, h));
        assertFalse(OctaveMarkDetector.containsPrintedMark(List.of(), 60, 215, w, h));
    }
}
