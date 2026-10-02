// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original zigzag and straight-tail rest profiles at independently varied scales. */
public class RoundedQuarterNotchTest {
    @Test
    public void smallRoundedUpperNotchKeepsTheCompleteHook() {
        assertTrue(SixteenthRestDetector.shortQuarterHook(6, 8, 6, 7.2, 6, 6.6, 12));
    }

    @Test
    public void mediumRoundedUpperNotchKeepsTheCompleteHook() {
        assertTrue(SixteenthRestDetector.shortQuarterHook(6, 10, 7.4, 9, 7.5, 8, 16));
    }

    @Test
    public void largeRoundedUpperNotchKeepsTheCompleteHook() {
        assertTrue(SixteenthRestDetector.shortQuarterHook(10, 16, 12, 14.5, 12, 13, 24));
    }

    @Test
    public void absentUpperNotchIsRejected() {
        assertFalse(SixteenthRestDetector.shortQuarterHook(6, 10, 9.8, 11.2, 9.8, 10.4, 16));
    }

    @Test
    public void absentInitialRiseIsRejected() {
        assertFalse(SixteenthRestDetector.shortQuarterHook(10, 10, 7.4, 9, 7.5, 8, 16));
    }

    @Test
    public void absentLowerHookIsRejected() {
        assertFalse(SixteenthRestDetector.shortQuarterHook(6, 10, 7.4, 9, 9, 9, 16));
    }

    @Test
    public void disconnectedFootIsRejected() {
        assertFalse(SixteenthRestDetector.shortQuarterHook(6, 10, 7.4, 9, 7.5, 3, 16));
    }

    @Test
    public void subpixelSizedGlyphIsRejected() {
        assertFalse(SixteenthRestDetector.shortQuarterHook(6, 10, 7.4, 9, 7.5, 8, 3));
    }

    @Test
    public void nanScaleIsRejected() {
        assertFalse(SixteenthRestDetector.shortQuarterHook(6, 10, 7.4, 9, 7.5, 8, Float.NaN));
    }

    @Test
    public void infiniteScaleIsRejected() {
        assertFalse(
                SixteenthRestDetector.shortQuarterHook(
                        6, 10, 7.4, 9, 7.5, 8, Float.POSITIVE_INFINITY));
    }

    @Test
    public void clearlyFormedUnroundedNotchRemainsAccepted() {
        assertTrue(SixteenthRestDetector.shortQuarterHook(6, 10, 6.5, 9, 7.5, 8, 16));
    }
}
