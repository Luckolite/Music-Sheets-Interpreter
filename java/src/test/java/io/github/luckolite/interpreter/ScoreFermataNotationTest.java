// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Procedural ink and explicit written identities, without source scans or fonts. */
public class ScoreFermataNotationTest {
    private List<?> plan(ScorePageInterpretation score, List<Integer> available) throws Exception {
        try {
            var type = Class.forName("io.github.luckolite.interpreter.ScoreFermataNotation");
            return (List<?>)
                    type.getMethod("plan", ScorePageInterpretation.class, Collection.class)
                            .invoke(null, score, available);
        } catch (ClassNotFoundException absent) {
            return List.of();
        }
    }

    private Object field(Object symbol, String name) throws Exception {
        return symbol.getClass().getMethod(name).invoke(symbol);
    }

    private ScorePageInterpretation score() throws Exception {
        return new ScoreFermataDetectorTest().detected(true, true);
    }

    @Test
    public void noteAndChordEachKeepOneSymbol() throws Exception {
        var symbols = plan(score(), List.of(0, 1, 2, 3, 4, 5));
        assertEquals(2, symbols.size());
        assertEquals(1, field(symbols.get(0), "sourceIndex"));
        assertEquals(3, field(symbols.get(1), "sourceIndex"));
        assertEquals(new ScoreAnchor(1, 0), field(symbols.get(1), "release"));
    }

    @Test
    public void aFilteredPartCannotAttachToTheOtherStaff() throws Exception {
        var symbols = plan(score(), List.of(0, 1));
        assertEquals(1, symbols.size());
        assertEquals(1, field(symbols.get(0), "sourceIndex"));
    }

    @Test
    public void survivingChordMemberKeepsTheAttackSymbol() throws Exception {
        var symbols = plan(score(), List.of(0, 1, 4, 5));
        assertEquals(2, symbols.size());
        assertEquals(5, field(symbols.get(1), "sourceIndex"));
    }

    @Test
    public void invertedSymbolUsesTheBottomChordMember() throws Exception {
        var drawing = new ScoreFermataDetectorTest();
        var gray = drawing.paper();
        drawing.mark(gray, 362, 302, true, true, false);
        var score = drawing.detect(drawing.page(true, true), gray, drawing.staffs());
        var symbols = plan(score, List.of(0, 1, 2, 3, 4, 5));
        assertEquals(1, symbols.size());
        assertEquals(5, field(symbols.get(0), "sourceIndex"));
        assertEquals(true, field(symbols.get(0), "inverted"));
    }

    @Test
    public void unresolvedTimingDoesNotInventAnEngravedNoteTarget() throws Exception {
        assertTrue(
                plan(
                                new ScoreFermataDetectorTest().detected(false, true),
                                List.of(0, 1, 2, 3, 4, 5))
                        .isEmpty());
    }

    @Test
    public void independentlyAuthoredTargetIsNotReinterpreted() throws Exception {
        var page = score();
        var event =
                new ScoreExpressiveEvent(
                        "author",
                        ScoreExpressiveEvent.Kind.FERMATA,
                        Optional.of(new ScoreAnchor(0, 1.5)),
                        Optional.of(new ScoreAnchor(1, 0)),
                        ScoreExpressiveEvent.Scope.NOTE,
                        0,
                        2,
                        Optional.of("other-note"),
                        ScoreExpressiveEvent.Strength.UNSPECIFIED,
                        "fermata",
                        List.of(
                                new ScoreExpressiveEvent.Evidence(
                                        "author", 0, .5f, 0, 2, "fermata")));
        assertTrue(
                plan(page.withExpressiveEvents(List.of(event)), List.of(0, 1, 2, 3, 4, 5))
                        .isEmpty());
    }

    @Test
    public void omittedAttackDoesNotProduceAnOrphanSymbol() throws Exception {
        assertTrue(plan(score(), List.of(0, 2, 4)).isEmpty());
    }

    @Test
    public void tiedHoldStaysOnItsFinalWrittenMember() throws Exception {
        var notes =
                List.of(
                        new ScoreNoteEvent(0, .2f, 0, 0, 1, .3f, false, 1, 0, 2, 2),
                        new ScoreNoteEvent(1, .2f, 0, 0, 1, .3f, true, 1, 0, 2, 2));
        var event =
                new ScoreExpressiveEvent(
                        "printed-fermata:synthetic-tie",
                        ScoreExpressiveEvent.Kind.FERMATA,
                        Optional.empty(),
                        Optional.empty(),
                        ScoreExpressiveEvent.Scope.UNRESOLVED,
                        0,
                        1,
                        Optional.of("printed-attack:1:0:1:" + Float.floatToIntBits(.2f)),
                        ScoreExpressiveEvent.Strength.UNSPECIFIED,
                        "fermata",
                        List.of(
                                new ScoreExpressiveEvent.Evidence(
                                        "fermata-raw-ink", 0, .3f, 0, 1, "fermata")));
        var page =
                new ScorePageInterpretation(
                        Collections.nCopies(2, new MeasureRegion(.1f, .9f, .2f, .6f)),
                        notes,
                        1,
                        List.of(),
                        List.of(),
                        List.of(new ScoreMeterChange(0, 6, 8)),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(event));
        var symbols = plan(page, List.of(0, 1));
        assertEquals(1, symbols.size());
        assertEquals(1, field(symbols.get(0), "sourceIndex"));
        assertEquals(new ScoreAnchor(2, 0), field(symbols.get(0), "release"));
    }
}
