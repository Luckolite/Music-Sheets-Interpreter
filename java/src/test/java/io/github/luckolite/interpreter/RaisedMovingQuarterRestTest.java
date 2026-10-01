// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original raised zigzag rest above a separate downward-stem voice. */
public class RaisedMovingQuarterRestTest {
    private List<ScoreRestEvent> read(boolean stem, int staff, int measure, boolean dot) {
        var p = new CompactQuarterRestTest.Page(16, true, true, false, false);
        Arrays.fill(p.gray, (byte) 240);
        for (int y = 100; y <= 164; y += 16) for (int x = 20; x < 580; x++) p.gray[y * 600 + x] = 0;
        int[][] rows = {
            {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 8}, {7, 10}, {7, 11}, {6, 11},
            {6, 11}, {5, 11}, {5, 11}, {4, 10}, {4, 9}, {5, 9}, {6, 9}, {7, 10}, {8, 11}, {6, 12},
            {4, 13}, {3, 13}, {2, 13}, {2, 6}, {3, 6}, {3, 6}, {4, 7}, {5, 7}, {6, 8}, {7, 9},
            {8, 9}
        };
        for (int row = 0; row < rows.length; row++)
            for (int y = 49 + Math.round(row * 1.5f); y <= 50 + Math.round(row * 1.5f); y++)
                for (int x = 150 + rows[row][0]; x <= 150 + rows[row][1]; x++)
                    p.gray[y * 600 + x] = 0;
        if (stem)
            for (int y = 110; y <= 150; y++)
                for (int x = 148; x <= 151; x++) p.gray[y * 600 + x] = 0;
        if (dot)
            for (int y = 58; y <= 62; y++)
                for (int x = 173; x <= 177; x++)
                    if ((x - 175) * (x - 175) + (y - 60) * (y - 60) <= 4) p.gray[y * 600 + x] = 0;
        var note =
                new ScoreNoteEvent(measure, 160 / 600f, 0, staff, 1, 110 / 280f, false, 0, 1, 2, 0);
        return SixteenthRestDetector.detect(
                p.gray,
                600,
                280,
                List.of(new MeasureRegion(0, 1, 0, 1)),
                List.of(new SixteenthRestDetector.Staff(100, 164, 16, 0, 1)),
                List.of(note));
    }

    @Test
    public void printedDownwardVoiceAllowsRaisedQuarterRest() {
        assertTrue(
                read(true, 0, 0, false).stream()
                        .anyMatch(r -> r.durationBeats() == 1 && r.pageY() < .35));
    }

    @Test
    public void printedDownwardVoiceAllowsDottedQuarterRest() {
        assertTrue(
                read(true, 0, 0, true).stream()
                        .anyMatch(r -> r.durationBeats() == 1.5 && r.pageY() < .35));
    }

    @Test
    public void bareHeadDoesNotAuthorizeRaisedPlacement() {
        assertTrue(read(false, 0, 0, false).isEmpty());
    }

    @Test
    public void otherStaffCannotAuthorizeRaisedPlacement() {
        assertTrue(read(true, 1, 0, false).isEmpty());
    }

    @Test
    public void otherMeasureCannotAuthorizeRaisedPlacement() {
        assertTrue(read(true, 0, 1, false).isEmpty());
    }
}
