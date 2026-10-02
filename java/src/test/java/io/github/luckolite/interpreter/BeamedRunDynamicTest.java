// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generated below-staff accompaniment and adjacent dynamics, no score pixels. */
public class BeamedRunDynamicTest {
    private final List<MeasureRegion> bars = List.of(new MeasureRegion(.1f, .9f, .2f, .45f));
    private final List<PlayingTechniqueDetector.Staff> staffs =
            List.of(new PlayingTechniqueDetector.Staff(60, 100, 10, 0, 1));

    private ScoreNoteEvent note(float x, float y, int beams) {
        return new ScoreNoteEvent(0, (x - .1f) / .8f, -3, 0, 1, y, false, 0, beams, 2, 0, 1, 0, 0);
    }

    private PlayingTechniqueDetector.Word word(String text, float l, float t, float r, float b) {
        return new PlayingTechniqueDetector.Word(text, l, t, r, b);
    }

    private List<ScoreDynamicChange> detect(
            PlayingTechniqueDetector.Word word, List<ScoreNoteEvent> notes) {
        return ScoreDynamicsDetector.detect(List.of(word), staffs, bars, notes, null, 600, 300);
    }

    @Test
    public void accompanimentHeadsCannotBecomeSoftDynamicLetters() {
        for (String text : List.of("P", "p", "ppp", "mp")) {
            var notes = List.of(note(.3f, .4f, 2), note(.36f, .4f, 2), note(.42f, .4f, 2));
            assertTrue(text, detect(word(text, .28f, .38f, .45f, .46f), notes).isEmpty());
        }
    }

    @Test
    public void clippedLastHeadCannotRemainAsALoneP() {
        assertTrue(detect(word("p", .4f, .38f, .45f, .46f), List.of(note(.42f, .4f, 2))).isEmpty());
    }

    @Test
    public void ordinaryLevelBesideTheRunIsPreserved() {
        var notes = List.of(note(.3f, .4f, 2), note(.36f, .4f, 2));
        var changes = detect(word("mp", .19f, .39f, .24f, .45f), notes);
        assertEquals(1, changes.size());
        assertEquals(-4, changes.get(0).decibels(), 0);
    }

    @Test
    public void levelBelowTheBeamKeepsItsDifferentVerticalIdentity() {
        var changes = detect(word("p", .28f, .49f, .34f, .55f), List.of(note(.3f, .4f, 2)));
        assertEquals(1, changes.size());
        assertEquals(-8, changes.get(0).decibels(), 0);
    }

    @Test
    public void stemCrossingAWordDoesNotOwnTheDistantHead() {
        var changes = detect(word("mf", .28f, .38f, .36f, .46f), List.of(note(.3f, .7f, 2)));
        assertEquals(1, changes.size());
        assertEquals(0, changes.get(0).decibels(), 0);
    }

    @Test
    public void unrelatedMeasureCannotSupplyAnOverlappingHead() {
        var invalid = new ScoreNoteEvent(8, .25f, -3, 0, 1, .4f, false, 0, 2, 2, 0, 1, 0, 0);
        assertEquals(1, detect(word("p", .28f, .38f, .36f, .46f), List.of(invalid)).size());
    }

    @Test
    public void explicitSuddenLevelsCannotComeFromTheRunEither() {
        assertTrue(
                detect(word("subito pp", .28f, .38f, .45f, .46f), List.of(note(.3f, .4f, 2)))
                        .isEmpty());
    }

    @Test
    public void gradualTextRetainsItsMeaningBesideTheRun() {
        var changes = detect(word("cresc.", .28f, .38f, .45f, .46f), List.of(note(.3f, .4f, 2)));
        assertEquals(1, changes.size());
        assertEquals(1, changes.get(0).direction());
    }
}
