// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two-stem beam geometry with a small semantic island inside its ink. */
public class SingleBeamInteriorHeadTest {
    private static final int W=400,H=260;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    public SingleBeamInteriorHeadTest(){
        Arrays.fill(gray,(byte)255);
        for(int y=100;y<=164;y+=16)for(int x=10;x<390;x++)pixel(x,y,4);
        note(140,176,9,7);note(204,166,9,7);
        for(int y=120;y<=176;y++)pixel(147,y,1);
        for(int y=110;y<=166;y++)pixel(211,y,1);
        for(int x=147;x<=211;x++){int top=Math.round(120-(x-147)*10/64f);for(int y=top;y<=top+7;y++)pixel(x,y,5);}
        for(int y=119;y<=125;y++)for(int x=156;x<=162;x++)if(labels[y*W+x]==5)labels[y*W+x]=2;
    }
    private void pixel(int x,int y,int label){labels[y*W+x]=(byte)label;gray[y*W+x]=0;}
    private void note(int cx,int cy,int rx,int ry){for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1)pixel(x,y,2);}
    private boolean fragment(){return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(0,1,.15f,.9f))).notes().stream().anyMatch(n->Math.abs(n.positionInMeasure()*W-159)<12&&n.pageY()*H<145);}
    @Test public void thinPrintedBeamIslandIsNotANote(){assertFalse(fragment());}
    @Test public void missingPrintedBeamDoesNotProveFragment(){
        for(int x=166;x<=195;x++)for(int y=105;y<=130;y++)if(labels[y*W+x]==5){labels[y*W+x]=0;gray[y*W+x]=(byte)255;}
        assertTrue(fragment());
    }
    @Test public void roundedHeadBulgeMustSurvive(){byte[] original=labels.clone();note(159,126,9,8);System.arraycopy(original,0,labels,0,labels.length);assertTrue(fragment());}
    @Test public void separatelyStemmedSmallNoteSurvives(){for(int y=123;y<=160;y++)pixel(156,y,1);assertTrue(fragment());}
    @Test public void oneStemCannotEstablishInteriorBeam(){
        for(int y=127;y<=166;y++){labels[y*W+211]=0;gray[y*W+211]=(byte)255;}assertTrue(fragment());
    }
    @Test public void sourcePixelsAndLabelsStayUnchanged(){byte[] l=labels.clone(),g=gray.clone();fragment();assertArrayEquals(l,labels);assertArrayEquals(g,gray);}
}
