// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic tempo patterns, with no score or library fixtures. */
public class ScoreTempoConversionTest {
    @Test
    public void eighthPulseRetainsItsClockAndPrintedUnit() {
        var printed = List.of(new ScoreTempoChange(0, 0, 77.5, .5));
        var converted = ScoreTempoSelection.forEngraving(155, 155, 1, printed);
        assertEquals(printed, converted);
        var clock =
                ScorePerformanceTimeline.numeric(77.5, new ScoreMeterMap(4, List.of()), converted);
        assertEquals(
                3097,
                Math.round(clock.secondsAtBeat(4, ScorePerformanceTimeline.Boundary.AFTER) * 1000));
    }

    @Test
    public void selectedTempoScalesOpeningBarAndLaterMarksWithoutDroppingThem() {
        var printed =
                List.of(
                        new ScoreTempoChange(3, 0, 45, .5),
                        new ScoreTempoChange(0, .5f, 60, 1.5),
                        new ScoreTempoChange(0, 0, 90, .5));
        var converted = ScoreTempoSelection.forEngraving(240, 240, 1, printed);
        assertEquals(
                List.of(
                        new ScoreTempoChange(0, 0, 120, .5),
                        new ScoreTempoChange(0, .5f, 80, 1.5),
                        new ScoreTempoChange(3, 0, 60, .5)),
                converted);
    }

    @Test
    public void absentOpeningUsesMeterPulseAndKeepsLaterAbsoluteTempo() {
        var later = new ScoreTempoChange(0, .75f, 93, 1);
        assertEquals(
                List.of(new ScoreTempoChange(0, 0, 180, 1.5), later),
                ScoreTempoSelection.forEngraving(120, 180, 1.5, List.of(later)));
    }

    @Test
    public void noSelectionPreservesPrintedTempoExactly() {
        var printed = List.of(new ScoreTempoChange(0, 0, 62.75, 2));
        assertEquals(printed, ScoreTempoSelection.forEngraving(0, 0, 1, printed));
    }
}
