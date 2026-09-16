// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class ScorePageTimelineTest {
    @Test public void missedMeasureOneKeepsUnknownPageWithRestartedPart() {
        int[] counts = {30, 28, 15, 12, 10, 11};
        int[] starts = {1, 31, 59, 0, 10, 20};
        var firstPart = new ScorePageTimeline.PageRange(0, 3);
        var restartedPart = new ScorePageTimeline.PageRange(3, 6);

        assertEquals(List.of(firstPart, restartedPart),
                ScorePageTimeline.arrangements(counts, starts));
        assertEquals(restartedPart, ScorePageTimeline.selectedArrangement(counts, starts, 3));
        assertEquals(3, ScorePageTimeline.pageForMeasure(0, counts, starts, restartedPart));
    }

    @Test public void ordinaryUnknownContinuationDoesNotCreateAnotherPart() {
        assertEquals(List.of(new ScorePageTimeline.PageRange(0, 3)),
                ScorePageTimeline.arrangements(
                        new int[]{12, 8, 7}, new int[]{1, 0, 21}));
    }
}
