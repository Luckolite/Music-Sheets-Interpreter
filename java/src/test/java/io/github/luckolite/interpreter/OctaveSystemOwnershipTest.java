// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic eight, dashed span and hook between two separate systems. */
public class OctaveSystemOwnershipTest {
    private static final int W = 320, H = 340;
    private static final List<PlayingTechniqueDetector.Staff> STAFFS =
            List.of(
                    new PlayingTechniqueDetector.Staff(100, 148, 12, 0, 1),
                    new PlayingTechniqueDetector.Staff(250, 298, 12, 0, 1));

    private static byte[] page(int hook) {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int cy : new int[] {215, 224})
            for (int y = cy - 5; y <= cy + 5; y++)
                for (int x = 55; x <= 65; x++) {
                    double r = Math.pow((x - 60) / 5d, 2) + Math.pow((y - cy) / 5d, 2);
                    if (r <= 1.1 && r >= .3) gray[y * W + x] = 0;
                }
        for (int x = 76; x <= 212; x++) if ((x - 76) % 12 < 5) gray[213 * W + x] = 0;
        for (int x = 208; x <= 212; x++) gray[213 * W + x] = 0;
        for (int y = Math.min(213, 213 + hook); y <= Math.max(213, 213 + hook); y++)
            gray[y * W + 212] = 0;
        return gray;
    }

    private List<ScoreNoteEvent> apply(int hook) {
        return apply(hook, 148);
    }

    private List<ScoreNoteEvent> apply(int hook, int previousBottom) {
        var regions =
                List.of(new MeasureRegion(0, 1, .25f, .5f), new MeasureRegion(0, 1, .7f, .9f));
        var previous =
                new ScoreNoteEvent(0, .4f, 0, 0, 1, (previousBottom - 18f) / H, false, 0, 0, 2, 1);
        var following = new ScoreNoteEvent(1, .4f, 0, 0, 1, 275f / H, false, 0, 0, 2, 1);
        var staffs =
                List.of(
                        new PlayingTechniqueDetector.Staff(
                                previousBottom - 48, previousBottom, 12, 0, 1),
                        STAFFS.get(1));
        return OctaveMarkDetector.apply(
                List.of(), staffs, regions, List.of(previous, following), page(hook), W, H);
    }

    @Test
    public void upwardHookBelongsToPreviousSystemEvenWhenNextStaffIsCloser() {
        var notes = apply(-12);
        assertEquals(-1, notes.get(0).octaveShift());
        assertEquals(0, notes.get(1).octaveShift());
    }

    @Test
    public void downwardHookBelongsToFollowingSystem() {
        var notes = apply(12);
        assertEquals(0, notes.get(0).octaveShift());
        assertEquals(1, notes.get(1).octaveShift());
    }

    @Test
    public void unhookedSpanRetainsNearestStaffFallback() {
        var notes = apply(0);
        assertEquals(0, notes.get(0).octaveShift());
        assertEquals(1, notes.get(1).octaveShift());
    }

    @Test
    public void lowerDirectionBeyondNineGapsStillOwnsItsPreviousStaff() {
        var notes = apply(-12, 90);
        assertEquals(-1, notes.get(0).octaveShift());
        assertEquals(0, notes.get(1).octaveShift());
    }
}
