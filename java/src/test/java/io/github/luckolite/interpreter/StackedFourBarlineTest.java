// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric four counters on a continuous vertical semantic stroke. */
public class StackedFourBarlineTest {
    private static final int W=240,H=140,X=100;
    private final byte[] gray=new byte[W*H];
    private final int[] rules={40,54,68,82,96};
    public StackedFourBarlineTest() {
        Arrays.fill(gray,(byte)255);
        for(int y:rules)rect(10,230,y,y,0);
        rect(X,X+2,40,96,0);
    }
    private void rect(int a,int b,int t,int z,int value) {
        for(int y=t;y<=z;y++)for(int x=a;x<=b;x++)gray[y*W+x]=(byte)value;
    }
    private void counter(int dx,int y,boolean triangle) {
        rect(X-9+dx,X+2+dx,y-2,y+6,0);
        for(int row=0;row<5;row++) {
            int width=triangle?Math.min(6,row+2):6;
            rect(X-2+dx-width+1,X-2+dx,y+row,y+row,255);
        }
    }
    private boolean digit(){return OmrMeasurePostProcessor.stackedFourCounters(gray,W,H,X,38,98,14);}
    private int bar()throws Exception {
        var m=OmrMeasurePostProcessor.class.getDeclaredMethod("rawBarlineColumn",byte[].class,int.class,int.class,int.class,int[].class,float.class,float.class,float.class);
        m.setAccessible(true);return (int)m.invoke(null,gray,W,H,X,rules,14f,0f,0f);
    }
    @Test public void pairedTriangularCountersAreNotABarline()throws Exception {
        counter(0,55,true);counter(0,83,true);assertTrue(digit());assertEquals(Integer.MIN_VALUE,bar());
    }
    @Test public void aSingleCounterCannotVetoABarline()throws Exception {
        counter(0,55,true);assertFalse(digit());assertNotEquals(Integer.MIN_VALUE,bar());
    }
    @Test public void rectangularSpacesBetweenParallelRulesArePreserved()throws Exception {
        counter(0,55,false);counter(0,83,false);assertFalse(digit());assertNotEquals(Integer.MIN_VALUE,bar());
    }
    @Test public void staggeredCountersCannotProveStackedDigits() {
        counter(0,55,true);counter(-6,83,true);assertFalse(digit());
    }
    @Test public void adjacentStaffSpacesAreNotTwoDigits() {
        counter(0,55,true);counter(0,69,true);assertFalse(digit());
    }
    @Test public void plainAndDoubleBarsRemainBars()throws Exception {
        assertFalse(digit());assertNotEquals(Integer.MIN_VALUE,bar());
        rect(X-6,X-5,40,96,0);assertFalse(digit());assertNotEquals(Integer.MIN_VALUE,bar());
    }
    @Test public void beamsCrossingABarDoNotCreateCounters()throws Exception {
        rect(80,122,58,60,0);rect(80,122,86,88,0);assertFalse(digit());assertNotEquals(Integer.MIN_VALUE,bar());
    }
    @Test public void checkingCountersPreservesSourcePixels() {
        counter(0,55,true);counter(0,83,true);byte[] before=gray.clone();digit();assertArrayEquals(before,gray);
    }
}
