// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original tilted five-rule pages, with known staff-relative pitches. */
public class HalfSpacingStaffAliasTest {
    @Test public void negativeTiltCannotHalveThePrintedStaffSpacing() {
        var page=new CurvedRestRecognitionTest.Page(true,0,false,0);var a=page.analyze();
        assertEquals(a.notes().toString(),1,a.notes().size());assertEquals(1,a.notes().get(0).staffStep());
        assertEquals(1,a.rests().size());
    }
    @Test public void bothSlopeDirectionsAgreeOnThePrintedPitch() {
        for(boolean reverse:new boolean[]{false,true}){
            var page=new CurvedRestRecognitionTest.Page(reverse,0,false,0);
            assertEquals(1,page.analyze().notes().get(0).staffStep());
        }
    }
    private static OmrScoreInterpreter.Analysis miniature(float slope,int gap) {
        int w=1200,h=360,bottom=210;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)240);
        for(int x=30;x<w-30;x++)for(int line=0;line<5;line++)for(int dy=0;dy<2;dy++){
            int y=Math.round(bottom-line*gap+slope*(x-w*.5f))+dy;gray[y*w+x]=0;labels[y*w+x]=4;
        }
        for(int cx:new int[]{220,600,1020}){
            int cy=bottom-gap;
            for(int y=cy-3;y<=cy+3;y++)for(int x=cx-5;x<=cx+5;x++)if(Math.pow((x-cx)/5.0,2)+Math.pow((y-cy)/3.0,2)<=1){int at=Math.round(y+slope*(x-w*.5f))*w+x;gray[at]=0;labels[at]=2;}
            for(int y=cy-gap*3;y<=cy;y++){int at=Math.round(y+slope*(cx+5-w*.5f))*w+cx+5;gray[at]=0;labels[at]=1;}
        }
        return OmrScoreInterpreter.analyze(labels,gray,w,h,List.of(new MeasureRegion(.02f,.98f,.2f,.8f)));
    }
    @Test public void genuinelySmallStaffIsNotPromotedToDoubleSpacing() {
        var a=miniature(-.025f,8);assertEquals(a.notes().toString(),3,a.notes().size());
        for(var n:a.notes())assertEquals(2,n.staffStep());
    }
    @Test public void separatedHeadPositionsKeepOnePitchAcrossTilt() {
        var a=miniature(-.025f,16);assertEquals(a.notes().toString(),3,a.notes().size());
        for(var n:a.notes())assertEquals(2,n.staffStep());
    }
    @Test public void horizontalStaffKeepsItsOriginalSpacing() {
        var a=miniature(0,16);assertEquals(3,a.notes().size());for(var n:a.notes())assertEquals(2,n.staffStep());
    }
    @Test public void decodingLeavesTheSemanticRulesAndRawPixelsIntact() {
        var page=new CurvedRestRecognitionTest.Page(true,0,false,0);byte[] a=page.labels.clone(),b=page.gray.clone();page.analyze();
        assertArrayEquals(a,page.labels);assertArrayEquals(b,page.gray);
    }
}
