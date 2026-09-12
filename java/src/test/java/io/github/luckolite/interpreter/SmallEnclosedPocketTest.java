// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original heavy note outlines with white pockets divided by a staff stripe. */
public class SmallEnclosedPocketTest {
    private static final int W=80,H=80;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    public SmallEnclosedPocketTest() {
        Arrays.fill(gray,(byte)255);
        for(int y=20;y<40;y++)for(int x=20;x<44;x++) {
            labels[y*W+x]=2;
            if(Math.pow((x-31.5)/12,2)+Math.pow((y-29.5)/10,2)<=1)gray[y*W+x]=0;
        }
    }
    private void pocket(int left,int right,int top,int bottom) {
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++)gray[y*W+x]=(byte)255;
    }
    private boolean open(float gap)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var head=ctor.newInstance(480,20,43,20,39,31.5f,29.5f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("hasOpenCenter",byte[].class,byte[].class,int.class,int.class,type,float.class);method.setAccessible(true);
        return (boolean)method.invoke(null,labels,gray,W,H,head,gap);
    }
    @Test public void smallPocketsAboveAndBelowStaffInkRemainOpen()throws Exception {
        pocket(29,33,26,27);pocket(29,33,32,33);assertTrue(open(16));
    }
    @Test public void aCompactEnclosedInteriorRemainsOpen()throws Exception {
        pocket(29,33,27,30);assertTrue(open(16));
    }
    @Test public void aFilledHeadRemainsClosed()throws Exception {assertFalse(open(16));}
    @Test public void scatteredPinholesDoNotMakeAnOpenHead()throws Exception {
        pocket(30,31,25,28);assertFalse(open(16));
    }
    @Test public void aShallowSlitDoesNotUseTheSmallPocketAllowance()throws Exception {
        pocket(28,34,28,30);assertFalse(open(16));
    }
    @Test public void reducedHeadsDoNotUseTheExpandedPocketAllowance()throws Exception {
        pocket(29,33,26,27);pocket(29,33,32,33);assertFalse(open(28));
    }
    @Test public void aLargerOpenInteriorStillPasses()throws Exception {
        pocket(27,36,24,34);assertTrue(open(16));
    }
    @Test public void analysisPreservesPixelsAndLabels()throws Exception {
        pocket(29,33,26,27);pocket(29,33,32,33);var g=gray.clone();var l=labels.clone();open(16);assertArrayEquals(g,gray);assertArrayEquals(l,labels);
    }
}
