// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original sloped rules and elliptical rest bulbs; no score pixels or fonts. */
public class SymbolStaffRestTest {
    static final int W=1600,H=400;
    static final float TOP=150,GAP=14.25f,BOTTOM=207;
    static final List<MeasureRegion> M=List.of(new MeasureRegion(.02f,.98f,.2f,.8f));
    static class Page {
        final byte[] gray=new byte[W*H];
        final boolean reversed;
        Page(boolean reversed,int lines,int ruleInk,boolean narrow){
            this.reversed=reversed;Arrays.fill(gray,(byte)245);
            for(int x=narrow?600:40;x<(narrow?1000:1560);x++)for(int line=0;line<lines;line++) {
                int y=Math.round(TOP+line*GAP+shift(x));
                gray[y*W+x]=(byte)ruleInk;
                gray[(y+1)*W+x]=(byte)Math.min(240,ruleInk+35);
            }
        }
        float shift(int x){float delta=9*(x/(float)W-.5f);return reversed?-delta:delta;}
        void ink(int x,float y){gray[Math.round(y+shift(x))*W+x]=35;}
        void rest(int cx,boolean tail){
            for(int y=167;y<=173;y++)for(int x=cx-4;x<=cx+4;x++)
                if(Math.pow((x-cx)/4d,2)+Math.pow((y-170)/3d,2)<=1)ink(x,y);
            if(tail)for(int y=167;y<=193;y++){int x=cx+8-(y-167)*10/26;ink(x,y);ink(x+1,y);}
        }
        StaffPitchTrack track(){return StaffPitchTrack.detectForSymbols(gray,W,H,TOP,BOTTOM,GAP);}
        List<ScoreRestEvent> rests(List<ScoreNoteEvent> notes){return SixteenthRestDetector.detect(gray,W,H,M,
                List.of(new SixteenthRestDetector.Staff(TOP,BOTTOM,GAP,0,1)),notes);}
    }
    @Test public void subtleSlopingRulesDoNotEraseRestBulbs(){
        var p=new Page(false,5,160,false);for(int x:new int[]{260,650,1050,1400})p.rest(x,true);
        var r=p.rests(List.of());assertEquals(r.toString(),4,r.size());
        for(var rest:r)assertEquals(.5,rest.durationBeats(),0);
    }
    @Test public void oppositeSlantKeepsTheSameFourRests(){
        var p=new Page(true,5,160,false);for(int x:new int[]{260,650,1050,1400})p.rest(x,true);
        assertEquals(p.rests(List.of()).toString(),4,p.rests(List.of()).size());
    }
    @Test public void paleRulesStillRequireLocalContrast(){
        var p=new Page(false,5,205,false);assertNotNull(p.track());
        assertNull(new Page(false,5,235,false).track());
    }
    @Test public void incompleteFourLineGroupCannotInventAStaff(){assertNull(new Page(false,4,160,false).track());}
    @Test public void sixthRuleMakesThePhaseAmbiguous(){assertNull(new Page(false,6,160,false).track());}
    @Test public void narrowFragmentsCannotEstablishPageWideTracking(){assertNull(new Page(false,5,160,true).track());}
    @Test public void blankPageDoesNotCreateATrack(){var p=new Page(false,0,160,false);assertNull(p.track());assertTrue(p.rests(List.of()).isEmpty());}
    @Test public void aBulbWithoutItsTailCannotBecomeSilence(){var p=new Page(false,5,160,false);p.rest(1050,false);assertTrue(p.rests(List.of()).isEmpty());}
    @Test public void noteColumnOwnershipStillRejectsAFalseRest(){
        var p=new Page(false,5,160,false);p.rest(1050,true);
        var n=new ScoreNoteEvent(0,(1050f/W-.02f)/.96f,3,0,1,(180+p.shift(1050))/H,false,0,1);
        assertTrue(p.rests(List.of(n)).isEmpty());
    }
    @Test public void symbolTrackingAndRestDetectionPreservePixels(){var p=new Page(false,5,160,false);p.rest(1050,true);var before=p.gray.clone();p.rests(List.of());assertArrayEquals(before,p.gray);}
}
