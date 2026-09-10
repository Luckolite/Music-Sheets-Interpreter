// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public final class ScoreNoteTimingTest {
    @Test public void doubleDottedEighthPushesThirtySecondToSevenEighthsOfBeat() {
        ScoreNoteEvent longNote = new ScoreNoteEvent(0, .25f, 0, 0, 1,
                .4f, false, 2, 1);
        ScoreNoteEvent fastNote = new ScoreNoteEvent(0, .42f, 1, 0, 1,
                .4f, false, 0, 3);
        List<ScoreNoteEvent> phrase = List.of(longNote, fastNote);

        assertEquals(.875, ScoreNoteTiming.writtenDurationBeats(longNote), .0001);
        assertEquals(.125, ScoreNoteTiming.writtenDurationBeats(fastNote), .0001);
        assertEquals(1.875, ScoreNoteTiming.beatInMeasure(fastNote, phrase, 4f), .0001);
    }

    @Test public void opticallyClassifiedUnbeamedNotesKeepTheirWrittenValues() {
        ScoreNoteEvent quarter = new ScoreNoteEvent(0, .1f, 0, 0, 1,
                .4f, false, 0, 0, ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                ScoreNoteEvent.DURATION_QUARTER);
        ScoreNoteEvent half = new ScoreNoteEvent(0, .5f, 1, 0, 1,
                .4f, false, 1, 0, ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                ScoreNoteEvent.DURATION_HALF);

        assertEquals(1, ScoreNoteTiming.writtenDurationBeats(quarter), .0001);
        assertEquals(3, ScoreNoteTiming.writtenDurationBeats(half), .0001);
    }

    @Test public void fastBeamPhraseUsesWrittenThirtySecondSpacingDespiteEngravingRoom() {
        ScoreNoteEvent first = new ScoreNoteEvent(0, .25f, 0, 0, 1,
                .4f, false, 0, 3);
        ScoreNoteEvent second = new ScoreNoteEvent(0, .35f, 1, 0, 1,
                .4f, false, 0, 3);
        ScoreNoteEvent third = new ScoreNoteEvent(0, .45f, 2, 0, 1,
                .4f, false, 0, 3);
        List<ScoreNoteEvent> phrase = List.of(first, second, third);

        assertEquals(1.0, ScoreNoteTiming.beatInMeasure(first, phrase, 4f), .0001);
        assertEquals(1.125, ScoreNoteTiming.beatInMeasure(second, phrase, 4f), .0001);
        assertEquals(1.25, ScoreNoteTiming.beatInMeasure(third, phrase, 4f), .0001);
    }

    @Test public void rapidPhraseGetsAThirtySecondGridButOrdinaryNotesKeepSixteenths() {
        List<ScoreNoteEvent> rapid = List.of(new ScoreNoteEvent(0, .03f, 0, 0, 1,
                .4f, false, 0, 3));
        List<ScoreNoteEvent> ordinary = List.of(new ScoreNoteEvent(0, .03f, 0, 0, 1));

        assertEquals(.125, ScoreNoteTiming.rhythmicGrid(rapid), .0001);
        assertEquals(.25, ScoreNoteTiming.rhythmicGrid(ordinary), .0001);
    }

    @Test public void equalEighthsIgnoreUnevenEngravingSpaceAroundSlurs() {
        List<ScoreNoteEvent> phrase = List.of(
                new ScoreNoteEvent(0, .10f, 0, 0, 1, .4f, false, 0, 1),
                new ScoreNoteEvent(0, .31f, 1, 0, 1, .4f, false, 0, 1),
                new ScoreNoteEvent(0, .50f, 2, 0, 1, .4f, false, 0, 1),
                new ScoreNoteEvent(0, .62f, 3, 0, 1, .4f, false, 0, 1));

        assertEquals(0, ScoreNoteTiming.beatInMeasure(phrase.get(0), phrase, 4f), .0001);
        assertEquals(.50, ScoreNoteTiming.beatInMeasure(phrase.get(1), phrase, 4f), .0001);
        assertEquals(1.00, ScoreNoteTiming.beatInMeasure(phrase.get(2), phrase, 4f), .0001);
        assertEquals(1.50, ScoreNoteTiming.beatInMeasure(phrase.get(3), phrase, 4f), .0001);
    }

    @Test public void oneMissedBeamInAnEvenRunUsesItsMatchingNeighbors() {
        List<ScoreNoteEvent> phrase = List.of(
                new ScoreNoteEvent(0, .10f, 0, 0, 1, .4f, false, 0, 0),
                new ScoreNoteEvent(0, .28f, 1, 0, 1, .4f, false, 0, 1),
                new ScoreNoteEvent(0, .46f, 2, 0, 1, .4f, false, 0, 1),
                new ScoreNoteEvent(0, .64f, 3, 0, 1, .4f, false, 0, 1));

        assertEquals(.5, ScoreNoteTiming.resolvedWrittenDurationBeats(phrase.get(0), phrase), .0001);
        assertEquals(.5, ScoreNoteTiming.beatInMeasure(phrase.get(1), phrase, 4f), .0001);
        assertEquals(1.0, ScoreNoteTiming.beatInMeasure(phrase.get(2), phrase, 4f), .0001);
    }

    @Test public void openHalfNoteBetweenEighthsIsNotCollapsedIntoTheFastRun() {
        List<ScoreNoteEvent> phrase = List.of(
                beamed(.10f, 0, 1),
                new ScoreNoteEvent(0, .15f, 1, 0, 1, .4f, false,
                        0, 0, ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                        ScoreNoteEvent.DURATION_HALF),
                beamed(.55f, 2, 1));

        assertEquals(2, ScoreNoteTiming.resolvedWrittenDurationBeats(
                phrase.get(1), phrase), .0001);
        assertEquals(2.5, ScoreNoteTiming.beatInMeasure(
                phrase.get(2), phrase, 4f), .0001);
    }

    @Test public void isolatedQuarterBetweenEighthPairsKeepsItsFullWrittenBeat() {
        List<ScoreNoteEvent> phrase = List.of(
                beamed(.10f, 0, 1), beamed(.20f, 1, 1),
                classifiedQuarter(.30f, 2),
                beamed(.50f, 3, 1), beamed(.60f, 4, 1));

        assertEquals(1, ScoreNoteTiming.resolvedWrittenDurationBeats(
                phrase.get(2), phrase), .0001);
        assertEquals(0, ScoreNoteTiming.beatInMeasure(phrase.get(0), phrase, 3f), .0001);
        assertEquals(.5, ScoreNoteTiming.beatInMeasure(phrase.get(1), phrase, 3f), .0001);
        assertEquals(1, ScoreNoteTiming.beatInMeasure(phrase.get(2), phrase, 3f), .0001);
        assertEquals(2, ScoreNoteTiming.beatInMeasure(phrase.get(3), phrase, 3f), .0001);
        assertEquals(2.5, ScoreNoteTiming.beatInMeasure(phrase.get(4), phrase, 3f), .0001);
    }

    @Test public void dottedHeadWithMissedBeamStaysInItsThirtySecondRun() {
        List<ScoreNoteEvent> phrase = List.of(
                new ScoreNoteEvent(0, .10f, 0, 0, 1, .4f, false, 0, 3),
                new ScoreNoteEvent(0, .15f, 1, 0, 1, .4f, false, 0, 3),
                new ScoreNoteEvent(0, .20f, 2, 0, 1, .4f, false, 1, 0,
                        ScoreNoteEvent.ACCIDENTAL_FROM_KEY, ScoreNoteEvent.DURATION_QUARTER),
                new ScoreNoteEvent(0, .30f, 3, 0, 1, .4f, false, 0, 3));

        assertEquals(.1875, ScoreNoteTiming.resolvedWrittenDurationBeats(
                phrase.get(2), phrase), .0001);
        assertEquals(.4375, ScoreNoteTiming.beatInMeasure(phrase.get(3), phrase, 4f), .0001);
    }

    @Test public void falseFourthBeamCannotTurnThirtySecondsIntoSixtyFourths() {
        ScoreNoteEvent splitBeam = new ScoreNoteEvent(0, .10f, 0, 0, 1,
                .4f, false, 0, 4);

        assertEquals(.125, ScoreNoteTiming.writtenDurationBeats(splitBeam), .0001);
        assertEquals(.125, ScoreNoteTiming.rhythmicGrid(List.of(splitBeam)), .0001);
    }

    @Test public void fullMeasureUsesWrittenClockAcrossUnevenEngravingAndSlurs() {
        List<ScoreNoteEvent> phrase = List.of(
                classifiedQuarter(.10f, 0), classifiedQuarter(.18f, 1),
                classifiedQuarter(.72f, 2), classifiedQuarter(.82f, 3));

        assertEquals(0, ScoreNoteTiming.beatInMeasure(phrase.get(0), phrase, 4f), .0001);
        assertEquals(1, ScoreNoteTiming.beatInMeasure(phrase.get(1), phrase, 4f), .0001);
        assertEquals(2, ScoreNoteTiming.beatInMeasure(phrase.get(2), phrase, 4f), .0001);
        assertEquals(3, ScoreNoteTiming.beatInMeasure(phrase.get(3), phrase, 4f), .0001);
    }

    @Test public void conspicuousGapInIncompleteMeasureStillPreservesWrittenRest() {
        List<ScoreNoteEvent> phrase = List.of(
                classifiedQuarter(.10f, 0), classifiedQuarter(.60f, 1),
                classifiedQuarter(.85f, 2));

        assertEquals(0, ScoreNoteTiming.beatInMeasure(phrase.get(0), phrase, 4f), .0001);
        assertEquals(2, ScoreNoteTiming.beatInMeasure(phrase.get(1), phrase, 4f), .0001);
        assertEquals(3, ScoreNoteTiming.beatInMeasure(phrase.get(2), phrase, 4f), .0001);
    }

    @Test public void sixteenthRestCreatesOneSilentSixteenthSlot() {
        List<ScoreNoteEvent> phrase = List.of(
                beamed(.10f, 0, 2), beamed(.15f, 1, 2), beamed(.20f, 2, 2),
                beamed(.30f, 3, 2), beamed(.35f, 4, 2), beamed(.40f, 5, 2));

        assertEquals(.50, ScoreNoteTiming.beatInMeasure(phrase.get(2), phrase, 4f), .0001);
        assertEquals(1.00, ScoreNoteTiming.beatInMeasure(phrase.get(3), phrase, 4f), .0001);
    }

    @Test public void eighthRestCreatesOneSilentEighthSlot() {
        List<ScoreNoteEvent> phrase = List.of(
                beamed(.10f, 0, 1), beamed(.20f, 1, 1), beamed(.40f, 2, 1),
                beamed(.50f, 3, 1), beamed(.60f, 4, 1));

        assertEquals(.50, ScoreNoteTiming.beatInMeasure(phrase.get(1), phrase, 4f), .0001);
        assertEquals(1.50, ScoreNoteTiming.beatInMeasure(phrase.get(2), phrase, 4f), .0001);
    }

    @Test public void repeatedShortRestsStayInsideDenseHumoresqueMeasure() {
        List<ScoreNoteEvent> phrase = List.of(
                beamed(.037f, 0, 2), beamed(.207f, 1, 3),
                beamed(.285f, 2, 2), beamed(.454f, 3, 3),
                beamed(.531f, 4, 2), beamed(.700f, 5, 3),
                beamed(.777f, 6, 2), beamed(.945f, 7, 3));

        double[] expected = {0, .375, .5, .875, 1, 1.375, 1.5, 1.875};
        for (int index = 0; index < phrase.size(); index++)
            assertEquals(expected[index], ScoreNoteTiming.beatInMeasure(
                    phrase.get(index), phrase, 2f), .0001);
    }

    @Test public void missedFastHeadsDoNotPackSurvivingNotesAtStartOfMeasure() {
        List<ScoreNoteEvent> surviving = List.of(
                beamed(.08f, 0, 2), beamed(.22f, 1, 2), beamed(.38f, 2, 2),
                beamed(.55f, 3, 2), beamed(.72f, 4, 2), beamed(.88f, 5, 2));

        assertEquals(0, ScoreNoteTiming.beatInMeasure(surviving.get(0), surviving, 4f), .0001);
        assertEquals(3.25, ScoreNoteTiming.beatInMeasure(surviving.get(5), surviving, 4f), .0001);
    }

    @Test public void alternatingMissedBeamEndsRemainAnEvenEighthNoteMeasure() {
        List<ScoreNoteEvent> phrase = List.of(
                missedBeam(.06f, 0, 1), beamed(.17f, 1, 1),
                missedBeam(.29f, 2, 1), beamed(.40f, 3, 1),
                missedBeam(.52f, 4, 0), beamed(.63f, 5, 1),
                missedBeam(.75f, 6, 0), beamed(.86f, 7, 1));

        for (int index = 0; index < phrase.size(); index++) {
            assertEquals(.5, ScoreNoteTiming.resolvedWrittenDurationBeats(
                    phrase.get(index), phrase), .0001);
            assertEquals(index * .5, ScoreNoteTiming.beatInMeasure(
                    phrase.get(index), phrase, 4f), .0001);
        }
    }

    @Test public void equalSpacingUsesOneMeasureWideBeamConsensus() {
        List<ScoreNoteEvent> phrase = List.of(
                beamed(.06f, 0, 1), beamed(.17f, 1, 1),
                beamed(.28f, 2, 3), beamed(.39f, 3, 1),
                beamed(.50f, 4, 3), beamed(.61f, 5, 1),
                beamed(.72f, 6, 2), beamed(.83f, 7, 1));

        for (ScoreNoteEvent note : phrase)
            assertEquals(.5, ScoreNoteTiming.resolvedWrittenDurationBeats(
                    note, phrase), .0001);
    }

    @Test public void spacingConsensusKeepsShorterSubdivisionSlotsSeparate() {
        List<ScoreNoteEvent> phrase = List.of(
                beamed(.08f, 0, 1), beamed(.20f, 1, 1),
                beamed(.32f, 2, 3), beamed(.38f, 3, 2),
                beamed(.44f, 4, 2), beamed(.50f, 5, 2));

        assertEquals(.5, ScoreNoteTiming.resolvedWrittenDurationBeats(
                phrase.get(0), phrase), .0001);
        assertEquals(.25, ScoreNoteTiming.resolvedWrittenDurationBeats(
                phrase.get(2), phrase), .0001);
    }

    @Test public void meterAwareDurationDoesNotReleaseSixteenthsAsThirtySeconds() {
        List<ScoreNoteEvent> phrase = List.of(
                beamed(.04f, 0, 3), beamed(.11f, 1, 2),
                beamed(.18f, 2, 3), beamed(.25f, 3, 3),
                beamed(.39f, 4, 3), beamed(.46f, 5, 3),
                beamed(.53f, 6, 2), beamed(.60f, 7, 3),
                beamed(.72f, 8, 3), beamed(.79f, 9, 3),
                beamed(.85f, 10, 2), beamed(.91f, 11, 3),
                beamed(.97f, 12, 2));

        for (ScoreNoteEvent note : phrase)
            assertEquals(.25, ScoreNoteTiming.resolvedWrittenDurationBeats(
                    note, phrase, 4f), .0001);
    }

    @Test public void meterAwareDurationPreservesRealThirtySecondRun() {
        List<ScoreNoteEvent> phrase = List.of(
                beamed(.04f, 0, 3), beamed(.071f, 1, 3),
                beamed(.102f, 2, 3), beamed(.133f, 3, 3));

        for (ScoreNoteEvent note : phrase)
            assertEquals(.125, ScoreNoteTiming.resolvedWrittenDurationBeats(
                    note, phrase, 4f), .0001);
    }

    @Test public void overfilledOpticalMeasureNeverMovesLaterNotesBackward() {
        List<ScoreNoteEvent> phrase = List.of(
                classifiedQuarter(.06f, 0), classifiedQuarter(.20f, 1),
                classifiedQuarter(.35f, 2), classifiedQuarter(.50f, 3),
                classifiedQuarter(.66f, 4), classifiedQuarter(.82f, 5));

        double previous = -1;
        for (ScoreNoteEvent note : phrase) {
            double onset = ScoreNoteTiming.beatInMeasure(note, phrase, 4f);
            org.junit.Assert.assertTrue(onset > previous);
            previous = onset;
        }
    }

    @Test public void mixedAtonementDurationsNeverMoveFinalFastNotesBackward() {
        List<ScoreNoteEvent> phrase = List.of(
                new ScoreNoteEvent(0, .047f, 0, 0, 1, .4f, false, 1, 0),
                beamed(.245f, 1, 1),
                classifiedQuarter(.380f, 2),
                beamed(.546f, 3, 3), beamed(.666f, 4, 3),
                beamed(.785f, 5, 3), beamed(.904f, 6, 3));

        double previous = -1;
        for (ScoreNoteEvent note : phrase) {
            double onset = ScoreNoteTiming.beatInMeasure(note, phrase, 4f);
            org.junit.Assert.assertTrue(onset > previous);
            org.junit.Assert.assertTrue(onset < 4);
            previous = onset;
        }
    }

    @Test public void ordinaryEngravingInsetDoesNotBecomeAFalseLeadingRest() {
        ScoreNoteEvent target = new ScoreNoteEvent(1, .11f, 2, 0, 1,
                .4f, false, 0, 1);
        List<ScoreNoteEvent> score = List.of(
                new ScoreNoteEvent(0, .07f, 0, 0, 1, .4f, false, 0, 1),
                new ScoreNoteEvent(0, .22f, 1, 0, 1, .4f, false, 0, 1),
                target,
                new ScoreNoteEvent(1, .27f, 3, 0, 1, .4f, false, 0, 1),
                new ScoreNoteEvent(2, .09f, 4, 0, 1, .4f, false, 0, 1));

        assertEquals(0, ScoreNoteTiming.beatInMeasure(target, score, 4f), .0001);
    }

    @Test public void significantlyLaterFirstNoteStillPreservesALeadingRest() {
        ScoreNoteEvent target = new ScoreNoteEvent(2, .31f, 2, 0, 1,
                .4f, false, 0, 1);
        List<ScoreNoteEvent> score = List.of(
                new ScoreNoteEvent(0, .07f, 0, 0, 1, .4f, false, 0, 1),
                new ScoreNoteEvent(1, .09f, 1, 0, 1, .4f, false, 0, 1),
                target,
                new ScoreNoteEvent(2, .46f, 3, 0, 1, .4f, false, 0, 1));

        assertEquals(1.25, ScoreNoteTiming.beatInMeasure(target, score, 4f), .0001);
    }

    @Test public void barlinePaddingMovesTheEntireUnbeamedMeasureClock() {
        List<ScoreNoteEvent> quarters = List.of(
                new ScoreNoteEvent(0, .10f, 0, 0, 1, .20f, false),
                new ScoreNoteEvent(0, .35f, 1, 0, 1, .20f, false),
                new ScoreNoteEvent(0, .60f, 2, 0, 1, .20f, false),
                new ScoreNoteEvent(0, .85f, 3, 0, 1, .20f, false));

        assertEquals(0, ScoreNoteTiming.beatInMeasure(quarters.get(0), quarters, 4f), .0001);
        assertEquals(1, ScoreNoteTiming.beatInMeasure(quarters.get(1), quarters, 4f), .0001);
        assertEquals(2, ScoreNoteTiming.beatInMeasure(quarters.get(2), quarters, 4f), .0001);
        assertEquals(3, ScoreNoteTiming.beatInMeasure(quarters.get(3), quarters, 4f), .0001);
    }

    @Test public void newSystemHeaderWidthDoesNotDelayOrRushTheNextRow() {
        List<ScoreNoteEvent> score = new java.util.ArrayList<>();
        addMeasure(score, 0, .15f, .236f, .444f, .652f, .859f);
        addMeasure(score, 1, .15f, .08f, .33f, .58f, .83f);
        addMeasure(score, 2, .35f, .236f, .444f, .652f, .859f);
        addMeasure(score, 3, .35f, .08f, .33f, .58f, .83f);

        List<ScoreNoteEvent> nextRow = score.subList(8, 12);
        assertEquals(0, ScoreNoteTiming.beatInMeasure(nextRow.get(0), score, 4f), .0001);
        assertEquals(1, ScoreNoteTiming.beatInMeasure(nextRow.get(1), score, 4f), .0001);
        assertEquals(2, ScoreNoteTiming.beatInMeasure(nextRow.get(2), score, 4f), .0001);
        assertEquals(3, ScoreNoteTiming.beatInMeasure(nextRow.get(3), score, 4f), .0001);
    }

    @Test public void newSystemCorrectionStillKeepsARealLeadingRest() {
        List<ScoreNoteEvent> score = new java.util.ArrayList<>();
        addMeasure(score, 0, .15f, .236f, .444f, .652f, .859f);
        addMeasure(score, 1, .15f, .08f, .33f, .58f, .83f);
        addMeasure(score, 2, .35f, .444f, .652f, .859f);
        addMeasure(score, 3, .35f, .08f, .33f, .58f, .83f);

        assertEquals(1.25, ScoreNoteTiming.beatInMeasure(score.get(8), score, 4f), .0001);
    }

    @Test public void connectedStaffAttacksShareOneClockWhenOneVoiceHasDamagedRhythm() {
        List<ScoreNoteEvent> score = List.of(
                connectedBeamed(.080f, 0, 0, 1), connectedBeamed(.200f, 1, 0, 1),
                connectedBeamed(.320f, 2, 0, 1), connectedBeamed(.440f, 3, 0, 1),
                connectedQuarter(.089f, 5, 1), connectedQuarter(.209f, 6, 1),
                connectedQuarter(.329f, 7, 1), connectedQuarter(.449f, 8, 1));

        for (int index = 0; index < 4; index++) {
            assertEquals(ScoreNoteTiming.beatInMeasure(score.get(index), score, 4f),
                    ScoreNoteTiming.beatInMeasure(score.get(index + 4), score, 4f), .0001);
        }
        assertEquals(0, ScoreNoteTiming.beatInMeasure(score.get(0), score, 4f), .0001);
        assertEquals(.5, ScoreNoteTiming.beatInMeasure(score.get(1), score, 4f), .0001);
    }

    @Test public void nearbyButStaggeredConnectedStaffNotesRemainSeparate() {
        List<ScoreNoteEvent> score = List.of(
                connectedBeamed(.080f, 0, 0, 1), connectedBeamed(.200f, 1, 0, 1),
                connectedBeamed(.320f, 2, 0, 1), connectedBeamed(.440f, 3, 0, 1),
                connectedQuarter(.089f, 5, 1), connectedQuarter(.250f, 6, 1),
                connectedQuarter(.570f, 7, 1), connectedQuarter(.830f, 8, 1));

        assertEquals(0, ScoreNoteTiming.beatInMeasure(score.get(0), score, 4f), .0001);
        assertEquals(0, ScoreNoteTiming.beatInMeasure(score.get(4), score, 4f), .0001);
        assertEquals(.5, ScoreNoteTiming.beatInMeasure(score.get(1), score, 4f), .0001);
        assertEquals(1, ScoreNoteTiming.beatInMeasure(score.get(5), score, 4f), .0001);
    }

    private static void addMeasure(List<ScoreNoteEvent> destination, int measure, float pageY,
                                   float... positions) {
        for (int index = 0; index < positions.length; index++)
            destination.add(new ScoreNoteEvent(measure, positions[index], index,
                    0, 1, pageY, false));
    }

    private static ScoreNoteEvent classifiedQuarter(float position, int step) {
        return new ScoreNoteEvent(0, position, step, 0, 1, .4f, false,
                0, 0, ScoreNoteEvent.ACCIDENTAL_FROM_KEY, ScoreNoteEvent.DURATION_QUARTER);
    }

    private static ScoreNoteEvent beamed(float position, int step, int beams) {
        return new ScoreNoteEvent(0, position, step, 0, 1, .4f, false, 0, beams);
    }

    private static ScoreNoteEvent missedBeam(float position, int step, int dots) {
        return new ScoreNoteEvent(0, position, step, 0, 1, .4f, false, dots, 0,
                ScoreNoteEvent.ACCIDENTAL_FROM_KEY, ScoreNoteEvent.DURATION_QUARTER);
    }

    private static ScoreNoteEvent connectedBeamed(float position, int step, int staff,
                                                   int beams) {
        return new ScoreNoteEvent(0, position, step, staff, 2,
                staff == 0 ? .4f : .5f, false, 0, beams);
    }

    private static ScoreNoteEvent connectedQuarter(float position, int step, int staff) {
        return new ScoreNoteEvent(0, position, step, staff, 2,
                staff == 0 ? .4f : .5f, false, 0, 0,
                ScoreNoteEvent.ACCIDENTAL_FROM_KEY, ScoreNoteEvent.DURATION_QUARTER);
    }
}
