// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original shifted rectangles, independent voices and beam-fragment controls. */
public class ShiftedRectangleRestTest {
    private static final int W = 400, H = 240;
    private final byte[] gray = new byte[W * H];

    public ShiftedRectangleRestTest() {
        Arrays.fill(gray, (byte) 245);
        for (int y = 60; y <= 124; y += 16) box(20, y, 380, y);
    }

    private void box(int l, int t, int r, int b) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) gray[y * W + x] = 0;
    }

    private List<ScoreRestEvent> read() {
        return SixteenthRestDetector.detect(
                gray,
                W,
                H,
                List.of(new MeasureRegion(0, 1, .05f, .95f)),
                List.of(new SixteenthRestDetector.Staff(60, 124, 16, 0, 1)),
                List.of());
    }

    @Test
    public void halfRestBelowOrdinaryPositionIsRetained() {
        box(100, 116, 119, 123);
        var r = read();
        assertEquals(r.toString(), 1, r.size());
        assertEquals(2, r.get(0).durationBeats(), 0);
    }

    @Test
    public void wholeRestBelowOrdinaryPositionIsRetained() {
        box(100, 125, 119, 132);
        var r = read();
        assertEquals(r.toString(), 1, r.size());
        assertEquals(4, r.get(0).durationBeats(), 0);
    }

    @Test
    public void separateVoicesCanRestAtSameHorizontalPosition() {
        box(100, 68, 119, 75);
        box(100, 116, 119, 123);
        var r = read();
        assertEquals(r.toString(), 2, r.size());
        assertTrue(r.stream().allMatch(v -> v.durationBeats() == 2));
    }

    @Test
    public void downwardStemAndFlagAreNotWholeRest() {
        box(100, 125, 119, 132);
        box(100, 86, 101, 132);
        assertTrue(read().toString(), read().isEmpty());
    }

    @Test
    public void detachedBeamLacksSupportingRule() {
        box(100, 132, 119, 139);
        assertTrue(read().toString(), read().isEmpty());
    }

    @Test
    public void sourcePixelsArePreserved() {
        box(100, 116, 119, 123);
        byte[] saved = gray.clone();
        read();
        assertArrayEquals(saved, gray);
    }

    @Test
    public void upwardVoiceDoesNotBorrowLowVoiceRest() {
        assertFalse(OmrScoreInterpreter.restSharesStemVoice(124, 92, 16, -1));
        assertTrue(OmrScoreInterpreter.restSharesStemVoice(60, 92, 16, -1));
    }

    @Test
    public void downwardVoiceDoesNotBorrowHighVoiceRest() {
        assertFalse(OmrScoreInterpreter.restSharesStemVoice(60, 92, 16, 1));
        assertTrue(OmrScoreInterpreter.restSharesStemVoice(124, 92, 16, 1));
    }

    @Test
    public void unprovedVoiceKeepsOrdinaryRestTiming() {
        assertTrue(OmrScoreInterpreter.restSharesStemVoice(124, 92, 16, 0));
        assertTrue(OmrScoreInterpreter.restSharesStemVoice(60, 92, 16, 0));
    }

    @Test
    public void sameColumnRestCannotDelayAnIndependentQuarter() {
        var note = new ScoreNoteEvent(0, .2f, 10, 0, 1, .3f, false, 0, 0, 2, 1);
        assertFalse(
                OmrScoreInterpreter.restIsSeparateAttack(
                        new ScoreRestEvent(0, .19f, .5f, .1f, 0, 1, .5), note));
        assertFalse(
                OmrScoreInterpreter.restIsSeparateAttack(
                        new ScoreRestEvent(0, .21f, .5f, .1f, 0, 1, .5), note));
        assertTrue(
                OmrScoreInterpreter.restIsSeparateAttack(
                        new ScoreRestEvent(0, .1f, .5f, .1f, 0, 1, .5), note));
        assertTrue(
                OmrScoreInterpreter.restIsSeparateAttack(
                        new ScoreRestEvent(0, .3f, .5f, .1f, 0, 1, .5), note));
    }

    @Test
    public void completeSixteenthWinsOverItsClippedLowerBulb() throws Exception {
        var clipped = new ScoreRestEvent(0, .49f, .51f, .018f, 0, 1, .5);
        var complete = new ScoreRestEvent(0, .5f, .5f, .03f, 0, 1, .25);
        var method =
                SixteenthRestDetector.class.getDeclaredMethod("collected", List.class, List.class);
        method.setAccessible(true);
        var result =
                (SixteenthRestDetector.Detection)
                        method.invoke(
                                null,
                                new java.util.ArrayList<>(List.of(clipped, complete)),
                                List.of());
        assertEquals(List.of(complete), result.rests());
    }

    @Test
    public void collectionDoesNotMergeVerticallySeparateRestVoices() throws Exception {
        var upper = new ScoreRestEvent(0, .5f, .4f, .03f, 0, 1, 1);
        var lower = new ScoreRestEvent(0, .5f, .46f, .03f, 0, 1, 1);
        var method =
                SixteenthRestDetector.class.getDeclaredMethod("collected", List.class, List.class);
        method.setAccessible(true);
        var result =
                (SixteenthRestDetector.Detection)
                        method.invoke(
                                null, new java.util.ArrayList<>(List.of(upper, lower)), List.of());
        assertEquals(List.of(upper, lower), result.rests());
    }

    @Test
    public void roundedShortHookKeepsItsFullQuarterZigzag() {
        assertTrue(SixteenthRestDetector.shortQuarterHook(8, 10, 5.5, 7.5, 6.2, 6, 16));
    }

    @Test
    public void roundedHookStillRequiresUpperZigzag() {
        assertFalse(SixteenthRestDetector.shortQuarterHook(10, 10, 5.5, 7.5, 6.2, 6, 16));
    }

    @Test
    public void roundedHookStillRequiresLowerReturn() {
        assertFalse(SixteenthRestDetector.shortQuarterHook(8, 10, 5.5, 7.5, 7.5, 7.5, 16));
    }

    @Test
    public void steepContinuationIsNotShortQuarterFoot() {
        assertFalse(SixteenthRestDetector.shortQuarterHook(8, 10, 5.5, 7.5, 6.2, 3, 16));
    }

    @Test
    public void displacedRestKeepsItsPrintedStaffMeasureWhenOutsideVerticalBox() {
        box(100, 125, 119, 132);
        var r =
                SixteenthRestDetector.detect(
                        gray,
                        W,
                        H,
                        List.of(new MeasureRegion(0, 1, .2f, .52f)),
                        List.of(new SixteenthRestDetector.Staff(60, 124, 16, 0, 1)),
                        List.of());
        assertEquals(r.toString(), 1, r.size());
        assertEquals(4, r.get(0).durationBeats(), 0);
    }

    @Test
    public void nearbyMeasureWithoutPrintedStaffCannotOwnDisplacedRest() {
        box(100, 125, 119, 132);
        var r =
                SixteenthRestDetector.detect(
                        gray,
                        W,
                        H,
                        List.of(new MeasureRegion(0, 1, .52f, .8f)),
                        List.of(new SixteenthRestDetector.Staff(60, 124, 16, 0, 1)),
                        List.of());
        assertTrue(r.toString(), r.isEmpty());
    }
}
