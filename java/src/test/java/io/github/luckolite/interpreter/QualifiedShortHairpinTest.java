// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original raster wedges beside an explicit dynamic, with accent rejection controls. */
public class QualifiedShortHairpinTest {
    private static final int W = 400, H = 240;

    private byte[] page(int direction, int span) {
        var gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int x = 130; x <= 130 + span; x++) {
            int opening =
                    (int)
                            Math.round(
                                    7 * (direction > 0 ? x - 130 : 130 + span - x) / (double) span);
            gray[(132 - opening) * W + x] = 0;
            gray[(132 + opening) * W + x] = 0;
        }
        for (int y = 126; y < 138; y++) for (int x = 110; x < 117; x++) gray[y * W + x] = 0;
        return gray;
    }

    private PlayingTechniqueDetector.Word word(String text, int left, int top) {
        return new PlayingTechniqueDetector.Word(
                text,
                left / (float) W,
                top / (float) H,
                (left + 8) / (float) W,
                (top + 12) / (float) H);
    }

    private List<ScoreDynamicChange> ramps(byte[] gray, PlayingTechniqueDetector.Word word) {
        return ScoreDynamicsDetector.detect(
                        word == null ? List.of() : List.of(word),
                        List.of(new PlayingTechniqueDetector.Staff(60, 100, 10, 0, 1)),
                        List.of(new MeasureRegion(.1f, .9f, .2f, .45f)),
                        List.of(),
                        gray,
                        W,
                        H)
                .stream()
                .filter(c -> c.direction() != 0)
                .toList();
    }

    @Test
    public void shortOpenCrescendoBesidePrintedLevelSurvives() {
        var result = ramps(page(1, 40), word("p", 110, 126));
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).direction());
    }

    @Test
    public void shortOpenDiminuendoBesidePrintedLevelSurvives() {
        var result = ramps(page(-1, 40), word("mf", 110, 126));
        assertEquals(1, result.size());
        assertEquals(-1, result.get(0).direction());
    }

    @Test
    public void isolatedWideAccentRemainsRejected() {
        assertTrue(ramps(page(1, 40), null).isEmpty());
    }

    @Test
    public void narrowAccentBesideLevelRemainsRejected() {
        assertTrue(ramps(page(1, 18), word("p", 110, 126)).isEmpty());
    }

    @Test
    public void unprintedOcrLevelCannotAuthorizeWedge() {
        var gray = page(1, 40);
        for (int y = 126; y < 138; y++)
            for (int x = 110; x < 118; x++) gray[y * W + x] = (byte) 255;
        assertTrue(ramps(gray, word("p", 110, 126)).isEmpty());
    }

    @Test
    public void distantLevelCannotAuthorizeWedge() {
        assertTrue(ramps(page(1, 40), word("p", 50, 126)).isEmpty());
    }

    @Test
    public void levelOnDifferentVerticalRowCannotAuthorizeWedge() {
        var gray = page(1, 40);
        for (int y = 155; y < 167; y++) for (int x = 110; x < 117; x++) gray[y * W + x] = 0;
        assertTrue(ramps(gray, word("p", 110, 155)).isEmpty());
    }

    @Test
    public void styleWordCannotAuthorizeWedge() {
        assertTrue(ramps(page(1, 40), word("dolce", 110, 126)).isEmpty());
    }

    @Test
    public void paddedLevelBoxCanIncludeFollowingHairpin() {
        var padded = new PlayingTechniqueDetector.Word("p", 110f / W, 125f / H, 171f / W, 140f / H);
        assertEquals(1, ramps(page(1, 40), padded).size());
    }

    @Test
    public void hairpinInkAloneCannotCorroboratePaddedLevel() {
        var gray = page(1, 40);
        for (int y = 126; y < 138; y++)
            for (int x = 110; x < 118; x++) gray[y * W + x] = (byte) 255;
        var padded = new PlayingTechniqueDetector.Word("p", 110f / W, 125f / H, 171f / W, 140f / H);
        assertTrue(ramps(gray, padded).isEmpty());
    }

    @Test
    public void filledWedgeBesideLevelRemainsRejected() {
        var gray = page(1, 40);
        for (int x = 130; x <= 170; x++) {
            int opening = (int) Math.round(7 * (x - 130) / 40.0);
            for (int y = 132 - opening; y <= 132 + opening; y++) gray[y * W + x] = 0;
        }
        assertTrue(ramps(gray, word("p", 110, 126)).isEmpty());
    }

    @Test
    public void remoteOversizedOcrBoxCannotBorrowNearbyLevelInk() {
        var padded = new PlayingTechniqueDetector.Word("p", 50f / W, 125f / H, 171f / W, 140f / H);
        assertTrue(ramps(page(1, 40), padded).isEmpty());
    }
}
