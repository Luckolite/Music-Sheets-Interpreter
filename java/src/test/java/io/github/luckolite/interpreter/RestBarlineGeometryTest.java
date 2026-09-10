// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class RestBarlineGeometryTest {
    private static final int W=320,H=180;
    private final byte[] labels=new byte[W*H], gray=new byte[W*H];

    private void ink(int x,int y,int value,int label) {
        if(x>=0&&x<W&&y>=0&&y<H){gray[y*W+x]=(byte)value;labels[y*W+x]=(byte)label;}
    }
    private void staff() {
        Arrays.fill(gray,(byte)255);
        for(int y=60;y<=108;y+=12)for(int x=16;x<=304;x++)ink(x,y,70,4);
    }
    private void bar(int shade,boolean interrupted) {
        for(int y=60;y<=108;y++)if(!interrupted||y!=77&&y!=78)
            for(int x=209;x<=211;x++)ink(x,y,shade,1);
    }
    @Test public void diagonalMultiFlagRestDoesNotDivideTheStaff() {
        staff();bar(30,false);
        for(int y=65;y<=108;y++) {
            int x=122-Math.round((y-65)*.20f);
            for(int dx=-1;dx<=1;dx++)ink(x+dx,y,30,1);
        }
        for(int cy:new int[]{65,77,89}) {
            int cx=118-Math.round((cy-65)*.20f);
            for(int dy=-3;dy<=3;dy++)for(int dx=-3;dx<=3;dx++)
                if(dx*dx+dy*dy<=10)ink(cx+dx,cy+dy,30,1);
        }
        var measures=OmrMeasurePostProcessor.process(labels,gray,W,H);
        assertEquals("Only the genuine barline divides this staff",2,measures.size());
        assertTrue(measures.get(0).right()>190f/W&&measures.get(0).right()<210f/W);
    }
    @Test public void faintBarWithSmallScanGapStillDividesTheStaff() {
        staff();bar(190,true);
        assertEquals(2,OmrMeasurePostProcessor.process(labels,gray,W,H).size());
    }
    @Test public void isolatedRestLikeVerticalFragmentIsNotABar() {
        staff();
        for(int y=71;y<=108;y++)for(int x=209;x<=211;x++)ink(x,y,30,1);
        assertEquals(1,OmrMeasurePostProcessor.process(labels,gray,W,H).size());
    }
    @Test public void rawStaffExtentKeepsTheEndingWhenSemanticStripeFades() {
        staff();
        for(int y=60;y<=108;y+=12)for(int x=260;x<=304;x++)labels[y*W+x]=0;
        var measures=OmrMeasurePostProcessor.process(labels,gray,W,H);
        assertEquals(1,measures.size());
        assertTrue(measures.get(0).right()>280f/W);
    }
    @Test public void strayHeaderLabelCannotCropTheFirstNote() {
        staff();
        for(int y=79;y<=89;y++)for(int x=82;x<=98;x++)
            if(Math.pow((x-90)/8d,2)+Math.pow((y-84)/5d,2)<=1)ink(x,y,0,2);
        for(int y=63;y<=68;y++)ink(105,y,0,3);
        var measures=OmrMeasurePostProcessor.process(labels,gray,W,H);
        assertEquals(1,measures.size());
        assertTrue(measures.get(0).left()<90f/W);
    }
    @Test public void longFadedStaffEndingUsesAllFivePrintedLines() {
        staff();
        for(int y=60;y<=108;y+=12)for(int x=210;x<=304;x++)labels[y*W+x]=0;
        var measures=OmrMeasurePostProcessor.process(labels,gray,W,H);
        assertEquals(1,measures.size());
        assertTrue(measures.get(0).right()>280f/W);
    }
    @Test public void mislabelledTrebleCurlDoesNotExposeTheClefAsANote() {
        staff();
        for(int y=42;y<=112;y++)for(int x=24;x<=32;x++)ink(x,y,0,3);
        for(int y=112;y<=122;y++)for(int x=26;x<=36;x++)ink(x,y,0,2);
        var measures=OmrMeasurePostProcessor.process(labels,gray,W,H);
        assertEquals(1,measures.size());
        assertTrue(measures.get(0).left()>36f/W);
    }
    @Test public void branchedMeterLikeStrokeIsNotAThinBarline() {
        staff();bar(30,false);
        for(int y=60;y<=108;y++)for(int x=79;x<=81;x++)ink(x,y,30,5);
        for(int cy:new int[]{67,80,91,103})for(int y=cy-2;y<=cy+2;y++)
            for(int x=70;x<=90;x++)ink(x,y,30,5);
        assertEquals(2,OmrMeasurePostProcessor.process(labels,gray,W,H).size());
    }
    @Test public void barlineCrossingOneSlurRetainsItsMeasureBoundary() {
        staff();bar(30,false);
        for(int y=77;y<=79;y++)for(int x=190;x<=230;x++)ink(x,y,30,5);
        assertEquals(2,OmrMeasurePostProcessor.process(labels,gray,W,H).size());
    }
}
