// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original small ellipses, stems and ledger rules; no imported score pixels. */
public class GraceLedgerGeometryTest {
    static final int W=420,H=320;
    static final List<MeasureRegion> M=List.of(new MeasureRegion(.04f,.96f,.08f,.9f));
    static class Page {
        byte[] labels=new byte[W*H],gray=new byte[W*H];
        final int y;final boolean above;
        Page(boolean above,boolean stem,boolean outer,boolean inner){
            this.above=above;y=above?80:208;Arrays.fill(gray,(byte)255);
            for(int row=112;row<=176;row+=16)for(int x=15;x<405;x++){pixel(x,row,4);pixel(x,row+1,4);}
            if(outer)rule(90,110,y);if(inner)rule(90,110,y+(above?16:-16));
            head(100,y,7,5,false);
            if(stem)for(int row=Math.min(y,y+(above?32:-32));row<=Math.max(y,y+(above?32:-32));row++)pixel(above?93:107,row,1);
            int mainY=above?104:184;head(138,mainY,11,7,true);
            for(int row=mainY-48;row<=mainY;row++)pixel(149,row,1);
            head(250,160,11,7,false);for(int row=112;row<=160;row++)pixel(261,row,1);
        }
        void pixel(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
        void rule(int a,int b,int y){for(int x=a;x<=b;x++)pixel(x,y,4);}
        void head(int cx,int cy,int rx,int ry,boolean open){
            for(int yy=cy-ry;yy<=cy+ry;yy++)for(int x=cx-rx;x<=cx+rx;x++)
                if(Math.pow((x-cx)/(double)rx,2)+Math.pow((yy-cy)/(double)ry,2)<=1){
                    pixel(x,yy,2);if(open&&Math.pow((x-cx)/(double)(rx-3),2)+Math.pow((yy-cy)/(double)(ry-3),2)<1)gray[yy*W+x]=(byte)255;
                }
        }
        List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.analyze(labels,gray,W,H,M).notes();}
        ScoreNoteEvent grace(){return notes().stream().filter(n->Math.abs((M.get(0).left()+n.positionInMeasure()*.92f)*W-100)<3).findFirst().orElse(null);}
    }
    @Test public void shortLedgerRulesPreserveTheLowGracePitch(){var p=new Page(false,true,true,true);assertNotNull(p.grace());assertEquals(-4,p.grace().staffStep());}
    @Test public void shortLedgerRulesPreserveTheHighGracePitch(){var p=new Page(true,true,true,true);assertNotNull(p.grace());assertEquals(12,p.grace().staffStep());}
    @Test public void recoveredGraceIsAnOrnament(){var p=new Page(false,true,true,true);assertNotNull(p.grace());assertTrue((p.grace().articulations()&NoteOrnament.GRACE)!=0);}
    @Test public void principalKeepsItsPitchAndWrittenHalf(){var p=new Page(false,true,true,true);var n=p.notes().get(1);assertEquals(-1,n.staffStep());assertEquals(2,n.unbeamedDurationBeats(),.0001);}
    @Test public void graceBorrowsTimeFromPrincipal(){var p=new Page(false,true,true,true);var ns=p.notes();assertEquals(.25,ScoreNoteTiming.resolvedWrittenDurationBeats(ns.get(0),ns,3),.0001);assertEquals(1.75,ScoreNoteTiming.resolvedWrittenDurationBeats(ns.get(1),ns,3),.0001);}
    @Test public void oneShortUnderlineIsInsufficient(){assertNull(new Page(false,true,true,false).grace());}
    @Test public void anInnerRuleCannotReplaceTheHeadLedger(){assertNull(new Page(false,true,false,true).grace());}
    @Test public void aSmallUnstemmedBlobDoesNotGetReducedLedgerAllowance(){assertNull(new Page(false,false,true,true).grace());}
    @Test public void ledgerInkMustExtendOnBothSides(){var p=new Page(false,true,false,true);p.rule(100,120,p.y);assertNull(p.grace());}
    @Test public void aNearbySlurFragmentStaysSilent(){var p=new Page(false,true,true,true);p.head(119,220,3,2,false);assertEquals(3,p.notes().size());}
}
