// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.io.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic page geometry, staff ownership and printed direction tokens. */
public class ScoreDynamicContinuationTest {
    private ScoreDynamicChange ramp(int staff, int count, int direction) {
        return new ScoreDynamicChange(0, .2f, staff, count, 0, 1, 0, direction);
    }

    private ScoreDynamicChange level(int measure, int staff, int count, float db) {
        return new ScoreDynamicChange(measure, 0, staff, count, measure, 0, db, 0);
    }

    private ScorePageInterpretation page(
            int first, int bars, int count, List<ScoreDynamicChange> changes, boolean words) {
        var measures = new ArrayList<MeasureRegion>();
        var notes = new ArrayList<ScoreNoteEvent>();
        for (int m = 0; m < bars; m++) {
            measures.add(new MeasureRegion(.1f, .9f, .3f, .52f));
            for (int s = 0; s < count; s++)
                notes.add(new ScoreNoteEvent(m, .2f, 0, s, count, .4f, false, 1, 0, 2, 2));
        }
        var events =
                changes.stream()
                        .filter(c -> words && c.direction() != 0)
                        .map(
                                c ->
                                        ScoreDynamicContinuation.evidence(
                                                c, c.direction() > 0 ? "cresc." : "dim.", .26f))
                        .toList();
        return new ScorePageInterpretation(
                measures, notes, first, List.of(), List.of(), List.of(), List.of(), List.of(),
                changes, List.of(), events);
    }

    private List<ScoreDynamicChange> join(ScorePageInterpretation... pages) {
        return ScoreDynamicContinuation.join(List.of(pages));
    }

    @Test
    public void lexicalCrescendoReachesNextPagePrintedArrival() {
        var result =
                join(
                        page(1, 1, 1, List.of(ramp(0, 1, 1)), true),
                        page(2, 2, 1, List.of(level(1, 0, 1, 6)), false));
        assertEquals(2, result.get(0).endMeasureIndex());
        assertEquals(0, result.get(0).endPosition(), 0);
    }

    @Test
    public void lexicalDiminuendoCrossesMoreThanOnePage() {
        var result =
                join(
                        page(20, 1, 1, List.of(ramp(0, 1, -1)), true),
                        page(21, 2, 1, List.of(), false),
                        page(23, 1, 1, List.of(level(0, 0, 1, -12)), false));
        assertEquals(3, result.get(0).endMeasureIndex());
    }

    @Test
    public void graphicPageEdgeDoesNotAuthorizeContinuation() {
        var change = ramp(0, 1, 1);
        assertEquals(
                change,
                join(
                                page(1, 1, 1, List.of(change), false),
                                page(2, 2, 1, List.of(level(1, 0, 1, 6)), false))
                        .get(0));
    }

    @Test
    public void newDirectionStopsEarlierLexicalInstruction() {
        var result =
                join(
                        page(1, 1, 1, List.of(ramp(0, 1, 1)), true),
                        page(2, 2, 1, List.of(ramp(0, 1, -1), level(1, 0, 1, -8)), false));
        assertEquals(1, result.get(0).endMeasureIndex());
        assertEquals(.2f, result.get(0).endPosition(), 0);
    }

    @Test
    public void movementRestartDoesNotSupplyDestination() {
        var change = ramp(0, 1, 1);
        assertEquals(
                change,
                join(
                                page(30, 1, 1, List.of(change), true),
                                page(1, 2, 1, List.of(level(1, 0, 1, 6)), false))
                        .get(0));
    }

    @Test
    public void changedTopologyCannotBorrowAnArrival() {
        var change = ramp(0, 1, 1);
        assertEquals(
                change,
                join(
                                page(1, 1, 1, List.of(change), true),
                                page(2, 2, 2, List.of(level(1, 0, 2, 6)), false))
                        .get(0));
    }

    @Test
    public void ensembleLanesHaveIndependentArrivals() {
        var result =
                join(
                        page(1, 1, 3, List.of(ramp(0, 3, 1), ramp(1, 3, 1), ramp(2, 3, 1)), true),
                        page(
                                2,
                                3,
                                3,
                                List.of(level(0, 0, 3, 6), level(1, 1, 3, 3), level(2, 2, 3, 3)),
                                false));
        assertEquals(1, result.get(0).endMeasureIndex());
        assertEquals(2, result.get(1).endMeasureIndex());
        assertEquals(3, result.get(2).endMeasureIndex());
    }

    @Test
    public void missingArrivalLeavesTheExistingFallback() {
        var change = ramp(0, 2, 1);
        assertEquals(
                change,
                join(
                                page(1, 1, 2, List.of(change), true),
                                page(2, 2, 2, List.of(level(1, 1, 2, 6)), false))
                        .get(0));
    }

    @Test
    public void fixedTargetsRemainUntouched() {
        var change = new ScoreDynamicChange(0, .2f, 0, 1, 0, 1, 3, 1, false, true);
        assertEquals(
                change,
                join(
                                page(1, 1, 1, List.of(change), true),
                                page(2, 2, 1, List.of(level(1, 0, 1, 6)), false))
                        .get(0));
    }

    @Test
    public void explicitWithinPageSpanRemainsUntouched() {
        var change = new ScoreDynamicChange(0, .2f, 0, 1, 0, .8f, 0, 1);
        assertEquals(
                change,
                join(
                                page(1, 1, 1, List.of(change), true),
                                page(2, 2, 1, List.of(level(1, 0, 1, 6)), false))
                        .get(0));
    }

    @Test
    public void discontinuousOffsetsCannotInventABridge() {
        var change = ramp(0, 1, 1);
        var pages =
                List.of(
                        page(1, 1, 1, List.of(change), true),
                        page(2, 2, 1, List.of(level(1, 0, 1, 6)), false));
        assertEquals(
                change.offset(5), ScoreDynamicContinuation.join(pages, new int[] {5, 9}).get(0));
    }

    @Test
    public void interruptedLocalInstructionIsNotProlonged() {
        var change = ramp(0, 1, 1);
        var later = new ScoreDynamicChange(0, .7f, 0, 1, 0, .7f, 0, 0);
        assertEquals(
                change,
                join(
                                page(1, 1, 1, List.of(change, later), true),
                                page(2, 2, 1, List.of(level(1, 0, 1, 6)), false))
                        .get(0));
    }

    @Test
    public void blankPageDoesNotProveContinuation() {
        var change = ramp(0, 1, 1);
        assertEquals(
                change,
                join(
                                page(1, 1, 1, List.of(change), true),
                                page(0, 0, 1, List.of(), false),
                                page(2, 2, 1, List.of(level(1, 0, 1, 6)), false))
                        .get(0));
    }

    @Test
    public void sharedKeyboardWordReachesSharedArrival() {
        var change = new ScoreDynamicChange(0, .2f, 0, 2, 0, 1, 0, 1, true);
        var target = new ScoreDynamicChange(1, 0, 0, 2, 1, 0, 3, 0, true);
        assertEquals(
                2,
                join(page(1, 1, 2, List.of(change), true), page(2, 2, 2, List.of(target), false))
                        .get(0)
                        .endMeasureIndex());
    }

    @Test
    public void privateHandLevelCannotResolveSharedKeyboardWord() {
        var change = new ScoreDynamicChange(0, .2f, 0, 2, 0, 1, 0, 1, true);
        var target = new ScoreDynamicChange(1, 0, 0, 2, 1, 0, 3, 0, true);
        assertEquals(
                change,
                join(
                                page(1, 1, 2, List.of(change), true),
                                page(2, 2, 2, List.of(level(0, 1, 2, -8), target), false))
                        .get(0));
    }

    @Test
    public void openWordEvidenceRoundTripsExistingSemanticFrame() throws Exception {
        var event = ScoreDynamicContinuation.evidence(ramp(0, 1, 1), "cresc.", .26f);
        var bytes = new ByteArrayOutputStream();
        ScoreSemanticWire.writeExpressions(new DataOutputStream(bytes), List.of(event), 1);
        assertEquals(
                List.of(event),
                ScoreSemanticWire.readExpressions(
                        new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), 1));
        assertTrue(event.start().isEmpty());
        assertTrue(event.end().isEmpty());
        assertNotEquals(
                ScoreDynamicContinuation.offsetEvidence(event, 0, 0).eventId(),
                ScoreDynamicContinuation.offsetEvidence(event, 1, 1).eventId());
        assertEquals(
                1,
                ScoreDynamicContinuation.offsetEvidence(event, 1, 1).evidence().get(0).pageIndex());
    }

    @Test
    public void detectorRetainsOnlyOpenLexicalWords() {
        var note = new ScoreNoteEvent(0, .2f, 0, 0, 1, .4f, false, 1, 0, 2, 2);
        var detection =
                ScoreDynamicsDetector.detectWithEvidence(
                        List.of(new PlayingTechniqueDetector.Word("cresc.", .26f, .56f, .4f, .62f)),
                        List.of(new PlayingTechniqueDetector.Staff(80, 120, 10, 0, 1)),
                        List.of(new MeasureRegion(.1f, .9f, .3f, .52f)),
                        List.of(note),
                        null,
                        400,
                        240);
        assertEquals(1, detection.events().size());
        assertEquals(ScoreExpressiveEvent.Kind.CRESCENDO, detection.events().get(0).kind());
        var recognized =
                new ScorePageInterpretation(
                        List.of(new MeasureRegion(.1f, .9f, .3f, .52f)),
                        List.of(note),
                        1,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        detection.changes(),
                        List.of(),
                        detection.events());
        assertEquals(
                2,
                join(recognized, page(2, 2, 1, List.of(level(1, 0, 1, 6)), false))
                        .get(0)
                        .endMeasureIndex());
    }
}
