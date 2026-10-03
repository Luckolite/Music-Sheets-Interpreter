// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic glyph bounds and staff geometry; no private score material. */
public class LiteralDynamicAttackAnchorTest {
    private final List<PlayingTechniqueDetector.Staff> staffs =
            List.of(new PlayingTechniqueDetector.Staff(80, 120, 10, 0, 1));
    private final List<MeasureRegion> measures = List.of(new MeasureRegion(.1f, .9f, .1f, .4f));

    private ScoreNoteEvent note(float position, int staff) {
        return new ScoreNoteEvent(
                0, position, 2, staff, 1, .2f, false, 0, 0, ScoreNoteEvent.ACCIDENTAL_FROM_KEY, 1f);
    }

    private ScoreDynamicChange detect(float left, float right, List<ScoreNoteEvent> notes) {
        var word = new PlayingTechniqueDetector.Word("f", left, .34f, right, .37f);
        var result =
                ScoreDynamicsDetector.detect(
                        List.of(word), staffs, measures, notes, null, 1000, 400);
        assertEquals(1, result.size());
        return result.get(0);
    }

    @Test
    public void glyphCenteredUnderAttackDoesNotBeginInPrecedingRest() {
        assertEquals(.5f, detect(.485f, .515f, List.of(note(.5f, 0))).positionInMeasure(), .00001f);
    }

    @Test
    public void displacedChordHeadsStillProveOneColumn() {
        float p = detect(.485f, .515f, List.of(note(.5f, 0), note(.5005f, 0))).positionInMeasure();
        assertTrue(Math.abs(p - .5f) <= .00051f);
    }

    @Test
    public void twoDistinctColumnsInsideBodyRemainAmbiguous() {
        assertEquals(
                (.48f - .1f) / .8f,
                detect(.48f, .52f, List.of(note(.49625f, 0), note(.50375f, 0))).positionInMeasure(),
                .00001f);
    }

    @Test
    public void wideInstructionRetainsEstablishedAnchor() {
        assertEquals(
                (.45f - .1f) / .8f,
                detect(.45f, .55f, List.of(note(.5f, 0))).positionInMeasure(),
                .00001f);
    }

    @Test
    public void OtherStaffCannotSupplyAttack() {
        assertEquals(
                (.485f - .1f) / .8f,
                detect(.485f, .515f, List.of(note(.5f, 1))).positionInMeasure(),
                .00001f);
    }

    @Test
    public void EmptyGlyphBodyRetainsPrintedClock() {
        assertEquals(
                (.485f - .1f) / .8f,
                detect(.485f, .515f, List.of(note(.7f, 0))).positionInMeasure(),
                .00001f);
    }

    @Test
    public void nonFiniteBoundsCannotCreateAnchor() {
        var word = new PlayingTechniqueDetector.Word("f", Float.NaN, .3f, .515f, .33f);
        assertEquals(
                .2f,
                ScoreDynamicsDetector.narrowLiteralAttack(
                        word, staffs.get(0), measures, List.of(note(.5f, 0)), 1000, 400, .2f),
                0);
    }
}
