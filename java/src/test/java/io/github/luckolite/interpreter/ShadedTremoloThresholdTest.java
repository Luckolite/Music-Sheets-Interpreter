// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original isolated stem, genuine cross-stroke, and a one-sided flag on shaded paper. */
public final class ShadedTremoloThresholdTest {
    private static final int W=240,H=200;
    private byte[] image(boolean real,int paper) {
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)paper);
        for(int y=60;y<=135;y++)p[y*W+110]=0;
        for(int y=125;y<=135;y++)for(int x=90;x<=110;x++)p[y*W+x]=0;
        for(int x=97;x<=123;x++)for(int dy=-2;dy<=2;dy++) {
            int y=75+Math.round((110-x)*.2f)+dy;
            p[y*W+x]=(byte)(real||x>=110?40:160);
        }
        return p;
    }
    private int[] count(byte[] p)throws Exception {
        Class<?> h=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var ctor=h.getDeclaredConstructor(int.class,int.class,int.class,int.class,int.class,float.class,float.class);
        ctor.setAccessible(true);Object head=ctor.newInstance(210,90,110,125,135,100f,130f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("tremoloStrokeCounts",byte[].class,int.class,int.class,h,float.class,List.class);
        m.setAccessible(true);return (int[])m.invoke(null,p,W,H,head,14f,List.of(head));
    }
    @Test public void shadingBesideOneSidedFlagIsNotTremolo()throws Exception {assertEquals(0,count(image(false,185))[0]);}
    @Test public void darkCrossStrokeOnShadedPaperRemainsTremolo()throws Exception {assertEquals(1,count(image(true,185))[0]);}
    @Test public void darkCrossStrokeOnWhitePaperRemainsTremolo()throws Exception {assertEquals(1,count(image(true,255))[0]);}
    @Test public void sourcePixelsRemainUnchanged()throws Exception {byte[] p=image(false,185),before=p.clone();count(p);assertArrayEquals(before,p);}
}
