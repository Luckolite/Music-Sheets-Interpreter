// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original scalar slur, real tie, and page-boundary controls. */
public class ScoreTiePitchGuardTest {
    private ScoreNoteEvent note(int measure,float x,int step,boolean tie) {
        return new ScoreNoteEvent(measure,x,step,0,1,.35f,tie,0,2,
                ScoreNoteEvent.ACCIDENTAL_FROM_KEY,0,1,0).withClef(ScoreNoteEvent.CLEF_TREBLE);
    }
    private ScoreNoteEvent explicit(int measure,float x,int step,boolean tie,int accidental) {
        return new ScoreNoteEvent(measure,x,step,0,1,.35f,tie,0,2,
                accidental,0,1,0).withClef(ScoreNoteEvent.CLEF_TREBLE);
    }
    @Test public void slurredScaleWithoutEarlierSamePitchCannotBeTied() {
        var notes=List.of(note(7,.1f,0,false),note(7,.3f,1,true),note(7,.5f,2,true));
        var result=ScoreTiePitchGuard.apply(notes,List.of(new ScoreKeyChange(0,0)));
        assertFalse(result.get(1).tiedFromPrevious());
        assertFalse(result.get(2).tiedFromPrevious());
    }
    @Test public void repeatedSamePitchWithEarlierHeadKeepsTie() {
        var notes=List.of(note(7,.1f,0,false),note(7,.3f,0,true));
        assertTrue(ScoreTiePitchGuard.apply(notes,List.of(new ScoreKeyChange(0,0)))
                .get(1).tiedFromPrevious());
    }
    @Test public void firstMeasureKeepsPossibleCrossPageTie() {
        var notes=List.of(note(0,.1f,0,true));
        assertTrue(ScoreTiePitchGuard.apply(notes,List.of(new ScoreKeyChange(0,0)))
                .get(0).tiedFromPrevious());
    }
    @Test public void uncertainKeyLeavesTieUntouched() {
        var notes=List.of(note(7,.1f,0,false),note(7,.3f,1,true));
        assertTrue(ScoreTiePitchGuard.apply(notes,List.of()).get(1).tiedFromPrevious());
    }
    @Test public void differentExplicitAccidentalsClearTieWithoutKey() {
        var natural=explicit(7,.1f,0,false,ScoreNoteEvent.ACCIDENTAL_NATURAL);
        var sharp=explicit(7,.3f,0,true,ScoreNoteEvent.ACCIDENTAL_SHARP);
        assertFalse(ScoreTiePitchGuard.apply(List.of(natural,sharp),List.of())
                .get(1).tiedFromPrevious());
    }
    @Test public void matchingExplicitAccidentalsKeepTieWithoutKey() {
        var first=explicit(7,.1f,0,false,ScoreNoteEvent.ACCIDENTAL_SHARP);
        var second=explicit(7,.3f,0,true,ScoreNoteEvent.ACCIDENTAL_SHARP);
        assertTrue(ScoreTiePitchGuard.apply(List.of(first,second),List.of())
                .get(1).tiedFromPrevious());
    }
}
