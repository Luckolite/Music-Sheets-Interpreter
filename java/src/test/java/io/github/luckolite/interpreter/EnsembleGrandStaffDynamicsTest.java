// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class EnsembleGrandStaffDynamicsTest {
    private final int w = 500, h = 400;
    private final byte[] gray = new byte[w * h];
    private final List<PlayingTechniqueDetector.Staff> staffs =
            List.of(
                    new PlayingTechniqueDetector.Staff(20, 60, 10, 0, 3),
                    new PlayingTechniqueDetector.Staff(130, 170, 10, 1, 3),
                    new PlayingTechniqueDetector.Staff(260, 300, 10, 2, 3));
    private final List<MeasureRegion> bars = List.of(new MeasureRegion(.3f, .95f, 0, .8f));

    public EnsembleGrandStaffDynamicsTest() {
        Arrays.fill(gray, (byte) 255);
        brace();
    }

    private void brace() {
        for (int y = 130; y <= 300; y++) {
            double t = (y - 130) / 170.0;
            int x = 90 + (int) Math.round(7 * Math.abs(Math.sin(2 * Math.PI * t)));
            if (t > .25 && t < .75)
                x = 90 + (int) Math.round(7 * Math.sin(Math.abs(t - .5) * Math.PI * 2));
            for (int dx = 0; dx < 2; dx++) gray[y * w + x + dx] = 0;
        }
    }

    private List<ScoreDynamicChange> detect(List<PlayingTechniqueDetector.Word> words) {
        return ScoreDynamicsDetector.detect(words, staffs, bars, List.of(), gray, w, h);
    }

    private PlayingTechniqueDetector.Word word(String text, float x, float top) {
        return new PlayingTechniqueDetector.Word(text, x, top, x + .04f, top + .025f);
    }

    @Test
    public void braceInsideEnsembleIsRecognized() {
        var pairs = GrandStaffDynamics.bracedPairs(staffs, bars, gray, w, h);
        assertEquals(1, pairs.size());
        assertEquals(1, pairs.get(0).upper().index());
        assertEquals(2, pairs.get(0).lower().index());
    }

    @Test
    public void pianoLevelReachesBothHandsButNotSoloist() {
        var found = detect(List.of(word("p", .4f, .5f)));
        assertEquals(2, found.size());
        assertEquals(
                Set.of(1, 2),
                new HashSet<>(found.stream().map(ScoreDynamicChange::staffIndex).toList()));
        assertTrue(
                found.stream()
                        .allMatch(
                                c -> c.decibels() == -8 && !c.sharedStaffs() && !c.sharedTiming()));
    }

    @Test
    public void soloLevelRemainsLocal() {
        var found = detect(List.of(word("f", .4f, .19f)));
        assertEquals(1, found.size());
        assertEquals(0, found.get(0).staffIndex());
    }

    @Test
    public void soloLevelDoesNotEndKeyboardCrescendo() {
        var found =
                detect(
                        List.of(
                                word("cresc.", .4f, .5f),
                                word("f", .6f, .19f),
                                word("ff", .8f, .5f)));
        var ramps = found.stream().filter(c -> c.direction() == 1).toList();
        assertEquals(2, ramps.size());
        for (var c : ramps) assertEquals((.8f - .3f) / (.95f - .3f), c.endPosition(), .0001f);
    }

    @Test
    public void noBraceDoesNotShare() {
        Arrays.fill(gray, (byte) 255);
        var found = detect(List.of(word("p", .4f, .5f)));
        assertEquals(1, found.size());
    }

    @Test
    public void straightBracketDoesNotShare() {
        Arrays.fill(gray, (byte) 255);
        for (int y = 130; y <= 300; y++) gray[y * w + 90] = 0;
        for (int x = 90; x < 102; x++) {
            gray[130 * w + x] = 0;
            gray[300 * w + x] = 0;
        }
        assertTrue(GrandStaffDynamics.bracedPairs(staffs, bars, gray, w, h).isEmpty());
    }

    @Test
    public void separateSystemsCannotBecomePair() {
        var rows =
                List.of(
                        new PlayingTechniqueDetector.Staff(20, 60, 10, 1, 2),
                        new PlayingTechniqueDetector.Staff(130, 170, 10, 0, 2));
        assertTrue(GrandStaffDynamics.bracedPairs(rows, bars, gray, w, h).isEmpty());
    }
}
