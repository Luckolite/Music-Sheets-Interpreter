// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometry: directions below ledger notes must not migrate into the next system. */
public class LedgerDynamicOwnershipTest {
    @Test
    public void ledgerMarginLetterStillSuppliesGlyphBox() {
        int w = 300, h = 300;
        byte[] gray = new byte[w * h];
        java.util.Arrays.fill(gray, (byte) 255);
        for (int y = 208; y < 218; y++) for (int x = 80; x < 90; x++) gray[y * w + x] = 0;
        assertEquals(1, ScoreDynamicsDetector.symbolBoxes(gray, List.of(bass), w, h).size());
    }

    @Test
    public void compactItalicOverhangUsesFollowingBarBody() {
        var bars =
                List.of(
                        new MeasureRegion(.1f, .49f, .05f, .2f),
                        new MeasureRegion(.5f, .9f, .05f, .2f));
        var changes =
                ScoreDynamicsDetector.detect(
                        List.of(new PlayingTechniqueDetector.Word("mf", .494f, .15f, .53f, .158f)),
                        List.of(bass),
                        bars,
                        List.of(),
                        null,
                        1000,
                        1000);
        assertEquals(1, changes.size());
        assertEquals(1, changes.get(0).measureIndex());
    }

    private final PlayingTechniqueDetector.Staff bass =
            new PlayingTechniqueDetector.Staff(100, 140, 10, 1, 2);
    private final PlayingTechniqueDetector.Staff next =
            new PlayingTechniqueDetector.Staff(230, 270, 10, 0, 2);
    private final List<MeasureRegion> measures =
            List.of(
                    new MeasureRegion(.1f, .9f, .05f, .2f),
                    new MeasureRegion(.1f, .9f, .23f, .35f));

    private ScoreNoteEvent low(int measure, int staff, float y) {
        return new ScoreNoteEvent(measure, .5f, -10, staff, 2, y, false, 0, 1, 0, 0, 1);
    }

    @Test
    public void lowLedgerDirectionKeepsPreviousBass() {
        assertEquals(
                bass,
                ScoreDynamicsDetector.directionOwner(
                        List.of(bass, next), 204, 214, List.of(low(0, 1, .185f)), measures, 1000));
    }

    @Test
    public void remoteOtherSystemNotesDoNotMoveOwner() {
        assertEquals(
                next,
                ScoreDynamicsDetector.directionOwner(
                        List.of(bass, next), 204, 214, List.of(low(1, 1, .30f)), measures, 1000));
    }

    @Test
    public void ordinaryBelowStaffDirectionKeepsOwner() {
        assertEquals(
                bass,
                ScoreDynamicsDetector.directionOwner(
                        List.of(bass, next), 150, 158, List.of(low(0, 1, .14f)), measures, 1000));
    }

    @Test
    public void unrelatedLaneLedgerDoesNotMoveOwner() {
        assertEquals(
                next,
                ScoreDynamicsDetector.directionOwner(
                        List.of(bass, next), 204, 214, List.of(low(0, 0, .185f)), measures, 1000));
    }
}
