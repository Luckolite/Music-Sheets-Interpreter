// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original touching seconds with two outer, opposing shafts. */
public class OpposingDisplacedSecondOnsetTest extends IndependentTieVoiceTest {
    Object eventNote(int x, int pitch, boolean up, boolean printed, boolean dotted, int staff)
            throws Exception {
        Object n = note(0, x, pitch, x / (float) W, up, printed, dotted);
        var h = n.getClass().getDeclaredMethod("head");
        h.setAccessible(true);
        return make(
                "DetectedNote",
                new ScoreNoteEvent(
                        0,
                        x / (float) W,
                        pitch,
                        staff,
                        2,
                        (80 - pitch * 7) / (float) H,
                        false,
                        dotted ? 1 : 0,
                        dotted ? 1 : 2,
                        2,
                        0),
                h.invoke(n),
                14f);
    }

    List<ScoreNoteEvent> aligned(boolean opposite, boolean printed, int distance, int staff)
            throws Exception {
        var notes =
                List.of(
                        eventNote(220, 1, false, true, false, 0),
                        eventNote(220 + distance, 2, opposite, printed, true, staff));
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "alignDisplacedSeconds", List.class, byte[].class, int.class, int.class);
        m.setAccessible(true);
        var result = (List<?>) m.invoke(null, notes, gray, W, H);
        var events = new ArrayList<ScoreNoteEvent>();
        for (Object n : result) {
            var e = n.getClass().getDeclaredMethod("event");
            e.setAccessible(true);
            events.add((ScoreNoteEvent) e.invoke(n));
        }
        return events;
    }

    @Test
    public void opposingTouchingValuesShareOneAttackWithoutSharingRhythm() throws Exception {
        var n = aligned(true, true, 16, 0);
        assertEquals(n.get(0).positionInMeasure(), n.get(1).positionInMeasure(), .00001);
        assertEquals(2, n.get(0).beamCount());
        assertEquals(1, n.get(1).beamCount());
        assertEquals(0, n.get(0).augmentationDots());
        assertEquals(1, n.get(1).augmentationDots());
    }

    @Test
    public void sameDirectionDoesNotAuthorizeSharedAttack() throws Exception {
        var n = aligned(false, true, 16, 0);
        assertNotEquals(n.get(0).positionInMeasure(), n.get(1).positionInMeasure(), .00001);
    }

    @Test
    public void missingOpposingShaftDoesNotAuthorizeSharedAttack() throws Exception {
        var n = aligned(true, false, 16, 0);
        assertNotEquals(n.get(0).positionInMeasure(), n.get(1).positionInMeasure(), .00001);
    }

    @Test
    public void separatedHeadsKeepIndependentAttacks() throws Exception {
        var n = aligned(true, true, 30, 0);
        assertNotEquals(n.get(0).positionInMeasure(), n.get(1).positionInMeasure(), .00001);
    }

    @Test
    public void differentStavesDoNotJoin() throws Exception {
        var n = aligned(true, true, 16, 1);
        assertNotEquals(n.get(0).positionInMeasure(), n.get(1).positionInMeasure(), .00001);
    }
}
