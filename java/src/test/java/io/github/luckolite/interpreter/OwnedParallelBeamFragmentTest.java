// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original paired-beam raster and independent note-owner controls. */
public class OwnedParallelBeamFragmentTest {
    private static final int W=280,H=240,G=16;
    private final byte[] gray=new byte[W*H];
    public OwnedParallelBeamFragmentTest(){
        Arrays.fill(gray,(byte)255);
        for(int x=60;x<=190;x++) {
            int cy=Math.round(110+(x-150)*.15f);
            for(int dy=-10;dy<=10;dy++)if(dy<=-3||dy>=3)gray[(cy+dy)*W+x]=0;
        }
        // A complete note below the beam owns the stem at the false fragment.
        for(int y=109;y<=160;y++)gray[y*W+140]=0;
        for(int y=151;y<=169;y++)for(int x=122;x<=142;x++)
            if(Math.pow((x-132)/10d,2)+Math.pow((y-160)/9d,2)<=1)gray[y*W+x]=0;
    }
    private int rejected(boolean owner)throws Exception {
        var component=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var cc=component.getDeclaredConstructors()[0];cc.setAccessible(true);
        var main=cc.newInstance(310,122,142,151,169,132f,160f);
        var fragment=cc.newInstance(219,141,159,103,117,150f,110f);
        var staff=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var sc=staff.getDeclaredConstructors()[0];sc.setAccessible(true);
        var line=sc.newInstance(80f,144f,16f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("mergedBeamInteriorHeads",byte[].class,
                int.class,int.class,List.class,List.class);method.setAccessible(true);
        return ((List<?>)method.invoke(null,gray,W,H,owner?List.of(main,fragment):List.of(fragment),List.of(line))).size();
    }
    @Test public void sizeableMaskIslandOnTwoStemOwnedBeamsIsRejected()throws Exception {
        assertTrue(ParallelBeamTip.matches(gray,W,H,150,110,G));assertEquals(1,rejected(true));
    }
    @Test public void ownerIsRequired()throws Exception {assertEquals(0,rejected(false));}
    @Test public void roundedHeadBulgeProtectsAnIndependentNote()throws Exception {
        for(int y=94;y<=126;y++)for(int x=137;x<=163;x++)
            if(Math.pow((x-150)/13d,2)+Math.pow((y-110)/16d,2)<=1)gray[y*W+x]=0;
        assertEquals(0,rejected(true));
    }
    @Test public void aSingleBeamCannotOwnTheFragment()throws Exception {
        for(int x=60;x<=190;x++)for(int y=111;y<=125;y++)
            if(x!=140&&gray[y*W+x]==0)gray[y*W+x]=(byte)255;
        assertEquals(0,rejected(true));
    }
}
