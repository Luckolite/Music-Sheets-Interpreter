// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original short systems sheared as a book scan, with stable pitch and bar ownership. */
public class BookTiltStaffTest {
    static final int W=1600,H=500,TOP=180,GAP=16;
    static class Drawing {
        final byte[] gray=new byte[W*H],labels=new byte[W*H];
        final float slope;
        Drawing(float slope,boolean printed) {
            this.slope=slope;Arrays.fill(gray,(byte)255);
            for(int line=0;line<5;line++)for(int x=80;x<=680;x++)
                ink(x,TOP+line*GAP,4,printed);
            for(int x:new int[]{80,380,680})for(int y=TOP;y<=TOP+4*GAP;y++)ink(x,y,1,printed);
            for(int i=0;i<4;i++) {
                int cx=new int[]{180,300,450,580}[i],cy=TOP+4*GAP-(i+1)*GAP;
                for(int y=cy-3*GAP;y<=cy;y++)ink(cx+9,y,1,printed);
                for(int dy=-6;dy<=6;dy++)for(int dx=-9;dx<=9;dx++)
                    if(dx*dx/81d+dy*dy/36d<=1)ink(cx+dx,cy+dy,2,printed);
            }
        }
        void ink(int x,int y,int label,boolean printed) {
            int row=Math.round(y+slope*(x-W*.5f));
            labels[row*W+x]=(byte)label;if(printed)gray[row*W+x]=0;
        }
    }
    private void check(float slope) {
        var d=new Drawing(slope,true);
        var measures=OmrMeasurePostProcessor.process(d.labels,d.gray,W,H);
        assertEquals("Printed middle bar survives shear",2,measures.size());
        var notes=OmrScoreInterpreter.extract(d.labels,d.gray,W,H,measures);
        assertEquals("Short tilted staff remains playable",4,notes.size());
        assertEquals(List.of(2,4,6,8),notes.stream().map(ScoreNoteEvent::staffStep).toList());
        assertEquals(List.of(0,0,1,1),notes.stream().map(ScoreNoteEvent::measureIndex).toList());
    }
    @Test public void leftTiltPreservesShortStaffPitchesAndMeasures(){check(-.07f);}
    @Test public void rightTiltPreservesShortStaffPitchesAndMeasures(){check(.07f);}
    @Test public void strongerBookTiltPreservesStaffPitchesAndMeasures(){check(-.12f);}
    @Test public void straightShortStaffRetainsPitchesAndMeasures(){check(0f);}
    @Test public void semanticRulesAloneCannotProveAShortTiltedStaff() {
        var d=new Drawing(-.07f,false);
        var measures=List.of(new MeasureRegion(.05f,.425f,.15f,.65f));
        assertTrue(OmrScoreInterpreter.extract(d.labels,d.gray,W,H,measures).isEmpty());
    }
}
