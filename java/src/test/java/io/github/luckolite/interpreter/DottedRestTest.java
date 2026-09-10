// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original polygonal rest and dot geometry; no score images. */
public class DottedRestTest {
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

    private static byte[] page() {
        byte[] gray=rest(90);
        for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++)gray[y*400+x]=0;
        return gray;
    }
    private static void dot(byte[] gray,int x,int y,int radius) {
        for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++)
            if(dx*dx+dy*dy<=radius*radius)gray[(y+dy)*400+x+dx]=0;
    }
    private static List<ScoreRestEvent> detect(byte[] gray,List<ScoreNoteEvent> notes) {
        return SixteenthRestDetector.detect(gray,400,240,List.of(new MeasureRegion(0,1,.1f,.8f)),
                List.of(new SixteenthRestDetector.Staff(80,144,16,0,1)),notes);
    }
    private static void duration(byte[] gray,List<ScoreNoteEvent> notes,double expected) {
        var rests=detect(gray,notes);assertEquals(1,rests.size());assertEquals(expected,rests.get(0).durationBeats(),.0001);
    }
    @Test public void ordinaryQuarterRestKeepsOneBeat() {duration(page(),List.of(),1);}
    @Test public void adjacentDotAddsHalfTheRestValue() {
        byte[] g=page();dot(g,194,104,3);duration(g,List.of(),1.5);
    }
    @Test public void twoDotsAddHalfAndQuarter() {
        byte[] g=page();dot(g,194,104,3);dot(g,207,104,3);duration(g,List.of(),1.75);
    }
    @Test public void highArticulationIsNotARestDot() {
        byte[] g=page();dot(g,194,88,3);duration(g,List.of(),1);
    }
    @Test public void distantInkIsNotARestDot() {
        byte[] g=page();dot(g,216,104,3);duration(g,List.of(),1);
    }
    @Test public void aNoteheadIsTooLargeToBeARestDot() {
        byte[] g=page();dot(g,198,104,7);duration(g,List.of(),1);
    }
    @Test public void aStemCrossingTheSearchBandIsNotADot() {
        byte[] g=page();for(int y=84;y<=128;y++)for(int x=194;x<=196;x++)g[y*400+x]=0;duration(g,List.of(),1);
    }
    @Test public void aNotesArticulationBelongsToThatNote() {
        byte[] g=page();dot(g,200,104,3);
        var n=new ScoreNoteEvent(0,.5f,0,0,1,.59f,false,0,0,ScoreNoteEvent.ACCIDENTAL_FROM_KEY,1);
        duration(g,List.of(n),1);
    }
    @Test public void dotsFollowARaisedVoiceRest() {
        byte[] g=rest(58);for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++)g[y*400+x]=0;
        dot(g,194,72,3);duration(g,List.of(),1.5);
    }
    private static byte[] flaggedRest(boolean sixteenth) {
        byte[] g=new byte[400*240];Arrays.fill(g,(byte)255);
        for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++)g[y*400+x]=0;
        for(int cy:new int[]{102,sixteenth?118:102})
            for(int y=cy-5;y<=cy+5;y++)for(int x=171;x<=183;x++)
                if(Math.pow((x-177)/6.0,2)+Math.pow((y-cy)/5.0,2)<=1)g[y*400+x]=0;
        int end=sixteenth?144:128;
        for(int y=99;y<=end;y++) {
            int x=185-(y-99)*12/(end-99);g[y*400+x]=0;g[y*400+x+1]=0;
        }
        return g;
    }
    @Test public void dottedEighthRestUsesThreeQuarterBeats() {
        byte[] g=flaggedRest(false);dot(g,197,104,3);duration(g,List.of(),.75);
    }
    @Test public void dottedSixteenthRestUsesThreeEighthBeats() {
        byte[] g=flaggedRest(true);dot(g,197,104,3);duration(g,List.of(),.375);
    }
}
