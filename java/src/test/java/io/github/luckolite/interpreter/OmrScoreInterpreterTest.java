// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class OmrScoreInterpreterTest {
    @Test public void detectsAFlatSignatureRunAfterAMeasureBoundary() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        for (int x : new int[]{80, 84}) for (int y = 78; y <= 122; y++)
            labels[y * width + x] = OmrMeasurePostProcessor.STEM_OR_REST;
        flat(labels, width, 92, 76);
        flat(labels, width, 108, 83);
        flat(labels, width, 124, 73);
        flat(labels, width, 140, 80);
        head(labels, width, 190, 110);

        OmrScoreInterpreter.Analysis analysis = OmrScoreInterpreter.analyze(
                labels, null, width, height,
                List.of(new MeasureRegion(.20f, .90f, .25f, .55f)));

        assertEquals(List.of(new ScoreKeyChange(0, -4)), analysis.keyChanges());
    }

    @Test public void extractsPitchStepsAndMeasurePositionsFromTrackerLabels() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 100, 120);
        head(labels, width, 220, 105);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.10f, .90f, .20f, .62f)));

        assertEquals(2, notes.size());
        assertEquals(0, notes.get(0).staffStep());
        assertEquals(3, notes.get(1).staffStep());
        assertTrue(notes.get(0).positionInMeasure() > .17f
                && notes.get(0).positionInMeasure() < .22f);
        assertTrue(notes.get(1).positionInMeasure() > .52f
                && notes.get(1).positionInMeasure() < .57f);
    }

    @Test public void staffLabelNoiseBetweenLinesDoesNotHideEveryNote() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        // Faint scanned notation may be classified as staff in every interline space. The
        // stronger five-line peaks still define a valid staff for note interpretation.
        for (int y = 80; y <= 120; y++) for (int x = 30; x <= 60; x++)
            labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 220, 105);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(3, notes.get(0).staffStep());
    }

    @Test public void localStaffLineKeepsPitchCorrectOnASlopedScan() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        for (int baseY : new int[]{80, 90, 100, 110, 120}) {
            for (int x = 20; x < 380; x++) {
                int y = baseY + Math.round((x - 200) * .020f);
                labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
            }
        }
        int localBottom = 120 + Math.round((350 - 200) * .020f);
        head(labels, width, 350, localBottom);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(0, notes.get(0).staffStep());
    }

    @Test public void rawStaffFallbackKeepsNotesWhenSemanticStaffMaskIsMissing() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        byte[] gray = new byte[width * height];
        java.util.Arrays.fill(gray, (byte) 0xff);
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) gray[y * width + x] = 0;
        head(labels, width, 150, 120);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, gray, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(0, notes.get(0).staffStep());
    }

    @Test public void compactAlignedViolinSystemsKeepIndependentStaffIdentity() {
        int width = 400, height = 1000;
        byte[] labels = new byte[width * height];
        java.util.ArrayList<MeasureRegion> measures = new java.util.ArrayList<>();
        for (int index = 0; index < 8; index++) {
            int top = 50 + index * 115;
            for (int line = 0; line < 5; line++)
                for (int x = 20; x < width - 20; x++)
                    labels[(top + line * 10) * width + x] = OmrMeasurePostProcessor.STAFF;
            head(labels, width, 180, top + 40);
            measures.add(new MeasureRegion(.05f, .95f, (top - 20f) / height,
                    (top + 60f) / height));
        }

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(
                labels, width, height, measures);

        assertEquals(8, notes.size());
        for (ScoreNoteEvent note : notes) {
            assertEquals(0, note.staffIndex());
            assertEquals(1, note.staffCount());
        }
    }

    @Test public void closeGrandStaffKeepsSharedSystemIdentity() {
        int width = 400, height = 320;
        byte[] labels = new byte[width * height];
        for (int top : new int[]{50, 150}) {
            for (int line = 0; line < 5; line++)
                for (int x = 20; x < width - 20; x++)
                    labels[(top + line * 10) * width + x] = OmrMeasurePostProcessor.STAFF;
            head(labels, width, 180, top + 40);
        }

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .05f, .70f)));

        assertEquals(2, notes.size());
        assertEquals(0, notes.get(0).staffIndex());
        assertEquals(2, notes.get(0).staffCount());
        assertEquals(1, notes.get(1).staffIndex());
        assertEquals(2, notes.get(1).staffCount());
    }

    @Test public void stemDirectionAssignsAmbiguousLedgerHeadToOwningStaff() {
        int width = 400, height = 380;
        byte[] labels = new byte[width * height];
        for (int top : new int[]{50, 150, 250})
            for (int line = 0; line < 5; line++)
                for (int x = 20; x < width - 20; x++)
                    labels[(top + line * 10) * width + x]
                            = OmrMeasurePostProcessor.STAFF;
        // At y=119 the head is slightly nearer the upper staff, but its downward stem points
        // into the middle staff and proves that it is a high piano note, not a low violin note.
        head(labels, width, 180, 119);
        for (int y = 122; y <= 146; y++)
            labels[y * width + 184] = OmrMeasurePostProcessor.STEM_OR_REST;

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .05f, .88f)));

        assertEquals(1, notes.size());
        assertEquals(1, notes.get(0).staffIndex());
        assertEquals(3, notes.get(0).staffCount());
    }

    @Test public void mixedSoloAndDuetRowsReceiveLocalSystemIdentity() {
        int width = 400, height = 600;
        byte[] labels = new byte[width * height];
        int[] tops = {50, 180, 260, 430};
        for (int top : tops) {
            for (int line = 0; line < 5; line++)
                for (int x = 20; x < width - 20; x++)
                    labels[(top + line * 10) * width + x] = OmrMeasurePostProcessor.STAFF;
            head(labels, width, 180, top + 40);
        }
        List<MeasureRegion> measures = List.of(
                new MeasureRegion(.05f, .95f, .04f, .20f),
                new MeasureRegion(.05f, .95f, .25f, .55f),
                new MeasureRegion(.05f, .95f, .68f, .86f));

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(
                labels, width, height, measures);

        assertEquals(4, notes.size());
        assertEquals(1, notes.get(0).staffCount());
        assertEquals(0, notes.get(0).staffIndex());
        assertEquals(2, notes.get(1).staffCount());
        assertEquals(0, notes.get(1).staffIndex());
        assertEquals(2, notes.get(2).staffCount());
        assertEquals(1, notes.get(2).staffIndex());
        assertEquals(1, notes.get(3).staffCount());
        assertEquals(0, notes.get(3).staffIndex());
        assertEquals(notes.get(1).measureIndex(), notes.get(2).measureIndex());
    }

    @Test public void keepsNinthPositionViolinHeadSixSpacesAboveTrebleStaff() {
        int width = 400, height = 260;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{100, 110, 120, 130, 140})
            for (int x = 20; x < 380; x++) labels[y * width + x]
                    = OmrMeasurePostProcessor.STAFF;
        // D7 is written twelve diatonic steps above the treble top line (staff step 20 from E4).
        // It occurs in upper violin positions and used to be discarded by the five-gap cutoff.
        head(labels, width, 190, 40);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .30f, .64f)));

        assertEquals(1, notes.size());
        assertEquals(20, notes.get(0).staffStep());
    }

    @Test public void splitsTwoVerticallyJoinedChordHeadsIntoSeparateNotes() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        head(labels, width, 150, 100);
        head(labels, width, 150, 112);
        for (int y = 103; y <= 109; y++)
            labels[y * width + 150] = OmrMeasurePostProcessor.NOTEHEAD;

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(2, notes.size());
        assertEquals(notes.get(0).measureIndex(), notes.get(1).measureIndex());
        assertTrue(Math.abs(notes.get(0).positionInMeasure()
                - notes.get(1).positionInMeasure()) < .01f);
        assertTrue(notes.get(0).staffStep() != notes.get(1).staffStep());
    }

    @Test public void ignoresNoteheadsOutsideEveryIndexedMeasure() {
        int width = 300, height = 200;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{50, 60, 70, 80, 90})
            for (int x = 10; x < 290; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 250, 90);

        assertTrue(OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .50f, .20f, .55f))).isEmpty());
    }

    @Test public void marksSamePitchContinuationWhenSymbolMaskContainsTieArc() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 180, 120);
        head(labels, width, 220, 120);
        for (int x = 184; x <= 216; x++) {
            int curveY = 126 + Math.round((x - 200) * (x - 200) / 180f);
            labels[curveY * width + x] = OmrMeasurePostProcessor.SYMBOL;
        }

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .50f, .20f, .62f),
                        new MeasureRegion(.50f, .95f, .20f, .62f)));

        assertEquals(2, notes.size());
        assertTrue(notes.get(1).tiedFromPrevious());
        assertTrue(notes.get(1).pageY() > .49f && notes.get(1).pageY() < .51f);
    }

    @Test public void joinsPrintedTieWhenSemanticMaskDropsItsThinArc() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        byte[] gray = new byte[width * height];
        java.util.Arrays.fill(gray, (byte) 0xff);
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 180, 120);
        head(labels, width, 240, 120);
        for (int x = 184; x <= 236; x++) {
            float normalized = (x - 210) / 26f;
            int y = 126 + Math.round(normalized * normalized * 4f);
            gray[y * width + x] = 0;
        }

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, gray, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(2, notes.size());
        assertTrue(notes.get(1).tiedFromPrevious());
    }

    @Test public void curvedSlurBetweenDifferentPitchesDoesNotCreateATie() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 180, 120);
        head(labels, width, 220, 115);
        for (int x = 184; x <= 216; x++) {
            int curveY = 126 + Math.round((x - 200) * (x - 200) / 180f);
            labels[curveY * width + x] = OmrMeasurePostProcessor.SYMBOL;
        }

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .50f, .20f, .62f),
                        new MeasureRegion(.50f, .95f, .20f, .62f)));

        assertEquals(2, notes.size());
        assertEquals(0, notes.get(0).staffStep());
        assertEquals(1, notes.get(1).staffStep());
        assertFalse(notes.get(1).tiedFromPrevious());
    }

    @Test public void threeSamePitchHeadsConnectedByTieArcsFormOneContinuationChain() {
        int width = 420, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 400; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 140, 110);
        head(labels, width, 200, 110);
        head(labels, width, 260, 110);
        tieArc(labels, width, 144, 196, 116, OmrMeasurePostProcessor.STEM_OR_REST);
        tieArc(labels, width, 204, 256, 116, OmrMeasurePostProcessor.STEM_OR_REST);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(3, notes.size());
        assertFalse(notes.get(0).tiedFromPrevious());
        assertTrue(notes.get(1).tiedFromPrevious());
        assertTrue(notes.get(2).tiedFromPrevious());
    }

    @Test public void phraseSlurCannotReconnectToAnOlderMatchingPitch() {
        int width = 420, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 400; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 140, 110);
        head(labels, width, 200, 100);
        head(labels, width, 260, 110);
        tieArc(labels, width, 144, 256, 116, OmrMeasurePostProcessor.SYMBOL);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(3, notes.size());
        assertFalse(notes.get(2).tiedFromPrevious());
    }

    @Test public void doubleAugmentationDotsAreMetadataNotExtraNoteheads() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 150, 105);
        dot(labels, width, 160, 105, OmrMeasurePostProcessor.NOTEHEAD);
        dot(labels, width, 165, 105, OmrMeasurePostProcessor.SYMBOL);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(2, notes.get(0).augmentationDots());
    }

    @Test public void staccatoMarkAboveTheHeadIsNotAnAugmentationDot() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 150, 105);
        dot(labels, width, 150, 95, OmrMeasurePostProcessor.SYMBOL);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));
        assertEquals(1, notes.size());
        assertEquals(0, notes.get(0).augmentationDots());
    }

    @Test public void readsTripleBeamAsAThirtySecondNote() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 150, 105);
        for (int y = 66; y <= 105; y++) labels[y * width + 153] = OmrMeasurePostProcessor.STEM_OR_REST;
        for (int y : new int[]{70, 76, 82}) for (int x = 153; x <= 180; x++)
            labels[y * width + x] = OmrMeasurePostProcessor.STEM_OR_REST;

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(3, notes.get(0).beamCount());
    }

    @Test public void filledUnbeamedHeadWithStemIsAQuarterNote() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        byte[] gray = whitePage(width, height);
        head(labels, width, 150, 105);
        darkHead(gray, width, 150, 105);
        for (int y = 67; y <= 103; y++) labels[y * width + 153]
                = OmrMeasurePostProcessor.STEM_OR_REST;

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, gray, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(ScoreNoteEvent.DURATION_QUARTER,
                notes.get(0).unbeamedDurationBeats(), .0001f);
    }

    @Test public void openUnbeamedHeadUsesItsStemToDistinguishHalfFromWhole() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        byte[] gray = whitePage(width, height);
        openHead(labels, gray, width, 140, 105);
        openHead(labels, gray, width, 250, 105);
        for (int y = 67; y <= 103; y++) labels[y * width + 143]
                = OmrMeasurePostProcessor.STEM_OR_REST;

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, gray, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(2, notes.size());
        assertEquals(ScoreNoteEvent.DURATION_HALF,
                notes.get(0).unbeamedDurationBeats(), .0001f);
        assertEquals(ScoreNoteEvent.DURATION_WHOLE,
                notes.get(1).unbeamedDurationBeats(), .0001f);
    }

    @Test public void openHeadWinsOverAFalseNearbyBeamBand() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        byte[] gray = whitePage(width, height);
        openHead(labels, gray, width, 150, 105);
        for (int y = 67; y <= 103; y++) labels[y * width + 153]
                = OmrMeasurePostProcessor.STEM_OR_REST;
        for (int x = 153; x <= 180; x++) labels[70 * width + x]
                = OmrMeasurePostProcessor.STEM_OR_REST;

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, gray, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(0, notes.get(0).beamCount());
        assertEquals(ScoreNoteEvent.DURATION_HALF,
                notes.get(0).unbeamedDurationBeats(), .0001f);
    }

    @Test public void partialBeamsBelongToTheStemTheyActuallyTouch() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 150, 105); head(labels, width, 190, 105);
        for (int y = 66; y <= 105; y++) {
            labels[y * width + 153] = OmrMeasurePostProcessor.STEM_OR_REST;
            labels[y * width + 193] = OmrMeasurePostProcessor.STEM_OR_REST;
        }
        // The primary beam reaches both stems. The two partial beams only reach the second,
        // making an eighth + 32nd pair rather than two 32nd notes.
        for (int x = 153; x <= 193; x++)
            labels[70 * width + x] = OmrMeasurePostProcessor.STEM_OR_REST;
        for (int y : new int[]{76, 82}) for (int x = 181; x <= 193; x++)
            labels[y * width + x] = OmrMeasurePostProcessor.STEM_OR_REST;

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(2, notes.size());
        assertEquals(1, notes.get(0).beamCount());
        assertEquals(3, notes.get(1).beamCount());
    }

    @Test public void readsDoubleDotsDirectlyFromUndecoratedPagePixels() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        byte[] gray = new byte[width * height]; java.util.Arrays.fill(gray, (byte) 0xff);
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 150, 105);
        darkDot(gray, width, 160, 105); darkDot(gray, width, 165, 105);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, gray, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));
        assertEquals(1, notes.size());
        assertEquals(2, notes.get(0).augmentationDots());
    }

    @Test public void detachedArticulationAboveTheNextSlotIsNotAnAugmentationDot() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        byte[] gray = whitePage(width, height);
        head(labels, width, 150, 105);
        darkHead(gray, width, 150, 105);
        darkDot(gray, width, 160, 113);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, gray, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(0, notes.get(0).augmentationDots());
    }

    @Test public void nearbyBowingWedgeIsNotAnAugmentationDot() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        byte[] gray = whitePage(width, height);
        head(labels, width, 150, 105);
        darkHead(gray, width, 150, 105);
        // A compact up/down-bow fragment can be beside a dense note while still sitting
        // noticeably above its centre. It must not lengthen the note by fifty percent.
        for (int y = 99; y <= 103; y++) {
            int reach = y - 98;
            for (int x = 164 - reach; x <= 164 + reach; x++) gray[y * width + x] = 0;
        }

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, gray, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(0, notes.get(0).augmentationDots());
    }

    @Test public void distantRoundNotationFragmentIsNotAnAugmentationDot() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        byte[] gray = whitePage(width, height);
        head(labels, width, 150, 105);
        darkHead(gray, width, 150, 105);
        darkDot(gray, width, 171, 105);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, gray, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(0, notes.get(0).augmentationDots());
    }

    @Test public void detachedRawNoteheadEdgeIslandIsNotAnAugmentationDot() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        byte[] gray = whitePage(width, height);
        head(labels, width, 150, 105);
        darkHead(gray, width, 150, 105);
        // Artemis's rendered ovals leave a tiny antialiased island about .9 staff gaps from
        // the centre. It is part of the head, not a rhythmic dot.
        for (int y = 104; y <= 106; y++)
            for (int x = 158; x <= 160; x++) gray[y * width + x] = 0;

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, gray, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(0, notes.get(0).augmentationDots());
    }

    @Test public void oversizedStemlessDotIsMetadataNotASecondPlayedNote() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 150, 105);
        // A 4x4 halo-enlarged dot clears the generic plausible-head threshold but has no stem.
        for (int y = 103; y <= 106; y++) for (int x = 164; x <= 167; x++)
            labels[y * width + x] = OmrMeasurePostProcessor.NOTEHEAD;

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .95f, .20f, .62f)));

        assertEquals(1, notes.size());
        assertEquals(1, notes.get(0).augmentationDots());
    }

    @Test public void localFlatAppliesToTheNoteAndCarriesOnlyThroughItsMeasure() {
        int width = 400, height = 240;
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < 380; x++) labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        head(labels, width, 200, 105);
        head(labels, width, 250, 105);
        head(labels, width, 340, 105);
        flat(labels, width, 185, 94);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .70f, .20f, .62f),
                        new MeasureRegion(.70f, .95f, .20f, .62f)));

        assertEquals(3, notes.size());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT, notes.get(0).writtenAccidental());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT, notes.get(1).writtenAccidental());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY, notes.get(2).writtenAccidental());
    }

    @Test public void localSharpAppliesToTheNoteAndCarriesOnlyThroughItsMeasure() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        head(labels, width, 200, 105);
        head(labels, width, 250, 105);
        head(labels, width, 340, 105);
        sharp(labels, width, 185, 90);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .70f, .20f, .62f),
                        new MeasureRegion(.70f, .95f, .20f, .62f)));

        assertEquals(3, notes.size());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP, notes.get(0).writtenAccidental());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP, notes.get(1).writtenAccidental());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY, notes.get(2).writtenAccidental());
    }

    @Test public void localNaturalAppliesToTheNoteAndCarriesOnlyThroughItsMeasure() {
        int width = 400, height = 240;
        byte[] labels = staffPage(width, height);
        head(labels, width, 200, 105);
        head(labels, width, 250, 105);
        head(labels, width, 340, 105);
        natural(labels, width, 185, 90);

        List<ScoreNoteEvent> notes = OmrScoreInterpreter.extract(labels, width, height,
                List.of(new MeasureRegion(.05f, .70f, .20f, .62f),
                        new MeasureRegion(.70f, .95f, .20f, .62f)));

        assertEquals(3, notes.size());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_NATURAL, notes.get(0).writtenAccidental());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_NATURAL, notes.get(1).writtenAccidental());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY, notes.get(2).writtenAccidental());
    }

    private static void head(byte[] labels, int width, int centerX, int centerY) {
        for (int y = centerY - 2; y <= centerY + 2; y++)
            for (int x = centerX - 3; x <= centerX + 3; x++)
                labels[y * width + x] = OmrMeasurePostProcessor.NOTEHEAD;
    }

    private static byte[] staffPage(int width, int height) {
        byte[] labels = new byte[width * height];
        for (int y : new int[]{80, 90, 100, 110, 120})
            for (int x = 20; x < width - 20; x++)
                labels[y * width + x] = OmrMeasurePostProcessor.STAFF;
        return labels;
    }

    private static byte[] whitePage(int width, int height) {
        byte[] gray = new byte[width * height];
        java.util.Arrays.fill(gray, (byte) 0xff);
        return gray;
    }

    private static void darkHead(byte[] gray, int width, int centerX, int centerY) {
        for (int y = centerY - 2; y <= centerY + 2; y++)
            for (int x = centerX - 3; x <= centerX + 3; x++) gray[y * width + x] = 0;
    }

    private static void openHead(byte[] labels, byte[] gray, int width,
                                 int centerX, int centerY) {
        for (int y = centerY - 2; y <= centerY + 2; y++)
            for (int x = centerX - 3; x <= centerX + 3; x++) {
                boolean edge = y == centerY - 2 || y == centerY + 2
                        || x == centerX - 3 || x == centerX + 3;
                if (!edge) continue;
                labels[y * width + x] = OmrMeasurePostProcessor.NOTEHEAD;
                gray[y * width + x] = 0;
            }
    }

    private static void tieArc(byte[] labels, int width, int left, int right, int centerY,
                               byte label) {
        float half = (right - left) / 2f;
        float middle = (left + right) / 2f;
        for (int x = left; x <= right; x++) {
            float normalized = (x - middle) / half;
            int y = centerY + Math.round(normalized * normalized * 3f);
            labels[y * width + x] = label;
        }
    }

    private static void dot(byte[] labels, int width, int centerX, int centerY, byte label) {
        for (int y = centerY - 1; y <= centerY + 1; y++)
            for (int x = centerX - 1; x <= centerX + 1; x++) labels[y * width + x] = label;
    }

    private static void darkDot(byte[] gray, int width, int centerX, int centerY) {
        for (int y = centerY - 1; y <= centerY + 1; y++)
            for (int x = centerX - 1; x <= centerX + 1; x++) gray[y * width + x] = 0;
    }

    private static void flat(byte[] labels, int width, int left, int top) {
        for (int y = top; y <= top + 20; y++)
            labels[y * width + left] = OmrMeasurePostProcessor.CLEF_OR_KEY;
        for (int y = top + 10; y <= top + 17; y++) {
            int reach = y <= top + 13 ? y - (top + 9) : top + 18 - y;
            for (int x = left + 1; x <= left + Math.max(2, reach); x++)
                labels[y * width + x] = OmrMeasurePostProcessor.CLEF_OR_KEY;
        }
    }

    private static void sharp(byte[] labels, int width, int left, int top) {
        byte value = OmrMeasurePostProcessor.CLEF_OR_KEY;
        for (int y = top; y <= top + 30; y++) for (int x : new int[]{left + 2, left + 3,
                left + 8, left + 9}) labels[y * width + x] = value;
        for (int y : new int[]{top + 9, top + 10, top + 11, top + 19, top + 20, top + 21})
            for (int x = left; x <= left + 11; x++) labels[y * width + x] = value;
    }

    private static void natural(byte[] labels, int width, int left, int top) {
        byte value = OmrMeasurePostProcessor.CLEF_OR_KEY;
        for (int y = top; y <= top + 22; y++)
            for (int x : new int[]{left + 2, left + 3}) labels[y * width + x] = value;
        for (int y = top + 8; y <= top + 30; y++)
            for (int x : new int[]{left + 8, left + 9}) labels[y * width + x] = value;
        for (int y : new int[]{top + 9, top + 10, top + 19, top + 20})
            for (int x = left + 2; x <= left + 9; x++) labels[y * width + x] = value;
    }
}
