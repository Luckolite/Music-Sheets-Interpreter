// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScoreExpressiveEvent.*;

public class ExpressiveCopyTest {
    private ScorePageInterpretation score(List<ScoreNoteEvent> notes) {
        var event =
                new ScoreExpressiveEvent(
                        "synthetic:expression",
                        Kind.RALLENTANDO,
                        Optional.of(new ScoreAnchor(0, 0)),
                        Optional.empty(),
                        Scope.SCORE,
                        0,
                        1,
                        Optional.empty(),
                        Strength.MOLTO,
                        "molto",
                        List.of(new Evidence("synthetic:source", 0, .3f, 0, 1, "molto rall.")));
        return new ScorePageInterpretation(List.of(new MeasureRegion(.1f, .9f, .1f, .9f)), notes)
                .withExpressiveEvents(List.of(event));
    }

    @Test
    public void numericTempoCopyKeepsSemanticEvidence() {
        var source = score(List.of());
        assertEquals(
                source.expressiveEvents(),
                TabTempo.apply(source, List.of(), List.of(), 1000, 1000).expressiveEvents());
    }

    @Test
    public void numericMeterCopyKeepsSemanticEvidence() {
        var source = score(List.of());
        assertEquals(
                source.expressiveEvents(),
                TabMeter.apply(source, List.of(), List.of(), 1000, 1000).expressiveEvents());
    }

    @Test
    public void pairedTabCopyKeepsSemanticEvidence() {
        var source = score(List.of(new ScoreNoteEvent(0, .2f, 0, 0, 1)));
        var tab = new TablatureDecoder.Staff(100, 10, 30, List.of(), List.of());
        assertEquals(
                source.expressiveEvents(),
                TablatureDecoder.apply(source, List.of(tab), 1000, 1000).expressiveEvents());
    }

    @Test
    public void expressionBearingEmptyGeometryIsNotDiscardedAsSpurious() {
        var source = score(List.of());
        var tab = new TablatureDecoder.Staff(100, 10, -1, List.of(), List.of());
        var copied = TablatureDecoder.apply(source, List.of(tab), 1000, 1000);
        assertEquals(source.expressiveEvents(), copied.expressiveEvents());
        assertFalse(copied.measures().isEmpty());
    }

    @Test
    public void changedTieContextCopyKeepsSemanticEvidence() {
        var natural =
                new ScoreNoteEvent(
                                0,
                                .1f,
                                0,
                                0,
                                1,
                                .4f,
                                false,
                                0,
                                1,
                                ScoreNoteEvent.ACCIDENTAL_NATURAL,
                                0,
                                1,
                                0)
                        .withClef(ScoreNoteEvent.CLEF_TREBLE);
        var sharp =
                new ScoreNoteEvent(
                                0,
                                .3f,
                                0,
                                0,
                                1,
                                .4f,
                                true,
                                0,
                                1,
                                ScoreNoteEvent.ACCIDENTAL_SHARP,
                                0,
                                1,
                                0)
                        .withClef(ScoreNoteEvent.CLEF_TREBLE);
        var source = score(List.of(natural, sharp));
        var copied = ScoreTiePitchGuard.withInitialKeyContext(source, 0);
        assertFalse(copied.notes().get(1).tiedFromPrevious());
        assertEquals(source.expressiveEvents(), copied.expressiveEvents());
    }
}
