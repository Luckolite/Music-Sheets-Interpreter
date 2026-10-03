// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScorePedalDetectorTest {
    @Test
    public void flattenedNativeAudioRegionsDoNotLoseThePrintedHookClock() {
        var original = page();
        var flattened =
                new ScorePageInterpretation(
                                List.of(
                                        new MeasureRegion(0, 1, 0, 1),
                                        new MeasureRegion(0, 1, 0, 1)),
                                original.notes())
                        .withExpressiveEvents(original.expressiveEvents());
        var resolved = ScorePedalDetector.resolve(flattened, new ScoreMeterMap(4, List.of()));
        assertEquals(
                new ScoreAnchor(0, 2), resolved.expressiveEvents().get(0).start().orElseThrow());
        assertEquals(
                new ScoreAnchor(1, 0), resolved.expressiveEvents().get(1).start().orElseThrow());
    }

    private static final PlayingTechniqueDetector.Staff STAFF =
            new PlayingTechniqueDetector.Staff(160, 200, 10, 1, 2);
    private static final List<MeasureRegion> MEASURES =
            List.of(
                    new MeasureRegion(.1f, .5f, .25f, .45f),
                    new MeasureRegion(.5f, .9f, .25f, .45f));

    private static ScoreNoteEvent note(int measure, float position, int step) {
        return new ScoreNoteEvent(
                measure,
                position,
                step,
                1,
                2,
                (200 - step * 5f) / 500,
                false,
                0,
                0,
                99,
                1,
                1,
                0,
                0,
                0,
                false,
                0,
                false,
                0,
                0);
    }

    private static List<ScoreNoteEvent> notes(int offset) {
        return List.of(
                note(offset, .12f, 0),
                note(offset, .25f, 2),
                note(offset, .6f, 4),
                note(offset, .86f, 6),
                note(offset + 1, .1f, 0));
    }

    private static ScorePageInterpretation page() {
        var page = new ScorePageInterpretation(MEASURES, notes(0));
        return ScorePedalDetector.withBrackets(
                page,
                List.of(new PedalBracketDetector.Bracket(340, 499, 270, 0, 10, 1, 2)),
                List.of(STAFF),
                1000,
                500);
    }

    @Test
    public void unknownMeterLeavesBothHooksUnresolved() {
        var page = ScorePedalDetector.resolve(page(), null);
        assertEquals(2, page.expressiveEvents().size());
        for (var event : page.expressiveEvents()) {
            assertTrue(event.start().isEmpty());
            assertEquals(ScoreExpressiveEvent.Scope.UNRESOLVED, event.scope());
        }
    }

    @Test
    public void knownMeterResolvesButRetainsEvidenceAndNotes() {
        var original = page();
        var resolved = ScorePedalDetector.resolve(original, new ScoreMeterMap(4, List.of()));
        assertEquals(
                new ScoreAnchor(0, 2), resolved.expressiveEvents().get(0).start().orElseThrow());
        assertEquals(
                new ScoreAnchor(1, 0), resolved.expressiveEvents().get(1).start().orElseThrow());
        assertEquals(original.notes(), resolved.notes());
        assertEquals(
                original.expressiveEvents().get(0).eventId(),
                resolved.expressiveEvents().get(0).eventId());
        assertEquals(
                original.expressiveEvents().get(0).evidence(),
                resolved.expressiveEvents().get(0).evidence());
    }

    @Test
    public void offsetPageRangeDoesNotBindToOverlappingGeometryOnEarlierPage() {
        var original = page();
        var measures = new ArrayList<>(MEASURES);
        measures.addAll(MEASURES);
        var allNotes = new ArrayList<>(notes(0));
        allNotes.addAll(notes(2));
        var events =
                original.expressiveEvents().stream()
                        .map(e -> ScorePedalDetector.offsetEvidence(e, 2, 1))
                        .toList();
        var combined = new ScorePageInterpretation(measures, allNotes).withExpressiveEvents(events);
        var resolved = ScorePedalDetector.resolve(combined, new ScoreMeterMap(4, List.of()));
        assertEquals(
                new ScoreAnchor(2, 2), resolved.expressiveEvents().get(0).start().orElseThrow());
        assertEquals(
                new ScoreAnchor(3, 0), resolved.expressiveEvents().get(1).start().orElseThrow());
        assertEquals(1, resolved.expressiveEvents().get(0).evidence().get(0).pageIndex());
        assertTrue(resolved.expressiveEvents().get(0).eventId().startsWith("page:1/"));
    }

    @Test
    public void staleCachedAnchorsClearWhenMeterIsUnavailable() {
        var known = ScorePedalDetector.resolve(page(), new ScoreMeterMap(4, List.of()));
        var unknown = ScorePedalDetector.resolve(known, null);
        assertTrue(unknown.expressiveEvents().get(0).start().isEmpty());
        assertTrue(unknown.expressiveEvents().get(0).end().isEmpty());
    }

    @Test
    public void missingStaffWitnessDoesNotInventOwnership() {
        var page = page();
        var missing =
                new ScorePageInterpretation(MEASURES, List.of())
                        .withExpressiveEvents(page.expressiveEvents());
        for (var event :
                ScorePedalDetector.resolve(missing, new ScoreMeterMap(4, List.of()))
                        .expressiveEvents()) assertTrue(event.start().isEmpty());
    }

    @Test
    public void repeatedDetectionKeepsStableSourceIdentity() {
        var page = page();
        var second =
                ScorePedalDetector.withBrackets(
                        page,
                        List.of(new PedalBracketDetector.Bracket(340, 499, 270, 0, 10, 1, 2)),
                        List.of(STAFF),
                        1000,
                        500);
        assertEquals(page.expressiveEvents(), second.expressiveEvents());
    }

    @Test
    public void unrelatedPedalEvidenceIsPreserved() {
        var external =
                new ScoreExpressiveEvent(
                        "imported",
                        ScoreExpressiveEvent.Kind.PEDAL_DOWN,
                        Optional.of(new ScoreAnchor(0, 0)),
                        Optional.of(new ScoreAnchor(1, 0)),
                        ScoreExpressiveEvent.Scope.PART,
                        1,
                        2,
                        Optional.of("another-importer"),
                        ScoreExpressiveEvent.Strength.UNSPECIFIED,
                        "",
                        List.of(new ScoreExpressiveEvent.Evidence("external", 0, .4f, 1, 2, "")));
        var original = page();
        var events = new ArrayList<>(original.expressiveEvents());
        events.add(external);
        var resolved =
                ScorePedalDetector.resolve(
                        original.withExpressiveEvents(events), new ScoreMeterMap(4, List.of()));
        assertEquals(external, resolved.expressiveEvents().get(2));
    }

    @Test
    public void cachedSemanticWireRetainsDeferredHooksAndCanResolveLater() throws Exception {
        var original = page();
        var bytes = new ByteArrayOutputStream();
        ScoreSemanticWire.writeExpressions(
                new DataOutputStream(bytes), original.expressiveEvents(), 2);
        var events =
                ScoreSemanticWire.readExpressions(
                        new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), 2);
        assertEquals(original.expressiveEvents(), events);
        var resolved =
                ScorePedalDetector.resolve(
                        original.withExpressiveEvents(events), new ScoreMeterMap(4, List.of()));
        assertEquals(
                new ScoreAnchor(0, 2), resolved.expressiveEvents().get(0).start().orElseThrow());
        assertEquals(
                new ScoreAnchor(1, 0), resolved.expressiveEvents().get(1).start().orElseThrow());
    }
}
