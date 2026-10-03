// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural numeral, sloping beams and unrelated pitches; no source-score fixture. */
public class NonBinaryTupletMembershipTest {
    private static final int W = 640, H = 360;
    private static final float[] X = {200, 230, 265, 325, 360, 400};

    private static byte[] image(boolean numeral, boolean endBreak, boolean startBreak) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        if (numeral)
            for (int y = 0; y < 22; y++)
                for (int x = 0; x < 12; x++) {
                    boolean ink =
                            y < 3
                                    || y < 9 && x < 3
                                    || y >= 9 && y < 12
                                    || y >= 12 && y < 19 && x >= 9
                                    || y >= 15 && y < 19 && x < 3
                                    || y >= 19 && x >= 2 && x <= 8;
                    if (ink) gray[(40 + y) * W + 274 + x] = 0;
                }
        for (int x = 209; x <= 409; x++) {
            int y = Math.round(79 + (x - 289) * .1f);
            for (int dy = -3; dy <= 3; dy++) gray[(y + dy) * W + x] = 0;
            if (x <= 369 || !endBreak)
                for (int dy = -3; dy <= 3; dy++) gray[(y + 12 + dy) * W + x] = 0;
        }
        if (!startBreak)
            for (int x = 185; x < 209; x++) {
                int y = Math.round(91 + (x - 289) * .1f);
                for (int dy = -3; dy <= 3; dy++) gray[(y + dy) * W + x] = 0;
            }
        return gray;
    }

    private static List<ScoreNoteEvent> notes(boolean independentClock) {
        List<ScoreNoteEvent> notes = new ArrayList<>();
        for (float x : new float[] {.08f, .16f, .24f})
            notes.add(new ScoreNoteEvent(0, x, 0, 0, 2, 130f / H, false, 0, 0, 2, 1));
        for (float x : X)
            notes.add(new ScoreNoteEvent(0, x / W, 0, 0, 2, 130f / H, false, 0, 2, 2, 0));
        // A regular final chord counts once and must retain both of its heads.
        notes.add(new ScoreNoteEvent(0, X[5] / W, -2, 0, 2, 150f / H, false, 0, 2, 2, 0));
        if (independentClock)
            for (float x : new float[] {.08f, .30f, .55f, .8f})
                notes.add(new ScoreNoteEvent(0, x, 0, 1, 2, .82f, false, 0, 0, 2, 1));
        return notes;
    }

    private static List<ScoreNoteEvent> apply(List<ScoreNoteEvent> notes, byte[] gray) {
        return TripletRhythmDetector.apply(
                notes, List.of(new MeasureRegion(0, 1, .1f, .9888889f)), gray, W, H);
    }

    @Test
    public void innerBeamBreakLeavesFinalChordRegularDespiteUnevenHeadSpacing() {
        var result = apply(notes(true), image(true, true, true));
        for (int i = 3; i < 8; i++) {
            assertEquals(5, result.get(i).tupletDivisor());
            assertEquals(3, result.get(i).tupletNormalNotes());
            assertEquals(.15, ScoreNoteTiming.writtenDurationBeats(result.get(i)), 1e-8);
        }
        for (int i : new int[] {8, 9}) {
            assertEquals(1, result.get(i).tupletDivisor());
            assertEquals(.25, ScoreNoteTiming.writtenDurationBeats(result.get(i)), 1e-8);
        }
        assertEquals(3.75, ScoreNoteTiming.beatInMeasure(result.get(8), result, 4), 1e-7);
    }

    @Test
    public void fullSixAttackInnerBeamDoesNotInventShorterMembership() {
        var result = apply(notes(true), image(true, false, true));
        assertTrue(result.stream().noneMatch(n -> n.tupletNormalNotes() == 3));
    }

    @Test
    public void continuationBeforeOpeningDoesNotInventMembership() {
        var result = apply(notes(true), image(true, true, false));
        assertTrue(result.stream().noneMatch(n -> n.tupletNormalNotes() == 3));
    }

    @Test
    public void missingPrintedNumeralDoesNotInferFromBarLength() {
        var notes = notes(true);
        assertEquals(notes, apply(notes, image(false, true, true)));
    }

    @Test
    public void noIndependentClockCannotChooseNonBinaryRatio() {
        var result = apply(notes(false), image(true, true, true));
        assertTrue(result.stream().noneMatch(n -> n.tupletNormalNotes() == 3));
    }

    @Test
    public void contradictoryOtherStaffDoesNotChooseNonBinaryRatio() {
        var notes = notes(true);
        notes.remove(notes.size() - 1);
        var result = apply(notes, image(true, true, true));
        assertTrue(result.stream().noneMatch(n -> n.tupletNormalNotes() == 3));
    }
}
