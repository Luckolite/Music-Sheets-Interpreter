// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original same-staff-step slur and courtesy-accidental controls. */
public class TieAccidentalSlurTest {
    private ScoreNoteEvent note(int step,float x,boolean tied,int accidental) {
        return new ScoreNoteEvent(7,x,step,0,1,.34f,tied,0,2,accidental,0,1,0)
                .withClef(ScoreNoteEvent.CLEF_TREBLE);
    }
    @SuppressWarnings("unchecked")
    private List<ScoreNoteEvent> guard(List<ScoreNoteEvent> notes,List<ScoreKeyChange> keys)throws Exception {
        var method=OmrScoreInterpreter.class.getDeclaredMethod("withoutPitchChangedTies",List.class,List.class,boolean[].class);
        method.setAccessible(true);
        return (List<ScoreNoteEvent>)method.invoke(null,notes,keys,new boolean[]{false,true});
    }
    @Test public void explicitSharpChangingSoundingPitchMakesSlurNotTie()throws Exception {
        // D major: G is natural. A printed sharp on the second G changes pitch.
        var notes=List.of(note(2,.50f,false,ScoreNoteEvent.ACCIDENTAL_FROM_KEY),
                note(2,.59f,true,ScoreNoteEvent.ACCIDENTAL_SHARP));
        assertFalse(guard(notes,List.of(new ScoreKeyChange(0,2))).get(1).tiedFromPrevious());
    }
    @Test public void redundantSharpInKeySignatureCanStillTie()throws Exception {
        // D major: F is already sharp; an explicit F sharp may be cautionary.
        var notes=List.of(note(1,.50f,false,ScoreNoteEvent.ACCIDENTAL_FROM_KEY),
                note(1,.59f,true,ScoreNoteEvent.ACCIDENTAL_SHARP));
        assertTrue(guard(notes,List.of(new ScoreKeyChange(0,2))).get(1).tiedFromPrevious());
    }
    @Test public void absentKeyEvidenceKeepsExistingTie()throws Exception {
        var notes=List.of(note(2,.50f,false,ScoreNoteEvent.ACCIDENTAL_FROM_KEY),
                note(2,.59f,true,ScoreNoteEvent.ACCIDENTAL_SHARP));
        assertTrue(guard(notes,List.of()).get(1).tiedFromPrevious());
    }
    @Test public void carriedAccidentalDoesNotPretendToBePrinted()throws Exception {
        var notes=List.of(note(2,.50f,false,ScoreNoteEvent.ACCIDENTAL_FROM_KEY),
                note(2,.59f,true,ScoreNoteEvent.ACCIDENTAL_SHARP));
        var method=OmrScoreInterpreter.class.getDeclaredMethod("withoutPitchChangedTies",
                List.class,List.class,boolean[].class);method.setAccessible(true);
        @SuppressWarnings("unchecked")
        var output=(List<ScoreNoteEvent>)method.invoke(null,notes,List.of(new ScoreKeyChange(0,2)),
                new boolean[]{false,false});
        assertTrue(output.get(1).tiedFromPrevious());
    }
}
