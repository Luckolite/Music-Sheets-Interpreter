// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometry only; no score scans, glyph fonts or model assets. */
public class CommonTimeMeterTest {
    private HeaderSymbolNormalizationTest.Page page(boolean cut) {
        return new HeaderSymbolNormalizationTest.Page(cut,true);
    }
    private int read(HeaderSymbolNormalizationTest.Page p) {
        return CommonTimeMeter.read(p.gray,p.w,p.h,110,142,80,16,0);
    }
    @Test public void openCIsFourFour(){assertEquals(4,read(page(false)));}
    @Test public void centralCutIsTwoTwo(){assertEquals(2,read(page(true)));}
    @Test public void candidateRequiresPrintedClef(){
        var p=new HeaderSymbolNormalizationTest.Page(true,false);
        assertTrue(CommonTimeMeter.candidates(p.labels,p.gray,p.w,p.h,80,16,0).isEmpty());
    }
    @Test public void commonAndCutKeepDistinctBeatUnits(){
        for(boolean cut:new boolean[]{false,true}) {
            var p=page(cut);var r=CommonTimeMeter.candidates(p.labels,p.gray,p.w,p.h,80,16,0);
            assertEquals(1,r.size());assertEquals(cut?2:4,r.get(0).numerator());
            assertEquals(cut?2:4,r.get(0).denominator());
        }
    }
    @Test public void closedOvalIsNotC(){
        var p=page(false);
        for(int y=101;y<=123;y++)for(int x=137;x<=142;x++)p.gray[y*p.w+x]=0;
        assertEquals(0,read(p));
    }
    @Test public void filledHeadIsNotC(){
        var p=page(false);p.eraseSign();p.note(126,112,16,12,false);assertEquals(0,read(p));
    }
    @Test public void hollowHeadIsNotC(){
        var p=page(false);p.eraseSign();p.note(126,112,16,9,true);assertEquals(0,read(p));
    }
    @Test public void pairedHollowHeadsAreNotC(){
        var p=page(false);p.eraseSign();p.note(126,104,16,7,true);p.note(126,120,16,7,true);
        assertEquals(0,read(p));
    }
    @Test public void stemOrBarAloneIsNotCutC(){
        var p=page(true);p.eraseSign();for(int y=80;y<=144;y++)p.gray[y*p.w+126]=0;
        assertEquals(0,read(p));
    }
    @Test public void noLeftArcIsNotC(){
        var p=page(false);
        for(int y=100;y<=124;y++)for(int x=110;x<120;x++)p.gray[y*p.w+x]=(byte)255;
        p.staff();assertEquals(0,read(p));
    }
    @Test public void onlyOneTerminalIsNotC(){
        var p=page(false);
        for(int y=115;y<=140;y++)for(int x=128;x<=144;x++)p.gray[y*p.w+x]=(byte)255;
        p.staff();assertEquals(0,read(p));
    }
    @Test public void sourceMasksStayUntouched(){
        var p=page(true);var labels=p.labels.clone();var gray=p.gray.clone();
        CommonTimeMeter.candidates(p.labels,p.gray,p.w,p.h,80,16,0);
        assertArrayEquals(labels,p.labels);assertArrayEquals(gray,p.gray);
    }
    @Test public void zigZagQuarterRestIsNotCutC(){
        var p=page(true);p.eraseSign();
        for(int y=88;y<=126;y++) {
            int x=y<104?122+(y-88)/2:y<112?130-(y-104):122+(y-112)/2;
            for(int dx=-2;dx<=2;dx++)p.gray[y*p.w+x+dx]=0;
        }
        for(int y=120;y<=135;y++)for(int x=115;x<=128;x++) {
            double outer=Math.pow((x-122)/7d,2)+Math.pow((y-127)/8d,2);
            double inner=Math.pow((x-122)/4d,2)+Math.pow((y-127)/5d,2);
            if(outer<=1&&inner>=1&&x<126)p.gray[y*p.w+x]=0;
        }
        p.staff();assertEquals(0,read(p));
    }
}
