// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrintedHeadCenterTest {
    private static final int W=240,H=220;
    private static byte[] printed(float center,boolean hollow,boolean stemOnly) {
        byte[] gray=new byte[W*H];Arrays.fill(gray,(byte)245);
        for(int line=0;line<5;line++)for(int x=20;x<220;x++)gray[(96+line*16)*W+x]=(byte)180;
        if(!stemOnly)for(int y=100;y<145;y++)for(int x=90;x<=110;x++) {
            double d=(x-100)*(x-100)/100.0+(y-center)*(y-center)/49.0;
            if(d<=1&&(!hollow||d>=.55))gray[y*W+x]=0;
        }
        for(int y=85;y<=Math.round(center);y++)gray[y*W+110]=0;
        return gray;
    }
    private static int pitch(byte[] gray,float semanticY)throws Exception {
        Class<?> head=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var constructor=head.getDeclaredConstructors()[0];constructor.setAccessible(true);
        Object component=constructor.newInstance(220,90,110,Math.round(semanticY)-8,Math.round(semanticY)+8,100f,semanticY);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("printedPitchStep",byte[].class,int.class,int.class,head,float.class,float.class);
        method.setAccessible(true);return (int)method.invoke(null,gray,W,H,component,160f,16f);
    }
    @Test public void printedHeadResolvesDownwardSemanticCenterBias()throws Exception {
        assertEquals(5,pitch(printed(122.8f,false,false),124.3f));
    }
    @Test public void printedHeadResolvesUpwardSemanticCenterBias()throws Exception {
        assertEquals(4,pitch(printed(125.3f,false,false),123.7f));
    }
    @Test public void hollowCenterRetainsTheSemanticPitch()throws Exception {
        assertEquals(4,pitch(printed(122.8f,true,false),124.3f));
    }
    @Test public void thinStemCannotReplaceTheNoteCenter()throws Exception {
        assertEquals(4,pitch(printed(122.8f,false,true),124.3f));
    }
    @Test public void largeDisplacementCannotSnapToNearbyInk()throws Exception {
        assertEquals(4,pitch(printed(119f,false,false),124.3f));
    }
    @Test public void unambiguousPitchKeepsItsExistingReading()throws Exception {
        assertEquals(5,pitch(printed(122.8f,false,false),120f));
    }
    private static int analyzedPitch(float printedY,float semanticY) {
        byte[] gray=printed(printedY,false,false),labels=new byte[W*H];
        for(int line=0;line<5;line++)for(int x=20;x<220;x++)labels[(96+line*16)*W+x]=4;
        for(int y=110;y<140;y++)for(int x=90;x<=110;x++)
            if((x-100)*(x-100)/100f+(y-semanticY)*(y-semanticY)/49f<=1)labels[y*W+x]=2;
        for(int y=85;y<=Math.round(semanticY);y++)labels[y*W+110]=1;
        var result=OmrScoreInterpreter.analyze(labels,gray,W,H,java.util.List.of(new MeasureRegion(.05f,.95f,.3f,.85f)));
        assertEquals(1,result.notes().size());return result.notes().get(0).staffStep();
    }
    @Test public void decoderUsesThePrintedHeadAboveTheBoundary() {
        assertEquals(5,analyzedPitch(122.8f,124.8f));
    }
    @Test public void decoderUsesThePrintedHeadBelowTheBoundary() {
        assertEquals(4,analyzedPitch(125.3f,123.2f));
    }
}
