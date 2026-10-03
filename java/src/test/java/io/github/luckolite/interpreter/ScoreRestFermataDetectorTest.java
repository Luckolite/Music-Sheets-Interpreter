// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScoreExpressiveEvent.*;

/** Original dot/arch ink, decoded rests and explicit musical clocks. */
public final class ScoreRestFermataDetectorTest {
    private ScorePageInterpretation page(double duration, boolean meter) {
        return new ScorePageInterpretation(
                List.of(new MeasureRegion(.1f, .9f, .2f, .9f)),
                List.of(),
                1,
                List.of(),
                List.of(),
                meter ? List.of(new ScoreMeterChange(0, 4, 4)) : List.of(),
                List.of(new ScoreRestEvent(0, .5f, .36f, .04f, 0, 2, duration)),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private ScorePageInterpretation detect(ScorePageInterpretation page, boolean roof) {
        var drawing = new ScoreFermataDetectorTest();
        var gray = drawing.paper();
        drawing.mark(gray, 250, 106, false, roof, false);
        return ScoreFermataDetector.withFermatas(
                page, new byte[500 * 400], gray, 500, 400, drawing.staffs());
    }

    @Test
    public void completeSilentBarOwnsRestHoldWithoutInventingSound() {
        var original = page(4, true);
        var score = detect(original, true);
        assertEquals(1, score.expressiveEvents().size());
        var event = score.expressiveEvents().get(0);
        assertEquals(Scope.REST, event.scope());
        assertEquals(new ScoreAnchor(0, 0), event.start().orElseThrow());
        assertEquals(new ScoreAnchor(1, 0), event.end().orElseThrow());
        assertEquals(original.notes(), score.notes());
        assertEquals(original.rests(), score.rests());
        var performance =
                ScoreExpressivePerformance.resolve(
                        120,
                        new ScoreMeterMap(4, List.of()),
                        1,
                        List.of(),
                        score.expressiveEvents(),
                        List.of(),
                        Map.of(),
                        ScoreExpressivePerformance.Policy.preview());
        assertEquals(2, performance.timeline().holds().get(0).seconds(), 0);
        assertTrue(performance.timeline().holds().get(0).sustainedTargets().isEmpty());
    }

    @Test
    public void unaccountedSilentSlotCannotTurnOpticalPositionIntoBeat() {
        var event = detect(page(1, true), true).expressiveEvents().get(0);
        assertEquals(Scope.UNRESOLVED, event.scope());
        assertTrue(event.start().isEmpty());
        assertTrue(event.end().isEmpty());
    }

    @Test
    public void continuationRestWaitsForMeterAndRebasesItsIdentity() {
        var score = detect(page(4, false), true);
        assertEquals(Scope.UNRESOLVED, score.expressiveEvents().get(0).scope());
        score = ScoreFermataDetector.resolve(score, 4);
        assertEquals(Scope.REST, score.expressiveEvents().get(0).scope());
        var event = ScoreDynamicContinuation.offsetEvidence(score.expressiveEvents().get(0), 3, 2);
        assertTrue(event.targetEventId().orElseThrow().startsWith("printed-rest:3:"));
        assertEquals(new ScoreAnchor(3, 0), event.start().orElseThrow());
        assertEquals(2, event.evidence().get(0).pageIndex());
    }

    @Test
    public void bareDotIsNotARestFermata() {
        assertTrue(detect(page(4, true), false).expressiveEvents().isEmpty());
    }
}
