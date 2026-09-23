// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original paired-beam end with an erroneous small notehead mask on one core. */
public class OffsetParallelBeamIslandTest {
    private static final int W=260,H=190,G=16;
    private final byte[] gray=new byte[W*H];
    public OffsetParallelBeamIslandTest(){
        Arrays.fill(gray,(byte)255);
        for(int x=35;x<=180;x++)for(int y:new int[]{98,112})for(int dy=0;dy<6;dy++)gray[(y+dy)*W+x]=0;
    }
    private int rejected(boolean wideHead)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var cc=type.getDeclaredConstructors()[0];cc.setAccessible(true);
        var fragment=wideHead?cc.newInstance(140,154,174,91,107,164f,99f):
                cc.newInstance(40,160,169,97,101,164.5f,99f);
        var staff=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var sc=staff.getDeclaredConstructors()[0];sc.setAccessible(true);
        var line=sc.newInstance(80f,144f,16f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("offsetParallelBeamIslandHeads",
                byte[].class,int.class,int.class,List.class,List.class);
        method.setAccessible(true);
        return ((List<?>)method.invoke(null,gray,W,H,List.of(fragment),List.of(line))).size();
    }
    @Test public void smallIslandOnUpperCoreIsRejected()throws Exception {assertEquals(1,rejected(false));}
    @Test public void singleBeamIsInsufficient()throws Exception {
        for(int x=35;x<=180;x++)for(int y=112;y<118;y++)gray[y*W+x]=(byte)255;
        assertEquals(0,rejected(false));
    }
    @Test public void normalSizedHeadIsPreserved()throws Exception {assertEquals(0,rejected(true));}
    @Test public void independentStemIsPreserved()throws Exception {
        for(int y=100;y<=149;y++)gray[y*W+160]=0;
        assertEquals(0,rejected(false));
    }
    @Test public void sourcePixelsRemainUnchanged()throws Exception {
        var copy=gray.clone();rejected(false);assertArrayEquals(copy,gray);
    }
}
