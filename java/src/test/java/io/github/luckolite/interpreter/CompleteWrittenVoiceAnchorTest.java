// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original complete accompaniment against a damaged neighbouring rhythm. */
public class CompleteWrittenVoiceAnchorTest {
    private static ScoreNoteEvent note(int staff,float x,int beams,int divisor,float value,int step) {
        return new ScoreNoteEvent(0,x,step,staff,2,.3f+staff*.25f+step*.01f,false,0,beams,2,value,divisor);
    }
    private static List<ScoreNoteEvent> bass() {
        var result=new ArrayList<ScoreNoteEvent>();
        float[] x={.02f,.26f,.555f,.635f,.715f,.798f,.919f};
        for(int i=0;i<x.length;i++)for(int step:new int[]{0,7})
            result.add(note(1,x[i],i<2?0:1,i>=2&&i<5?3:1,i<2?1:0,step));
        return result;
    }
    private static List<ScoreNoteEvent> damagedUpper() {
        var result=new ArrayList<ScoreNoteEvent>();
        for(float x:new float[]{.021f,.262f,.556f,.636f,.716f,.799f,.92f})result.add(note(0,x,1,1,0,0));
        return result;
    }
    @Test public void completeChordRhythmKeepsExactTripletOnsetsAcrossStaves() {
        var bass=bass();var upper=damagedUpper();var score=new ArrayList<>(bass);score.addAll(upper);
        double[] expected={0,1,2,2+1.0/3,2+2.0/3,3,3.5};
        for(int i=0;i<7;i++){
            assertEquals(expected[i],ScoreNoteTiming.beatInMeasure(bass.get(i*2),score,4),.0001);
            assertEquals(expected[i],ScoreNoteTiming.beatInMeasure(bass.get(i*2+1),score,4),.0001);
            assertEquals(expected[i],ScoreNoteTiming.beatInMeasure(upper.get(i),score,4),.0001);
        }
    }
    @Test public void staggeredNotesDoNotBorrowACompleteNeighbouringClock() {
        var upper=damagedUpper();upper.set(3,note(0,.678f,1,1,0,0));
        double own=ScoreNoteTiming.beatInMeasure(upper.get(3),upper,4);
        var score=new ArrayList<>(bass());score.addAll(upper);
        assertEquals(own,ScoreNoteTiming.beatInMeasure(upper.get(3),score,4),.0001);
    }
    @Test public void anOrdinaryCompleteWrittenVoiceAlsoAnchorsItsAlignedPartner() {
        var score=new ArrayList<ScoreNoteEvent>();
        for(float x:new float[]{.03f,.27f,.58f,.89f}){score.add(note(1,x,0,1,1,0));score.add(note(0,x+.001f,1,1,0,0));}
        for(int i=0;i<4;i++)for(int j=0;j<2;j++)assertEquals(i,ScoreNoteTiming.beatInMeasure(score.get(2*i+j),score,4),.0001);
    }
    @Test public void contradictoryCompleteRhythmsKeepTheirOwnWrittenClock() {
        var bass=bass();var score=new ArrayList<>(bass);
        var upper=List.of(note(0,.02f,0,1,2,0),note(0,.555f,0,1,1,0),note(0,.635f,1,1,0,0),note(0,.919f,1,1,0,0));
        score.addAll(upper);
        assertEquals(3,ScoreNoteTiming.beatInMeasure(upper.get(2),score,4),.0001);
        assertEquals(2+1.0/3,ScoreNoteTiming.beatInMeasure(bass.get(6),score,4),.0001);
    }
    private static ScoreNoteEvent thirdStaff(ScoreNoteEvent n,int staff) {
        return new ScoreNoteEvent(n.measureIndex(),n.positionInMeasure(),n.staffStep(),staff,3,
                .15f+staff*.25f,false,n.augmentationDots(),n.beamCount(),2,n.unbeamedDurationBeats(),n.tupletDivisor());
    }
    @Test public void aThirdStaffOrderingConflictDoesNotDisableConsistentPianoAlignment() {
        var score=new ArrayList<ScoreNoteEvent>();
        for(var n:bass())score.add(thirdStaff(n,2));
        var upper=new ArrayList<ScoreNoteEvent>();
        for(var n:damagedUpper()){var mapped=thirdStaff(n,1);upper.add(mapped);score.add(mapped);}
        for(float x:new float[]{.02f,.26f,.556f,.58f,.636f,.716f,.8f,.92f,.98f})
            score.add(thirdStaff(note(0,x,1,1,0,0),0));
        assertEquals(2,ScoreNoteTiming.beatInMeasure(upper.get(2),score,4),.0001);
        assertEquals(2+1.0/3,ScoreNoteTiming.beatInMeasure(upper.get(3),score,4),.0001);
        // The problematic melody still keeps distinct attacks in reading order.
        var melody=score.stream().filter(n->n.staffIndex()==0).collect(java.util.stream.Collectors.toList());
        double previous=-1;
        for(var n:melody){double onset=ScoreNoteTiming.beatInMeasure(n,score,4);assertTrue(onset>previous);previous=onset;}
    }
}
