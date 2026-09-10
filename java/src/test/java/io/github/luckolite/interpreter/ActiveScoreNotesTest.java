// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ActiveScoreNotesTest {
    @Test public void sustainedVoiceStaysLitWhileAccompanimentAdvances() {
        var held=new ScoreNoteEvent(0,0,11,0,1,.3f,false,1,0,2,2);
        var moving=new ScoreNoteEvent(0,.5f,0,0,1,.4f,false,0,0,2,.5f);
        var notes=List.of(held,moving);
        assertTrue(ActiveScoreNotes.at(notes,0,2.5f,3).contains(held));
        assertTrue(ActiveScoreNotes.at(notes,0,3.1f,3).isEmpty());
    }
    @Test public void followsTheLatestFixedClockOnsetAndKeepsChordsTogether() {
        ScoreNoteEvent first = new ScoreNoteEvent(0, 0f, 0, 0, 1, .30f, false);
        ScoreNoteEvent chordHigh = new ScoreNoteEvent(0, .5f, 3, 0, 1, .25f, false);
        ScoreNoteEvent chordLow = new ScoreNoteEvent(0, .5f, 0, 0, 1, .30f, false);

        assertEquals(List.of(first), ActiveScoreNotes.at(
                List.of(first, chordHigh, chordLow), 0, 1.75f, 4f));
        assertEquals(List.of(chordHigh, chordLow), ActiveScoreNotes.at(
                List.of(first, chordHigh, chordLow), 0, 2f, 4f));
    }

    @Test public void tieContinuationMovesHighlightWithoutRequiringAnotherAttack() {
        ScoreNoteEvent continuation = new ScoreNoteEvent(1, 0f, 0, 0, 1, .4f, true);
        assertEquals(List.of(continuation), ActiveScoreNotes.at(
                List.of(continuation), 1, 0f, 4f));
        assertTrue(ActiveScoreNotes.at(List.of(continuation), 0, 3f, 4f).isEmpty());
    }

    @Test public void thirtySecondHighlightUsesItsShortWrittenDuration() {
        ScoreNoteEvent longNote = new ScoreNoteEvent(0, .25f, 0, 0, 1,
                .4f, false, 2, 1);
        ScoreNoteEvent fast = new ScoreNoteEvent(0, .42f, 1, 0, 1,
                .4f, false, 0, 3);
        List<ScoreNoteEvent> phrase = List.of(longNote, fast);
        assertEquals(List.of(fast), ActiveScoreNotes.at(phrase, 0, 1.90f, 4f));
        assertTrue(ActiveScoreNotes.at(phrase, 0, 2.05f, 4f).isEmpty());
    }
}
