// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rectangles, ruled paper and negative note/stem geometry. */
public class HalfRestRuleBodyTest {
    private static final int W=400,H=200,TOP=54,BOTTOM=119;
    private final byte[] g=new byte[W*H];
    public HalfRestRuleBodyTest(){Arrays.fill(g,(byte)245);for(int y=50;y<=114;y+=16)box(20,y,380,y,0);}
    private void box(int l,int t,int r,int b,int v){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)g[y*W+x]=(byte)v;}
    private void half(){box(100,74,119,81,0);}
    private int[] ink(){int[] a=new int[BOTTOM-TOP+1];for(int y=TOP;y<=BOTTOM;y++)for(int x=100;x<=119;x++)if(Math.abs(y-(50+Math.round((y-50)/16f)*16))>3&&(g[y*W+x]&255)<170)a[y-TOP]++;return a;}
    private int[] find(){return HalfRestRuleBody.find(g,W,H,50,16,100,119,TOP,ink());}
    @Test public void rawRectangleSurvivesGloballyErasedBottomRows(){half();assertArrayEquals(new int[]{74,81},find());}
    @Test public void globalRuleEvidenceCannotEraseTheLocalHalfRest(){box(100,77,119,81,0);box(230,77,365,81,0);var r=SixteenthRestDetector.detect(g,W,H,List.of(new MeasureRegion(0,1,.1f,.9f)),List.of(new SixteenthRestDetector.Staff(50,114,16,0,1)),List.of());assertEquals(r.toString(),1,r.size());assertEquals(2,r.get(0).durationBeats(),0);}
    @Test public void antialiasedUpperFringeDoesNotIncreaseRectangleHeight(){half();box(106,73,109,73,0);assertArrayEquals(new int[]{74,81},find());}
    @Test public void neighboringRuleEdgeIsNotPartOfTheRectangle(){half();box(75,99,105,99,0);box(112,99,147,99,0);int[] a=ink();a[99-TOP]=14;assertArrayEquals(new int[]{74,81},HalfRestRuleBody.find(g,W,H,50,16,100,119,TOP,a));}
    @Test public void stemCannotBeIgnoredEvenWhenItTouchesTheRule(){half();box(118,68,119,115,0);assertNull(find());}
    @Test public void unrelatedLowerInkRejectsTheCandidate(){half();box(108,105,111,109,0);assertNull(find());}
    @Test public void hangingWholeRectangleIsNotAHalfRest(){box(100,67,119,74,0);assertNull(find());}
    @Test public void detachedRectangleMustNotJumpToMiddleRule(){box(100,72,119,79,0);assertNull(find());}
    @Test public void thinTenutoLacksRestBodyHeight(){box(100,80,119,81,0);assertNull(find());}
    @Test public void roundedOvalCannotSupplyEnoughWideRows(){for(int y=73;y<=81;y++)for(int x=100;x<=119;x++)if(Math.pow((x-109.5)/9.5,2)+Math.pow((y-77)/4.,2)<=1)g[y*W+x]=0;assertNull(find());}
    @Test public void supportingRuleMustContinueOnBothSides(){half();box(20,82,99,82,245);assertNull(find());}
    @Test public void shortUnderlineIsNotAStaffRule(){half();box(20,82,88,82,245);box(132,82,380,82,245);assertNull(find());}
    @Test public void excessiveSolidHeightDoesNotBecomeAShortRest(){box(100,68,119,81,0);assertNull(find());}
    @Test public void connectedSideInkCannotMasqueradeAsRectangle(){half();box(119,76,130,79,0);assertNull(find());}
    @Test public void inputBoundsAreCheckedBeforeRasterAccess(){assertNull(HalfRestRuleBody.find(g,W,H,50,16,-1,19,TOP,ink()));assertNull(HalfRestRuleBody.find(g,W,H,50,Float.NaN,100,119,TOP,ink()));}
    @Test public void originalPixelsAndMaskCountsAreUnchanged(){half();byte[] b=g.clone();int[] a=ink(),saved=a.clone();HalfRestRuleBody.find(g,W,H,50,16,100,119,TOP,a);assertArrayEquals(b,g);assertArrayEquals(saved,a);}
    @Test public void noteOwnedColumnStillVetoesTheRest(){half();var n=new ScoreNoteEvent(0,109.5f/W,2,0,1,78f/H,false,0,1);var r=SixteenthRestDetector.detect(g,W,H,List.of(new MeasureRegion(0,1,.1f,.9f)),List.of(new SixteenthRestDetector.Staff(50,114,16,0,1)),List.of(n));assertTrue(r.isEmpty());}
}
