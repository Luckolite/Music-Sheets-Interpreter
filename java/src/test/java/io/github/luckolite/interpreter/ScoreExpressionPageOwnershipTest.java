// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original logical anchors only; no commercial score data. */
public class ScoreExpressionPageOwnershipTest {
    private static ScoreExpressiveEvent event(
            String id,
            ScoreExpressiveEvent.Kind kind,
            Optional<ScoreAnchor> start,
            Optional<ScoreAnchor> end) {
        return new ScoreExpressiveEvent(
                id,
                kind,
                start,
                end,
                ScoreExpressiveEvent.Scope.UNRESOLVED,
                0,
                1,
                Optional.empty(),
                ScoreExpressiveEvent.Strength.UNSPECIFIED,
                "",
                List.of(
                        new ScoreExpressiveEvent.Evidence(
                                "original", 7, .4f, 0, 1, "printed mark")));
    }

    @Test
    public void crossPageSpanAndAnchorlessEvidenceSurviveOnce() {
        var ramp =
                event(
                        "ramp",
                        ScoreExpressiveEvent.Kind.RITARDANDO,
                        Optional.of(new ScoreAnchor(1, 1)),
                        Optional.of(new ScoreAnchor(4, 0)));
        var unknown =
                event(
                        "unknown",
                        ScoreExpressiveEvent.Kind.UNRESOLVED_DIRECTION,
                        Optional.empty(),
                        Optional.empty());
        var events = List.of(ramp, unknown);
        assertEquals(
                List.of(unknown), ScorePageInterpretation.expressionsStartingOnPage(events, 0, 1));
        assertEquals(
                List.of(ramp.offset(-1)),
                ScorePageInterpretation.expressionsStartingOnPage(events, 1, 3));
        assertTrue(ScorePageInterpretation.expressionsStartingOnPage(events, 3, 5, true).isEmpty());
    }

    @Test
    public void terminalReleaseAndOrdinaryPageBoundaryHaveDistinctOwnership() {
        var release =
                event(
                        "release",
                        ScoreExpressiveEvent.Kind.PEDAL_UP,
                        Optional.of(new ScoreAnchor(4, 0)),
                        Optional.empty());
        assertTrue(
                ScorePageInterpretation.expressionsStartingOnPage(List.of(release), 0, 4, false)
                        .isEmpty());
        assertEquals(
                List.of(release.offset(-2)),
                ScorePageInterpretation.expressionsStartingOnPage(List.of(release), 2, 4, true));
        var boundary = release.offset(-2);
        assertTrue(
                ScorePageInterpretation.expressionsStartingOnPage(List.of(boundary), 0, 2, false)
                        .isEmpty());
        assertEquals(
                List.of(boundary.offset(-2)),
                ScorePageInterpretation.expressionsStartingOnPage(List.of(boundary), 2, 4, true));
    }
}
