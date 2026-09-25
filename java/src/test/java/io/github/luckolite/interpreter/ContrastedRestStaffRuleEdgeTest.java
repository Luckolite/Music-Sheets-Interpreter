// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original gray staff-edge strips and procedural rest glyphs, never score pixels. */
public class ContrastedRestStaffRuleEdgeTest {
    private static final int W=200,H=100;
    private final byte[] gray=new byte[W*H];private final boolean[] mask=new boolean[60];
    public ContrastedRestStaffRuleEdgeTest(){Arrays.fill(gray,(byte)245);mask[20]=true;box(40,40,100,40,80);}
    private void box(int l,int t,int r,int b,int v){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)gray[y*W+x]=(byte)v;}
    private boolean[] read(){return RestStaffRuleEdge.extendContrasted(gray,W,H,20,20,10,mask);}
    @Test public void contrastedPaleEdgeIsAdditionalEvidenceOnly(){box(50,39,79,39,185);assertSame(mask,RestStaffRuleEdge.extend(gray,W,H,20,20,10,mask));assertTrue(read()[19]);}
    @Test public void subthresholdTwoHundredInkRemainsRequired(){box(50,39,79,39,200);assertSame(mask,read());}
    @Test public void bothEdgeAndAdjacentCoreMustBeVisible(){box(50,39,79,39,185);box(50,40,79,40,210);assertSame(mask,read());}
    @Test public void grayPaperCannotSupplyContrast(){box(40,35,100,44,190);box(50,39,79,39,185);box(40,40,100,40,80);assertSame(mask,read());}
    @Test public void oneDarkShoulderDoesNotProvePaperOnBothSides(){box(50,39,79,39,185);box(40,43,100,43,190);assertSame(mask,read());}
    @Test public void shorterGlyphStrokeCannotSupplyThreeGapSupport(){box(50,39,78,39,185);assertSame(mask,read());}
    @Test public void fragmentedEdgesCannotBridgeAWhiteColumn(){box(40,39,100,39,185);box(60,39,60,39,245);box(80,39,80,39,245);assertSame(mask,read());}
    @Test public void noRecursiveMaskGrowth(){box(50,39,79,39,185);box(50,38,79,38,185);var r=read();assertTrue(r[19]);assertFalse(r[18]);}
    @Test public void detachedStrokeIsNotAnEdge(){box(50,38,79,38,185);assertSame(mask,read());}
    @Test public void wrongStaffPhaseStillRejects(){Arrays.fill(mask,false);mask[26]=true;box(40,46,100,46,80);box(50,45,79,45,185);assertSame(mask,read());}
    @Test public void aLowerEdgeUsesTheSameBoundedProof(){box(50,41,79,41,185);assertTrue(read()[21]);}
    @Test public void sourceRasterAndOriginalMaskStayImmutable(){box(50,39,79,39,185);byte[] g=gray.clone();boolean[] m=mask.clone();read();assertArrayEquals(g,gray);assertArrayEquals(m,mask);}
    @Test public void invalidOrNonfiniteGeometryAbstains(){assertSame(mask,RestStaffRuleEdge.extendContrasted(gray,W,H,-1,20,10,mask));assertSame(mask,RestStaffRuleEdge.extendContrasted(gray,W,H,20,Float.NaN,10,mask));assertSame(mask,RestStaffRuleEdge.extendContrasted(gray,W,H,70,20,10,mask));}
    private static List<ScoreRestEvent> fixture(boolean bulb,boolean tail,boolean edge) {
        return fixture(bulb,tail,edge,0,185);
    }
    private static List<ScoreRestEvent> fixture(boolean bulb,boolean tail,boolean edge,int shift,int edgeValue) {
        int w=800,h=240;byte[] g=new byte[w*h];Arrays.fill(g,(byte)245);
        for(int n=0;n<5;n++){int y=80+n*14;for(int x=20;x<780;x++)g[y*w+x]=80;}
        if(bulb)for(int y=96;y<=104;y++)for(int x=171;x<=183;x++)if(Math.pow((x-177)/6.,2)+Math.pow((y-100)/4.,2)<=1)g[(y+shift)*w+x]=30;
        if(tail)for(int y=97;y<=121;y++){int x=185-(y-97)*10/24;g[(y+shift)*w+x]=30;g[(y+shift)*w+x+1]=30;}
        if(edge){for(int x=155;x<=260;x++)g[(123+shift)*w+x]=(byte)edgeValue;for(int x=173;x<=186;x++)g[(123+shift)*w+x]=80;}
        return SixteenthRestDetector.detect(g,w,h,List.of(new MeasureRegion(0,1,.2f,.8f)),List.of(new SixteenthRestDetector.Staff(80,136,14,0,1)),List.of());
    }
    @Test public void grayRuleFringeCannotMergeAnEntireRestWithTheStaff(){var r=fixture(true,true,true);assertEquals(r.toString(),1,r.size());assertEquals(.5,r.get(0).durationBeats(),0);}
    @Test public void cleanAcceptedRestRetainsItsExactEvent(){assertEquals(fixture(true,true,false),fixture(true,true,true));}
    @Test public void noBulbOrTailMeansNoRest(){assertTrue(fixture(false,false,true).isEmpty());assertTrue(fixture(true,false,true).isEmpty());}
    @Test public void paleMaskRetryDoesNotAuthorizeAVirtualRaisedStaff(){assertTrue(fixture(true,true,true,-14,185).isEmpty());}
    @Test public void existingDarkMaskStillRecognizesRaisedVoice(){var r=fixture(true,true,true,-14,80);assertEquals(r.toString(),1,r.size());assertEquals(.5,r.get(0).durationBeats(),0);}
}
