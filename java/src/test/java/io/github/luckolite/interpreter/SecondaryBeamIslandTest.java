// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two-stem beam with a short secondary stroke and a semantic island. */
public class SecondaryBeamIslandTest {
    private static final int W=400,H=280;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    public SecondaryBeamIslandTest(){
        Arrays.fill(gray,(byte)255);
        for(int y=104;y<=168;y+=16)for(int x=10;x<390;x++)pixel(x,y,4);
        note(140,166,10,7);note(220,158,10,7);
        for(int y=110;y<=166;y++)pixel(148,y,1);
        for(int y=102;y<=158;y++)pixel(228,y,1);
        for(int x=148;x<=228;x++){int top=110-Math.round((x-148)*.1f);for(int y=top;y<=top+7;y++)pixel(x,y,5);}
        for(int x=194;x<=228;x++){int top=126-Math.round((x-148)*.1f);for(int y=top;y<=top+7;y++)pixel(x,y,5);}
        for(int y=122;y<=128;y++)for(int x=198;x<=204;x++)if(labels[y*W+x]==5)labels[y*W+x]=2;
    }
    private void pixel(int x,int y,int value){labels[y*W+x]=(byte)value;gray[y*W+x]=value==0?(byte)255:0;}
    private void note(int cx,int cy,int rx,int ry){for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1)pixel(x,y,2);}
    private boolean fragment(boolean mirrored){return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(0,1,.15f,.9f))).notes().stream().anyMatch(n->Math.abs(n.positionInMeasure()*W-201)<12&&(mirrored?n.pageY()*H>135:n.pageY()*H<145));}
    @Test public void shortSecondaryBeamIslandIsNotANote(){assertFalse(fragment(false));}
    @Test public void downwardStemsUseTheSameBeamEvidence(){
        for(int y=0;y<H/2;y++)for(int x=0;x<W;x++){int a=y*W+x,b=(H-1-y)*W+(W-1-x);byte v=labels[a];labels[a]=labels[b];labels[b]=v;v=gray[a];gray[a]=gray[b];gray[b]=v;}assertFalse(fragment(true));
    }
    @Test public void missingPrimaryBeamCannotEstablishOwnership(){
        for(int x=155;x<220;x++){int top=110-Math.round((x-148)*.1f);for(int y=top;y<=top+7;y++)pixel(x,y,0);}assertTrue(fragment(false));
    }
    @Test public void roundedChordHeadBulgeMustSurvive(){var original=labels.clone();note(201,125,11,9);System.arraycopy(original,0,labels,0,labels.length);assertTrue(fragment(false));}
    @Test public void independentlyStemmedSmallHeadMustSurvive(){for(int y=125;y<=165;y++)pixel(199,y,1);assertTrue(fragment(false));}
    @Test public void sourceArraysStayUnchanged(){var l=labels.clone();var g=gray.clone();fragment(false);assertArrayEquals(l,labels);assertArrayEquals(g,gray);}
}
