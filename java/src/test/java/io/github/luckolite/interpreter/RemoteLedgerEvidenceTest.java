// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ellipses and rules distinguish remote notes from underlined instruction ink. */
public class RemoteLedgerEvidenceTest {
    static final int W=480,H=384,GAP=16;
    static final List<MeasureRegion> M=List.of(new MeasureRegion(.025f,.975f,.04f,.92f));
    static class Page {
        final byte[] labels=new byte[W*H],gray=new byte[W*H];
        final int y, direction;
        Page(boolean above,boolean space,boolean complete,boolean stem,boolean hollow) {
            this(above,space,complete,stem,hollow,false);
        }
        Page(boolean above,boolean space,boolean complete,boolean stem,boolean hollow,boolean small) {
            Arrays.fill(gray,(byte)255);
            for(int row=128;row<=192;row+=GAP)rule(14,465,row,4);
            direction=above?1:-1;
            y=(above?128:192)+(above?-1:1)*(space?72:64);
            int nearest=y+(space?direction*8:0);
            int left=small?95:86,right=small?115:124;
            for(int n=0;n<(complete?4:2);n++)rule(left,right,nearest+direction*n*GAP,4);
            head(105,y,small?7:11,small?5:7,hollow);
            // Printed ledger ink remains visible through the hollow center.
            for(int n=0;n<(complete?4:2);n++)for(int x=left;x<=right;x++)gray[(nearest+direction*n*GAP)*W+x]=0;
            if(stem)stem(105+(above?-1:1)*(small?7:11),y,y+direction*(small?32:48));
            head(300,176,11,7,false);stem(311,176,128);
        }
        void pixel(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
        void rule(int left,int right,int y,int label){for(int x=left;x<=right;x++)pixel(x,y,label);}
        void stem(int x,int a,int b){for(int y=Math.min(a,b);y<=Math.max(a,b);y++){pixel(x,y,1);pixel(x+1,y,1);}}
        void head(int cx,int cy,int rx,int ry,boolean hollow){
            for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
                if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1){
                    pixel(x,y,2);
                    if(hollow&&Math.pow((x-cx)/(double)(rx-3),2)+Math.pow((y-cy)/(double)(ry-3),2)<1)gray[y*W+x]=(byte)255;
                }
        }
        List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.analyze(labels,gray,W,H,M).notes();}
        ScoreNoteEvent remote(){return notes().stream().filter(n->Math.abs((M.get(0).left()+n.positionInMeasure()*.95f)*W-105)<3).findFirst().orElse(null);}
    }
    @Test public void twoUnderlinesBelowStaffAreNotFourLedgerNotation(){assertNull(new Page(false,false,false,false,true).remote());}
    @Test public void twoUnderlinesAboveStaffAreNotFourLedgerNotation(){assertNull(new Page(true,false,false,false,true).remote());}
    @Test public void verticalLetterStrokeDoesNotValidateMissingLedgerSeries(){assertNull(new Page(false,false,false,true,false).remote());}
    @Test public void remoteUpperLineKeepsPitch(){var n=new Page(true,false,true,true,false).remote();assertNotNull(n);assertEquals(16,n.staffStep());}
    @Test public void remoteLowerLineKeepsPitch(){var n=new Page(false,false,true,true,false).remote();assertNotNull(n);assertEquals(-8,n.staffStep());}
    @Test public void remoteUpperSpaceKeepsPitch(){var n=new Page(true,true,true,true,false).remote();assertNotNull(n);assertEquals(17,n.staffStep());}
    @Test public void remoteLowerSpaceKeepsPitch(){var n=new Page(false,true,true,true,false).remote();assertNotNull(n);assertEquals(-9,n.staffStep());}
    @Test public void wholeNoteWithoutStemRetainsItsLedgers(){var n=new Page(false,false,true,false,true).remote();assertNotNull(n);assertEquals(-8,n.staffStep());assertEquals(4,n.unbeamedDurationBeats(),.0001);}
    @Test public void highWholeNoteWithoutStemRetainsItsLedgers(){var n=new Page(true,false,true,false,true).remote();assertNotNull(n);assertEquals(16,n.staffStep());}
    @Test public void ordinaryStaffNoteRemainsWhenInstructionRejected(){var ns=new Page(false,false,false,false,true).notes();assertEquals(1,ns.size());assertEquals(2,ns.get(0).staffStep());}
    @Test public void smallRemoteHeadKeepsShortLowerLedgers(){var n=new Page(false,false,true,true,false,true).remote();assertNotNull(n);assertEquals(-8,n.staffStep());}
    @Test public void smallRemoteHeadKeepsShortUpperLedgers(){var n=new Page(true,false,true,true,false,true).remote();assertNotNull(n);assertEquals(16,n.staffStep());}
}
