// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original raw curves with deliberately incomplete semantic head islands. */
public class RawSlurFragmentTest {
    static final int W=400,H=320,G=16;
    static final List<MeasureRegion> M=List.of(new MeasureRegion(.025f,.975f,.08f,.92f));
    static class Page {
        final byte[] labels=new byte[W*H],gray=new byte[W*H];final boolean above;
        Page(boolean above,boolean curved,boolean stem,boolean ledgerOval) {
            this(above,curved,stem,ledgerOval,false);
        }
        Page(boolean above,boolean curved,boolean stem,boolean ledgerOval,boolean unequal) {
            this.above=above;Arrays.fill(gray,(byte)255);
            for(int y=112;y<=176;y+=G)for(int x=10;x<390;x++){pixel(x,y,4);pixel(x,y+1,4);}
            head(132,168,11,7,true,false);head(188,unequal?160:168,11,7,true,false);
            if(ledgerOval) {
                for(int x=140;x<=180;x++)raw(x,184);
                head(160,184,5,3,false,true);
            } else {
                for(int x=140;x<=180;x++) {
                    double t=(x-140)/40.0;
                    int y=curved?(int)Math.round(171+14*4*t*(1-t)):184;
                    for(int dy=-1;dy<=2;dy++)pixel(x,y+dy,5);
                }
                for(int y=181;y<=187;y++)for(int x=155;x<=165;x++)
                    if(Math.pow((x-160)/5.0,2)+Math.pow((y-184)/3.0,2)<=1)labels[mapY(y)*W+x]=2;
            }
            if(stem)for(int y=136;y<=184;y++)pixel(165,y,1);
        }
        int mapY(int y){return above?288-y:y;}
        void raw(int x,int y){gray[mapY(y)*W+x]=0;}
        void pixel(int x,int y,int label){raw(x,y);labels[mapY(y)*W+x]=(byte)label;}
        void head(int cx,int cy,int rx,int ry,boolean stem,boolean open){
            for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
                if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1){
                    pixel(x,y,2);
                    if(open&&Math.pow((x-cx)/(double)Math.max(1,rx-2),2)+Math.pow((y-cy)/(double)Math.max(1,ry-1),2)<1)gray[mapY(y)*W+x]=(byte)255;
                }
            if(stem)for(int y=cy-48;y<=cy;y++)pixel(cx+rx,y,1);
        }
        List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.analyze(labels,gray,W,H,M).notes();}
        ScoreNoteEvent island(){return notes().stream().filter(n->Math.abs((M.get(0).left()+n.positionInMeasure()*.95f)*W-160)<3).findFirst().orElse(null);}
        void widerIsland(){for(int y=181;y<=187;y++)for(int x=152;x<=168;x++)if(Math.pow((x-160)/8.0,2)+Math.pow((y-184)/3.0,2)<=1)labels[mapY(y)*W+x]=2;}
    }
    @Test public void lowerSlurBowlDoesNotBecomeANote(){assertNull(new Page(false,true,false,false).island());}
    @Test public void upperSlurBowlDoesNotBecomeANote(){assertNull(new Page(true,true,false,false).island());}
    @Test public void surroundingPrintedNotesKeepTheirPitches(){var ns=new Page(false,true,false,false).notes();assertEquals(2,ns.size());assertEquals(1,ns.get(0).staffStep());assertEquals(1,ns.get(1).staffStep());}
    @Test public void attachedStemProtectsASmallRealHead(){assertNotNull(new Page(false,true,true,false).island());}
    @Test public void straightInkAloneIsNotEvidenceOfASlur(){assertNotNull(new Page(false,false,false,false).island());}
    @Test public void smallLedgerOvalIsNotACurvedFragment(){assertNotNull(new Page(false,false,false,true).island());}
    @Test public void mirroredLedgerOvalIsPreserved(){assertNotNull(new Page(true,false,false,true).island());}
    @Test public void removedIslandNoLongerMasksThePrintedTie(){var p=new Page(false,true,false,false);p.widerIsland();assertTrue(p.notes().get(1).tiedFromPrevious());}
    @Test public void clearingFalseHeadLabelsDoesNotChangeInput(){var p=new Page(false,true,false,false);byte[] before=p.labels.clone();p.notes();assertArrayEquals(before,p.labels);}
    @Test public void slurBetweenDifferentPitchesDoesNotBecomeATie(){var ns=new Page(false,true,false,false,true).notes();assertEquals(2,ns.size());assertFalse(ns.get(1).tiedFromPrevious());}
}
