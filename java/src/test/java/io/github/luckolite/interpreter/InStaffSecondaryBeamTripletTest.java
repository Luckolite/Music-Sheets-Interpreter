// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

/** Original generated engraving: three double-beamed attacks followed by one eighth. */
public class InStaffSecondaryBeamTripletTest {
    private static final int W = 400, H = 240;
    private static final List<MeasureRegion> BARS =
            List.of(new MeasureRegion(0, 1, .25f, .7166667f));
    private static final String[] THREE = {
        "..#######...", ".##########.", "###......###", "####.....###",
        "####.....###", "####.....###", ".##.....####", ".......####.",
        "......####..", "....#####...", "....#####...", "....#####...",
        "......####..", ".......####.", "##.....####.", "###....####.",
        "###....####.", "###....####.", ".###....###.", "..########..",
        "..########..", "....####...."
    };

    private static byte[] ink(boolean numeral, boolean secondary) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int row : new int[] {110, 124, 138, 152, 166})
            for (int x = 20; x < 380; x++) gray[row * W + x] = 0;
        for (int x = 94; x <= 169; x++) for (int y = 112; y <= 115; y++) gray[y * W + x] = 0;
        if (secondary)
            for (int x = 94; x <= 144; x++) for (int y = 104; y <= 107; y++) gray[y * W + x] = 0;
        for (int x : new int[] {94, 119, 144, 169})
            for (int y = 72; y <= 115; y++) gray[y * W + x] = 0;
        if (numeral)
            for (int y = 0; y < THREE.length; y++)
                for (int x = 0; x < 12; x++)
                    if (THREE[y].charAt(x) == '#') gray[(143 + y) * W + 119 + x] = 0;
        return gray;
    }

    private static List<ScoreNoteEvent> notes() {
        List<ScoreNoteEvent> result = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            result.add(
                    new ScoreNoteEvent(
                            0,
                            .25f + i * .0625f,
                            i == 1 ? 12 : 11,
                            0,
                            1,
                            (i == 1 ? 72f : 79f) / H,
                            false,
                            0,
                            i < 3 ? 2 : 1,
                            2,
                            0));
        return result;
    }

    @Test
    public void physicalSecondaryBeamOwnsPrintedThreeInsideStaff() {
        var result = TripletRhythmDetector.apply(notes(), BARS, ink(true, true), W, H);
        for (int i = 0; i < 3; i++) assertEquals(3, result.get(i).tupletDivisor());
        assertEquals(1, result.get(3).tupletDivisor());
    }

    @Test
    public void secondaryBeamWithoutPrintedNumeralDoesNotInventTuplet() {
        var original = notes();
        assertEquals(original, TripletRhythmDetector.apply(original, BARS, ink(false, true), W, H));
    }

    @Test
    public void printedThreeWithoutPhysicalSecondaryBeamCannotGroup() {
        var original = notes();
        assertEquals(original, TripletRhythmDetector.apply(original, BARS, ink(true, false), W, H));
    }

    @Test
    public void separateSecondaryBeamsCannotSupplyOneGroup() {
        byte[] gray = ink(true, true);
        for (int x = 121; x < 134; x++)
            for (int y = 104; y <= 107; y++) gray[y * W + x] = (byte) 255;
        var original = notes();
        assertEquals(original, TripletRhythmDetector.apply(original, BARS, gray, W, H));
    }
}
