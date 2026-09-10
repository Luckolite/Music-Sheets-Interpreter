// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original polygonal quarter rest, dots and note ellipses. */
public class RestDotHeadTest {
    private static final int W=400,H=240;
    private static class Page {
        final byte[] gray=new byte[W*H],labels=new byte[W*H];
        Page(){Arrays.fill(gray,(byte)255);}
        void pixel(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
        void ellipse(int cx,int cy,int rx,int ry,int label) {
            for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
                if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1)pixel(x,y,label);
        }
        OmrScoreInterpreter.Analysis analyze() {
            return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(.05f,.95f,.1f,.8f)));
        }
    }
    private static Page page(boolean rest,int dots,int dotLabel) {
        Page p=new Page();
        for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++)p.pixel(x,y,4);
        if(rest) {
            int[][] rows={{0,1,2},{1,2,3},{2,3,4},{3,4,5},{4,5,6},{5,6,7},{6,7,8},
                    {7,7,10},{8,7,11},{9,6,11},{10,6,11},{11,5,11},{12,5,11},
                    {13,4,10},{14,4,9},{15,5,9},{16,6,9},{17,7,10},{18,8,11},
                    {19,6,12},{20,4,13},{21,3,13},{22,2,13},{23,2,6},{24,3,6},
                    {25,3,6},{26,4,7},{27,5,7},{28,6,8},{29,7,9},{30,8,9}};
            for(int[] row:rows)for(int dy=0;dy<2;dy++)for(int x=row[1];x<=row[2];x++)
                p.pixel(170+x,90+(int)Math.round(row[0]*1.5)+dy,0);
        }
        if(dots>0)p.ellipse(194,104,3,3,dotLabel);
        if(dots>1)p.ellipse(207,104,3,3,dotLabel);
        p.ellipse(270,144,10,7,2);
        for(int y=96;y<144;y++)p.pixel(280,y,1);
        return p;
    }
    @Test public void aPredictedDotHeadDoesNotSound() {
        var s=page(true,1,2).analyze();assertEquals(1,s.notes().size());assertEquals(1,s.rests().size());
        assertEquals(1.5,s.rests().get(0).durationBeats(),.0001);
        assertEquals(1.5,s.notes().get(0).leadingRestBeats(),.0001);
    }
    @Test public void twoPredictedDotHeadsGiveADoubleDottedRest() {
        var s=page(true,2,2).analyze();assertEquals(1,s.notes().size());assertEquals(1,s.rests().size());
        assertEquals(1.75,s.rests().get(0).durationBeats(),.0001);
    }
    @Test public void ordinarySymbolDotsKeepTheSameRestValue() {
        var s=page(true,2,5).analyze();assertEquals(1,s.notes().size());assertEquals(1.75,s.rests().get(0).durationBeats(),.0001);
    }
    @Test public void aSmallHeadWithAStemRemainsANote() {
        Page p=page(true,1,2);for(int y=56;y<104;y++)p.pixel(197,y,1);
        var s=p.analyze();assertEquals(2,s.notes().size());
    }
    @Test public void nearbyInkNeedsAnIndependentlyRecognizedRest() {
        var s=page(false,1,2).analyze();assertEquals(2,s.notes().size());assertTrue(s.rests().isEmpty());
    }
    @Test public void aNormalSizeHeadCannotBeDemotedToARestDot() {
        Page p=page(true,0,2);p.ellipse(200,104,10,7,2);
        assertEquals(2,p.analyze().notes().size());
    }
}
