// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original polygonal rests and abstract note events, without score scans. */
public class PolyphonicLeadingRestTest {
    private static byte[] rest(int top) {
        byte[] gray=new byte[400*240];Arrays.fill(gray,(byte)255);
        int[][] rows={{0,1,2},{1,2,3},{2,3,4},{3,4,5},{4,5,6},{5,6,7},{6,7,8},
                {7,7,10},{8,7,11},{9,6,11},{10,6,11},{11,5,11},{12,5,11},
                {13,4,10},{14,4,9},{15,5,9},{16,6,9},{17,7,10},{18,8,11},
                {19,6,12},{20,4,13},{21,3,13},{22,2,13},{23,2,6},{24,3,6},
                {25,3,6},{26,4,7},{27,5,7},{28,6,8},{29,7,9},{30,8,9}};
        for(int[] row:rows)for(int dy=0;dy<2;dy++)for(int x=row[1];x<=row[2];x++)
            gray[(top+(int)Math.round(row[0]*1.5)+dy)*400+170+x]=0;
        return gray;
    }
    private static ScoreNoteEvent note(float x,float y,double duration,int dots,int beams,int divisor) {
        return new ScoreNoteEvent(0,x,0,0,1,y,false,dots,beams,
                ScoreNoteEvent.ACCIDENTAL_FROM_KEY,(float)duration,divisor);
    }
    private static List<ScoreRestEvent> detect(int top,ScoreNoteEvent note) {
        return SixteenthRestDetector.detect(rest(top),400,240,
                List.of(new MeasureRegion(0,1,.1f,.75f)),
                List.of(new SixteenthRestDetector.Staff(80,144,16,0,1)),List.of(note));
    }
    @Test public void raisedRestCanShareAColumnWithAHeldLowerVoice() {
        var found=detect(58,note(.4425f,.56f,2,1,0,1));
        assertEquals(1,found.size());assertEquals(1,found.get(0).durationBeats(),.0001);
    }
    @Test public void lastPixelBandCountsAtTheQuarterRestBoundary() {
        var found=detect(54,note(.4425f,.56f,2,1,0,1));
        assertEquals(1,found.size());assertEquals(1,found.get(0).durationBeats(),.0001);
    }
    @Test public void aNoteFragmentInTheSameVerticalBandIsRejected() {
        assertTrue(detect(58,note(.4425f,.36f,2,1,0,1)).isEmpty());
    }
    @Test public void anOrdinaryFilledNoteDoesNotAuthorizeAnOverlappingRest() {
        assertTrue(detect(58,note(.4425f,.56f,1,0,0,1)).isEmpty());
    }
    @Test public void extractionAssignsTheRestToTheMovingVoiceOnly() {
        byte[] gray=rest(58),labels=new byte[gray.length];
        for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++) {
            labels[y*400+x]=4;gray[y*400+x]=0;
        }
        for(int i=0;i<2;i++) {
            int cx=i==0?176:260,cy=i==0?120:144;
            for(int y=cy-8;y<=cy+8;y++)for(int x=cx-10;x<=cx+10;x++) {
                double d=Math.pow((x-cx)/10.0,2)+Math.pow((y-cy)/8.0,2);
                if(d<=1) {labels[y*400+x]=2;gray[y*400+x]=i==0&&d<.45?(byte)255:0;}
            }
            int stemX=i==0?cx-10:cx+10;
            for(int y=i==0?cy:cy-48;y<(i==0?cy+48:cy);y++) {
                labels[y*400+stemX]=1;gray[y*400+stemX]=0;
            }
        }
        var score=OmrScoreInterpreter.analyze(labels,gray,400,240,
                List.of(new MeasureRegion(.05f,.95f,.1f,.8f)));
        assertEquals(2,score.notes().size());assertEquals(1,score.rests().size());
        var held=score.notes().get(0);var moving=score.notes().get(1);
        assertTrue(ScoreNoteTiming.hasIndependentSustain(held));
        assertEquals(0,held.leadingRestBeats(),.0001);
        assertEquals(0,held.followingRestBeats(),.0001);
        assertEquals(1,moving.leadingRestBeats(),.0001);
    }
    @Test public void printedRestTimesTheMovingVoiceWithoutDelayingItsHeldPartner() {
        var held=note(.075f,.56f,2,1,0,1);
        var first=note(.275f,.65f,1,0,0,1).withLeadingRest(1);
        var second=note(.475f,.62f,1,0,0,1);
        var chord=note(.675f,.68f,1,0,0,1);
        var t1=note(.675f,.54f,0,0,1,3);
        var t2=note(.81f,.56f,0,0,1,3);
        var t3=note(.914f,.58f,0,0,1,3);
        var notes=List.of(held,first,second,chord,t1,t2,t3);
        double[] onsets={0,1,2,3,3,3+1.0/3,3+2.0/3};
        double[] lengths={3,1,1,1,1.0/3,1.0/3,1.0/3};
        for(int i=0;i<notes.size();i++) {
            assertEquals("onset "+i,onsets[i],ScoreNoteTiming.beatInMeasure(notes.get(i),notes,4),.0001);
            assertEquals("duration "+i,lengths[i],ScoreNoteTiming.resolvedWrittenDurationBeats(notes.get(i),notes,4),.0001);
        }
    }
}
