// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.*;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original raster rules around asymmetrically centered semantic ovals. */
public class LedgerSpaceRasterTest {
    static final int W=240,H=300;
    final byte[] gray=new byte[W*H];
    public LedgerSpaceRasterTest(){Arrays.fill(gray,(byte)255);}
    void rule(int left,int right,int y){for(int x=left;x<=right;x++)gray[y*W+x]=0;}
    void stem(int x,int top,int bottom){for(int y=top;y<=bottom;y++)gray[y*W+x]=0;}
    Object head(float cy,boolean small)throws Exception {
        Class<?> type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var c=type.getDeclaredConstructors()[0];c.setAccessible(true);
        int rx=small?7:13,ry=small?5:8;
        for(int y=Math.round(cy)-ry;y<=Math.round(cy)+ry;y++)for(int x=100-rx;x<=100+rx;x++)
            if(Math.pow((x-100)/(double)rx,2)+Math.pow((y-cy)/ry,2)<=1)gray[y*W+x]=(byte)150;
        return c.newInstance(small?75:310,100-rx,100+rx,Math.round(cy)-ry,Math.round(cy)+ry,100f,cy);
    }
    int pitch(float cy,boolean continued)throws Exception {
        Object h=head(cy,false);rule(76,124,200);rule(76,124,180);
        if(continued)rule(30,180,200);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("printedPitchStep",byte[].class,int.class,int.class,h.getClass(),float.class,float.class);
        m.setAccessible(true);return (int)m.invoke(null,gray,W,H,h,160f,20f);
    }
    boolean ledger(float cy,boolean small,boolean stemmed)throws Exception {
        Object h=head(cy,small);rule(76,124,Math.round(cy)-15);
        if(stemmed)stem(100+(small?7:13),Math.round(cy)-65,Math.round(cy));
        var m=OmrScoreInterpreter.class.getDeclaredMethod("hasLedgerInk",byte[].class,int.class,int.class,h.getClass(),float.class,boolean.class);
        m.setAccessible(true);return (boolean)m.invoke(null,gray,W,H,h,20f,false);
    }
    @Test public void twoInnerLedgersRestoreThePrintedSpace()throws Exception{assertEquals(-5,pitch(216,false));}
    @Test public void missingBoundedLedgerCannotBorrowABeam()throws Exception{assertEquals(-6,pitch(216,true));}
    @Test public void extendedOuterInkPreventsAFalseMissingLedger()throws Exception{
        rule(30,180,220);assertEquals(-6,pitch(216,false));
    }
    @Test public void fullSizeStemmedOvalMayLieFartherFromItsOwnRule()throws Exception{assertTrue(ledger(216,false,true));}
    @Test public void reducedOvalCannotBorrowThatRemoteRule()throws Exception{assertFalse(ledger(216,true,true));}
    @Test public void stemlessAnnotationCannotBorrowThatRemoteRule()throws Exception{assertFalse(ledger(216,false,false));}
}
