// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rest bulbs at fractional staff scale and finite staff-rule remnants. */
public class RasterEighthRestTest {
    private static final int W = 800, H = 240;
    private final byte[] gray = new byte[W * H];

    private void ellipse(int cy, int rx, int ry) {
        for (int y = cy - ry; y <= cy + ry; y++)
            for (int x = 177 - rx; x <= 177 + rx; x++)
                if (Math.pow((x - 177) / (double) rx, 2) + Math.pow((y - cy) / (double) ry, 2) <= 1)
                    gray[y * W + x] = 0;
    }

    private void setup(boolean partialRule, boolean small, boolean tail) {
        Arrays.fill(gray, (byte) 255);
        for (int line = 0; line < 5; line++) {
            int y = Math.round(80 + line * 14.25f);
            for (int x = partialRule && line == 3 ? 140 : 20;
                    x <= (partialRule && line == 3 ? 230 : 780);
                    x++) gray[y * W + x] = 0;
        }
        ellipse(100, small ? 4 : 6, small ? 3 : 5);
        if (tail)
            for (int y = 97; y <= 123; y++) {
                int x = 185 - (y - 97) * 10 / 26;
                gray[y * W + x] = 0;
                gray[y * W + x + 1] = 0;
            }
    }

    private List<ScoreRestEvent> rests() {
        return SixteenthRestDetector.detect(
                gray,
                W,
                H,
                List.of(new MeasureRegion(0, 1, .2f, .8f)),
                List.of(new SixteenthRestDetector.Staff(80, 137, 14.25f, 0, 1)),
                List.of());
    }

    @Test
    public void aThreeRowBulbAtFractionalScaleIsAnEighthRest() {
        setup(false, true, true);
        var r = rests();
        assertEquals(r.toString(), 1, r.size());
        assertEquals(.5, r.get(0).durationBeats(), 0);
    }

    @Test
    public void aFiniteRuleRemnantDoesNotJoinTheRestToTheStaff() {
        setup(true, false, true);
        var r = rests();
        assertEquals(r.toString(), 1, r.size());
        assertEquals(.5, r.get(0).durationBeats(), 0);
    }

    @Test
    public void aThinStaffEdgeCannotMakeTheEighthRestTailTooBroad() {
        setup(false, true, true);
        for (int x = 173; x <= 182; x++) gray[122 * W + x] = 0;
        var r = rests();
        assertEquals(r.toString(), 1, r.size());
        assertEquals(.5, r.get(0).durationBeats(), 0);
    }

    @Test
    public void aBulbWithoutItsDescendingTailIsNotARest() {
        setup(false, true, false);
        assertTrue(rests().isEmpty());
    }

    @Test
    public void aRuleWithoutAGlyphDoesNotCreateSilence() {
        setup(true, false, false);
        for (int y = 94; y <= 106; y++)
            for (int x = 170; x <= 184; x++) gray[y * W + x] = (byte) 255;
        assertTrue(rests().isEmpty());
    }

    @Test
    public void sourcePixelsRemainUnchanged() {
        setup(true, false, true);
        var before = gray.clone();
        rests();
        assertArrayEquals(before, gray);
    }

    @Test
    public void roundedScanBoundaryRetainsTheFirstBulbRow() {
        Arrays.fill(gray, (byte) 255);
        for (int line = 0; line < 5; line++)
            for (int x = 20; x <= 780; x++) gray[Math.round(80 + line * 14.25f) * W + x] = 0;
        ellipse(97, 6, 5);
        for (int y = 92; y <= 123; y++) {
            int x = 185 - (y - 92) * 10 / 31;
            gray[y * W + x] = 0;
            gray[y * W + x + 1] = 0;
        }
        var detected = rests();
        assertEquals(detected.toString(), 1, detected.size());
        assertEquals(.5, detected.get(0).durationBeats(), 0);
    }

    private List<ScoreRestEvent> beneathBeam(boolean connected, boolean bothStems) {
        return beneathBeam(connected, bothStems, false);
    }

    private List<ScoreRestEvent> beneathBeam(
            boolean connected, boolean bothStems, boolean sloping) {
        return beneathBeam(connected, bothStems, sloping, true);
    }

    private List<ScoreRestEvent> beneathBeam(
            boolean connected, boolean bothStems, boolean sloping, boolean above) {
        setup(false, false, true);
        byte[] original = gray.clone();
        for (int y = 94; y <= 124; y++)
            for (int x = 170; x <= 188; x++) gray[y * W + x] = (byte) 255;
        for (int y = 80; y <= 137; y += 14) for (int x = 170; x <= 188; x++) gray[y * W + x] = 0;
        for (int y = 94; y <= 124; y++)
            for (int x = 170; x <= 188; x++)
                if ((original[y * W + x] & 255) < 170 && y != 109 && y != 123)
                    gray[(y + 14) * W + x] = original[y * W + x];
        int first = above ? 138 : 122, last = above ? 228 : 212, noteY = above ? 145 : 95;
        int beamStart = above ? (sloping ? 86 : 90) : 160, beamEnd = beamStart + (sloping ? 12 : 0);
        for (int x = first; x <= last; x++)
            if (connected || x < 160 || x > 200) {
                int start =
                        Math.round(
                                beamStart
                                        + (x - first)
                                                * (beamEnd - beamStart)
                                                / (float) (last - first));
                for (int y = start; y < start + 4; y++) gray[y * W + x] = 0;
            }
        for (int y = Math.min(beamStart, noteY); y <= Math.max(beamStart, noteY); y++)
            gray[y * W + first] = 0;
        if (bothStems)
            for (int y = Math.min(beamEnd, noteY); y <= Math.max(beamEnd, noteY); y++)
                gray[y * W + last] = 0;
        var a = new ScoreNoteEvent(0, 130 / 800f, 0, 0, 1, noteY / 240f, false, 0, 1, 2, 0);
        var b = new ScoreNoteEvent(0, 220 / 800f, 0, 0, 1, noteY / 240f, false, 0, 1, 2, 0);
        return SixteenthRestDetector.detect(
                        gray,
                        W,
                        H,
                        List.of(new MeasureRegion(0, 1, 0, 1)),
                        List.of(new SixteenthRestDetector.Staff(80, 137, 14.25f, 0, 1)),
                        List.of(a, b))
                .stream()
                .filter(r -> Math.abs(r.positionInMeasure() - 180 / 800f) < .02f)
                .toList();
    }

    @Test
    public void lowerRestCanInterruptIndependentlyPrintedBeam() {
        assertTrue(beneathBeam(true, true).stream().anyMatch(r -> r.durationBeats() == .5));
    }

    @Test
    public void detachedStrokeCannotAuthorizeLowerRest() {
        var r = beneathBeam(false, true);
        assertTrue(r.toString(), r.isEmpty());
    }

    @Test
    public void beamWithoutBothStemsCannotAuthorizeLowerRest() {
        var r = beneathBeam(true, false);
        assertTrue(r.toString(), r.isEmpty());
    }

    @Test
    public void slopingBeamCanContainIndependentLowerRest() {
        assertTrue(beneathBeam(true, true, true).stream().anyMatch(r -> r.durationBeats() == .5));
    }

    @Test
    public void separatedSlopingSegmentsCannotAuthorizeRest() {
        assertTrue(beneathBeam(false, true, true).isEmpty());
    }

    @Test
    public void slopingBeamStillNeedsBothPrintedShafts() {
        assertTrue(beneathBeam(true, false, true).isEmpty());
    }

    @Test
    public void downStemmedBeamCanContainRest() {
        assertTrue(
                beneathBeam(true, true, false, false).stream()
                        .anyMatch(r -> r.durationBeats() == .5));
    }

    @Test
    public void brokenDownStemmedBeamCannotAuthorizeRest() {
        assertTrue(beneathBeam(false, true, false, false).isEmpty());
    }

    private List<ScoreRestEvent> ordinaryMovingVoice(boolean printStem, boolean below) {
        setup(false, false, true);
        int noteY = below ? 157 : 70;
        if (printStem)
            for (int y = noteY; y <= noteY + 45; y++)
                for (int x = 169; x <= 170; x++) gray[y * W + x] = 0;
        var note = new ScoreNoteEvent(0, 177 / 800f, 0, 0, 1, noteY / 240f, false, 0, 1, 2, 0);
        return SixteenthRestDetector.detect(
                gray,
                W,
                H,
                List.of(new MeasureRegion(0, 1, 0, 1)),
                List.of(new SixteenthRestDetector.Staff(80, 137, 14.25f, 0, 1)),
                List.of(note));
    }

    @Test
    public void ordinaryRestCanShareColumnWithProvedLowerMovingVoice() {
        assertEquals(1, ordinaryMovingVoice(true, true).size());
    }

    @Test
    public void unprovedLowerMovingVoiceCannotAuthorizeRest() {
        assertTrue(ordinaryMovingVoice(false, true).isEmpty());
    }

    @Test
    public void ownDownwardFlagCannotAuthorizeOrdinaryRest() {
        assertTrue(ordinaryMovingVoice(true, false).isEmpty());
    }

    @Test
    public void lowerBeamDoesNotJoinAnOrdinaryRestToUnrelatedColumns() {
        setup(false, false, true);
        for (int x = 130; x <= 220; x++) for (int y = 133; y <= 136; y++) gray[y * W + x] = 0;
        var r = rests();
        assertEquals(r.toString(), 1, r.size());
        assertEquals(.5, r.get(0).durationBeats(), 0);
    }

    @Test
    public void completePairedBulbsRemainSixteenthBesideAnAlteredNote() {
        Arrays.fill(gray, (byte) 255);
        for (int y = 80; y <= 144; y += 16) for (int x = 20; x <= 780; x++) gray[y * W + x] = 0;
        ellipse(102, 6, 5);
        ellipse(118, 6, 5);
        for (int y = 99; y <= 144; y++) {
            int x = 185 - (y - 99) * 12 / 45;
            gray[y * W + x] = 0;
            gray[y * W + x + 1] = 0;
        }
        var altered = new ScoreNoteEvent(0, 208 / 800f, 0, 0, 1, 112 / 240f, false, 0, 1, 1, 0);
        var r =
                SixteenthRestDetector.detect(
                        gray,
                        W,
                        H,
                        List.of(new MeasureRegion(0, 1, 0, 1)),
                        List.of(new SixteenthRestDetector.Staff(80, 144, 16, 0, 1)),
                        List.of(altered));
        assertEquals(r.toString(), 1, r.size());
        assertEquals(.25, r.get(0).durationBeats(), 0);
    }

    private List<ScoreRestEvent> noteDotBelowRest(boolean ownNote) {
        setup(false, false, true);
        ellipse(137, 3, 3);
        for (int y = 157; y <= 202; y++) for (int x = 169; x <= 170; x++) gray[y * W + x] = 0;
        var note = new ScoreNoteEvent(0, 177 / 800f, 0, 0, 1, 157 / 240f, false, 0, 1, 2, 0);
        return SixteenthRestDetector.detect(
                gray,
                W,
                H,
                List.of(new MeasureRegion(0, 1, 0, 1)),
                List.of(new SixteenthRestDetector.Staff(80, 137, 14.25f, 0, 1)),
                ownNote ? List.of(note) : List.of());
    }

    @Test
    public void lowerNotesStaccatoDoesNotLengthenTheRest() {
        var r = noteDotBelowRest(true);
        assertEquals(r.toString(), 1, r.size());
        assertEquals(.5, r.get(0).durationBeats(), 0);
    }
}
