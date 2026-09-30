// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScoreOpeningDurationTest {
    static List<ScoreNoteEvent> closingNotes() {
        return List.of(
                new ScoreNoteEvent(
                        1, .1f, 3, 0, 2, .25f, false, 0, 3, 2, 0, 1, .875f, 0, 30, false, 0, false,
                        0),
                new ScoreNoteEvent(1, .2f, 3, 1, 2, .35f, false, 0, 3, 2, 0).withLeadingRest(.125f),
                new ScoreNoteEvent(1, .4f, 5, 1, 2, .35f, false, 0, 3, 2, 0),
                new ScoreNoteEvent(
                        1, .6f, 7, 1, 2, .35f, false, 0, 3, 2, 0, 1, .5f, 0, 30, false, 0, false,
                        0));
    }

    static List<ScoreRestEvent> closingRests() {
        return List.of(
                new ScoreRestEvent(1, .2f, .25f, .02f, 0, 2, .125),
                new ScoreRestEvent(1, .4f, .25f, .02f, 0, 2, .25),
                new ScoreRestEvent(1, .8f, .25f, .02f, 0, 2, .5),
                new ScoreRestEvent(1, .1f, .35f, .02f, 1, 2, .125),
                new ScoreRestEvent(1, .8f, .35f, .02f, 1, 2, .5));
    }

    @Test
    public void independentTerminalRestsProveOneBeat() {
        assertEquals(
                1,
                ScoreOpeningDuration.provedClosingQuarterBeats(
                        closingNotes(), closingRests(), 2, 4),
                .0001);
        var clock = new ScoreMeterMap(4, List.of()).withBoundaryQuarterBeats(0, 1, 2);
        assertEquals(5, clock.startBeat(2), .0001);
        assertEquals(4, clock.beatsInMeasure(0), .0001);
    }

    @Test
    public void missingTerminalRestOrGapDoesNotShorten() {
        for (int i = 0; i < closingRests().size(); i++) {
            var rests = new ArrayList<>(closingRests());
            rests.remove(i);
            assertTrue(
                    Double.isNaN(
                            ScoreOpeningDuration.provedClosingQuarterBeats(
                                    closingNotes(), rests, 2, 4)));
        }
    }

    @Test
    public void nominalOrDisagreeingEndingCannotShorten() {
        assertTrue(
                Double.isNaN(
                        ScoreOpeningDuration.provedClosingQuarterBeats(
                                closingNotes(), closingRests(), 2, 1)));
        var rests = new ArrayList<>(closingRests());
        rests.set(4, new ScoreRestEvent(1, .8f, .35f, .02f, 1, 2, 1));
        assertTrue(
                Double.isNaN(
                        ScoreOpeningDuration.provedClosingQuarterBeats(
                                closingNotes(), rests, 2, 4)));
    }

    @Test
    public void tieEvidenceCannotShortenEnding() {
        var notes = new ArrayList<>(closingNotes());
        notes.set(0, notes.get(0).withBoundaryTies(1));
        assertTrue(
                Double.isNaN(
                        ScoreOpeningDuration.provedClosingQuarterBeats(
                                notes, closingRests(), 2, 4)));
    }

    @Test
    public void unprovedBoundariesPreserveAndSingleBarCannotConflict() {
        assertEquals(
                8,
                new ScoreMeterMap(4, List.of()).withBoundaryQuarterBeats(0, 0, 2).startBeat(2),
                .0001);
        try {
            new ScoreMeterMap(4, List.of()).withBoundaryQuarterBeats(1, 2, 1);
            fail("Conflicting one-bar boundaries");
        } catch (IllegalArgumentException expected) {
        }
    }

    static final List<MeasureRegion> SHORT =
            List.of(
                    new MeasureRegion(.1f, .2f, .2f, .4f),
                    new MeasureRegion(.21f, .46f, .2f, .4f),
                    new MeasureRegion(.47f, .75f, .2f, .4f));

    static List<ScoreNoteEvent> notes() {
        return List.of(
                new ScoreNoteEvent(0, .77f, 3, 0, 2, .25f, false, 0, 3, 2, 0).withLeadingRest(.75f),
                new ScoreNoteEvent(0, .9f, 5, 0, 2, .24f, false, 0, 3, 2, 0));
    }

    static List<ScoreRestEvent> rests() {
        return List.of(
                new ScoreRestEvent(0, .36f, .25f, .02f, 0, 2, .5),
                new ScoreRestEvent(0, .6f, .25f, .02f, 0, 2, .25),
                new ScoreRestEvent(0, .36f, .35f, .02f, 1, 2, 1));
    }

    static double infer(
            List<ScoreNoteEvent> n, List<ScoreRestEvent> r, List<MeasureRegion> m, int first) {
        return ScoreOpeningDuration.provedQuarterBeats(n, r, m, 4, first);
    }

    @Test
    public void agreeingRestAndNoteStaffsProveOneBeatWithoutFakeTimeSignature() {
        assertEquals(1, infer(notes(), rests(), SHORT, 1), .0001);
        var printed = List.of(new ScoreMeterChange(0, 4, 4));
        var clock = new ScoreMeterMap(4, printed).withOpeningQuarterBeats(1, 3);
        assertEquals(1, clock.startBeat(1), .0001);
        assertEquals(5, clock.startBeat(2), .0001);
        assertEquals(9, clock.startBeat(3), .0001);
        assertEquals(4, printed.get(0).quarterBeats(), .0001);
        assertEquals(
                .75,
                ScoreNoteTiming.beatInMeasure(notes().get(0), notes(), clock.beatsInMeasure(0)),
                .0001);
    }

    @Test
    public void fullWidthBarsKeepTheirClock() {
        var full =
                List.of(
                        new MeasureRegion(.1f, .4f, .2f, .4f),
                        new MeasureRegion(.41f, .65f, .2f, .4f),
                        new MeasureRegion(.66f, .9f, .2f, .4f));
        assertTrue(Double.isNaN(infer(notes(), rests(), full, 1)));
    }

    @Test
    public void unexplainedLeadingGapIsNotCollapsed() {
        assertTrue(Double.isNaN(infer(notes(), rests().subList(1, 3), SHORT, 1)));
    }

    @Test
    public void disagreeingStaffSpansKeepNominalClock() {
        var r = new ArrayList<>(rests());
        r.set(2, new ScoreRestEvent(0, .36f, .35f, .02f, 1, 2, 2));
        assertTrue(Double.isNaN(infer(notes(), r, SHORT, 1)));
    }

    @Test
    public void absentPrintedStaffCannotProveShortBar() {
        assertTrue(Double.isNaN(infer(notes(), rests().subList(0, 2), SHORT, 1)));
    }

    @Test
    public void continuationPageCannotBecomeOpeningPickup() {
        assertTrue(Double.isNaN(infer(notes(), rests(), SHORT, 16)));
    }

    @Test
    public void unknownDurationCannotShortenClock() {
        var n = List.of(new ScoreNoteEvent(0, .8f, 3, 0, 2, .25f, false, 0, 0));
        assertTrue(Double.isNaN(infer(n, rests(), SHORT, 1)));
    }

    @Test
    public void laterMeterChangesSurviveDurationProjection() {
        var source =
                new ScoreMeterMap(
                        4, List.of(new ScoreMeterChange(0, 4, 4), new ScoreMeterChange(2, 3, 4)));
        var clock = source.withOpeningQuarterBeats(1, 3);
        assertEquals(3, clock.beatsInMeasure(2), .0001);
        assertEquals(8, clock.startBeat(3), .0001);
        assertEquals(1.5, clock.measurePosition(3), .0001);
    }
}
