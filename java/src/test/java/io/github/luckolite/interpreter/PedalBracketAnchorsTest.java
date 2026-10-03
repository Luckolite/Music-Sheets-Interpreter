// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PedalBracketAnchorsTest {
    private static final int W = 1000, H = 500;
    private static final PlayingTechniqueDetector.Staff STAFF =
            new PlayingTechniqueDetector.Staff(160, 200, 10, 1, 2);

    private static ScoreNoteEvent note(int measure, float position, float duration, int step) {
        return new ScoreNoteEvent(
                measure,
                position,
                step,
                1,
                2,
                (200 - step * 5f) / H,
                false,
                0,
                0,
                99,
                duration,
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

    private static ScorePageInterpretation score(
            List<ScoreNoteEvent> notes, List<ScoreRestEvent> rests) {
        return new ScorePageInterpretation(
                List.of(
                        new MeasureRegion(.1f, .5f, .25f, .45f),
                        new MeasureRegion(.5f, .9f, .25f, .45f)),
                notes,
                1,
                List.of(),
                List.of(),
                List.of(),
                rests);
    }

    private static List<ScoreExpressiveEvent> bind(
            ScorePageInterpretation score, float left, float right, ScoreMeterMap meter) {
        return PedalBracketAnchors.bind(
                score,
                List.of(new PedalBracketDetector.Bracket(left, right, 270, 0, 10, 1, 2)),
                List.of(STAFF),
                W,
                H,
                meter);
    }

    private static List<ScoreNoteEvent> quarters() {
        return List.of(
                note(0, .12f, 1, 0),
                note(0, .25f, 1, 2),
                note(0, .6f, 1, 4),
                note(0, .86f, 1, 6),
                note(1, .1f, 4, 0));
    }

    @Test
    public void unevenSpacingUsesWrittenBeatAndRetainsNotes() {
        var score = score(quarters(), List.of());
        var before = score.notes();
        var events = bind(score, 340, 499, new ScoreMeterMap(4, List.of()));
        assertEquals(2, events.size());
        assertEquals(new ScoreAnchor(0, 2), events.get(0).start().orElseThrow());
        assertEquals(new ScoreAnchor(1, 0), events.get(0).end().orElseThrow());
        assertEquals(ScoreExpressiveEvent.Scope.PART, events.get(0).scope());
        assertSame(before, score.notes());
    }

    @Test
    public void independentSamePitchChordHeadsHaveOneAttack() {
        var notes = new ArrayList<>(quarters());
        notes.add(note(0, .6f, 1, 8));
        var events = bind(score(notes, List.of()), 340, 499, new ScoreMeterMap(4, List.of()));
        assertEquals(2, events.size());
        assertEquals(new ScoreAnchor(0, 2), events.get(0).start().orElseThrow());
    }

    @Test
    public void barlineHooksDoNotInventAnAttackBeforeLeadingRest() {
        var score =
                score(
                        List.of(note(0, .6f, 2, 0).withLeadingRest(2), note(1, .1f, 4, 0)),
                        List.of(new ScoreRestEvent(0, .2f, .37f, .02f, 1, 2, 2)));
        var events = bind(score, 101, 499, new ScoreMeterMap(4, List.of()));
        assertEquals(2, events.size());
        assertEquals(new ScoreAnchor(0, 0), events.get(0).start().orElseThrow());
    }

    @Test
    public void restHookUsesSilentSlotClock() {
        var before =
                new ScoreNoteEvent(
                        0, .12f, 0, 1, 2, .4f, false, 0, 0, 99, 1, 1, 1, 0, 0, false, 0, false, 0,
                        0);
        var score =
                score(
                        List.of(before, note(0, .6f, 2, 0), note(1, .1f, 4, 0)),
                        List.of(new ScoreRestEvent(0, .3f, .37f, .02f, 1, 2, 1)));
        var events = bind(score, 220, 499, new ScoreMeterMap(4, List.of()));
        assertEquals(2, events.size());
        assertEquals(new ScoreAnchor(0, 1), events.get(0).start().orElseThrow());
    }

    @Test
    public void absentHookAttackCannotBecomeAnInterpolatedBeat() {
        assertTrue(
                bind(score(quarters(), List.of()), 290, 499, new ScoreMeterMap(4, List.of()))
                        .isEmpty());
    }

    @Test
    public void nextSystemNotesCannotWitnessPriorStaffOwnership() {
        var notes =
                quarters().stream()
                        .map(
                                n ->
                                        new ScoreNoteEvent(
                                                n.measureIndex(),
                                                n.positionInMeasure(),
                                                n.staffStep(),
                                                1,
                                                2,
                                                n.pageY() + .3f,
                                                false,
                                                0,
                                                0,
                                                99,
                                                n.unbeamedDurationBeats(),
                                                1,
                                                0,
                                                0,
                                                0,
                                                false,
                                                0,
                                                false,
                                                0,
                                                0))
                        .toList();
        assertTrue(
                bind(score(notes, List.of()), 101, 499, new ScoreMeterMap(4, List.of())).isEmpty());
    }

    @Test
    public void sharedBoundaryRetainsBothReleaseAndRedepress() {
        var score = score(quarters(), List.of());
        var brackets =
                List.of(
                        new PedalBracketDetector.Bracket(340, 499, 270, 0, 10, 1, 2),
                        new PedalBracketDetector.Bracket(501, 899, 270, 0, 10, 1, 2));
        var events =
                PedalBracketAnchors.bind(
                        score, brackets, List.of(STAFF), W, H, new ScoreMeterMap(4, List.of()));
        assertEquals(4, events.size());
        assertEquals(events.get(1).start(), events.get(2).start());
        assertEquals(ScoreExpressiveEvent.Kind.PEDAL_UP, events.get(1).kind());
        assertEquals(ScoreExpressiveEvent.Kind.PEDAL_DOWN, events.get(2).kind());
    }

    @Test
    public void terminalBarlineUsesCanonicalScoreEnd() {
        var events = bind(score(quarters(), List.of()), 501, 899, new ScoreMeterMap(4, List.of()));
        assertEquals(2, events.size());
        assertEquals(new ScoreAnchor(2, 0), events.get(1).start().orElseThrow());
    }

    @Test
    public void shortenedOpeningUsesSuppliedProvedMeter() {
        var score = score(List.of(note(0, .15f, 1, 0), note(1, .1f, 4, 0)), List.of());
        var events =
                bind(
                        score,
                        101,
                        499,
                        new ScoreMeterMap(4, List.of()).withOpeningQuarterBeats(1, 2));
        assertEquals(2, events.size());
        assertEquals(
                1,
                events.get(1)
                        .start()
                        .orElseThrow()
                        .absoluteBeat(
                                new ScoreMeterMap(4, List.of()).withOpeningQuarterBeats(1, 2)),
                0);
    }

    @Test
    public void inheritedThreeQuarterMeterKeepsExactBeat() {
        var score =
                score(
                        List.of(
                                note(0, .12f, 1, 0),
                                note(0, .25f, 1, 2),
                                note(0, .6f, 1, 4),
                                note(1, .1f, 3, 0)),
                        List.of());
        var events = bind(score, 340, 499, new ScoreMeterMap(3, List.of()));
        assertEquals(2, events.size());
        assertEquals(new ScoreAnchor(0, 2), events.get(0).start().orElseThrow());
        assertEquals(
                3,
                events.get(1).start().orElseThrow().absoluteBeat(new ScoreMeterMap(3, List.of())),
                0);
    }

    @Test
    public void meterChangeAtSecondBarIsRetained() {
        var meter = new ScoreMeterMap(4, List.of(new ScoreMeterChange(1, 6, 8)));
        var events = bind(score(quarters(), List.of()), 501, 899, meter);
        assertEquals(2, events.size());
        assertEquals(7, events.get(1).start().orElseThrow().absoluteBeat(meter), 0);
    }

    @Test
    public void duplicateRailEvidenceDoesNotDuplicateControllers() {
        var bracket = new PedalBracketDetector.Bracket(340, 499, 270, 0, 10, 1, 2);
        var events =
                PedalBracketAnchors.bind(
                        score(quarters(), List.of()),
                        List.of(bracket, bracket),
                        List.of(STAFF),
                        W,
                        H,
                        new ScoreMeterMap(4, List.of()));
        assertEquals(2, events.size());
    }

    @Test
    public void restHookCannotAcquireBeatFromUnaccountedSilence() {
        var s =
                score(
                        List.of(note(0, .12f, 1, 0), note(1, .1f, 4, 0)),
                        List.of(new ScoreRestEvent(0, .3f, .37f, .02f, 1, 2, 2)));
        assertTrue(
                PedalBracketAnchors.anchor(
                                new PedalBracketAnchors.Hook(0, 2, .3f, .001f),
                                s,
                                1,
                                2,
                                new ScoreMeterMap(4, List.of()))
                        .isEmpty());
        assertTrue(bind(s, 220, 499, new ScoreMeterMap(4, List.of())).isEmpty());
    }

    @Test
    public void completeSilentSlotSupportsPersistedRestHook() {
        var s =
                score(
                        List.of(note(0, .12f, 1, 0), note(1, .1f, 4, 0)),
                        List.of(new ScoreRestEvent(0, .3f, .37f, .02f, 1, 2, 3)));
        assertEquals(
                Optional.of(new ScoreAnchor(0, 1)),
                PedalBracketAnchors.anchor(
                        new PedalBracketAnchors.Hook(0, 2, .3f, .001f),
                        s,
                        1,
                        2,
                        new ScoreMeterMap(4, List.of())));
    }

    @Test
    public void reversedOrUnownedBracketCannotAcquirePedalEvents() {
        assertTrue(
                bind(score(quarters(), List.of()), 499, 340, new ScoreMeterMap(4, List.of()))
                        .isEmpty());
        var bracket = new PedalBracketDetector.Bracket(340, 499, 270, 0, 10, 0, 2);
        assertTrue(
                PedalBracketAnchors.bind(
                                score(quarters(), List.of()),
                                List.of(bracket),
                                List.of(STAFF),
                                W,
                                H,
                                new ScoreMeterMap(4, List.of()))
                        .isEmpty());
    }
}
