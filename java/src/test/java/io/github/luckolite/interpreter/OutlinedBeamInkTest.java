// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original grey-filled outlined beam geometry, never score-derived pixels. */
public class OutlinedBeamInkTest {
    private static final int W=240,H=200;
    private byte[] page(int beams,int paper,int fill,boolean down,boolean left,float slope) {
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)paper);
        int direction=down?1:-1,side=left?-1:1,end=down?140:60;
        for(int y=Math.min(100,end);y<=Math.max(100,end);y++)p[y*W+100]=20;
        for(int beam=0;beam<beams;beam++)for(int dx=0;dx<=(beam==0?50:18);dx++)
            for(int dy=0;dy<=10;dy++) {
                int x=100+side*dx,y=end-direction*(beam*18+dy)+Math.round(dx*slope);
                p[y*W+x]=(byte)(dy<2||dy>8?20:fill);
            }
        return p;
    }
    private int count(byte[] p,boolean down) {
        return OutlinedBeamInk.count(p,W,H,new int[]{100,down?140:60,down?1:-1},down?80:120,20);
    }
    @Test public void oneOutlineIsOneBeamNotTwoEdges(){assertEquals(1,count(page(1,240,185,false,false,0),false));}
    @Test public void partialInnerHookIsASecondBeam(){assertEquals(2,count(page(2,240,185,false,false,0),false));}
    @Test public void leftFacingDownwardSixteenthHasTwoBeams(){assertEquals(2,count(page(2,240,185,true,true,0),true));}
    @Test public void slopingOutlinedMainAndHookStaySeparate(){assertEquals(2,count(page(2,240,185,true,true,.12f),true));}
    @Test public void whiteGapBetweenStaffRulesIsNotAFilledOutline(){assertEquals(0,count(page(1,240,240,false,false,0),false));}
    @Test public void shadedPaperCannotFillTheOutline(){assertEquals(0,count(page(1,170,160,false,false,0),false));}
    @Test public void ordinarySolidBeamLeavesExistingDetectorInCharge(){assertEquals(0,count(page(2,240,20,false,false,0),false));}
    @Test public void missingFarMainBeamProofRejectsShortFlagLikeBox(){byte[] p=page(2,240,185,false,false,0);for(int y=0;y<H;y++)for(int x=121;x<W;x++)p[y*W+x]=(byte)240;assertEquals(0,count(p,false));}
    @Test public void headTooCloseToEndpointDoesNotBecomeBeam(){assertEquals(0,OutlinedBeamInk.count(page(1,240,185,false,false,0),W,H,new int[]{100,60,-1},85,20));}
    @Test public void sourcePixelsAreImmutable(){byte[] p=page(2,240,185,true,true,0),before=p.clone();count(p,true);assertArrayEquals(before,p);}
    @Test public void printedRailBeyondTruncatedStemTipCanProveBeam(){assertEquals(1,OutlinedBeamInk.count(page(1,240,185,true,true,0),W,H,new int[]{100,128,1},80,20));}
}
