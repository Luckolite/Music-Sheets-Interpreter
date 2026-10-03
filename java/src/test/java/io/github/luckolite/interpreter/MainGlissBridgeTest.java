// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MainGlissBridgeTest {
    private static ScoreNoteEvent note(float x) {
        return new ScoreNoteEvent(
                0, x, 0, 0, 1, .4f, false, 0, 2, ScoreNoteEvent.ACCIDENTAL_FROM_KEY, 0, 1);
    }

    private static Map<String, Object> event(double onset, double duration, int midi) {
        var result = new HashMap<String, Object>();
        result.put("startBeat", onset);
        result.put("durationBeats", duration);
        result.put("midi", midi);
        return result;
    }

    @Test
    public void targetUsesAlreadyResolvedWrittenPitchAndOrdinaryOnset() {
        var source = note(.1f).withArticulations(NoteOrnament.GLISSANDO);
        var target = note(.7f);
        var first = event(0, .25, 84);
        var second = event(.25, .25, 61);
        Main.attachNotePerformance(List.of(source, target), List.of(first, second));
        assertEquals(Map.of("style", "white_keys", "targetMidi", 61), first.get("glissando"));
        assertFalse(second.containsKey("glissando"));
        assertEquals(.25, ((Number) second.get("startBeat")).doubleValue(), 0);
    }

    @Test
    public void restGapDoesNotInventAPerformanceEndpoint() {
        var first = event(0, .25, 84);
        var second = event(.75, .25, 60);
        Main.attachNotePerformance(
                List.of(note(.1f).withArticulations(NoteOrnament.GLISSANDO), note(.7f)),
                List.of(first, second));
        assertFalse(first.containsKey("glissando"));
    }

    @Test
    public void explicitNormalComponentSurvivesJsonBridge() {
        var first = event(0, .15, 60);
        var second = event(.15, .25, 62);
        Main.attachNotePerformance(
                List.of(note(.1f).withTupletRatio(5, 3), note(.7f)), List.of(first, second));
        assertEquals(5, first.get("tupletActualNotes"));
        assertEquals(3, first.get("tupletNormalNotes"));
        assertFalse(second.containsKey("tupletActualNotes"));
    }
}
