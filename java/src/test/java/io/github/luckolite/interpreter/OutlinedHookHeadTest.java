// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original outlined rails, synthetic stem and tiny prediction: no score-derived pixels. */
public class OutlinedHookHeadTest {
    final int W=240,H=200;byte[] gray=new byte[W*H];
    void page(int beams,boolean down){
        Arrays.fill(gray,(byte)240);int sign=down?1:-1,side=down?-1:1,end=down?140:60;
        for(int y=60;y<=140;y++)gray[y*W+100]=20;
        for(int beam=0;beam<beams;beam++)for(int dx=0;dx<=(beam==0?50:18);dx++)for(int dy=0;dy<=10;dy++)
            gray[(end-sign*(beam*18+dy))*W+100+side*dx]=(byte)(dy<2||dy>8?20:185);
    }
    boolean test(boolean down,int area,int boxWidth,float x,float mainY){return OutlinedHookHead.matches(gray,W,H,x,down?117:83,boxWidth,8,area,down?112:88,mainY,300,new int[]{100,down?140:60,down?1:-1},20);}
    @Test public void tinyPredictionInsideInnerDownwardHookIsNotANote(){page(2,true);assertTrue(test(true,45,9,91,78));}
    @Test public void reflectedUpwardHookAlsoHasOppositeHeadOwnership(){page(2,false);assertTrue(test(false,45,9,109,122));}
    @Test public void primaryBeamAloneCannotRejectHead(){page(1,true);assertFalse(test(true,45,9,91,78));}
    @Test public void headOnSameSideAsMainHeadIsNotThisArtifact(){page(2,true);assertFalse(test(true,45,9,110,78));}
    @Test public void FullSizeOvalCannotBeDiscarded(){page(2,true);assertFalse(test(true,170,21,91,78));}
    @Test public void nearbyChordToneCannotBecomeHook(){page(2,true);assertFalse(test(true,45,9,91,100));}
    @Test public void inputPixelsStayImmutable(){page(2,true);byte[] before=gray.clone();test(true,45,9,91,78);assertArrayEquals(before,gray);}
    @Test public void malformedImageIsSafe(){assertFalse(OutlinedHookHead.matches(new byte[1],W,H,91,117,9,8,45,112,78,300,new int[]{100,140,1},20));}
    @Test public void nonfiniteMainHeadIsSafe(){page(2,true);assertFalse(OutlinedHookHead.matches(gray,W,H,91,117,9,8,45,Float.NaN,78,300,new int[]{100,140,1},20));assertFalse(test(true,45,9,91,Float.POSITIVE_INFINITY));}
    @Test public void emptyOrNegativeHeadMetricsAreRejected(){page(2,true);assertFalse(test(true,0,9,91,78));assertFalse(test(true,45,-1,91,78));}
    @Test public void invalidStemDirectionIsRejected(){page(2,true);assertFalse(OutlinedHookHead.matches(gray,W,H,91,117,9,8,45,112,78,300,new int[]{100,140,0},20));}
    @Test public void outOfImageStemEndpointIsRejected(){page(2,true);assertFalse(OutlinedHookHead.matches(gray,W,H,91,117,9,8,45,112,78,300,new int[]{W,140,1},20));assertFalse(OutlinedHookHead.matches(gray,W,H,91,117,9,8,45,112,78,300,new int[]{100,H,1},20));}
}
