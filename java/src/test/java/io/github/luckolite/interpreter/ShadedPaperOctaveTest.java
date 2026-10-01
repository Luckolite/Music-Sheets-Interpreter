// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ring and stipple geometry; no source score pixels or font assets. */
public class ShadedPaperOctaveTest {
    private static final int W = 320, H = 300;

    private byte[] frame(int paper, int ink, boolean gradient) {
        byte[] gray = new byte[W * H];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                gray[y * W + x] = (byte) (paper + (gradient ? x * 30 / W : 0));
        for (int cy : new int[] {95, 104})
            for (int y = cy - 5; y <= cy + 5; y++)
                for (int x = 55; x <= 65; x++) {
                    double radius = Math.pow((x - 60) / 5d, 2) + Math.pow((y - cy) / 5d, 2);
                    if (radius <= 1.1 && radius >= .3) gray[y * W + x] = (byte) ink;
                }
        for (int x = 76; x < 210; x += 8)
            for (int xx = x; xx < x + 3; xx++) gray[93 * W + xx] = (byte) ink;
        return gray;
    }

    private List<ScoreNoteEvent> apply(byte[] gray) {
        var notes =
                List.of(
                        new ScoreNoteEvent(0, 100f / W, 2, 0, 1, 170f / H, false, 0, 1, 2, 0, 1),
                        new ScoreNoteEvent(0, 240f / W, 2, 0, 1, 170f / H, false, 0, 1, 2, 0, 1));
        return OctaveMarkDetector.apply(
                List.of(),
                List.of(new PlayingTechniqueDetector.Staff(150, 198, 12, 0, 1)),
                List.of(new MeasureRegion(0, 1, 0, 1)),
                notes,
                gray,
                W,
                H);
    }

    @Test
    public void shallowPaperRingsAndStipplesCannotTransposeNotes() {
        assertEquals(
                List.of(0, 0),
                apply(frame(176, 154, false)).stream().map(ScoreNoteEvent::octaveShift).toList());
    }

    @Test
    public void genuineDarkOctaveInkOnShadedPaperKeepsItsSpan() {
        assertEquals(
                List.of(1, 0),
                apply(frame(176, 70, false)).stream().map(ScoreNoteEvent::octaveShift).toList());
    }

    @Test
    public void genuineSpanSurvivesChangingPaperBrightness() {
        assertEquals(
                List.of(1, 0),
                apply(frame(174, 70, true)).stream().map(ScoreNoteEvent::octaveShift).toList());
    }

    @Test
    public void localContrastDoesNotModifyCallerPixels() {
        byte[] gray = frame(176, 70, false), before = gray.clone();
        apply(gray);
        assertTrue(Arrays.equals(before, gray));
    }
}
