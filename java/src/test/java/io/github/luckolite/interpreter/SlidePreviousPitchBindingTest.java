// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic lines and independent attack endpoints, without score imagery. */
public class SlidePreviousPitchBindingTest {
    private static final int W = 360, H = 240;

    private List<NoteSlideDetector.Stroke> detect(
            double slope, List<NoteSlideDetector.Head> extras) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        int left = 164, right = 205;
        float sourceY = 90;
        for (int x = left; x <= right; x++) {
            int y = (int) Math.round(sourceY + (x - 150) * slope);
            gray[y * W + x] = 0;
            gray[(y + 1) * W + x] = 0;
        }
        var heads = new ArrayList<>(extras);
        heads.add(new NoteSlideDetector.Head(218, (float) (sourceY + 68 * slope), 12, 0, 0));
        return NoteSlideDetector.detect(gray, W, H, List.of(), heads);
    }

    private NoteSlideDetector.Head prior(float y) {
        return new NoteSlideDetector.Head(150, y, 12, 0, 0);
    }

    @Test
    public void shallowPrintedEndpointsBindPreviousPitch() {
        var found = detect(.45, List.of(prior(90)));
        assertEquals(1, found.size());
        assertTrue(found.get(0).connected());
    }

    @Test
    public void descendingPrintedEndpointsBindPreviousPitch() {
        var found = detect(.7, List.of(prior(90)));
        assertEquals(1, found.size());
        assertEquals(-1, found.get(0).direction());
        assertTrue(found.get(0).connected());
    }

    @Test
    public void ascendingPrintedEndpointsBindPreviousPitch() {
        var found = detect(-.7, List.of(prior(90)));
        assertEquals(1, found.size());
        assertEquals(1, found.get(0).direction());
        assertTrue(found.get(0).connected());
    }

    @Test
    public void absenceOfSourceHeadKeepsAnIsolatedApproach() {
        var found = detect(.7, List.of());
        assertEquals(1, found.size());
        assertFalse(found.get(0).connected());
    }

    @Test
    public void sourceFromAnotherStaffCannotBindPitch() {
        var found = detect(.7, List.of(new NoteSlideDetector.Head(150, 90, 12, 1, 0)));
        assertEquals(1, found.size());
        assertFalse(found.get(0).connected());
    }

    @Test
    public void sourceFromAnotherMeasureCannotBindPitch() {
        var found = detect(.7, List.of(new NoteSlideDetector.Head(150, 90, 12, 0, 1)));
        assertEquals(1, found.size());
        assertFalse(found.get(0).connected());
    }

    @Test
    public void mismatchedSourceEndpointCannotBindPitch() {
        var found = detect(.7, List.of(prior(60)));
        assertEquals(1, found.size());
        assertFalse(found.get(0).connected());
    }

    @Test
    public void earlierMatchingHeadCannotSkipAnInterveningAttack() {
        var found = detect(.7, List.of(prior(90), new NoteSlideDetector.Head(160, 180, 12, 0, 0)));
        assertEquals(1, found.size());
        assertFalse(found.get(0).connected());
    }

    @Test
    public void ambiguousSourceChordCannotChooseAnArbitraryPitch() {
        var found = detect(.7, List.of(prior(90), prior(100)));
        assertEquals(1, found.size());
        assertFalse(found.get(0).connected());
    }

    @Test
    public void unrelatedPartDoesNotDisplaceImmediateSource() {
        var found = detect(.7, List.of(prior(90), new NoteSlideDetector.Head(160, 180, 12, 1, 0)));
        assertEquals(1, found.size());
        assertTrue(found.get(0).connected());
    }
}
