// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class NoteArticulationDetectorTest {
    private static final int W=1000,H=1000;
    private byte[] paper(){byte[] p=new byte[W*H];Arrays.fill(p,(byte)255);return p;}
    private void line(byte[] p,int x0,int y0,int x1,int y1) {
        int steps=Math.max(Math.abs(x1-x0),Math.abs(y1-y0));
        for(int i=0;i<=steps;i++){int x=x0+Math.round((x1-x0)*i/(float)Math.max(1,steps));
            int y=y0+Math.round((y1-y0)*i/(float)Math.max(1,steps));p[y*W+x]=0;}
    }
    private int detect(byte[] p){return NoteArticulationDetector.detect(new byte[W*H],p,W,H,
            List.of(new NoteArticulationDetector.Anchor(300,300,12,0)))[0];}
    @Test public void readsFiveMarksAndDotDashCombination() {
        byte[] p=paper();line(p,292,270,308,276);line(p,308,276,292,282);
        assertEquals(NoteArticulation.ACCENT,detect(p));
        p=paper();line(p,294,282,300,268);line(p,300,268,306,282);
        assertEquals(NoteArticulation.MARCATO,detect(p));
        p=paper();for(int y=276;y<=279;y++)for(int x=298;x<=301;x++)p[y*W+x]=0;
        assertEquals(NoteArticulation.STACCATO,detect(p));
        line(p,295,270,305,270);assertEquals(NoteArticulation.STACCATO|NoteArticulation.TENUTO,detect(p));
        p=paper();line(p,294,276,306,276);assertEquals(NoteArticulation.TENUTO,detect(p));
        p=paper();for(int y=270;y<=283;y++){int radius=Math.round(3*(283-y)/13f);
            for(int x=300-radius;x<=300+radius;x++)p[y*W+x]=0;}
        assertEquals(NoteArticulation.STACCATISSIMO,detect(p));
    }
    @Test public void rejectsBowingSlursLedgerLinesAndDurationDots() {
        byte[] p=paper();line(p,294,270,300,283);line(p,300,283,306,270);assertEquals(0,detect(p));
        p=paper();line(p,294,283,294,270);line(p,294,270,306,270);line(p,306,270,306,283);
        assertEquals(0,detect(p));
        p=paper();for(int x=282;x<=318;x++) {int y=270+Math.round(8*(float)Math.pow((x-300)/18f,2));p[y*W+x]=0;}
        assertEquals(0,detect(p));
        p=paper();for(int y=298;y<=301;y++)for(int x=310;x<=313;x++)p[y*W+x]=0;
        assertEquals(0,detect(p));
        p=paper();line(p,294,276,306,276);byte[] labels=new byte[W*H];
        for(int x=294;x<=306;x++)labels[276*W+x]=OmrMeasurePostProcessor.NOTEHEAD;
        assertEquals(0,NoteArticulationDetector.detect(labels,p,W,H,List.of(new NoteArticulationDetector.Anchor(300,300,12,0)))[0]);
    }
    @Test public void chordOwnershipCannotJumpToAnotherRowOrStaff() {
        byte[] p=paper();line(p,292,270,308,276);line(p,308,276,292,282);
        int[] marks=NoteArticulationDetector.detect(new byte[W*H],p,W,H,List.of(
                new NoteArticulationDetector.Anchor(300,300,12,0),
                new NoteArticulationDetector.Anchor(301,310,12,0),
                new NoteArticulationDetector.Anchor(324,300,12,0),
                new NoteArticulationDetector.Anchor(300,440,12,1)));
        assertArrayEquals(new int[]{1,1,0,0},marks);
    }
    @Test public void belowNoteMarksAndSemanticOnlyMasksWork() {
        byte[] p=paper();line(p,294,318,300,332);line(p,300,332,306,318);
        assertEquals(NoteArticulation.MARCATO,detect(p));
        byte[] labels=new byte[W*H];for(int i=0;i<p.length;i++)if(p[i]==0)labels[i]=OmrMeasurePostProcessor.SYMBOL;
        assertEquals(NoteArticulation.MARCATO,NoteArticulationDetector.detect(labels,null,W,H,
                List.of(new NoteArticulationDetector.Anchor(300,300,12,0)))[0]);
    }
    @Test public void fermataArchRejectsItsDotButStraightStaffDoesNotHideStaccato() {
        byte[] p=paper();
        for(int y=276;y<=279;y++)for(int x=298;x<=301;x++)p[y*W+x]=0;
        line(p,292,272,296,268);line(p,296,268,303,268);line(p,303,268,307,272);
        assertEquals(0,detect(p));
        p=paper();
        for(int y=276;y<=279;y++)for(int x=298;x<=301;x++)p[y*W+x]=0;
        line(p,260,270,340,270);
        byte[] labels=new byte[W*H];for(int x=260;x<=340;x++)labels[270*W+x]=OmrMeasurePostProcessor.STAFF;
        assertEquals(NoteArticulation.STACCATO,NoteArticulationDetector.detect(labels,p,W,H,
                List.of(new NoteArticulationDetector.Anchor(300,300,12,0)))[0]);
    }
}
