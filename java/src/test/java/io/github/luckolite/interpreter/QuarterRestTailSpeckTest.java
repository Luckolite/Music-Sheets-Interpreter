// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original row profiles and the existing original compact-rest drawing. */
public class QuarterRestTailSpeckTest {
    private static int[] profile(int gapRows,int pixels){int[] a=new int[70];for(int y=5;y<=45;y++)a[y]=4;a[45+gapRows]=pixels;return a;}
    @Test public void isolatedOnePixelTailCanBeRetriedAtTheRealContourEnd(){assertEquals(45,SixteenthRestDetector.quarterTailWithoutSpeck(profile(6,1),0,5,51,16));}
    @Test public void twoPixelsAreNotAnIsolatedScanSpeck(){assertEquals(51,SixteenthRestDetector.quarterTailWithoutSpeck(profile(6,2),0,5,51,16));}
    @Test public void closelyAdjacentInkIsNotRemoved(){assertEquals(47,SixteenthRestDetector.quarterTailWithoutSpeck(profile(2,1),0,5,47,16));}
    @Test public void connectedTailCannotBeShortened(){var a=profile(6,1);for(int y=46;y<51;y++)a[y]=1;assertEquals(51,SixteenthRestDetector.quarterTailWithoutSpeck(a,0,5,51,16));}
    @Test public void remainingBodyMustStillHaveQuarterRestHeight(){var a=new int[70];for(int y=5;y<20;y++)a[y]=4;a[25]=1;assertEquals(25,SixteenthRestDetector.quarterTailWithoutSpeck(a,0,5,25,16));}
    @Test public void invalidBoundsAreIgnored(){assertEquals(90,SixteenthRestDetector.quarterTailWithoutSpeck(profile(6,1),0,5,90,16));assertEquals(51,SixteenthRestDetector.quarterTailWithoutSpeck(null,0,5,51,16));}
    @Test public void profileCountsRemainUnmodified(){var a=profile(6,1);var b=a.clone();SixteenthRestDetector.quarterTailWithoutSpeck(a,0,5,51,16);assertArrayEquals(b,a);}
    @Test public void aRealQuarterRestSurvivesAnIsolatedLowerSpeck(){var p=new CompactQuarterRestTest.Page(16,true,true,false,false);p.gray[154*CompactQuarterRestTest.W+154]=0;var r=p.detect();assertEquals(r.toString(),1,r.size());assertEquals(1,r.get(0).durationBeats(),0);}
    @Test public void aSpeckCannotSupplyMissingQuarterRestShape(){var p=new CompactQuarterRestTest.Page(16,false,true,false,false);p.gray[154*CompactQuarterRestTest.W+154]=0;assertTrue(p.detect().isEmpty());}
}
