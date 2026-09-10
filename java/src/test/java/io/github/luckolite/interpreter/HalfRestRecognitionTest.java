// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original staff, rest rectangles and note ellipses. */
public class HalfRestRecognitionTest {
    static final int W=400,H=240;
    static final List<MeasureRegion> M=List.of(new MeasureRegion(0,1,.2f,.8f));
    static class Page {
        byte[] gray=new byte[W*H],labels=new byte[W*H];
        Page(){Arrays.fill(gray,(byte)240);for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++){gray[y*W+x]=0;labels[y*W+x]=4;}}
        void rect(int left,int top,int right,int bottom){for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++)gray[y*W+x]=0;}
        void ellipse(int cx,int cy,int rx,int ry,boolean head){for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1){gray[y*W+x]=0;if(head)labels[y*W+x]=2;}}
        List<ScoreRestEvent> detect(List<ScoreNoteEvent> notes){return SixteenthRestDetector.detect(gray,W,H,M,List.of(new SixteenthRestDetector.Staff(80,144,16,0,1)),notes);}
        void half(){rect(100,104,119,111);}
    }
    @Test public void aFilledRectangleOnTheMiddleRuleIsTwoBeatsOfSilence(){var p=new Page();p.half();var rests=p.detect(List.of());assertEquals(1,rests.size());assertEquals(2,rests.get(0).durationBeats(),0);}
    @Test public void aDotExtendsTheHalfRestToThreeBeats(){var p=new Page();p.half();p.ellipse(134,104,3,3,false);assertEquals(3,p.detect(List.of()).get(0).durationBeats(),0);}
    @Test public void coordinatesKeepTheOriginalRectangle(){var p=new Page();p.half();var r=p.detect(List.of()).get(0);assertEquals(109.5,r.positionInMeasure()*W,.01);assertEquals(107.5,r.pageY()*H,.01);}
    @Test public void aHangingWholeRestIsNotMisreadAsAHalfRest(){var p=new Page();p.rect(100,97,119,104);assertTrue(p.detect(List.of()).isEmpty());}
    @Test public void aThinTenutoMarkIsNotAHalfRest(){var p=new Page();p.rect(100,109,119,110);assertTrue(p.detect(List.of()).isEmpty());}
    @Test public void anOvalHeadDoesNotSupplyRectangularRestProof(){var p=new Page();p.ellipse(110,107,9,4,false);assertTrue(p.detect(List.of()).isEmpty());}
    @Test public void aBeamJoinedToItsStemIsNotARest(){var p=new Page();p.half();p.rect(119,96,120,146);assertTrue(p.detect(List.of()).isEmpty());}
    @Test public void anOwnedNoteColumnStillProtectsTheNote(){var p=new Page();p.half();var n=new ScoreNoteEvent(0,.274f,5,0,1,107.5f/H,false,0,1);assertTrue(p.detect(List.of(n)).isEmpty());}
    @Test public void rawExtractionAttachesTheHalfRestToTheFollowingPitch(){var p=new Page();p.half();p.ellipse(280,136,9,6,true);for(int y=90;y<=136;y++){p.gray[y*W+289]=0;p.labels[y*W+289]=1;}var a=OmrScoreInterpreter.analyze(p.labels,p.gray,W,H,M);assertEquals(a.notes().toString(),1,a.notes().size());assertEquals(1,a.notes().get(0).staffStep());assertEquals(2,a.notes().get(0).leadingRestBeats(),0);}
    @Test public void rawAndSemanticInputsRemainUnmodified(){var p=new Page();p.half();byte[] g=p.gray.clone(),l=p.labels.clone();p.detect(List.of());assertArrayEquals(g,p.gray);assertArrayEquals(l,p.labels);}
}
