// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original dark lower shaft with a pale upper continuation and semantic stem labels. */
public final class PartialSemanticFadedStemTest {
    private static final int W=300,H=240;
    private byte[] gray,labels;
    private void setup(int beams,boolean fullyPale,boolean flag) {
        gray=new byte[W*H];labels=new byte[W*H];Arrays.fill(gray,(byte)250);
        for(int y=90;y<=150;y++)for(int x=109;x<=111;x++){gray[y*W+x]=(byte)(fullyPale||y<119?225:30);labels[y*W+x]=1;}
        for(int b=0;b<beams;b++)for(int y=90+b*11;y<=95+b*11;y++)for(int x=110;x<180;x++)gray[y*W+x]=30;
        if(flag)for(int d=0;d<=32;d++){int x=110+Math.round(18*(float)Math.sin(Math.PI*d/32));for(int dx=0;dx<3;dx++)gray[(90+d)*W+x+dx]=30;}
    }
    private int count()throws Exception {
        var headType=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var staffType=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var hc=headType.getDeclaredConstructors()[0];hc.setAccessible(true);Object head=hc.newInstance(180,90,110,144,156,100f,150f);
        var sc=staffType.getDeclaredConstructor(float.class,float.class,float.class);sc.setAccessible(true);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("detectBeamCount",byte[].class,byte[].class,int.class,int.class,headType,staffType);m.setAccessible(true);
        return (int)m.invoke(null,labels,gray,W,H,head,sc.newInstance(40f,104f,16f));
    }
    @Test public void partialDarkStemContinuesToSingleBeam()throws Exception {setup(1,false,false);assertEquals(1,count());}
    @Test public void partialDarkStemContinuesToTwoBeams()throws Exception {setup(2,false,false);assertEquals(2,count());}
    @Test public void semanticStemDoesNotDisableFadedSingleBeamRecovery()throws Exception {setup(1,true,false);assertEquals(1,count());}
    @Test public void fadedStemCanOwnVerifiedReturningFlag()throws Exception {setup(0,true,true);assertEquals(1,count());}
    @Test public void unmarkedPaleQuarterDoesNotAcquireBeam()throws Exception {setup(0,false,false);assertEquals(0,count());}
    @Test public void sourceArraysArePreserved()throws Exception {setup(1,false,false);byte[] g=gray.clone(),l=labels.clone();count();assertArrayEquals(g,gray);assertArrayEquals(l,labels);}
}
