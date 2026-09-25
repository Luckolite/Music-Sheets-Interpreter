// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original event sequences: the closest same written pitch cannot change accidentals in a tie. */
public class ChangedAccidentalTieTest {
    private ScoreNoteEvent note(int measure,float x,int step,int accidental,boolean tied) {
        return new ScoreNoteEvent(measure,x,step,0,1,.4f,tied,0,1,accidental,0)
                .withClef(ScoreNoteEvent.CLEF_TREBLE);
    }
    @Test public void olderNaturalCannotBypassImmediateSharpToNaturalChange() {
        var notes=List.of(note(3,.1f,6,0,false),note(3,.4f,6,1,false),note(3,.7f,6,0,true));
        assertFalse(ScoreTiePitchGuard.apply(notes,List.of(new ScoreKeyChange(0,0))).get(2).tiedFromPrevious());
    }
    @Test public void olderSharpCannotBypassImmediateNaturalToSharpChange() {
        var notes=List.of(note(3,.1f,6,1,false),note(3,.4f,6,0,false),note(3,.7f,6,1,true));
        assertFalse(ScoreTiePitchGuard.apply(notes,List.of(new ScoreKeyChange(0,2))).get(2).tiedFromPrevious());
    }
    @Test public void explicitChangeWithinPageFirstMeasureIsKnown() {
        var notes=List.of(note(0,.1f,6,1,false),note(0,.4f,6,0,true));
        assertFalse(ScoreTiePitchGuard.apply(notes,List.of()).get(1).tiedFromPrevious());
    }
    @Test public void firstPageNoteStillAllowsUnknownCrossPageTie() {
        assertTrue(ScoreTiePitchGuard.apply(List.of(note(0,.1f,6,0,true)),List.of()).get(0).tiedFromPrevious());
    }
    @Test public void repeatedAccidentalKeepsTie() {
        var notes=List.of(note(3,.1f,6,1,false),note(3,.4f,6,1,true));
        assertTrue(ScoreTiePitchGuard.apply(notes,List.of(new ScoreKeyChange(0,0))).get(1).tiedFromPrevious());
    }
    @Test public void unknownInheritedAccidentalIsNotAssumedDifferent() {
        var notes=List.of(note(3,.1f,6,2,false),note(3,.4f,6,1,true));
        assertTrue(ScoreTiePitchGuard.apply(notes,List.of()).get(1).tiedFromPrevious());
    }
    @Test public void otherOctaveDoesNotSupplyAnAccidentalContradiction() {
        var notes=List.of(note(3,.1f,6,1,false),note(3,.3f,13,0,false),note(3,.5f,6,1,true));
        assertTrue(ScoreTiePitchGuard.apply(notes,List.of()).get(2).tiedFromPrevious());
    }
}
