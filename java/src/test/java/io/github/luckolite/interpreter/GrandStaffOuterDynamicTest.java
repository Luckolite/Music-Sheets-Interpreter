// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original keyboard brace and independently assigned direction owners. */
public class GrandStaffOuterDynamicTest {
    private final PlayingTechniqueDetector.Staff upper =
            new PlayingTechniqueDetector.Staff(100, 140, 10, 1, 3);
    private final PlayingTechniqueDetector.Staff lower =
            new PlayingTechniqueDetector.Staff(220, 260, 10, 2, 3);
    private final List<GrandStaffDynamics.Pair> pairs =
            List.of(new GrandStaffDynamics.Pair(upper, lower));

    @Test
    public void outerKeyboardDynamicUsesTheBracePart() {
        assertEquals(upper, GrandStaffDynamics.directionPart(pairs, lower, 302, 326));
        assertEquals(upper, GrandStaffDynamics.directionPart(pairs, lower, 335, 348));
    }

    @Test
    public void refinedPrintedRailsKeepTheirPhysicalPart() {
        var refined = new PlayingTechniqueDetector.Staff(220.5f, 260.5f, 10.01f, 2, 3);
        assertEquals(upper, GrandStaffDynamics.directionPart(pairs, refined, 302, 326));
    }

    @Test
    public void soloistAndNextSystemsDoNotInheritTheKeyboardDynamic() {
        assertNull(
                GrandStaffDynamics.directionPart(
                        pairs, new PlayingTechniqueDetector.Staff(20, 60, 10, 0, 3), 302, 326));
        assertNull(
                GrandStaffDynamics.directionPart(
                        pairs, new PlayingTechniqueDetector.Staff(500, 540, 10, 2, 3), 570, 580));
        assertNull(GrandStaffDynamics.directionPart(List.of(), lower, 302, 326));
    }

    @Test
    public void insideOrAboveTheOuterStaffIsNotAnOuterPartMarking() {
        assertNull(GrandStaffDynamics.directionPart(pairs, lower, 245, 258));
        assertNull(GrandStaffDynamics.directionPart(pairs, upper, 70, 90));
        assertNull(GrandStaffDynamics.directionPart(pairs, lower, Float.NaN, 320));
    }

    @Test
    public void betweenStaffSharingAndAmbiguityArePreserved() {
        assertEquals(upper, GrandStaffDynamics.directionPart(pairs, lower, 180, 200));
        assertNull(
                GrandStaffDynamics.directionPart(
                        List.of(pairs.get(0), pairs.get(0)), lower, 302, 326));
    }

    @Test
    public void detectorSharesOuterForteOnlyAcrossTheProvedPianoPair() {
        int w = 500, h = 400;
        byte[] gray = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int y = 100; y <= 260; y++) {
            double t = (y - 100) / 160.0;
            int x = 90 + (int) Math.round(7 * Math.abs(Math.sin(2 * Math.PI * t)));
            if (t > .25 && t < .75)
                x = 90 + (int) Math.round(7 * Math.sin(Math.abs(t - .5) * Math.PI * 2));
            gray[y * w + x] = gray[y * w + x + 1] = 0;
        }
        var staffs = List.of(new PlayingTechniqueDetector.Staff(20, 60, 10, 0, 3), upper, lower);
        var bars = List.of(new MeasureRegion(.3f, .95f, 0, 1));
        var word = new PlayingTechniqueDetector.Word("f", .4f, .755f, .44f, .805f);
        var found =
                ScoreDynamicsDetector.detect(List.of(word), staffs, bars, List.of(), gray, w, h);
        assertEquals(
                Set.of(1, 2),
                new HashSet<>(found.stream().map(ScoreDynamicChange::staffIndex).toList()));
        assertTrue(found.stream().allMatch(c -> c.decibels() == 3 && !c.sharedStaffs()));
    }
}
