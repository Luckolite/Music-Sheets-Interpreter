// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original rest polygon with independently varied semantic head predictions. */
public class QuarterRestBodySizeTest {
    private static CurvedRestRecognitionTest.Page page(boolean reverse,int rx,int ry,int dots) {
        var p=new CurvedRestRecognitionTest.Page(reverse,0,false,dots);
        prediction(p,887,171,rx,ry);
        return p;
    }
    private static void prediction(CurvedRestRecognitionTest.Page p,int cx,int cy,int rx,int ry) {
        for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
            if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1)
                p.labels[Math.round(y+p.shift(x))*CurvedRestRecognitionTest.W+x]=2;
    }
    @Test public void aWiderBodyPredictionIsNotRequiredToHaveDotDimensions() {
        var a=page(false,6,4,0).analyze();assertEquals(a.notes().toString(),1,a.notes().size());
        assertEquals(1,a.rests().size());assertEquals(1,a.rests().get(0).durationBeats(),0);
    }
    @Test public void aTallerBodyPredictionIsAlsoRemoved() {
        var a=page(false,6,6,0).analyze();assertEquals(a.notes().toString(),1,a.notes().size());
        assertEquals(1,a.notes().get(0).leadingRestBeats(),0);
    }
    @Test public void reverseSlopedRulesKeepTheSameRestOwnership() {
        var a=page(true,6,6,0).analyze();assertEquals(a.notes().toString(),1,a.notes().size());assertEquals(1,a.rests().size());
    }
    @Test public void realPitchAndRestLocationRemainStable() {
        var p=page(false,6,6,0);var a=p.analyze();assertEquals(1,a.notes().get(0).staffStep());
        var r=a.rests().get(0);assertEquals(886.5,(.02f+r.positionInMeasure()*.96f)*1200,2);
    }
    @Test public void aDottedRestRetainsItsDotAndSilence() {
        var a=page(false,6,6,1).analyze();assertEquals(1,a.notes().size());assertEquals(1.5,a.notes().get(0).leadingRestBeats(),0);
    }
    @Test public void independentlyVerifiedBodyAlsoOwnsItsPredictedSecondDot() {
        var p=page(false,6,6,2);prediction(p,917,164,3,3);var a=p.analyze();
        assertEquals(a.notes().toString(),1,a.notes().size());
        assertEquals(1.75,a.rests().get(0).durationBeats(),0);
        assertEquals(1.75,a.notes().get(0).leadingRestBeats(),0);
    }
    @Test public void nearbyGenuineSmallHeadRemainsAHead() {
        var p=page(false,6,6,0);p.ellipse(600,188,6,6,2);var a=p.analyze();
        assertEquals(a.notes().toString(),2,a.notes().size());
        assertTrue(a.notes().stream().anyMatch(n->Math.abs((.02f+n.positionInMeasure()*.96f)*1200-600)<3));
    }
    @Test public void aLargerHeadBesideTheRestIsNotDemotedAsADot() {
        var p=page(false,6,6,0);p.ellipse(910,164,6,6,2);var a=p.analyze();
        assertEquals(a.notes().toString(),2,a.notes().size());
        assertTrue(a.notes().stream().anyMatch(n->Math.abs((.02f+n.positionInMeasure()*.96f)*1200-910)<3));
    }
    @Test public void attachedStemProtectsAnOverlappingRealHead() {
        var p=page(false,6,6,0);p.ellipse(887,171,6,6,2);for(int y=120;y<=171;y++)p.pixel(893,y,1);
        assertTrue(p.analyze().notes().stream().anyMatch(n->Math.abs((.02f+n.positionInMeasure()*.96f)*1200-887)<3));
    }
    @Test public void withoutTheLowerHookThereIsNoCompleteRestProof() {
        var p=page(false,6,6,0);
        for(int y=181;y<200;y++)for(int x=879;x<897;x++){
            int at=Math.round(y+p.shift(x))*1200+x;
            if(p.labels[at]!=4)p.gray[at]=(byte)240;
        }
        assertTrue(p.analyze().notes().stream().anyMatch(n->Math.abs((.02f+n.positionInMeasure()*.96f)*1200-887)<3));
    }
    @Test public void analysisDoesNotRewriteLabelsOrGrayscale() {
        var p=page(false,6,6,0);byte[] labels=p.labels.clone(),gray=p.gray.clone();p.analyze();
        assertArrayEquals(labels,p.labels);assertArrayEquals(gray,p.gray);
    }
}
