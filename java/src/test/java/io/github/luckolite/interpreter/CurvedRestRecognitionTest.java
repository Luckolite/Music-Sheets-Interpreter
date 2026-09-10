// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original quarter-rest polygon, staff rules and note ellipses drawn along a sloping page. */
public class CurvedRestRecognitionTest {
    static final int W=1200,H=360;
    static final List<MeasureRegion> M=List.of(new MeasureRegion(.02f,.98f,.2f,.8f));
    static class Page {
        final byte[] gray=new byte[W*H],labels=new byte[W*H];final boolean reverse;final int offset;
        Page(boolean reverse,int offset,boolean fragment,int dots){
            this.reverse=reverse;this.offset=offset;Arrays.fill(gray,(byte)240);
            for(int x=30;x<W-30;x++)for(int line=0;line<5;line++){pixel(x,140+16*line,4);pixel(x,141+16*line,4);}
            int[][] rows={{0,1,2},{1,2,3},{2,3,4},{3,4,5},{4,5,6},{5,6,7},{6,7,8},
                    {7,7,10},{8,7,11},{9,6,11},{10,6,11},{11,5,11},{12,5,11},
                    {13,4,10},{14,4,9},{15,5,9},{16,6,9},{17,7,10},{18,8,11},
                    {19,6,12},{20,4,13},{21,3,13},{22,2,13},{23,2,6},{24,3,6},
                    {25,3,6},{26,4,7},{27,5,7},{28,6,8},{29,7,9},{30,8,9}};
            for(int[] r:rows)for(int dy=0;dy<2;dy++)for(int x=r[1];x<=r[2];x++)pixel(880+x,150+offset+(int)Math.round(r[0]*1.5)+dy,0);
            if(fragment)for(int y=167;y<=177;y++)for(int x=884;x<=891;x++){int at=Math.round(y+offset+shift(x))*W+x;if((gray[at]&255)<170)labels[at]=2;}
            if(dots>0)ellipse(904,164+offset,3,3,5);if(dots>1)ellipse(917,164+offset,3,3,5);
            ellipse(1050,196,10,7,2);for(int y=148;y<=196;y++)pixel(1060,y,1);
        }
        float shift(int x){float v=30*x/(float)W-15;return reverse?-v:v;}
        void pixel(int x,int y,int label){int at=Math.round(y+shift(x))*W+x;gray[at]=0;labels[at]=(byte)label;}
        void ellipse(int cx,int cy,int rx,int ry,int label){for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1)pixel(x,y,label);}
        StaffPitchTrack track(){var t=StaffPitchTrack.detect(gray,W,H,140,204,16);assertNotNull(t);return t;}
        SixteenthRestDetector.Detection detect(List<ScoreNoteEvent> notes){return SixteenthRestDetector.detectWithDots(gray,W,H,M,List.of(new SixteenthRestDetector.Staff(140,204,16,0,1,track())),notes);}
        OmrScoreInterpreter.Analysis analyze(){return OmrScoreInterpreter.analyze(labels,gray,W,H,M);}
        ScoreNoteEvent owner(float x,float y,float duration){return new ScoreNoteEvent(0,(x/W-.02f)/.96f,1,0,1,(y+shift(Math.round(x)))/H,false,0,0,2,duration,1);}
    }
    @Test public void aSlopingStaffDoesNotJoinTheQuarterRestToItsRules(){var p=new Page(false,0,false,0);var d=p.detect(List.of());assertEquals(1,d.rests().size());assertEquals(1,d.rests().get(0).durationBeats(),.0001);}
    @Test public void theOppositeSlopeIsAlsoRecognized(){var p=new Page(true,0,false,0);assertEquals(1,p.detect(List.of()).rests().size());}
    @Test public void restCoordinatesMapBackToTheOriginalPage(){var p=new Page(false,0,false,0);var r=p.detect(List.of()).rests().get(0);assertEquals(886.5,(M.get(0).left()+r.positionInMeasure()*.96)*W,2);assertEquals(173.5+p.shift(887),r.pageY()*H,2);assertEquals(47,r.pageHeight()*H,3);}
    @Test public void augmentationDotCoordinatesAndDurationMapBack(){var p=new Page(false,0,false,1);var d=p.detect(List.of());assertEquals(1.5,d.rests().get(0).durationBeats(),.0001);assertEquals(1,d.dots().size());assertEquals(904,d.dots().get(0).x(),1);assertEquals(164+p.shift(904),d.dots().get(0).y(),1);assertEquals(d.rests().get(0),d.dots().get(0).rest());}
    @Test public void twoDotsRemainDoubleDottedAfterRectification(){var p=new Page(false,0,false,2);assertEquals(1.75,p.detect(List.of()).rests().get(0).durationBeats(),.0001);}
    @Test public void aRealNoteStillOwnsItsPrintedColumn(){var p=new Page(false,0,false,0);assertTrue(p.detect(List.of(p.owner(887,173,1))).rests().isEmpty());}
    @Test public void anUpperVoiceRestCanClearAnIndependentHeldNote(){var p=new Page(false,-16,false,0);assertEquals(p.detect(List.of()).rests().toString(),1,p.detect(List.of(p.owner(887,196,4))).rests().size());}
    @Test public void aRestTwoSpacesAboveTheStaffAlsoSurvives(){var p=new Page(false,-32,false,0);assertEquals(1,p.detect(List.of(p.owner(887,196,4))).rests().size());}
    @Test public void rectificationPreservesInputPixels(){var p=new Page(false,0,false,1);byte[] g=p.gray.clone(),l=p.labels.clone();p.detect(List.of());assertArrayEquals(g,p.gray);assertArrayEquals(l,p.labels);}
    @Test public void aCompactBodyPredictionDoesNotSound(){var p=new Page(false,0,true,0);var a=p.analyze();assertEquals(a.notes().toString(),1,a.notes().size());assertEquals(a.rests().toString(),1,a.rests().size());assertEquals(1,a.notes().get(0).leadingRestBeats(),.0001);}
    @Test public void bodyRemovalPreservesTheRealNotesPitch(){var p=new Page(false,0,true,0);assertEquals(1,p.analyze().notes().get(0).staffStep());}
    @Test public void aSmallHeadWithARealStemIsRetained(){var p=new Page(false,0,false,0);p.ellipse(887,173,4,4,2);for(int y=125;y<=173;y++)p.pixel(891,y,1);var a=p.analyze();assertTrue(a.notes().toString(),a.notes().stream().anyMatch(n->Math.abs((.02f+n.positionInMeasure()*.96f)*W-887)<3));}
    @Test public void bodyRecognitionDoesNotRemoveUnrelatedSmallHeads(){var p=new Page(false,0,false,0);p.ellipse(600,188,5,4,2);var a=p.analyze();assertEquals(a.notes().toString(),2,a.notes().size());}
}
