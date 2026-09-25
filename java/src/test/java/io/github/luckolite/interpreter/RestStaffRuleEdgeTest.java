// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural staff edges and rest glyphs; no score image or annotation. */
public class RestStaffRuleEdgeTest {
    private static final int W=200,H=100;
    private final byte[] gray=new byte[W*H];
    private final boolean[] mask=new boolean[60];
    public RestStaffRuleEdgeTest(){Arrays.fill(gray,(byte)240);mask[20]=true;row(40,40,100);}
    private void row(int y,int left,int right){for(int x=left;x<=right;x++)gray[y*W+x]=80;}
    private boolean[] read(){return RestStaffRuleEdge.extend(gray,W,H,20,20,10,mask);}
    @Test public void attachedThreeGapEdgeIsRecovered(){row(39,50,79);assertTrue(read()[19]);}
    @Test public void shorterGlyphStrokeDoesNotExtendRule(){row(39,50,78);assertSame(mask,read());}
    @Test public void detachedLongStrokeIsNotAnEdge(){row(38,50,99);assertSame(mask,read());}
    @Test public void adjacentInkMustActuallyOverlap(){row(39,120,160);assertSame(mask,read());}
    @Test public void fragmentedInkIsNotAContinuousRule(){row(39,40,100);gray[39*W+60]=(byte)240;gray[39*W+80]=(byte)240;assertSame(mask,read());}
    @Test public void anEdgeCannotGrowRecursively(){row(39,40,100);row(38,40,100);var r=read();assertTrue(r[19]);assertFalse(r[18]);}
    @Test public void existingPhaseProofRemainsRequired(){Arrays.fill(mask,false);mask[26]=true;row(46,40,100);row(45,40,100);assertSame(mask,read());}
    @Test public void sourcePixelsAndMaskAreImmutable(){row(39,40,100);var before=gray.clone();var old=mask.clone();assertNotSame(mask,read());assertArrayEquals(before,gray);assertArrayEquals(old,mask);}
    @Test public void invalidDimensionsAndNonfiniteGeometryAbstain(){assertSame(mask,RestStaffRuleEdge.extend(gray,W,H,-1,20,10,mask));assertSame(mask,RestStaffRuleEdge.extend(gray,W,H,70,20,10,mask));assertSame(mask,RestStaffRuleEdge.extend(gray,W,H,20,Float.NaN,10,mask));assertSame(mask,RestStaffRuleEdge.extend(gray,W,H,20,20,Float.POSITIVE_INFINITY,mask));}
    private static List<ScoreRestEvent> fixture(boolean bulb,boolean tail,boolean edge) {
        int w=800,h=240;byte[] ink=new byte[w*h];Arrays.fill(ink,(byte)245);
        for(int line=0;line<5;line++) {
            int y=80+line*14;for(int x=20;x<780;x++)ink[y*w+x]=90;
            if(edge)for(int x=155;x<205;x++)ink[(y-1)*w+x]=95;
        }
        if(bulb)for(int y=96;y<=104;y++)for(int x=171;x<=183;x++)
            if(Math.pow((x-177)/6.,2)+Math.pow((y-100)/4.,2)<=1)ink[y*w+x]=30;
        if(tail)for(int y=97;y<=121;y++){int x=185-(y-97)*10/24;ink[y*w+x]=30;ink[y*w+x+1]=30;}
        return SixteenthRestDetector.detect(ink,w,h,List.of(new MeasureRegion(0,1,.2f,.8f)),
                List.of(new SixteenthRestDetector.Staff(80,136,14,0,1)),List.of());
    }
    @Test public void finiteEdgeKeepsEighthRestAndItsDuration(){var r=fixture(true,true,true);assertEquals(r.toString(),1,r.size());assertEquals(.5,r.get(0).durationBeats(),0);}
    @Test public void edgeWithoutRestNeverCreatesSilence(){assertTrue(fixture(false,false,true).isEmpty());}
    @Test public void bulbWithoutTailIsRejected(){assertTrue(fixture(true,false,true).isEmpty());}
    @Test public void originalUnbrokenRuleReadingIsPreserved(){var r=fixture(true,true,false);assertEquals(r.toString(),1,r.size());assertEquals(.5,r.get(0).durationBeats(),0);}
    private boolean owned(){return RestStaffRuleEdge.noteStem(gray,W,H,82,94,62,10,90,40);}
    @Test public void continuousNoteStemOwnsItsLowerFlag(){for(int y=40;y<=67;y++)gray[y*W+85]=50;assertTrue(owned());}
    @Test public void disconnectedVerticalGlyphRemainsIndependent(){for(int y=54;y<=67;y++)gray[y*W+85]=50;assertFalse(owned());}
    @Test public void neighboringStemDoesNotOwnRest(){for(int y=40;y<=67;y++)gray[y*W+70]=50;assertFalse(owned());}
    @Test public void aRuleCrossingAloneDoesNotProveAStem(){row(54,20,180);row(64,20,180);assertFalse(owned());}
}
