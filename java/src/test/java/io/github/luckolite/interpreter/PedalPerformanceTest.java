// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PedalPerformanceTest {
    private static final ScoreMeterMap METER = new ScoreMeterMap(4, List.of());

    private static ScoreExpressiveEvent event(
            String id, boolean down, int measure, double beat, ScoreAnchor end) {
        return new ScoreExpressiveEvent(
                id,
                down ? ScoreExpressiveEvent.Kind.PEDAL_DOWN : ScoreExpressiveEvent.Kind.PEDAL_UP,
                Optional.of(new ScoreAnchor(measure, beat)),
                Optional.ofNullable(end),
                ScoreExpressiveEvent.Scope.PART,
                1,
                2,
                Optional.empty(),
                ScoreExpressiveEvent.Strength.UNSPECIFIED,
                "",
                List.of(new ScoreExpressiveEvent.Evidence("synthetic-pedal", 0, .5f, 1, 2, "")));
    }

    private static PedalPerformance.Span span(double start, double end) {
        return new PedalPerformance.Span("pedal", start, end, 1, 2);
    }

    @Test
    public void resolvedPairKeepsFiniteReleaseAndDiagnosticsEmpty() {
        var result =
                PedalPerformance.resolve(
                        List.of(
                                event("down", true, 0, 2, new ScoreAnchor(1, 0)),
                                event("up", false, 1, 0, null)),
                        METER,
                        2);
        assertTrue(result.diagnostics().isEmpty());
        assertEquals(List.of(new PedalPerformance.Span("down", 2, 4, 1, 2)), result.spans());
    }

    @Test
    public void missingOrConflictingReleaseCannotSustainForever() {
        assertTrue(
                PedalPerformance.resolve(List.of(event("down", true, 0, 2, null)), METER, 2)
                        .spans()
                        .isEmpty());
        var result =
                PedalPerformance.resolve(
                        List.of(
                                event("down", true, 0, 2, new ScoreAnchor(1, 0)),
                                event("up", false, 1, 1, null)),
                        METER,
                        2);
        assertTrue(result.spans().isEmpty());
        assertEquals(1, result.diagnostics().size());
    }

    @Test
    public void sharedBoundaryProducesReleaseBeforeRedepress() {
        var controls = PedalPerformance.controls(List.of(span(2, 4), span(4, 8)));
        assertEquals(
                List.of(127, 0, 127, 0),
                controls.stream().map(PedalPerformance.Control::value).toList());
        assertEquals(4, controls.get(1).beat(), 0);
        assertEquals(4, controls.get(2).beat(), 0);
        var events =
                List.of(
                        event("second-down", true, 1, 0, new ScoreAnchor(2, 0)),
                        event("first-up", false, 1, 0, null),
                        event("first-down", true, 0, 2, new ScoreAnchor(1, 0)),
                        event("second-up", false, 2, 0, null));
        assertEquals(2, PedalPerformance.resolve(events, METER, 2).spans().size());
    }

    @Test
    public void releaseGatesDoNotChangeTheWrittenClockOrOtherPart() {
        var spans = List.of(span(2, 4));
        assertEquals(4, PedalPerformance.releaseBeat(3, 2, spans), 0);
        assertEquals(1, PedalPerformance.releaseBeat(1, 2, spans), 0);
        assertEquals(2, PedalPerformance.releaseBeat(2, 2, spans), 0);
        assertEquals(5, PedalPerformance.releaseBeat(5, 2, spans), 0);
        assertEquals(3, PedalPerformance.releaseBeat(3, 1, spans), 0);
        assertEquals(
                4,
                PedalPerformance.releaseBeat(4, 2, List.of(span(2, 4), span(4, 8))),
                0); // endpoint belongs to prior release
    }

    @Test
    public void contiguousBarsDoNotReleaseAndRepressOneRail() {
        var plan = ScoreNavigationPlan.create(3, List.of(), METER);
        var result = PedalPerformance.project(List.of(span(2, 10)), plan, METER);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).startBeat(), 0);
        assertEquals(10, result.get(0).endBeat(), 0);
    }

    @Test
    public void repeatedRegionHasFiniteIndependentPedalOccurrences() {
        var plan =
                ScoreNavigationPlan.create(
                        2,
                        List.of(
                                new ScorePlaybackDirection(
                                        0, ScorePlaybackDirection.Kind.REPEAT_START),
                                new ScorePlaybackDirection(
                                        2, ScorePlaybackDirection.Kind.REPEAT_END)),
                        METER);
        assertEquals(List.of(0, 1, 0, 1), plan.sourceMeasures());
        var result = PedalPerformance.project(List.of(span(2, 8)), plan, METER);
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).startBeat(), 0);
        assertEquals(8, result.get(0).endBeat(), 0);
        assertEquals(10, result.get(1).startBeat(), 0);
        assertEquals(16, result.get(1).endBeat(), 0);
    }

    @Test
    public void jumpInsideHeldRegionRestoresStateAndCutsOutgoingResonance() {
        var plan =
                ScoreNavigationPlan.create(
                        3,
                        List.of(
                                new ScorePlaybackDirection(
                                        1, ScorePlaybackDirection.Kind.REPEAT_START),
                                new ScorePlaybackDirection(
                                        2, ScorePlaybackDirection.Kind.REPEAT_END)),
                        METER);
        assertEquals(List.of(0, 1, 1, 2), plan.sourceMeasures());
        var result = PedalPerformance.project(List.of(span(2, 10)), plan, METER);
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).startBeat(), 0);
        assertEquals(8, result.get(0).endBeat(), 0);
        assertEquals(8, result.get(1).startBeat(), 0);
        assertEquals(14, result.get(1).endBeat(), 0);
    }

    @Test
    public void controlsAtTerminalExtentReleasePedal() {
        var result =
                PedalPerformance.project(
                        List.of(span(2, 8)),
                        ScoreNavigationPlan.create(2, List.of(), METER),
                        METER);
        assertEquals(8, PedalPerformance.controls(result).get(1).beat(), 0);
        try {
            PedalPerformance.project(
                    List.of(span(2, 9)), ScoreNavigationPlan.create(2, List.of(), METER), METER);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void conflictingDownsDoNotChooseTheLastInputAsAuthority() {
        var result =
                PedalPerformance.resolve(
                        List.of(
                                event("a", true, 0, 1, null),
                                event("b", true, 0, 2, null),
                                event("up", false, 1, 0, null)),
                        METER,
                        2);
        assertTrue(result.spans().isEmpty());
        assertFalse(result.diagnostics().isEmpty());
    }
}
