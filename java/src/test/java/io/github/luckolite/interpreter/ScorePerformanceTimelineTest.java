// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScorePerformanceTimeline.*;

/** Original synthetic beats, scopes and occurrence identities; no score-derived fixtures. */
public class ScorePerformanceTimelineTest {
    @Test
    public void fractionalMultipleHoldBoundariesAreExactlySharedByBothDirections() {
        var random = new Random(72341);
        for (int trial = 0; trial < 100; trial++) {
            var segments = new ArrayList<TempoSegment>();
            var holds = new ArrayList<Hold>();
            for (int i = 0; i < 12; i++) {
                segments.add(
                        new TempoSegment(
                                i * 4,
                                (i + 1) * 4,
                                40 + random.nextDouble() * 160,
                                40 + random.nextDouble() * 160));
                holds.add(new Hold("pause" + i, i * 4 + 2, .2 + random.nextDouble() * 2, Set.of()));
            }
            var clock = new ScorePerformanceTimeline(120, segments, holds);
            for (var hold : holds) {
                var before =
                        clock.positionAtSeconds(clock.secondsAtBeat(hold.beat(), Boundary.BEFORE));
                var after =
                        clock.positionAtSeconds(clock.secondsAtBeat(hold.beat(), Boundary.AFTER));
                assertEquals(Optional.of(hold.occurrenceId()), before.holdId());
                assertEquals(0, before.holdProgress(), 0);
                assertEquals(hold.beat(), before.beat(), 0);
                assertTrue(after.holdId().isEmpty());
                assertEquals(hold.beat(), after.beat(), 0);
            }
        }
    }

    private ScorePerformanceTimeline ramp(double endBpm, List<Hold> holds) {
        return new ScorePerformanceTimeline(
                120, List.of(new TempoSegment(0, 4, 120, endBpm)), holds);
    }

    @Test
    public void linearBpmRampIntegratesReciprocalTempo() {
        assertEquals(4 * Math.log(2), ramp(60, List.of()).secondsAtBeat(4, Boundary.AFTER), 1e-12);
        assertEquals(2 * Math.log(2), ramp(240, List.of()).secondsAtBeat(4, Boundary.AFTER), 1e-12);
    }

    @Test
    public void rampInverseAndConstantTailRoundTrip() {
        for (double end : new double[] {60, 120, 120.000000001, 240}) {
            var clock = ramp(end, List.of());
            for (double beat = 0; beat < 12; beat += .125)
                assertEquals(
                        beat,
                        clock.positionAtSeconds(clock.secondsAtBeat(beat, Boundary.AFTER)).beat(),
                        1e-10);
        }
    }

    @Test
    public void beforeAfterAndFrozenInverseHaveExplicitBoundaries() {
        var clock = ramp(120, List.of(new Hold("pause", 4, 2, Set.of("held"))));
        assertEquals(2, clock.secondsAtBeat(4, Boundary.BEFORE), 0);
        assertEquals(4, clock.secondsAtBeat(4, Boundary.AFTER), 0);
        var start = clock.positionAtSeconds(2);
        assertEquals(Optional.of("pause"), start.holdId());
        assertEquals(0, start.holdProgress(), 0);
        var middle = clock.positionAtSeconds(3);
        assertEquals(4, middle.beat(), 0);
        assertEquals(.5, middle.holdProgress(), 0);
        assertTrue(clock.positionAtSeconds(4).holdId().isEmpty());
        assertEquals(4, clock.positionAtSeconds(4).beat(), 0);
    }

    @Test
    public void duplicateEnsembleDepictionsUnionTargetsNotDelay() {
        var clock =
                ramp(
                        120,
                        List.of(
                                new Hold("ensemble", 4, 2, Set.of("upper")),
                                new Hold("ensemble", 4, 2, Set.of("lower"))));
        assertEquals(1, clock.holds().size());
        assertEquals(Set.of("upper", "lower"), clock.holds().get(0).sustainedTargets());
        assertEquals(4, clock.secondsAtBeat(4, Boundary.AFTER), 0);
    }

    @Test
    public void twoPauseColumnsAndRepeatOccurrencesAreNotCollapsed() {
        var clock =
                ramp(
                        120,
                        List.of(
                                new Hold("rest:visit1", 2, 1, Set.of()),
                                new Hold("chord:visit1", 4, 2, Set.of("chord")),
                                new Hold("rest:visit2", 6, 1, Set.of())));
        assertEquals(8, clock.secondsAtBeat(8, Boundary.AFTER), 0);
        assertEquals(3, clock.holds().size());
        assertTrue(clock.holds().get(0).sustainedTargets().isEmpty());
    }

    @Test
    public void activeMusicalTimeFreezesDuringHoldWithoutChangingNoHoldTiming() {
        var clock = ramp(60, List.of(new Hold("pause", 2, 3, Set.of())));
        double start = clock.secondsAtBeat(2, Boundary.BEFORE);
        assertEquals(start, clock.activeSecondsAtSeconds(start + 1), 1e-12);
        assertEquals(start, clock.activeSecondsAtSeconds(start + 3), 1e-12);
        assertEquals(start + 1, clock.activeSecondsAtSeconds(start + 4), 1e-12);
        assertEquals(10, ramp(60, List.of()).activeSecondsAtSeconds(10), 0);
    }

    @Test
    public void terminalSilentHoldCountsInDuration() {
        var clock = ramp(120, List.of(new Hold("terminal", 4, 2, Set.of())));
        assertEquals(4, clock.secondsAtBeat(4, Boundary.AFTER), 0);
        assertEquals(2, clock.secondsAtBeat(4, Boundary.BEFORE), 0);
    }

    @Test
    public void numericCompatibilityIncludesOpeningChangesAndVariableMeters() {
        var meter =
                new ScoreMeterMap(
                        5, List.of(new ScoreMeterChange(1, 4, 4), new ScoreMeterChange(3, 12, 8)));
        var clock =
                numeric(
                        120,
                        meter,
                        List.of(new ScoreTempoChange(3, 0, 60), new ScoreTempoChange(4, .5f, 180)));
        assertEquals(6.5, clock.secondsAtBeat(meter.startBeat(3), Boundary.AFTER), 0);
        assertEquals(12.5, clock.secondsAtBeat(meter.startBeat(4), Boundary.AFTER), 0);
        assertEquals(
                1,
                numeric(120, meter, List.of(new ScoreTempoChange(0, 0, 60)))
                        .secondsAtBeat(1, Boundary.AFTER),
                0);
    }

    @Test
    public void numericDuplicatesUseLastAuthoritativeValueWithoutEmptySegments() {
        var clock =
                numeric(
                        120,
                        new ScoreMeterMap(4, List.of()),
                        List.of(new ScoreTempoChange(1, 0, 60), new ScoreTempoChange(1, 0, 180)));
        assertEquals(2 + 4.0 / 3, clock.secondsAtBeat(8, Boundary.AFTER), 1e-12);
    }

    @Test
    public void inputsAndSustainTargetsAreImmutable() {
        var targets = new HashSet<>(Set.of("note"));
        var holds = new ArrayList<Hold>();
        holds.add(new Hold("pause", 4, 2, targets));
        var clock = ramp(120, holds);
        targets.clear();
        holds.clear();
        assertEquals(Set.of("note"), clock.holds().get(0).sustainedTargets());
    }

    private void rejects(Runnable action) {
        try {
            action.run();
            fail("Expected invalid timeline rejection");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void malformedSegmentsRejectGapsOverlapsAndNonfiniteValues() {
        rejects(() -> new TempoSegment(0, 0, 120, 120));
        rejects(() -> new TempoSegment(0, 4, 0, 120));
        rejects(() -> new TempoSegment(0, Double.POSITIVE_INFINITY, 120, 120));
        rejects(
                () ->
                        new ScorePerformanceTimeline(
                                120, List.of(new TempoSegment(1, 4, 120, 120)), List.of()));
        rejects(
                () ->
                        new ScorePerformanceTimeline(
                                120,
                                List.of(
                                        new TempoSegment(0, 4, 120, 120),
                                        new TempoSegment(3, 5, 120, 120)),
                                List.of()));
    }

    @Test
    public void conflictingOrUnorderedSameBeatHoldsAreNotSilentlySummed() {
        rejects(
                () ->
                        ramp(
                                120,
                                List.of(
                                        new Hold("a", 4, 1, Set.of()),
                                        new Hold("a", 4, 2, Set.of()))));
        rejects(
                () ->
                        ramp(
                                120,
                                List.of(
                                        new Hold("a", 4, 1, Set.of()),
                                        new Hold("b", 4, 1, Set.of()))));
        rejects(() -> new Hold("bad", 4, Double.NaN, Set.of()));
        rejects(() -> ramp(120, List.of()).secondsAtBeat(Double.NaN, Boundary.AFTER));
    }
}
