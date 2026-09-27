// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.OctaveMarkDetectorTest.*;
import java.util.List;

public class OctaveEndHookTest {
    @Test
    public void parenthesizedLowerDirectionWithHook() {
        assertEquals(-1, OctaveMarkDetector.shift("(8vb) 」"));
    }

    @Test
    public void upperAndDoubleDirectionsWithHook() {
        assertEquals(1, OctaveMarkDetector.shift("(8va)┐"));
        assertEquals(-2, OctaveMarkDetector.shift("15mb┘"));
    }

    @Test
    public void hookDoesNotTurnNumberIntoDirection() {
        assertEquals(0, OctaveMarkDetector.shift("8」"));
    }

    @Test
    public void embeddedHookAndProseStayRejected() {
        assertEquals(0, OctaveMarkDetector.shift("8」vb"));
        assertEquals(0, OctaveMarkDetector.shift("8vb lyrics」"));
    }

    @Test
    public void isolatedContinuationOnlyShiftsItsFirstChord() {
        var n =
                apply(
                        page(),
                        List.of(word("(8vb) 」", 130, 370)),
                        List.of(note(150, 1), note(300, 1), note(150, 0)));
        assertEquals(List.of(-1, 0, 0), n.stream().map(ScoreNoteEvent::octaveShift).toList());
    }

    @Test
    public void hookDoesNotExtendDashedSpan() {
        var g = page();
        dash(g, 120, 370, 390);
        var n =
                apply(
                        g,
                        List.of(word("8vb┘", 80, 370)),
                        List.of(note(150, 1), note(300, 1), note(500, 1)));
        assertEquals(List.of(-1, -1, 0), n.stream().map(ScoreNoteEvent::octaveShift).toList());
    }
}
