// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** A compact semantic connector can still seed a complete printed natural. */
public final class CompactNaturalSeedTest {
    private static final int W=100,H=100;
    private final byte[] gray=new byte[W*H];
    public CompactNaturalSeedTest(){Arrays.fill(gray,(byte)255);}
    private void rect(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)gray[yy*W+xx]=0;}
    private Object make(String type,Object...args)throws Exception{var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+type).getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);}
    private boolean recover(int lowerEnd)throws Exception {
        rect(25,18,2,29);rect(35,30,2,lowerEnd-30);rect(25,30,12,4);rect(25,44,12,4);
        Object seed=make("Component",60,25,36,28,37,30.5f,32.5f);
        Object head=make("Component",100,48,60,33,43,54f,38f);
        var cc=head.getClass();var method=OmrScoreInterpreter.class.getDeclaredMethod("rawNaturalFromCrossbars",byte[].class,int.class,int.class,List.class,cc,float.class);method.setAccessible(true);
        return (boolean)method.invoke(null,gray,W,H,List.of(make("AccidentalCandidate",seed,(byte)3)),head,14f);
    }
    @Test public void compactConnectorCanSeedCompleteNatural()throws Exception{assertTrue(recover(59));}
    @Test public void flatWithoutLowerRightExtensionStaysFlat()throws Exception{assertFalse(recover(47));}
    @Test public void croppedAnnotationCannotExtendFlatIntoNatural()throws Exception{assertFalse(recover(80));}
}
