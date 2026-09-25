// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.github.luckolite.interpreter.ScorePlaybackDirection.Kind.*;

/** Original logical scores: no private score scans, melodies, or inferred reference labels. */
public class ScoreNavigationProjectionTest {
    private static final ScoreNavigationProjection.Defaults DEFAULTS=new ScoreNavigationProjection.Defaults(0,60,4,4);
    private static ScoreNavigationPlan plan(){return ScoreNavigationPlan.create(8,List.of(
            new ScorePlaybackDirection(1,SEGNO),new ScorePlaybackDirection(3,TO_CODA),
            new ScorePlaybackDirection(5,DAL_SEGNO_AL_CODA),new ScorePlaybackDirection(6,CODA)));}
    // Source route: 0,1,2,3,4,1,2,6,7.
    private static ScorePageInterpretation score(List<ScoreNoteEvent> notes,List<ScoreKeyChange> keys,
            List<ScoreTempoChange> tempos,List<ScoreMeterChange> meters,List<ScoreRestEvent> rests,
            List<ScoreTechniqueChange> techniques,List<ScoreDynamicChange> dynamics) {
        var measures=new ArrayList<MeasureRegion>();for(int i=0;i<8;i++)measures.add(new MeasureRegion(i,i+1,0,1));
        return new ScorePageInterpretation(measures,notes,12,keys,tempos,meters,rests,techniques,dynamics);
    }
    private static List<ScoreNoteEvent> notes() {
        var notes=new ArrayList<ScoreNoteEvent>();
        for(int m=0;m<8;m++)for(int n=0;n<4;n++)notes.add(new ScoreNoteEvent(m,n/4f,0,0,1,.4f,false,0,0,2,1));
        return notes;
    }
    private static ScorePageInterpretation empty(){return score(notes(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());}
    private static ScorePageInterpretation project(ScorePageInterpretation source){return ScoreNavigationProjection.project(source,plan(),DEFAULTS);}
    private static ScoreDynamicChange level(int measure,float position,int staff,int count,float db){return new ScoreDynamicChange(measure,position,staff,count,measure,position,db,0);}
    private static float levelAt(ScorePageInterpretation score,int measure,int staff) {
        return score.dynamicChanges().stream().filter(c->c.measureIndex()==measure&&c.positionInMeasure()==0
                &&c.staffIndex()==staff&&c.direction()==0).reduce((a,b)->b).orElseThrow().decibels();
    }
    @Test public void noNavigationReturnsIdenticalImmutableObject() {
        var source=empty();assertSame(source,ScoreNavigationProjection.project(source,ScoreNavigationPlan.create(8,List.of()),DEFAULTS));
    }
    @Test public void measureOccurrencesPreserveSourceGeometryAndConsumeDirections() {
        var source=empty();var result=project(source);
        assertEquals(9,result.measures().size());assertEquals(12,result.firstMeasureNumber());
        assertSame(source.measures().get(1),result.measures().get(5));assertSame(source.measures().get(6),result.measures().get(7));
        assertTrue(result.playbackDirections().isEmpty());assertEquals(36,result.notes().size());assertEquals(32,source.notes().size());
    }
    @Test public void noteMetadataAndRestDurationsSurviveDuplication() {
        var note=new ScoreNoteEvent(1,.25f,3,0,1,.72f,false,2,3,-1,.5f,3,.5f,
                NoteArticulation.TENUTO,ScoreNoteEvent.CLEF_BASS,true,.25f,true,1);
        var source=score(List.of(note),List.of(),List.of(),List.of(),List.of(new ScoreRestEvent(2,.6f,.7f,.02f,0,1,.75)),List.of(),List.of());
        var result=project(source);assertEquals(2,result.notes().size());
        var copy=result.notes().get(1);assertEquals(5,copy.measureIndex());
        assertEquals(note,new ScoreNoteEvent(1,copy.positionInMeasure(),copy.staffStep(),copy.staffIndex(),copy.staffCount(),copy.pageY(),
                copy.tiedFromPrevious(),copy.augmentationDots(),copy.beamCount(),copy.writtenAccidental(),copy.unbeamedDurationBeats(),
                copy.tupletDivisor(),copy.followingRestBeats(),copy.articulations(),copy.clefBottomDiatonic(),copy.crossStaffBeam(),
                copy.leadingRestBeats(),copy.compactOpening(),copy.octaveShift()));
        assertEquals(2,result.rests().size());assertEquals(6,result.rests().get(1).measureIndex());assertEquals(.75,result.rests().get(1).durationBeats(),0);
    }
    @Test public void explicitOpeningDefaultsAreRestoredInsteadOfLaterPrintedState() {
        var source=score(notes(),List.of(new ScoreKeyChange(4,-3)),List.of(new ScoreTempoChange(4,0,180,2)),
                List.of(new ScoreMeterChange(4,3,8)),List.of(),List.of(),List.of());
        var result=ScoreNavigationProjection.project(source,plan(),new ScoreNavigationProjection.Defaults(2,72,6,8));
        assertEquals(new ScoreKeyChange(5,2),result.keyChanges().stream().filter(c->c.measureIndex()==5).findFirst().orElseThrow());
        assertEquals(new ScoreTempoChange(5,0,72,1),result.tempoChanges().stream().filter(c->c.measureIndex()==5).findFirst().orElseThrow());
        assertEquals(new ScoreMeterChange(5,6,8),result.meterChanges().stream().filter(c->c.measureIndex()==5).findFirst().orElseThrow());
        assertEquals(new ScoreKeyChange(7,-3),result.keyChanges().stream().filter(c->c.measureIndex()==7).findFirst().orElseThrow());
        assertEquals(new ScoreTempoChange(7,0,180,2),result.tempoChanges().stream().filter(c->c.measureIndex()==7).findFirst().orElseThrow());
    }
    @Test public void inlineTempoChangesAndPrintedPulseRepeatExactly() {
        var source=score(notes(),List.of(),List.of(new ScoreTempoChange(0,.5f,96,2),new ScoreTempoChange(1,.6f,120,.5)),List.of(),List.of(),List.of(),List.of());
        var result=project(source);
        assertTrue(result.tempoChanges().contains(new ScoreTempoChange(5,0,96,2)));
        assertTrue(result.tempoChanges().contains(new ScoreTempoChange(5,.6f,120,.5)));
    }
    @Test public void incomingTiesBreakButInternalTiesAndLinearPassRemain() {
        var source=score(List.of(new ScoreNoteEvent(1,0,0,0,1,.3f,true),new ScoreNoteEvent(1,.5f,0,0,1,.3f,true),
                new ScoreNoteEvent(2,0,0,0,1,.3f,true),new ScoreNoteEvent(6,0,0,0,1,.3f,true)),
                List.of(),List.of(),List.of(),List.of(),List.of(),List.of());
        var result=project(source);
        assertTrue(result.notes().get(0).tiedFromPrevious());
        assertFalse(result.notes().stream().filter(n->n.measureIndex()==5&&n.positionInMeasure()==0).findFirst().orElseThrow().tiedFromPrevious());
        assertTrue(result.notes().stream().filter(n->n.measureIndex()==5&&n.positionInMeasure()==.5f).findFirst().orElseThrow().tiedFromPrevious());
        assertTrue(result.notes().stream().filter(n->n.measureIndex()==6).findFirst().orElseThrow().tiedFromPrevious());
        assertFalse(result.notes().stream().filter(n->n.measureIndex()==7).findFirst().orElseThrow().tiedFromPrevious());
    }
    @Test public void bothTechniqueCategoriesResetAndPreserveInlineChanges() {
        var changes=List.of(new ScoreTechniqueChange(1,.5f,0,1,ScoreTechniqueChange.PIZZICATO),
                new ScoreTechniqueChange(4,0,0,1,ScoreTechniqueChange.MARCATO));
        var result=project(score(notes(),List.of(),List.of(),List.of(),List.of(),changes,List.of()));
        assertTrue(result.techniqueChanges().contains(new ScoreTechniqueChange(5,0,0,1,ScoreTechniqueChange.ARCO)));
        assertTrue(result.techniqueChanges().contains(new ScoreTechniqueChange(5,0,0,1,ScoreTechniqueChange.ORDINARIO)));
        assertTrue(result.techniqueChanges().contains(new ScoreTechniqueChange(5,.5f,0,1,ScoreTechniqueChange.PIZZICATO)));
        assertTrue(result.techniqueChanges().contains(new ScoreTechniqueChange(7,0,0,1,ScoreTechniqueChange.PIZZICATO)));
        assertTrue(result.techniqueChanges().contains(new ScoreTechniqueChange(7,0,0,1,ScoreTechniqueChange.MARCATO)));
    }
    @Test public void dynamicDefaultDoesNotLeakBackFromLaterForte() {
        var result=project(score(notes(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(level(4,0,0,1,6))));
        assertEquals(0,levelAt(result,5,0),0);assertEquals(6,levelAt(result,7,0),0);
    }
    @Test public void levelAtPreviousBarEndRestoresItsPersistentTargetNotTransientSmoothing() {
        var result=project(score(notes(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(level(0,1,0,1,-8))));
        assertEquals(-8,levelAt(result,5,0),0);
    }
    @Test public void clippedHairpinKeepsPartialTargetAndDoesNotCrossTheJump() {
        var result=project(score(notes(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(
                level(0,0,0,1,-12),new ScoreDynamicChange(2,0,0,1,6,0,0,1))));
        var wedge=result.dynamicChanges().stream().filter(c->c.measureIndex()==2&&c.direction()==1).findFirst().orElseThrow();
        assertEquals(4,wedge.endMeasureIndex());assertEquals(1,wedge.endPosition(),0);assertTrue(wedge.fixedTarget());
        assertEquals(-7.5,wedge.decibels(),.0001); // 3 of the source's 4 equal-time bars.
        assertEquals(-12,levelAt(result,5,0),0);
        var repeat=result.dynamicChanges().stream().filter(c->c.measureIndex()==6&&c.direction()==1).findFirst().orElseThrow();
        assertEquals(6,repeat.endMeasureIndex());assertEquals(-10.5,repeat.decibels(),.0001);
    }
    @Test public void destinationInsideHairpinRestoresProgressAndResumesOnlyRemainingGain() {
        var result=project(score(notes(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(
                level(0,0,0,1,-12),new ScoreDynamicChange(0,0,0,1,4,0,0,1))));
        assertEquals(-10.5,levelAt(result,5,0),.0001);
        var resumed=result.dynamicChanges().stream().filter(c->c.measureIndex()==5&&c.direction()==1).findFirst().orElseThrow();
        assertTrue(resumed.fixedTarget());assertEquals(6,resumed.endMeasureIndex());assertEquals(-7.5,resumed.decibels(),.0001);
    }
    @Test public void partialHairpinUsesTempoTimeRatherThanCountOfBars() {
        var result=project(score(notes(),List.of(),List.of(new ScoreTempoChange(1,0,120)),List.of(),List.of(),List.of(),List.of(
                level(0,0,0,1,-12),new ScoreDynamicChange(0,0,0,1,4,0,0,1))));
        // First bar takes 4 s, the other three take 2 s each: entry is 40%, not 25%.
        assertEquals(-9.6,levelAt(result,5,0),.0001);
    }
    @Test public void partialHairpinUsesChangingMeter() {
        var result=project(score(notes(),List.of(),List.of(),List.of(new ScoreMeterChange(1,2,4)),List.of(),List.of(),List.of(
                level(0,0,0,1,-12),new ScoreDynamicChange(0,0,0,1,4,0,0,1))));
        assertEquals(-9.6,levelAt(result,5,0),.0001);
    }
    @Test public void interruptedHairpinIsNotResurrectedAtDestination() {
        var result=project(score(notes(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(
                new ScoreDynamicChange(0,0,0,1,4,0,0,1),level(0,.5f,0,1,-8))));
        assertEquals(-8,levelAt(result,5,0),0);
        assertFalse(result.dynamicChanges().stream().anyMatch(c->c.measureIndex()==5&&c.direction()!=0));
    }
    @Test public void fixedHairpinTargetDoesNotBecomeAnotherSixDecibels() {
        var result=project(score(notes(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(
                level(0,0,0,1,-12),new ScoreDynamicChange(0,0,0,1,4,0,-4,1,false,true))));
        assertEquals(-10,levelAt(result,5,0),.0001);
    }
    @Test public void invalidDefaultsAndWrongSourcePlanAreRejected() {
        try{new ScoreNavigationProjection.Defaults(8,60,4,4);fail();}catch(IllegalArgumentException expected){}
        try{new ScoreNavigationProjection.Defaults(0,Double.NaN,4,4);fail();}catch(IllegalArgumentException expected){}
        try{ScoreNavigationProjection.project(empty(),ScoreNavigationPlan.create(7,List.of()),DEFAULTS);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void sharedHairpinRetainsEachStaffsInitialLevelAndSharedTiming() {
        var pair=new ArrayList<ScoreNoteEvent>();
        for(int m=0;m<8;m++)for(int s=0;s<2;s++)for(int n=0;n<4;n++)
            pair.add(new ScoreNoteEvent(m,n/4f,0,s,2,.4f,false,0,0,2,1));
        var result=project(score(pair,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(
                level(0,0,0,2,-12),level(0,0,1,2,-6),new ScoreDynamicChange(0,0,0,2,4,0,0,1,true))));
        assertEquals(-10.5,levelAt(result,5,0),.0001);assertEquals(-4.5,levelAt(result,5,1),.0001);
        var wedges=result.dynamicChanges().stream().filter(c->c.measureIndex()==5&&c.direction()==1).toList();
        assertEquals(2,wedges.size());
        for(var c:wedges){assertTrue(c.sharedTiming());assertFalse(c.sharedStaffs());assertTrue(c.fixedTarget());}
        assertEquals(-7.5,wedges.get(0).decibels(),.0001);assertEquals(-1.5,wedges.get(1).decibels(),.0001);
    }
}
