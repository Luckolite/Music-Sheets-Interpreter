// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometry: an italic mark ends at the first bar while its left serif overhangs. */
public class MarginDynamicAnchorTest {
    private final List<MeasureRegion> bars = List.of(new MeasureRegion(.3f, .9f, .2f, .45f));
    private final List<PlayingTechniqueDetector.Staff> staffs =
            List.of(new PlayingTechniqueDetector.Staff(60, 100, 10, 0, 1));

    @Test
    public void entireLevelInTheMarginBelongsToTheOpeningBar() {
        var word = new PlayingTechniqueDetector.Word("mp", .24f, .38f, .298f, .44f);
        var changes =
                ScoreDynamicsDetector.detect(
                        List.of(word), staffs, bars, List.of(), null, 600, 300);
        assertEquals(1, changes.size());
        assertEquals(0, changes.get(0).measureIndex());
        assertEquals(0, changes.get(0).positionInMeasure(), 0);
        assertEquals(-4, changes.get(0).decibels(), 0);
    }

    @Test
    public void distantLevelIsNotPulledAcrossAnEmptyMargin() {
        var word = new PlayingTechniqueDetector.Word("mp", .13f, .38f, .198f, .44f);
        assertTrue(
                ScoreDynamicsDetector.detect(List.of(word), staffs, bars, List.of(), null, 600, 300)
                        .isEmpty());
    }
}
