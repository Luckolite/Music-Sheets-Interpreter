// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScoreExpressiveEvent.*;

public final class MetricModulationTextTest {
    private static ScoreExpressiveEvent modulation(String text, int measure) {
        var pulses = MetricModulationText.parse(text).orElseThrow();
        return new ScoreExpressiveEvent(
                "metric:" + measure,
                Kind.METRIC_MODULATION,
                Optional.of(new ScoreAnchor(measure, 0)),
                Optional.empty(),
                Scope.SCORE,
                0,
                1,
                Optional.empty(),
                Strength.UNSPECIFIED,
                pulses.encode(),
                List.of(new Evidence("original-synthetic", 0, .25f, 0, 1, text)));
    }

    @Test
    public void dottedQuarterEqualsQuarterSlowsQuarterPulse() {
        var pulses = MetricModulationText.parse("dotted quarter = quarter").orElseThrow();
        assertEquals(2. / 3, pulses.ratio(), 0);
        assertEquals(pulses, MetricModulationText.decode(pulses.encode()).orElseThrow());
        assertEquals(pulses, MetricModulationText.parse("♩. = ♩").orElseThrow());
    }

    @Test
    public void quarterEqualsDottedQuarterSpeedsQuarterPulse() {
        assertEquals(
                1.5,
                MetricModulationText.parse("quarter = dotted quarter").orElseThrow().ratio(),
                0);
    }

    @Test
    public void rejectNumericTempoProseAndMalformedRelativePayload() {
        for (String s :
                List.of(
                        "quarter = 120",
                        "120 = quarter",
                        "half a note = quarter",
                        "quarter = quarter = eighth"))
            assertTrue(MetricModulationText.parse(s).isEmpty());
        for (String s :
                List.of(
                        "metric-pulse-v1:NaN:1",
                        "metric-pulse-v1:0:1",
                        "metric-pulse-v1:1:1.234",
                        "metric-pulse-v1:1")) assertTrue(MetricModulationText.decode(s).isEmpty());
    }

    @Test
    public void followsCurrentNumericTempoAndKeepsWrittenDurations() {
        var meter = new ScoreMeterMap(4, List.of());
        var result =
                ScoreExpressivePerformance.resolve(
                        120,
                        meter,
                        3,
                        List.of(new ScoreTempoChange(0, 0, 90)),
                        List.of(modulation("dotted quarter = quarter", 1)),
                        List.of(),
                        Map.of(),
                        ScoreExpressivePerformance.Policy.preview());
        assertEquals(4 * 60. / 90 + 8 * 60. / 60, result.timeline().activeSecondsAtBeat(12), 1e-9);
    }

    @Test
    public void changesMeterWithoutConfusingQuarterPulseUnits() {
        var meter = new ScoreMeterMap(4, List.of(new ScoreMeterChange(1, 6, 8)));
        var result =
                ScoreExpressivePerformance.resolve(
                        120,
                        meter,
                        3,
                        List.of(),
                        List.of(modulation("quarter = dotted quarter", 1)),
                        List.of(),
                        Map.of(),
                        ScoreExpressivePerformance.Policy.preview());
        assertEquals(2 + 6 * 60. / 180, result.timeline().activeSecondsAtBeat(10), 1e-9);
    }

    @Test
    public void extremeModulationIsExplicitlyRejectedRatherThanClamped() {
        var result =
                ScoreExpressivePerformance.resolve(
                        15,
                        new ScoreMeterMap(4, List.of()),
                        3,
                        List.of(),
                        List.of(modulation("whole = 32nd", 1)),
                        List.of(),
                        Map.of(),
                        ScoreExpressivePerformance.Policy.preview());
        assertEquals(48, result.timeline().activeSecondsAtBeat(12), 1e-9);
        assertEquals(1, result.diagnostics().size());
    }
}
