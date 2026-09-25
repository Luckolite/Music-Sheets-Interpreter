// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original tiny scan islands and resolved dot bodies at two staff scales. */
public class ResolvedAugmentationInkTest {
    static final int W=240,H=160;final byte[] g=new byte[W*H];
    public ResolvedAugmentationInkTest(){Arrays.fill(g,(byte)245);}
    void speck(int x){g[80*W+x]=20;g[80*W+x+1]=20;g[81*W+x]=20;}
    void dot(int x){for(int y=-2;y<=2;y++)for(int dx=-2;dx<=2;dx++)if(dx*dx+y*y<=4)g[(80+y)*W+x+dx]=20;}
    int count(float gap,boolean hollow)throws Exception {
        var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=hc.getDeclaredConstructors()[0];ctor.setAccessible(true);
        Object head=ctor.newInstance(120,94,106,75,85,100f,80f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("countAugmentationDots",List.class,hc,float.class,byte[].class,int.class,int.class,boolean.class);m.setAccessible(true);
        return (int)m.invoke(null,List.of(),head,gap,g,W,H,hollow);
    }
    @Test public void threePixelScanSpeckDoesNotLengthenFilledNote()throws Exception{speck(117);assertEquals(0,count(13.5f,false));}
    @Test public void resolvedDarkDotStillLengthensFilledNote()throws Exception{dot(117);assertEquals(1,count(13,false));}
    @Test public void resolvedDoubleDotsKeepBothMarks()throws Exception{dot(117);dot(127);assertEquals(2,count(13,false));}
    @Test public void aSpeckCannotAddSecondDotToRealDot()throws Exception{dot(117);speck(127);assertEquals(1,count(13.5f,false));}
    @Test public void tinyHollowHeadCoreRetainsLegacyAllowance()throws Exception{speck(117);assertEquals(1,count(13,true));}
    @Test public void genuineSmallScaleDotIsNotSubjectToFixedPixelFloor()throws Exception{speck(116);assertEquals(1,count(7,false));}
    @Test public void blankPaperStaysUndotted()throws Exception{assertEquals(0,count(13,false));}
    @Test public void sourceRasterIsPreserved()throws Exception{speck(117);var before=g.clone();count(13,false);assertArrayEquals(before,g);}
}
