// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original hanging rectangles, raster fringes and note/stem rejection controls. */
public class HangingRestRuleBodyTest {
    private static final int W=400,H=200,TOP=54,BOTTOM=119;
    private final byte[] g=new byte[W*H];
    public HangingRestRuleBodyTest(){Arrays.fill(g,(byte)245);for(int y=50;y<=114;y+=16)box(20,y,380,y,0);}
    private void box(int l,int t,int r,int b,int v){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)g[y*W+x]=(byte)v;}
    private void whole(){box(100,67,119,74,0);}
    private int[] ink(){int[] a=new int[BOTTOM-TOP+1];for(int y=TOP;y<=BOTTOM;y++)for(int x=100;x<=119;x++)if(Math.abs(y-(50+Math.round((y-50)/16f)*16))>3&&(g[y*W+x]&255)<170)a[y-TOP]++;return a;}
    private int[] find(){return HalfRestRuleBody.hanging(g,W,H,50,16,100,119,TOP,ink());}
    private List<ScoreRestEvent> detect(List<ScoreNoteEvent> notes){return SixteenthRestDetector.detect(g,W,H,List.of(new MeasureRegion(0,1,.1f,.9f)),List.of(new SixteenthRestDetector.Staff(50,114,16,0,1)),notes);}
    @Test public void hangingRectangleHasIndependentRawBodyProof(){whole();assertArrayEquals(new int[]{67,74},find());}
    @Test public void singleTaperedBottomRowDoesNotExtendTheCore(){whole();box(107,75,112,75,0);assertArrayEquals(new int[]{67,74},find());}
    @Test public void endToEndTaperedRestIsFourBeats(){whole();box(107,75,112,75,0);var r=detect(List.of());assertEquals(1,r.size());assertEquals(4,r.get(0).durationBeats(),0);}
    @Test public void bodyMayContinueBelowOneIndependentlyBackedRuleEdge(){whole();box(87,67,132,67,0);assertArrayEquals(new int[]{68,74},find());}
    @Test public void twoEdgeRowsAreNotOneRuleFringe(){whole();box(87,67,132,68,0);assertNull(find());}
    @Test public void isolatedEdgeCannotReplaceAContinuousSupportingRule(){whole();box(20,66,99,66,245);box(120,66,380,66,245);box(87,67,132,67,0);assertNull(find());}
    @Test public void oneSidedBroadEdgeDoesNotProveAStaffRule(){whole();box(87,67,122,67,0);assertNull(find());}
    @Test public void remoteThinRuleFragmentsDoNotBecomeStemInk(){whole();box(100,113,102,113,0);assertArrayEquals(new int[]{67,74},find());}
    @Test public void unexplainedDetachedInkStillRejects(){whole();box(105,103,107,104,0);assertNull(find());}
    @Test public void longStemCannotUseTheRuleFragmentAllowance(){whole();box(118,57,119,113,0);assertNull(find());}
    @Test public void roundedNoteheadCannotSupplyFlatRows(){for(int y=67;y<=75;y++)for(int x=100;x<=119;x++)if(Math.pow((x-109.5)/9.5,2)+Math.pow((y-71)/4.,2)<=1)g[y*W+x]=0;assertNull(find());}
    @Test public void middleRuleHalfRestIsNotAHangingWhole(){box(100,74,119,81,0);assertNull(find());assertArrayEquals(new int[]{74,81},HalfRestRuleBody.find(g,W,H,50,16,100,119,TOP,ink()));}
    @Test public void detachedRectangleCannotJumpAcrossWhiteRows(){box(100,69,119,75,0);assertNull(find());}
    @Test public void overlongRectangleIsNotAccepted(){box(100,67,119,79,0);assertNull(find());}
    @Test public void thinHorizontalMarkHasNoRestBody(){box(100,67,119,68,0);assertNull(find());}
    @Test public void originalNoteOwnershipStillVetoesTheFallback(){whole();box(107,75,112,75,0);var n=new ScoreNoteEvent(0,109.5f/W,5,0,1,71f/H,false,0,1);assertTrue(detect(List.of(n)).isEmpty());}
    @Test public void invalidInputCannotReadOutsideTheRaster(){assertNull(HalfRestRuleBody.hanging(g,W,H,50,Float.NaN,100,119,TOP,ink()));assertNull(HalfRestRuleBody.hanging(g,W,H,50,16,-1,19,TOP,ink()));assertNull(HalfRestRuleBody.hanging(g,W,H,50,16,100,119,H-1,ink()));}
    @Test public void originalPixelsAndMaskCountsRemainUnchanged(){whole();byte[] before=g.clone();int[] a=ink(),old=a.clone();HalfRestRuleBody.hanging(g,W,H,50,16,100,119,TOP,a);assertArrayEquals(before,g);assertArrayEquals(old,a);}
}
