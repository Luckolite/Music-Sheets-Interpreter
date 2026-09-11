// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original sharp and note drawings test pitch alignment independently of titles. */
public final class SharpPitchAlignmentTest {
    static final int W=420,H=260;
    static class Page {
        byte[] labels=new byte[W*H],gray=new byte[W*H];
        Page(int headY,int sharpY) {
            Arrays.fill(gray,(byte)255);
            for(int y=100;y<=164;y+=16)for(int x=10;x<410;x++)pixel(x,y,4);
            for(int y=headY-6;y<=headY+6;y++)for(int x=272;x<=288;x++)
                if(Math.pow((x-280)/8d,2)+Math.pow((y-headY)/6d,2)<=1)pixel(x,y,2);
            for(int y=headY;y<=headY+48;y++)pixel(272,y,1);
            for(int x:new int[]{251,252,259,260})for(int y=sharpY-22;y<=sharpY+22;y++)pixel(x,y,3);
            for(int center:new int[]{sharpY-5,sharpY+5})for(int y=center-1;y<=center+1;y++)for(int x=248;x<=263;x++)pixel(x,y,3);
        }
        void pixel(int x,int y,int label){labels[y*W+x]=(byte)label;gray[y*W+x]=0;}
        int accidental(byte[] raw) {
            return OmrScoreInterpreter.analyze(labels,raw,W,H,List.of(new MeasureRegion(0,1,.1f,.9f))).notes().stream()
                .filter(n->Math.abs(n.positionInMeasure()*W-280)<6).findFirst().orElseThrow().writtenAccidental();
        }
    }
    @Test public void matchingSharpApplies(){var p=new Page(124,124);assertEquals(1,p.accidental(p.gray));}
    @Test public void sharpOnAdjacentLowerPositionDoesNotApply(){var p=new Page(116,124);assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,p.accidental(p.gray));}
    @Test public void sharpOnAdjacentUpperPositionDoesNotApply(){var p=new Page(132,124);assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,p.accidental(p.gray));}
    @Test public void smallUpwardRoundingRetainsSharp(){var p=new Page(122,124);assertEquals(1,p.accidental(p.gray));}
    @Test public void smallDownwardRoundingRetainsSharp(){var p=new Page(126,124);assertEquals(1,p.accidental(p.gray));}
    @Test public void fartherSharpDoesNotApply(){var p=new Page(140,124);assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,p.accidental(p.gray));}
    @Test public void semanticOnlyInputUsesSamePitchAlignment(){var p=new Page(116,124);assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,p.accidental(null));}
    @Test public void inputArraysRemainUnchanged(){var p=new Page(116,124);var l=p.labels.clone();var g=p.gray.clone();p.accidental(p.gray);assertArrayEquals(l,p.labels);assertArrayEquals(g,p.gray);}
}
