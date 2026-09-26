// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original logical records for inherited key context, never score transcriptions. */
public class ContextualAccidentalTieTest {
    private static final int KEY=ScoreNoteEvent.ACCIDENTAL_FROM_KEY;
    private static final List<ScoreKeyChange> C=List.of(new ScoreKeyChange(0,0));
    private ScoreNoteEvent n(int bar,float x,int accidental,boolean tie) {
        return new ScoreNoteEvent(bar,x,1,0,1,.4f,tie,0,1,accidental,0).withClef(ScoreNoteEvent.CLEF_TREBLE);
    }
    private List<ScoreNoteEvent> read(List<ScoreNoteEvent> n,List<ScoreKeyChange> k) {
        return ScoreTiePitchGuard.recheckWithKeyContext(n,k);
    }
    @Test public void inheritedNaturalCannotTieToExplicitSharp() {
        assertFalse(read(List.of(n(8,.1f,KEY,false),n(8,.5f,1,true)),C).get(1).tiedFromPrevious());
    }
    @Test public void unknownStandalonePageKeepsPossibleTie() {
        assertTrue(read(List.of(n(8,.1f,KEY,false),n(8,.5f,1,true)),List.of()).get(1).tiedFromPrevious());
    }
    @Test public void genuinelySharpKeyKeepsRepeatedPitch() {
        assertTrue(read(List.of(n(8,.1f,KEY,false),n(8,.5f,1,true)),List.of(new ScoreKeyChange(0,1))).get(1).tiedFromPrevious());
    }
    @Test public void printedOpeningKeyOverridesInitialContext() {
        assertTrue(read(List.of(n(8,.1f,KEY,false),n(8,.5f,1,true)),List.of(new ScoreKeyChange(0,0),new ScoreKeyChange(0,1))).get(1).tiedFromPrevious());
    }
    @Test public void futureKeyCannotSupplyPastEvidence() {
        assertTrue(read(List.of(n(8,.1f,KEY,false),n(8,.5f,1,true)),List.of(new ScoreKeyChange(9,0))).get(1).tiedFromPrevious());
    }
    @Test public void firstMeasureContradictionIsKnownWithoutCrossPageGuessing() {
        var input=List.of(n(0,.1f,KEY,false),n(0,.5f,1,true));
        assertFalse(ScoreTiePitchGuard.apply(input,C).get(1).tiedFromPrevious());
    }
    @Test public void firstIncomingNoteStillKeepsCrossPageTie() {
        assertTrue(read(List.of(n(0,.1f,1,true)),C).get(0).tiedFromPrevious());
    }
    @Test public void assembledCrossPageRepeatedPitchKeepsTie() {
        assertTrue(read(List.of(n(8,.9f,0,false),n(9,.1f,0,true)),C).get(1).tiedFromPrevious());
    }
    @Test public void absenceOfPriorPageDoesNotAuthorizeANewRemoval() {
        assertTrue(read(List.of(n(8,.9f,0,false),new ScoreNoteEvent(9,.1f,2,0,1,.1f,true,0,1,KEY,0).withClef(30)),C).get(1).tiedFromPrevious());
    }
    @Test public void unknownClefCannotResolveInheritedAccidental() {
        var first=n(8,.1f,KEY,false).withClef(-1);var second=n(8,.5f,1,true).withClef(-1);
        assertTrue(read(List.of(first,second),C).get(1).tiedFromPrevious());
    }
    @Test public void explicitAccidentalCarryAcrossBarRetainsLegacyTie() {
        assertTrue(read(List.of(n(8,.9f,1,false),n(9,.1f,KEY,true)),C).get(1).tiedFromPrevious());
    }
    @Test public void anotherStaffDoesNotSupplyContradiction() {
        var other=new ScoreNoteEvent(8,.1f,1,1,2,.5f,false,0,1,KEY,0).withClef(30);
        var current=new ScoreNoteEvent(8,.5f,1,0,2,.4f,true,0,1,1,0).withClef(30);
        assertTrue(read(List.of(other,current),C).get(1).tiedFromPrevious());
    }
    @Test public void anotherOctaveDoesNotSupplyContradiction() {
        var other=new ScoreNoteEvent(8,.1f,8,0,1,.5f,false,0,1,KEY,0).withClef(30);
        assertTrue(read(List.of(other,n(8,.5f,1,true)),C).get(1).tiedFromPrevious());
    }
    @Test public void sameOnsetChordDoesNotSupplyContradiction() {
        assertTrue(read(List.of(n(8,.5f,KEY,false),n(8,.5f,1,true)),C).get(1).tiedFromPrevious());
    }
    @Test public void changedTieCopyPreservesEveryOtherFieldAndInput() {
        var first=n(8,.1f,KEY,false).withOctaveShift(1);
        var second=new ScoreNoteEvent(8,.5f,1,0,1,.4f,true,1,2,1,0,6,.5f,8,30,true,.25f,true,1);
        var input=List.of(first,second);var output=read(input,C);
        var expected=new ScoreNoteEvent(8,.5f,1,0,1,.4f,false,1,2,1,0,6,.5f,8,30,true,.25f,true,1);
        assertEquals(expected,output.get(1));assertSame(first,output.get(0));assertTrue(input.get(1).tiedFromPrevious());
    }
    @Test public void olderSamePitchCannotBypassImmediateInheritedContradiction() {
        assertFalse(read(List.of(n(8,.1f,1,false),n(8,.3f,KEY,false),n(8,.6f,1,true)),C).get(2).tiedFromPrevious());
    }
    @Test public void unchangedContextKeepsOriginalListIdentity() {
        var notes=List.of(n(8,.1f,KEY,false),n(8,.5f,1,true));
        assertSame(notes,read(notes,List.of()));
    }
    private ScorePageInterpretation page(List<ScoreKeyChange> keys) {
        return new ScorePageInterpretation(List.of(new MeasureRegion(.1f,.9f,.2f,.3f)),
                List.of(n(0,.1f,KEY,false),n(0,.5f,1,true)),7,keys,
                List.of(new ScoreTempoChange(0,0,80)),List.of(new ScoreMeterChange(0,2,2)),
                List.of(new ScoreRestEvent(0,.8f,.25f,.02f,0,1,.5f)),
                List.of(new ScoreTechniqueChange(0,0,0,1,1)),
                List.of(new ScoreDynamicChange(0,0,0,1,0,1,-3,1,false,true,true)),
                List.of(new ScorePlaybackDirection(0,ScorePlaybackDirection.Kind.SEGNO)));
    }
    @Test public void explicitContextPreservesAllPrintedMetadataWithoutInventingAKey() {
        var source=page(List.of());var result=ScoreTiePitchGuard.withInitialKeyContext(source,0);
        assertFalse(result.notes().get(1).tiedFromPrevious());assertTrue(source.notes().get(1).tiedFromPrevious());
        assertEquals(new ScorePageInterpretation(source.measures(),result.notes(),source.firstMeasureNumber(),
                source.keyChanges(),source.tempoChanges(),source.meterChanges(),source.rests(),
                source.techniqueChanges(),source.dynamicChanges(),source.playbackDirections()),result);
        assertTrue(result.keyChanges().isEmpty());
    }
    @Test public void explicitOpeningSignatureWinsAndPreservesPageIdentity() {
        var source=page(List.of(new ScoreKeyChange(0,1)));
        assertSame(source,ScoreTiePitchGuard.withInitialKeyContext(source,0));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidInitialKeyCannotEnterTheReader() {
        ScoreTiePitchGuard.withInitialKeyContext(page(List.of()),8);
    }
}
