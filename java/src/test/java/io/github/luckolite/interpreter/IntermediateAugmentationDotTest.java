// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic scan intensities only; no score-derived pixels. */
public class IntermediateAugmentationDotTest {
    private static final int W=240,H=160;
    private final byte[] gray=new byte[W*H];
    public IntermediateAugmentationDotTest(){Arrays.fill(gray,(byte)250);}
    private void dot(int cx,int cy) {
        for(int y=-3;y<=3;y++)for(int x=-3;x<=3;x++)if(x*x+y*y<=10)
            gray[(cy+y)*W+cx+x]=(byte)(x*x+y*y<=4?145:175);
    }
    private int count()throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var head=ctor.newInstance(180,90,110,73,87,100f,80f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("countAugmentationDots",List.class,type,float.class,byte[].class,int.class,int.class,boolean.class);method.setAccessible(true);
        return (int)method.invoke(null,List.of(),head,16f,gray,W,H,false);
    }
    @Test public void intermediateInkDotHasTwoIndependentIntensityBands()throws Exception {dot(126,80);assertEquals(1,count());}
    @Test public void fadedHaloCannotHideTheIsolatedIntermediateBody()throws Exception {
        for(int x=120;x<150;x++)for(int y=79;y<=81;y++)gray[y*W+x]=(byte)195;
        dot(126,80);assertEquals(1,count());
    }
    @Test public void twoIntermediateDotsRemainTwo()throws Exception {dot(124,80);dot(136,80);assertEquals(2,count());}
    @Test public void uniformIntermediateBlobHasNoDarkerCore()throws Exception {dot(126,80);for(int i=0;i<gray.length;i++)if((gray[i]&255)==145)gray[i]=(byte)175;assertEquals(0,count());}
    @Test public void tinyCoreDoesNotEstablishADot()throws Exception {dot(126,80);for(int i=0;i<gray.length;i++)if((gray[i]&255)==145)gray[i]=(byte)175;gray[80*W+126]=(byte)145;assertEquals(0,count());}
    @Test public void broadRectangularBodyIsNotADot()throws Exception {
        for(int y=77;y<=83;y++)for(int x=123;x<=129;x++)gray[y*W+x]=(byte)175;
        for(int y=79;y<=81;y++)for(int x=125;x<=127;x++)gray[y*W+x]=(byte)145;assertEquals(0,count());
    }
    @Test public void realIntermediateStemContinuationIsRejected()throws Exception {
        dot(126,80);for(int y=50;y<110;y++)gray[y*W+126]=(byte)175;assertEquals(0,count());
    }
    @Test public void sourcePixelsAreUnchanged()throws Exception {dot(126,80);byte[] before=gray.clone();count();assertArrayEquals(before,gray);}
}
