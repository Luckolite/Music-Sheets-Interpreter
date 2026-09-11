// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original drawings with light scan fill inside a natural's counter. */
public class NaturalCounterContrastTest {
    private static final int W=100,H=100;
    private final byte[] gray=new byte[W*H];
    public NaturalCounterContrastTest(){Arrays.fill(gray,(byte)245);}
    private void rect(int x,int y,int w,int h,int value) {
        for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)gray[yy*W+xx]=(byte)value;
    }
    private Object construct(String name,Object...args)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name);
        var c=type.getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);
    }
    private boolean recover()throws Exception {
        Object seed=construct("Component",96,25,36,30,47,30.5f,38.5f);
        Object head=construct("Component",100,48,60,33,43,54f,38f);
        for(var m:OmrScoreInterpreter.class.getDeclaredMethods())
            if(m.getName().equals("rawNaturalAtSeed")&&m.getParameterCount()==6) {
                m.setAccessible(true);return (boolean)m.invoke(null,gray,W,H,seed,head,14f);
            }
        throw new AssertionError("Natural recovery entry point missing");
    }
    private void glyph(int ink,int rightBottom) {
        rect(25,18,2,29,ink);rect(35,30,2,rightBottom-30,ink);
        rect(25,30,12,4,ink);rect(25,44,12,4,ink);
    }
    @Test public void grayCounterStillSeparatesBothCrossbars()throws Exception {
        rect(27,34,8,10,205);glyph(60,59);assertTrue(recover());
    }
    @Test public void graySurroundingPaperDoesNotFillTheCounter()throws Exception {
        Arrays.fill(gray,(byte)205);glyph(60,59);assertTrue(recover());
    }
    @Test public void faintSpinesRetainThePermissiveReading()throws Exception {
        glyph(200,59);assertTrue(recover());
    }
    @Test public void flatDoesNotAcquireALowerRightSpine()throws Exception {
        rect(27,34,8,10,205);glyph(60,47);assertFalse(recover());
    }
    @Test public void aStrokeLeavingTheCropRemainsRejected()throws Exception {
        rect(27,34,8,10,205);glyph(60,80);assertFalse(recover());
    }
    @Test public void aFilledRectangleDoesNotBecomeANatural()throws Exception {
        rect(25,18,12,41,60);assertFalse(recover());
    }
    @Test public void contrastRecoveryPreservesSourcePixels()throws Exception {
        rect(27,34,8,10,205);glyph(60,59);byte[] before=gray.clone();
        assertTrue(recover());assertArrayEquals(before,gray);
    }
}
