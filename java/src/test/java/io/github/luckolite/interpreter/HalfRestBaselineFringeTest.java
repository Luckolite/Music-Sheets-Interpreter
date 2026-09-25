// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original small baseline serifs, not source-score or font pixels. */
public class HalfRestBaselineFringeTest {
    private static final int W=300,H=160,TOP=44,BOTTOM=109;
    private final byte[] g=new byte[W*H];
    public HalfRestBaselineFringeTest(){Arrays.fill(g,(byte)245);for(int y=40;y<=104;y+=16)box(10,y,290,y);box(100,64,119,71);}
    private void box(int l,int t,int r,int b){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)g[y*W+x]=0;}
    private int[] find(){int[] a=new int[BOTTOM-TOP+1];for(int y=TOP;y<=BOTTOM;y++)for(int x=100;x<=119;x++)if(Math.abs(y-(40+Math.round((y-40)/16f)*16))>3&&(g[y*W+x]&255)<170)a[y-TOP]++;return HalfRestRuleBody.find(g,W,H,40,16,100,119,TOP,a);}
    @Test public void originalFlatFootStillWorks(){assertArrayEquals(new int[]{64,71},find());}
    @Test public void onePixelAntialiasedFootMayMeetTheSupportingRule(){box(99,71,120,71);assertArrayEquals(new int[]{64,71},find());}
    @Test public void twoPixelFootIsBoundedByTheStaffScale(){box(98,71,121,71);assertArrayEquals(new int[]{64,71},find());}
    @Test public void broadFootCannotBecomeAnUnrelatedHorizontalStroke(){box(97,71,122,71);assertNull(find());}
    @Test public void sideConnectionAboveTheBaselineStillRejects(){box(99,70,120,71);assertNull(find());}
    @Test public void earlierSideFringeCannotUseTheBaselineAllowance(){box(99,68,120,68);assertNull(find());}
    @Test public void verticalContinuationStillRejectsTheCandidate(){box(99,71,120,71);box(118,70,119,98);assertNull(find());}
    @Test public void inputInkIsNotChanged(){box(99,71,120,71);byte[] old=g.clone();find();assertArrayEquals(old,g);}
}
