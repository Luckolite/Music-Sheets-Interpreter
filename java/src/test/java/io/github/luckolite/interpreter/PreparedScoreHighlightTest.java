// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PreparedScoreHighlightTest {
    @Test public void preparedMeasureRetainsHeldVoiceAndMovingAttackBoundaries() {
        var held = new ScoreNoteEvent(0,0,11,0,1,.3f,false,1,0,2,2);
        var moving = new ScoreNoteEvent(0,.5f,0,0,1,.4f,false,0,0,2,.5f);
        var prepared = ActiveScoreNotes.prepare(List.of(held,moving),0,3);
        assertEquals(List.of(held),prepared.at(0));
        assertTrue(prepared.at(2.5f).contains(held));
        assertTrue(prepared.at(3.1f).isEmpty());
        assertEquals(List.of(held),prepared.at(0));
    }

    @Test public void shortNoteClearsDuringRestAndTieMovesToNextMeasure() {
        var longNote = new ScoreNoteEvent(0,.25f,0,0,1,.4f,false,2,1);
        var fast = new ScoreNoteEvent(0,.42f,1,0,1,.4f,false,0,3);
        var continuation = new ScoreNoteEvent(1,0,1,0,1,.4f,true);
        var notes = List.of(longNote,fast,continuation);
        var prepared = ActiveScoreNotes.prepare(notes,0,4);
        assertEquals(List.of(fast),prepared.at(1.9f));
        assertTrue(prepared.at(2.05f).isEmpty());
        assertEquals(List.of(continuation),ActiveScoreNotes.prepare(notes,1,4).at(0));
        assertTrue(ActiveScoreNotes.prepare(notes,2,4).at(0).isEmpty());
    }
}
