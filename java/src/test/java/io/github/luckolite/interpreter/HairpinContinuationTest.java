// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class HairpinContinuationTest {
    static final int W = 600, H = 500;

    static byte[] page() {
        var p = new byte[W * H];
        Arrays.fill(p, (byte) 255);
        return p;
    }

    static void line(byte[] p, int left, int right, int y0, int y1) {
        for (int x = left; x <= right; x++)
            p[(int) Math.round(y0 + (y1 - y0) * (x - left) / (double) (right - left)) * W + x] = 0;
    }

    static List<PlayingTechniqueDetector.Staff> staffs(int secondIndex) {
        return List.of(
                new PlayingTechniqueDetector.Staff(60, 100, 10, 0, 1),
                new PlayingTechniqueDetector.Staff(260, 300, 10, secondIndex, 1));
    }

    static List<MeasureRegion> bars() {
        return List.of(
                new MeasureRegion(.1f, .9f, .1f, .22f),
                new MeasureRegion(.1f, .5f, .5f, .62f),
                new MeasureRegion(.5f, .9f, .5f, .62f));
    }

    static List<ScoreDynamicChange> detect(byte[] p, int lane, boolean forte) {
        return ScoreDynamicsDetector.detect(
                forte
                        ? List.of(new PlayingTechniqueDetector.Word("f", .51f, .67f, .54f, .70f))
                        : List.of(),
                staffs(lane),
                bars(),
                List.of(),
                p,
                W,
                H);
    }

    static void wedge(byte[] p, int center) {
        line(p, 360, 540, center, center - 7);
        line(p, 360, 540, center, center + 7);
    }

    static void arms(byte[] p, int left) {
        line(p, left, 295, 342, 340);
        line(p, left, 295, 357, 359);
    }

    @Test
    public void recoversIsolatedDistantWedge() {
        var p = page();
        wedge(p, 155);
        var r = detect(p, 0, false);
        assertEquals(1, r.size());
        assertEquals(1, r.get(0).direction());
    }

    @Test
    public void connectsNextSystemAndArrivesAtPrintedForte() {
        var p = page();
        wedge(p, 140);
        arms(p, 60);
        var r = detect(p, 0, true);
        var ramp = r.stream().filter(c -> c.direction() == 1).findFirst().orElseThrow();
        assertEquals(2, ramp.endMeasureIndex());
        assertEquals(.025f, ramp.endPosition(), .001);
    }

    @Test
    public void doesNotConnectWrongLane() {
        var p = page();
        wedge(p, 140);
        arms(p, 60);
        var r = detect(p, 1, false);
        assertEquals(0, r.get(0).endMeasureIndex());
    }

    @Test
    public void doesNotConnectMidSystemPair() {
        var p = page();
        wedge(p, 140);
        arms(p, 100);
        var r = detect(p, 0, false);
        assertEquals(0, r.get(0).endMeasureIndex());
    }

    @Test
    public void isolatedParallelRulesAreNotHairpin() {
        var p = page();
        arms(p, 60);
        assertTrue(detect(p, 0, false).isEmpty());
    }

    @Test
    public void veryDistantWedgeRemainsRejected() {
        var p = page();
        wedge(p, 174);
        assertTrue(detect(p, 0, false).isEmpty());
    }

    @Test
    public void generalTextOwnershipIsNotExpanded() {
        var p = page();
        var words = List.of(new PlayingTechniqueDetector.Word("f", .2f, .304f, .24f, .33f));
        assertTrue(
                ScoreDynamicsDetector.detect(words, staffs(0), bars(), List.of(), p, W, H)
                        .isEmpty());
    }

    @Test
    public void thickBeamDoesNotBecomeContinuation() {
        var p = page();
        wedge(p, 140);
        for (int k = 0; k < 5; k++) {
            line(p, 60, 295, 342 + k, 340 + k);
            line(p, 60, 295, 357 + k, 359 + k);
        }
        var r = detect(p, 0, false);
        assertEquals(0, r.get(0).endMeasureIndex());
    }

    @Test
    public void ambiguousThreeRulesDoNotChoosePair() {
        var p = page();
        wedge(p, 140);
        line(p, 60, 295, 340, 340);
        line(p, 60, 295, 353, 353);
        line(p, 60, 295, 359, 359);
        var r = detect(p, 0, false);
        assertEquals(0, r.get(0).endMeasureIndex());
    }

    @Test
    public void explicitLevelAtNewSystemPreventsBlindContinuation() {
        var p = page();
        wedge(p, 140);
        arms(p, 60);
        var r =
                ScoreDynamicsDetector.detect(
                        List.of(new PlayingTechniqueDetector.Word("p", .12f, .66f, .15f, .70f)),
                        staffs(0),
                        bars(),
                        List.of(),
                        p,
                        W,
                        H);
        var ramp = r.stream().filter(c -> c.direction() == 1).findFirst().orElseThrow();
        assertEquals(0, ramp.endMeasureIndex());
    }
}
