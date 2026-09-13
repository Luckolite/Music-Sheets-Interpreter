// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original small natural glyphs and incomplete or differently shaped controls. */
public class CompactNaturalTest {
    private static final int W=320,H=220;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    public CompactNaturalTest(){
        Arrays.fill(gray,(byte)255);
        for(int y=100;y<=164;y+=16)box(10,y,300,1,4);
        for(int y=118;y<=130;y++)for(int x=212;x<=228;x++)
            if(Math.pow((x-220)/8d,2)+Math.pow((y-124)/6d,2)<=1)box(x,y,1,1,2);
        box(212,124,1,48,1);
        box(195,113,2,17,3);box(202,119,2,16,3);
        box(195,119,9,3,3);box(195,127,9,3,3);
    }
    private void box(int x,int y,int w,int h,int label){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++){labels[yy*W+xx]=(byte)label;gray[yy*W+xx]=label==0?(byte)255:0;}}
    private int accidental(){return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(0,1,.15f,.9f))).notes().stream().filter(n->Math.abs(n.positionInMeasure()*W-220)<5).findFirst().orElseThrow().writtenAccidental();}
    @Test public void completeCompactNaturalSurvivesRasterRounding(){assertEquals(0,accidental());}
    @Test public void oneMissingConnectorDoesNotProveNatural(){box(197,127,5,3,0);assertNotEquals(0,accidental());}
    @Test public void symmetricSpinesAreNotNatural(){box(202,113,2,6,3);box(195,129,2,6,3);assertNotEquals(0,accidental());}
    @Test public void detachedLowerInkCannotCompleteASpine(){box(202,129,2,4,0);assertNotEquals(0,accidental());}
    @Test public void undersizedMarkDoesNotBecomeNatural(){box(195,113,2,5,0);box(202,130,2,5,0);assertNotEquals(0,accidental());}
    @Test public void sourceArraysStayUnchanged(){var l=labels.clone();var g=gray.clone();accidental();assertArrayEquals(l,labels);assertArrayEquals(g,gray);}
}
