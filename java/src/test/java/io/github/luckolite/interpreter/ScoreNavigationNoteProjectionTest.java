// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScorePlaybackDirection.Kind.*;
import static io.github.luckolite.interpreter.ScoreNavigationNoteProjection.*;

public class ScoreNavigationNoteProjectionTest {
    private final ScoreMeterMap meter = new ScoreMeterMap(4, List.of());

    private static ScorePlaybackDirection mark(
            int measure, ScorePlaybackDirection.Kind kind, String id, String target, double beat) {
        return new ScorePlaybackDirection(
                measure,
                kind,
                new ScorePlaybackDirection.Details(
                        beat,
                        id,
                        target,
                        "",
                        "",
                        2,
                        List.of(),
                        Optional.empty(),
                        ScorePlaybackDirection.AfterJumpRepeats.DEFAULT,
                        "",
                        List.of()));
    }

    @Test
    public void partialFineKeepsAttacksAndClipsLastNoteWithoutPageRescaling() {
        var plan =
                ScoreNavigationPlan.create(
                        3,
                        List.of(
                                new ScorePlaybackDirection(2, DA_CAPO_AL_FINE),
                                mark(0, FINE, "fine", "", 1.5)));
        var source =
                List.of(
                        new SourceNote("a", 0, 0, 1),
                        new SourceNote("b", 1, 1, 2),
                        new SourceNote("c", 2, 2, 3),
                        new SourceNote("d", 3, 3, 4),
                        new SourceNote("e", 4, 4, 8),
                        new SourceNote("f", 5, 8, 12));
        var result = project(source, plan, meter, EntryPolicy.REJECT);
        assertEquals(9.5, result.performedBeats(), 0);
        assertArrayEquals(
                new double[] {0, 1, 2, 3, 4, 8, 9},
                result.notes().stream().mapToDouble(PerformedNote::startBeat).toArray(),
                0);
        assertArrayEquals(
                new double[] {1, 2, 3, 4, 8, 9, 9.5},
                result.notes().stream().mapToDouble(PerformedNote::endBeat).toArray(),
                0);
        assertTrue(result.notes().get(6).clippedRelease());
        assertEquals(
                7, result.notes().stream().map(PerformedNote::performanceId).distinct().count());
    }

    @Test
    public void barlineDoesNotReattackAndHoldTargetsMapAcrossOwnedOccurrences() {
        var plan =
                ScoreNavigationPlan.create(
                        2,
                        List.of(
                                new ScorePlaybackDirection(0, REPEAT_START),
                                new ScorePlaybackDirection(2, REPEAT_END)));
        var result =
                project(List.of(new SourceNote("tie", 0, 0, 8)), plan, meter, EntryPolicy.REJECT);
        assertEquals(2, result.notes().size());
        assertEquals(8, result.notes().get(0).endBeat(), 0);
        assertEquals(8, result.notes().get(1).startBeat(), 0);
        var mapper = result.targetMapper();
        var route = plan.traversal().occurrences();
        assertEquals(mapper.map("tie", route.get(0)), mapper.map("tie", route.get(1)));
        assertNotEquals(mapper.map("tie", route.get(1)), mapper.map("tie", route.get(2)));
        assertFalse(mapper.map("missing", route.get(0)).isPresent());
    }

    @Test
    public void jumpIntoSustainRequiresExplicitEntryPolicy() {
        var plan =
                ScoreNavigationPlan.create(
                        2, List.of(mark(0, SEGNO, "s", "", 2), mark(1, DAL_SEGNO, "ds", "s", 0)));
        var source = List.of(new SourceNote("held", 0, 0, 4));
        assertThrows(
                IllegalArgumentException.class,
                () -> project(source, plan, meter, EntryPolicy.REJECT));
        assertEquals(1, project(source, plan, meter, EntryPolicy.OMIT).notes().size());
        var result = project(source, plan, meter, EntryPolicy.REATTACK);
        assertEquals(2, result.notes().size());
        assertTrue(result.notes().get(1).clippedEntry());
        assertEquals(4, result.notes().get(1).startBeat(), 0);
        assertEquals(6, result.notes().get(1).endBeat(), 0);
    }

    @Test
    public void duplicateIdentityAndMismatchedMeterReject() {
        var plan = ScoreNavigationPlan.create(1, List.of());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        project(
                                List.of(new SourceNote("a", 0, 0, 1), new SourceNote("a", 1, 1, 2)),
                                plan,
                                meter,
                                EntryPolicy.REJECT));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        project(
                                List.of(),
                                plan,
                                new ScoreMeterMap(3, List.of()),
                                EntryPolicy.REJECT));
    }

    @Test
    public void projectedHoldsReferToActualRepeatedSoundingNotes() {
        var plan =
                ScoreNavigationPlan.create(
                        2,
                        List.of(
                                new ScorePlaybackDirection(0, REPEAT_START),
                                new ScorePlaybackDirection(2, REPEAT_END)));
        var notes =
                project(List.of(new SourceNote("tie", 0, 0, 8)), plan, meter, EntryPolicy.REJECT);
        var source =
                new ScorePerformanceTimeline(
                        120,
                        List.of(),
                        List.of(new ScorePerformanceTimeline.Hold("fermata", 8, 1, Set.of("tie"))));
        var clock =
                ScoreNavigationPerformance.project(
                        source,
                        plan,
                        meter,
                        Map.of("fermata", ScorePerformanceTimeline.Boundary.BEFORE),
                        notes.targetMapper());
        assertEquals(10, clock.durationSeconds(), 0);
        assertEquals(
                Set.of(notes.notes().get(0).performanceId()),
                clock.timeline().holds().get(0).sustainedTargets());
        assertEquals(
                Set.of(notes.notes().get(1).performanceId()),
                clock.timeline().holds().get(1).sustainedTargets());
    }

    @Test
    public void variableMeterAndDestinationPitchReferencesStayInSourceCoordinates() {
        var variable = new ScoreMeterMap(3, List.of(new ScoreMeterChange(1, 5, 8)));
        var plan =
                ScoreNavigationPlan.create(
                        2,
                        List.of(
                                new ScorePlaybackDirection(0, REPEAT_START),
                                new ScorePlaybackDirection(2, REPEAT_END)),
                        variable);
        var notes =
                project(
                        List.of(
                                new SourceNote("sharp", 0, 0, 3),
                                new SourceNote("natural", 1, 3, 5.5)),
                        plan,
                        variable,
                        EntryPolicy.REJECT);
        assertArrayEquals(
                new double[] {0, 3, 5.5, 8.5},
                notes.notes().stream().mapToDouble(PerformedNote::startBeat).toArray(),
                0);
        assertEquals(
                List.of(0, 1, 0, 1),
                notes.notes().stream().map(PerformedNote::sourceIndex).toList());
        assertEquals(11, notes.performedBeats(), 0);
    }

    @Test
    public void codaSkipsNotesAndDoesNotCarrySoundAcrossJump() {
        var plan =
                ScoreNavigationPlan.create(
                        5,
                        List.of(
                                new ScorePlaybackDirection(3, DA_CAPO_AL_CODA),
                                new ScorePlaybackDirection(1, TO_CODA),
                                new ScorePlaybackDirection(4, CODA)));
        var notes =
                project(
                        List.of(
                                new SourceNote("long", 0, 0, 8),
                                new SourceNote("skipped", 1, 12, 16),
                                new SourceNote("coda", 2, 16, 20)),
                        plan,
                        meter,
                        EntryPolicy.REJECT);
        assertEquals(
                List.of("long", "long", "coda"),
                notes.notes().stream().map(PerformedNote::sourceId).toList());
        assertEquals(16, notes.notes().get(1).endBeat(), 0);
        assertTrue(notes.notes().get(1).clippedRelease());
        assertEquals(16, notes.notes().get(2).startBeat(), 0);
    }
}
