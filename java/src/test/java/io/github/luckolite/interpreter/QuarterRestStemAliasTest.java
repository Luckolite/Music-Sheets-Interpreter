// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original quarter-rest polygon with a model head on its lower hook. */
public class QuarterRestStemAliasTest {
    private static CurvedRestRecognitionTest.Page page(boolean reverse,int dots) {
        var p=new CurvedRestRecognitionTest.Page(reverse,0,false,dots);
        for(int y=188;y<=198;y++)for(int x=883;x<=891;x++)
            if(Math.pow((x-887)/4.,2)+Math.pow((y-193)/5.,2)<=1)
                p.labels[Math.round(y+p.shift(x))*1200+x]=2;
        return p;
    }
    private static boolean hasHookHead(OmrScoreInterpreter.Analysis a) {
        return a.notes().stream().anyMatch(n->Math.abs((.02f+n.positionInMeasure()*.96f)*1200-887)<4);
    }
    @Test public void bridgingTheZigzagDoesNotTurnTheRestIntoANote() {
        var a=page(false,0).analyze();assertEquals(a.notes().toString(),1,a.notes().size());
        assertEquals(1,a.rests().size());assertEquals(1,a.notes().get(0).leadingRestBeats(),0);
    }
    @Test public void theRealFollowingPitchRemainsStable() {
        var a=page(false,0).analyze();assertEquals(1,a.notes().size());assertEquals(1,a.notes().get(0).staffStep());
    }
    @Test public void reverseSlopedStaffUsesTheSameRestProof() {
        var a=page(true,0).analyze();assertFalse(hasHookHead(a));assertEquals(1,a.rests().size());
    }
    @Test public void theDotStillExtendsSilence() {
        var a=page(false,1).analyze();assertEquals(1,a.notes().size());assertEquals(1.5,a.notes().get(0).leadingRestBeats(),0);
    }
    @Test public void twoDotsStillExtendSilence() {
        var a=page(false,2).analyze();assertEquals(1,a.notes().size());assertEquals(1.75,a.notes().get(0).leadingRestBeats(),0);
    }
    @Test public void aContinuousRealStemRetainsItsHead() {
        var p=page(false,0);p.ellipse(887,193,4,5,2);
        for(int y=145;y<=193;y++)p.pixel(891,y,1);
        assertTrue(hasHookHead(p.analyze()));
    }
    @Test public void aSinglePixelBreakDoesNotInvalidateARealStem() {
        var p=page(false,0);p.ellipse(887,193,4,5,2);
        for(int y=145;y<=193;y++)p.pixel(891,y,1);
        p.gray[Math.round(167+p.shift(891))*1200+891]=(byte)240;
        assertTrue(hasHookHead(p.analyze()));
    }
    @Test public void removingTheRestHookRemovesTheRestProof() {
        var p=page(false,0);
        for(int y=181;y<201;y++)for(int x=879;x<897;x++){
            int at=Math.round(y+p.shift(x))*1200+x;
            if(p.labels[at]!=4)p.gray[at]=(byte)240;
        }
        assertTrue(hasHookHead(p.analyze()));
    }
    @Test public void unrelatedSmallNotesRemainPresent() {
        var p=page(false,0);p.ellipse(600,188,4,5,2);
        var a=p.analyze();assertEquals(a.notes().toString(),2,a.notes().size());
        assertTrue(a.notes().stream().anyMatch(n->Math.abs((.02f+n.positionInMeasure()*.96f)*1200-600)<3));
    }
    @Test public void inputImagesRemainUnchanged() {
        var p=page(false,0);byte[] gray=p.gray.clone(),labels=p.labels.clone();p.analyze();
        assertArrayEquals(gray,p.gray);assertArrayEquals(labels,p.labels);
    }
}
