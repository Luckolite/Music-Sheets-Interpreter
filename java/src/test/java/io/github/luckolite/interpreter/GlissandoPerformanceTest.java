// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class GlissandoPerformanceTest {
    @Test
    public void whiteKeyStepsStayInsideStartingNoteAndExcludeTargetAttack() {
        var steps = GlissandoPerformance.whiteKeys(84, 60, .375);
        assertEquals(
                List.of(84, 83, 81, 79, 77, 76, 74, 72, 71, 69, 67, 65, 64, 62),
                steps.stream().map(GlissandoPerformance.Step::midi).toList());
        assertEquals(.25, steps.get(0).duration(), 1e-12);
        double end = 0;
        for (var step : steps) {
            assertEquals(end, step.offset(), 1e-12);
            assertTrue(step.duration() > 0);
            end = step.offset() + step.duration();
        }
        assertEquals(.375, end, 1e-12);
    }

    @Test
    public void ascendingStepsKeepAccidentalEndpointAndSkipIntermediateBlackKeys() {
        var steps = GlissandoPerformance.whiteKeys(61, 69, 1);
        assertEquals(
                List.of(61, 62, 64, 65, 67),
                steps.stream().map(GlissandoPerformance.Step::midi).toList());
    }

    @Test
    public void AdjacentAndUnisonEndpointsHaveNoInventedIntermediateAttack() {
        assertEquals(
                List.of(new GlissandoPerformance.Step(0, 1, 60)),
                GlissandoPerformance.whiteKeys(60, 62, 1));
        assertEquals(
                List.of(new GlissandoPerformance.Step(0, 1, 60)),
                GlissandoPerformance.whiteKeys(60, 60, 1));
    }

    @Test
    public void targetRequiresUniqueNextAttackAndCannotCrossWrittenSilence() {
        var source =
                new ScoreNoteEvent(0, .1f, 10, 0, 1, .3f, false, 0, 2)
                        .withArticulations(NoteOrnament.GLISSANDO);
        var target = new ScoreNoteEvent(0, .7f, 0, 0, 1, .4f, false, 0, 2);
        assertSame(target, GlissPitchTarget.next(source, List.of(source, target)));
        var chord = new ScoreNoteEvent(0, .7f, 2, 0, 1, .4f, false, 0, 2);
        assertNull(GlissPitchTarget.next(source, List.of(source, target, chord)));
        assertNull(GlissPitchTarget.next(source, List.of(source, target.withLeadingRest(.25f))));
    }
}
