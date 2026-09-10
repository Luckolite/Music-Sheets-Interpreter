// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original numeral pixels with interleaved note/rest events. */
public class RestTripletTest {
    static final int W=400,H=240;
    static final List<MeasureRegion> BARS=List.of(new MeasureRegion(0,1,.1f,.8f));
    static byte[] ink(boolean closed) {
        String[] rows={"..#######...", ".##########.", "###......###", "####.....###",
                "####.....###", "####.....###", ".##.....####", ".......####.",
                "......####..", "....#####...", "....#####...", "....#####...",
                "......####..", ".......####.", "##.....####.", "###....####.",
                "###....####.", "###....####.", ".###....###.", "..########..",
                "..########..", "....####...."};
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)255);
        for(int y=0;y<rows.length;y++)for(int x=0;x<12;x++)
            if(rows[y].charAt(x)=='#'||closed&&x<2&&y>1&&y<rows.length-2)gray[(145+y)*W+119+x]=0;
        return gray;
    }
    static ScoreNoteEvent note(float x,float following,float leading){return new ScoreNoteEvent(0,x,3,0,1,.4f,false,0,2,2,0,1,following).withLeadingRest(leading);}
    static ScoreRestEvent rest(float x){return new ScoreRestEvent(0,x,.45f,.08f,0,1,.25);}
    static TripletRhythmDetector.Rhythm apply(List<ScoreNoteEvent> n,List<ScoreRestEvent> r,byte[] g){return TripletRhythmDetector.withRests(n,r,BARS,g,W,H);}
    static void triplets(TripletRhythmDetector.Rhythm result,int notes,int rests){
        assertEquals(notes,result.notes().size());assertEquals(rests,result.rests().size());
        for(var n:result.notes()){assertEquals(3,n.tupletDivisor());assertEquals(1./6,ScoreNoteTiming.writtenDurationBeats(n),.00001);assertEquals(3,n.staffStep());}
        for(var r:result.rests())assertEquals(1./6,r.durationBeats(),.00001);
    }
    @Test public void trailingRestIsTheThirdSlot(){var r=apply(List.of(note(.25f,0,0),note(.3125f,.25f,0)),List.of(rest(.375f)),ink(false));triplets(r,2,1);assertEquals(1./6,r.notes().get(1).followingRestBeats(),.00001);}
    @Test public void middleRestIsSilentAndScalesItsAttachment(){var r=apply(List.of(note(.25f,.25f,0),note(.375f,0,0)),List.of(rest(.3125f)),ink(false));triplets(r,2,1);assertEquals(1./6,r.notes().get(0).followingRestBeats(),.00001);}
    @Test public void leadingRestScalesTheOpeningSilence(){var r=apply(List.of(note(.3125f,0,.25f),note(.375f,0,0)),List.of(rest(.25f)),ink(false));triplets(r,2,1);assertEquals(1./6,r.notes().get(0).leadingRestBeats(),.00001);}
    @Test public void twoRestsAndOneNoteStillHaveThreeSlots(){var r=apply(List.of(note(.375f,0,.5f)),List.of(rest(.25f),rest(.3125f)),ink(false));triplets(r,1,2);assertEquals(1./3,r.notes().get(0).leadingRestBeats(),.00001);}
    @Test public void chordMembersDoNotMultiplyTheSilentSlot(){var r=apply(List.of(note(.25f,0,0),note(.251f,0,0),note(.3125f,.25f,0),note(.3135f,.25f,0)),List.of(rest(.375f)),ink(false));triplets(r,4,1);}
    @Test public void aClosedEightDoesNotChangeNotesOrSilence(){var n=List.of(note(.25f,0,0),note(.3125f,.25f,0));var rests=List.of(rest(.375f));var r=apply(n,rests,ink(true));assertEquals(n,r.notes());assertEquals(rests,r.rests());}
    @Test public void anInterveningAttackPreventsSkippingToARest(){var n=List.of(note(.25f,0,0),note(.28f,0,0),note(.3125f,.25f,0));var rests=List.of(rest(.375f));var r=apply(n,rests,ink(false));assertEquals(n,r.notes());assertEquals(rests,r.rests());}
    @Test public void anotherStaffCannotSupplyTheThirdSlot(){var n=List.of(note(.25f,0,0),note(.3125f,0,0));var rests=List.of(new ScoreRestEvent(0,.375f,.45f,.08f,1,2,.25));var r=apply(n,rests,ink(false));assertEquals(n,r.notes());assertEquals(rests,r.rests());}
    @Test public void noSilenceIsSubtractedFromAnIndependentSustain(){var n=List.of(note(.25f,0,0),note(.3125f,0,0));var r=apply(n,List.of(rest(.375f)),ink(false));triplets(r,2,1);assertEquals(0,r.notes().get(1).followingRestBeats(),0);}
    @Test public void inputArraysAndListsRemainUnchanged(){var n=List.of(note(.25f,0,0),note(.3125f,.25f,0));var rests=List.of(rest(.375f));byte[] g=ink(false),copy=g.clone();apply(n,rests,g);assertArrayEquals(copy,g);assertEquals(1,n.get(0).tupletDivisor());assertEquals(.25,rests.get(0).durationBeats(),0);}
    @Test public void aLongRestCannotBeSkippedToJoinThreeNotes(){var n=List.of(note(.25f,0,0),note(.3125f,0,0),note(.375f,0,0));var rests=List.of(new ScoreRestEvent(0,.28f,.45f,.08f,0,1,2));var r=apply(n,rests,ink(false));assertEquals(n,r.notes());assertEquals(rests,r.rests());}
    @Test public void reapplyingDoesNotScaleSilenceTwice(){var r=apply(List.of(note(.25f,0,0),note(.3125f,.25f,0)),List.of(rest(.375f)),ink(false));assertEquals(r,apply(r.notes(),r.rests(),ink(false)));}
    @Test public void threeRestsNeverCreateSoundingNotes(){var r=apply(List.of(),List.of(rest(.25f),rest(.3125f),rest(.375f)),ink(false));triplets(r,0,3);}
    @Test public void aMovingVoiceOnlyScalesTheRestAttachedBesideItsHeldVoice(){
        var held=new ScoreNoteEvent(0,.25f,5,0,1,.4f,false,0,0,2,2,1);
        var moving=note(.6f,0,.25f);
        var r=apply(List.of(held,moving),List.of(rest(.25f),rest(.3125f),rest(.375f)),ink(false));
        assertEquals(2,ScoreNoteTiming.writtenDurationBeats(r.notes().get(0)),0);
        assertEquals(1./6,r.notes().get(1).leadingRestBeats(),.00001);
    }
}
