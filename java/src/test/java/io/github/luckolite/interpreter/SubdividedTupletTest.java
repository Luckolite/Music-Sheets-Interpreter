// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original block numerals and generated geometry, with no score scans. */
public class SubdividedTupletTest {
    private static byte[] five() {
        byte[] gray = new byte[500 * 240];
        Arrays.fill(gray, (byte) 255);
        for (int y = 0; y < 22; y++)
            for (int x = 0; x < 12; x++) {
                boolean ink =
                        y < 3
                                || y < 9 && x < 3
                                || y >= 9 && y < 12
                                || y >= 12 && y < 19 && x >= 9
                                || y >= 15 && y < 19 && x < 3
                                || y >= 19 && x >= 2 && x <= 8;
                if (ink) gray[(30 + y) * 500 + 174 + x] = 0;
            }
        return gray;
    }

    private static List<ScoreNoteEvent> divided() {
        List<ScoreNoteEvent> result = new ArrayList<>();
        float[] positions = {.20f, .28f, .36f, .44f, .50f, .52f};
        for (int i = 0; i < positions.length; i++)
            result.add(
                    new ScoreNoteEvent(
                            0, positions[i], 0, 0, 1, .4f, false, 0, i < 4 ? 2 : 3, 2, 0));
        return result;
    }

    @Test
    public void twoThirtySecondsReplaceOneQuintupletSixteenth() {
        var result =
                TripletRhythmDetector.apply(
                        divided(), List.of(new MeasureRegion(0, 1, .1f, .8f)), five(), 500, 240);
        double total = 0;
        for (int i = 0; i < result.size(); i++) {
            assertEquals(5, result.get(i).tupletDivisor());
            assertEquals(
                    i < 4 ? .2 : .1, ScoreNoteTiming.writtenDurationBeats(result.get(i)), 1e-7);
            total += ScoreNoteTiming.writtenDurationBeats(result.get(i));
        }
        assertEquals(1, total, 1e-7);
    }

    @Test
    public void missingNumeralDoesNotScaleMixedBeam() {
        byte[] gray = new byte[500 * 240];
        Arrays.fill(gray, (byte) 255);
        assertEquals(
                divided(),
                TripletRhythmDetector.apply(
                        divided(), List.of(new MeasureRegion(0, 1, .1f, .8f)), gray, 500, 240));
    }

    @Test
    public void subdivisionMayOpenAndCloseTheGroup() {
        var notes = divided();
        for (int i = 0; i < notes.size(); i++) {
            var n = notes.get(i);
            notes.set(
                    i,
                    new ScoreNoteEvent(
                            0,
                            n.positionInMeasure(),
                            0,
                            0,
                            1,
                            .4f,
                            false,
                            0,
                            i == 0 || i == 5 ? 3 : 2,
                            2,
                            0));
        }
        var result =
                TripletRhythmDetector.apply(
                        notes, List.of(new MeasureRegion(0, 1, .1f, .8f)), five(), 500, 240);
        for (var n : result) assertEquals(5, n.tupletDivisor());
    }

    @Test
    public void narrowCapFiveStillLabelsSubdividedTuplet() {
        byte[] gray = new byte[500 * 240];
        Arrays.fill(gray, (byte) 255);
        for (int y = 0; y < 24; y++)
            for (int x = 0; x < 17; x++) {
                boolean ink =
                        y < 3 && x >= 8
                                || y >= 3 && y < 7 && x >= 6 && x <= 8
                                || y >= 7 && y < 10 && x >= 5 && x <= 13
                                || y >= 10 && y < 19 && x >= 12 && x <= 14
                                || y >= 19 && y < 22 && x >= 2 && x <= 13
                                || y >= 22 && x >= 4 && x <= 8;
                if (ink) gray[(30 + y) * 500 + 172 + x] = 0;
            }
        var result =
                TripletRhythmDetector.apply(
                        divided(), List.of(new MeasureRegion(0, 1, .1f, .8f)), gray, 500, 240);
        for (var n : result) assertEquals(5, n.tupletDivisor());
    }

    @Test
    public void numeralBetweenStavesDoesNotScaleAccompaniment() {
        byte[] gray = new byte[400 * 240];
        Arrays.fill(gray, (byte) 255);
        String[] rows = {
            "..#######...",
            ".##########.",
            "###......###",
            "####.....###",
            "####.....###",
            "####.....###",
            ".##.....####",
            ".......####.",
            "......####..",
            "....#####...",
            "....#####...",
            "....#####...",
            "......####..",
            ".......####.",
            "##.....####.",
            "###....####.",
            "###....####.",
            "###....####.",
            ".###....###.",
            "..########..",
            "..########..",
            "....####...."
        };
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < 12; x++)
                if (rows[y].charAt(x) == '#') gray[(125 + y) * 400 + 119 + x] = 0;
        List<ScoreNoteEvent> notes = new ArrayList<>();
        for (float x : new float[] {.25f, .3125f, .375f})
            notes.add(new ScoreNoteEvent(0, x, 0, 0, 2, .4f, false, 0, 1, 2, 0));
        for (float x : new float[] {.25f, .3125f, .375f})
            notes.add(new ScoreNoteEvent(0, x, 8, 1, 2, .79f, false, 0, 2, 2, 0));
        var result =
                TripletRhythmDetector.apply(
                        notes, List.of(new MeasureRegion(0, 1, .1f, .9f)), gray, 400, 240);
        for (int i = 0; i < 3; i++) assertEquals(3, result.get(i).tupletDivisor());
        for (int i = 3; i < 6; i++) assertEquals(1, result.get(i).tupletDivisor());
    }
}
