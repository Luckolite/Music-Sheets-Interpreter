// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScoreExpressiveEvent.*;

public class ScoreExpressiveEventTest {
    private final Evidence evidence =
            new Evidence("synthetic:direction", 0, .8f, 0, 2, "rall. molto");

    private ScoreExpressiveEvent event(
            Optional<ScoreAnchor> start,
            Optional<ScoreAnchor> end,
            Kind kind,
            Scope scope,
            Optional<String> target) {
        return new ScoreExpressiveEvent(
                "synthetic:event",
                kind,
                start,
                end,
                scope,
                0,
                2,
                target,
                Strength.MOLTO,
                "molto",
                List.of(evidence));
    }

    private void rejects(Runnable work) {
        try {
            work.run();
            fail("Expected rejection");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void barEndCanonicalizesAcrossChangedMeterAndTerminalEnd() {
        var meter = new ScoreMeterMap(4, List.of(new ScoreMeterChange(1, 6, 8)));
        assertEquals(new ScoreAnchor(1, 0), new ScoreAnchor(0, 4).canonical(meter, 2));
        assertEquals(new ScoreAnchor(2, 0), new ScoreAnchor(1, 3).canonical(meter, 2));
        assertEquals(new ScoreAnchor(2, 0), new ScoreAnchor(2, 0).canonical(meter, 2));
        assertEquals(5, new ScoreAnchor(1, 1).absoluteBeat(meter), 0);
    }

    @Test
    public void canonicalAnchorsRejectUnknownOrOutOfBarCoordinates() {
        var meter = new ScoreMeterMap(4, List.of());
        rejects(() -> new ScoreAnchor(0, Double.NaN));
        rejects(() -> new ScoreAnchor(-1, 0));
        rejects(() -> new ScoreAnchor(0, 5).canonical(meter, 2));
        rejects(() -> new ScoreAnchor(2, 1).canonical(meter, 2));
        rejects(() -> new ScoreAnchor(3, 0).canonical(meter, 2));
    }

    @Test
    public void unresolvedEvidenceIsNotSilentlyConvertedToMusicalTime() {
        var unresolved =
                event(
                        Optional.empty(),
                        Optional.empty(),
                        Kind.RALLENTANDO,
                        Scope.UNRESOLVED,
                        Optional.empty());
        assertTrue(unresolved.start().isEmpty());
        assertEquals(.8f, unresolved.evidence().get(0).visualX(), 0);
        rejects(
                () ->
                        event(
                                Optional.empty(),
                                Optional.empty(),
                                Kind.RALLENTANDO,
                                Scope.SCORE,
                                Optional.empty()));
    }

    @Test
    public void resolvedRestFermataRequiresOwnedReleaseAndTarget() {
        var start = Optional.of(new ScoreAnchor(0, 0));
        var end = Optional.of(new ScoreAnchor(1, 0));
        var pause = event(start, end, Kind.FERMATA, Scope.REST, Optional.of("synthetic:rest"));
        assertEquals(Scope.REST, pause.scope());
        rejects(
                () ->
                        event(
                                start,
                                Optional.empty(),
                                Kind.FERMATA,
                                Scope.REST,
                                Optional.of("synthetic:rest")));
        rejects(() -> event(start, end, Kind.FERMATA, Scope.REST, Optional.empty()));
        rejects(() -> event(end, start, Kind.RALLENTANDO, Scope.SCORE, Optional.empty()));
    }

    @Test
    public void offsetsPreservePrintedOwnershipAndStableSourceIdentity() {
        var source =
                event(
                        Optional.of(new ScoreAnchor(0, 1)),
                        Optional.of(new ScoreAnchor(1, 0)),
                        Kind.RALLENTANDO,
                        Scope.SCORE,
                        Optional.empty());
        var shifted = source.offset(5);
        assertEquals(new ScoreAnchor(5, 1), shifted.start().get());
        assertEquals(new ScoreAnchor(6, 0), shifted.end().get());
        assertEquals(source.evidence(), shifted.evidence());
        assertEquals(source.eventId(), shifted.eventId());
    }

    @Test
    public void modelWithFunctionsKeepExpressiveEvidence() {
        var source =
                event(
                        Optional.empty(),
                        Optional.empty(),
                        Kind.RALLENTANDO,
                        Scope.UNRESOLVED,
                        Optional.empty());
        var page =
                new ScorePageInterpretation(List.of(), List.of())
                        .withExpressiveEvents(List.of(source));
        assertEquals(List.of(source), page.withPlaybackDirections(List.of()).expressiveEvents());
        assertEquals(
                List.of(), new ScorePageInterpretation(List.of(), List.of()).expressiveEvents());
        rejects(() -> page.withExpressiveEvents(List.of(source, source)));
    }
}
