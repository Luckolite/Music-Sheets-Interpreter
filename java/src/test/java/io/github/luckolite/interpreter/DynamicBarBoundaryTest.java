// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic measure geometry, without score or font fixtures. */
public class DynamicBarBoundaryTest {
    private ScoreDynamicChange detect(float left, float right) {
        var staffs = List.of(new PlayingTechniqueDetector.Staff(80, 120, 10, 0, 1));
        var measures =
                List.of(
                        new MeasureRegion(.1f, .495f, .15f, .35f),
                        new MeasureRegion(.505f, .9f, .15f, .35f));
        var word = new PlayingTechniqueDetector.Word("pp", left, .34f, right, .38f);
        var found =
                ScoreDynamicsDetector.detect(
                        List.of(word), staffs, measures, List.of(), null, 1000, 400);
        assertEquals(1, found.size());
        return found.get(0);
    }

    @Test
    public void rightCenteredDynamicOverhangBelongsToNextMeasure() {
        var found = detect(.494f, .522f);
        assertEquals(1, found.measureIndex());
        assertEquals((.508f - .505f) / (.9f - .505f), found.positionInMeasure(), .00001f);
    }

    @Test
    public void dynamicCenteredBeforeBarRemainsInPriorMeasure() {
        assertEquals(0, detect(.478f, .51f).measureIndex());
    }

    @Test
    public void wideBoxDoesNotMoveDynamicAcrossBar() {
        assertEquals(0, detect(.481f, .537f).measureIndex());
    }

    @Test
    public void ordinaryDynamicKeepsExistingLeftAnchor() {
        var found = detect(.55f, .58f);
        assertEquals(1, found.measureIndex());
        assertEquals((.55f - .505f) / (.9f - .505f), found.positionInMeasure(), .00001f);
    }
}
