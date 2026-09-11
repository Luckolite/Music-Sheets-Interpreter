// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original flat and natural drawings with strokes continuing outside a recovery crop. */
public class NaturalRecoveryBoundaryTest {
    private static final int W=100,H=100;
    private final byte[] gray=new byte[W*H];
    public NaturalRecoveryBoundaryTest(){Arrays.fill(gray,(byte)255);}
    private void rect(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)gray[yy*W+xx]=0;}
    private Object construct(String name,Object...args)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name);
        var c=type.getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);
    }
    private Object call(String name,Object...args)throws Exception {
        for(var m:OmrScoreInterpreter.class.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(null,args);}
        throw new AssertionError(name);
    }
    private void glyph(int leftTop,int rightBottom) {
        rect(25,leftTop,2,47-leftTop);rect(35,30,2,rightBottom-30);
        rect(25,30,12,4);rect(25,44,12,4);
    }
    private Object head()throws Exception{return construct("Component",100,48,60,33,43,54f,38f);}
    private boolean recoverSeed()throws Exception {
        Object seed=construct("Component",96,25,36,30,47,30.5f,38.5f);
        return (boolean)call("rawNaturalAtSeed",gray,W,H,seed,head(),14f);
    }
    private boolean recoverBars()throws Exception {
        Object upper=construct("Component",48,25,36,30,33,30.5f,31.5f);
        Object lower=construct("Component",48,25,36,44,47,30.5f,45.5f);
        return (boolean)call("rawNaturalFromCrossbars",gray,W,H,List.of(
            construct("AccidentalCandidate",upper,(byte)3),construct("AccidentalCandidate",lower,(byte)5)),head(),14f);
    }
    @Test public void intactNaturalSurvivesStemLabelSplits()throws Exception{glyph(18,59);assertTrue(recoverSeed());}
    @Test public void intactNaturalSurvivesDisconnectedSemanticCrossbars()throws Exception{glyph(18,59);assertTrue(recoverBars());}
    @Test public void flatIsNotANatural()throws Exception{glyph(18,47);assertFalse(recoverSeed());}
    @Test public void annotationExtendingBelowFlatCannotSupplyNaturalSpine()throws Exception{glyph(18,80);assertFalse(recoverSeed());}
    @Test public void annotationBelowSeparatedBarsCannotSupplyNaturalSpine()throws Exception{glyph(18,80);assertFalse(recoverBars());}
    @Test public void upperStrokeMustEndInsideSeedCrop()throws Exception{glyph(3,59);assertFalse(recoverSeed());}
    @Test public void upperStrokeMustEndInsideCrossbarCrop()throws Exception{glyph(3,59);assertFalse(recoverBars());}
    @Test public void aStaffRuleDoesNotEraseAnIntactNatural()throws Exception{glyph(18,59);rect(0,58,W,3);assertTrue(recoverSeed());}
    @Test public void recoveryDoesNotModifyPrintedInk()throws Exception{glyph(18,80);byte[] before=gray.clone();recoverSeed();recoverBars();assertArrayEquals(before,gray);}
}
