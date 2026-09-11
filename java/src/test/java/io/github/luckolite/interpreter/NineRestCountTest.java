// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class NineRestCountTest {
    private final int w=400,h=240;
    private final byte[] gray=new byte[w*h],labels=new byte[w*h];
    private final MeasureRegion region=new MeasureRegion(.2f,.8f,.35f,.85f);
    private void rect(int x,int y,int ww,int hh,int label) {
        for(int yy=y;yy<y+hh;yy++)for(int xx=x;xx<x+ww;xx++) {
            gray[yy*w+xx]=0;labels[yy*w+xx]=(byte)label;
        }
    }
    private void prepare() {
        Arrays.fill(gray,(byte)255);
        for(int i=0;i<5;i++)rect(40,100+i*16,320,1,4);
        rect(110,128,180,12,1);rect(110,122,2,24,1);rect(288,122,2,24,1);
    }
    private int count() {
        var counts=MultiMeasureRestDetector.detect(labels,gray,w,h,List.of(region),List.of());
        assertEquals(1,counts.size());return counts.get(0).value();
    }
    @Test public void aClosedUpperBowlAndCurvingTailMeanNine() {
        prepare();
        rect(194,61,14,4,5);rect(190,65,5,11,5);rect(207,64,5,24,5);
        rect(190,75,22,5,5);rect(193,88,15,5,5);rect(190,85,5,5,5);
        assertEquals(9,count());
    }
    @Test public void aClosedFourKeepsItsStraightLowerStem() {
        prepare();
        rect(204,61,5,32,5);rect(190,78,22,5,5);
        for(int y=61;y<80;y++)rect(204-(y-61)*14/18,y,3,1,5);
        assertEquals(4,count());
    }
}
