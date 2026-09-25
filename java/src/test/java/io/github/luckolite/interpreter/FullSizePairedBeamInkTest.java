// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original full-size beaming fixtures; no score pixels or geometry. */
public final class FullSizePairedBeamInkTest {
    private static final int W=240,H=180;
    private byte[] page(int beams,int paper,int thickness,boolean rule) {
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)paper);
        for(int x=100;x<=132;x++)for(int i=0;i<beams;i++)for(int dy=0;dy<thickness;dy++)
            p[(50+i*9+dy)*W+x]=20;
        if(rule)for(int x=70;x<=165;x++)for(int dy=0;dy<3;dy++)p[(69+dy)*W+x]=30;
        for(int y=50;y<108;y++){p[y*W+100]=20;p[y*W+132]=20;}
        return p;
    }
    private int count(byte[] p){return PairedGraceBeamInk.countFullSize(p,W,H,new int[]{100,50,-1},new int[]{132,50,-1},14);}
    @Test public void thirdFullSizeBeamIsRecovered(){assertEquals(3,count(page(3,255,6,false)));}
    @Test public void shadedThirdBeamIsRecovered(){assertEquals(3,count(page(3,160,6,false)));}
    @Test public void thickThirdBeamIsRecovered(){assertEquals(3,count(page(3,210,8,false)));}
    @Test public void threePixelStaffRuleIsNotAThirdBeam(){assertEquals(2,count(page(2,210,6,true)));}
    @Test public void singleBeamDoesNotCreateAPair(){assertEquals(0,count(page(1,255,6,false)));}
    @Test public void thinOutlinedBeamEdgesDoNotCreateThreeBeams(){assertEquals(0,count(page(3,255,3,false)));}
    @Test public void onlyOneProbeWithThirdInkIsRejected(){byte[] p=page(2,255,6,false);for(int x=106;x<=111;x++)for(int y=68;y<=73;y++)p[y*W+x]=20;assertEquals(0,count(p));}
    @Test public void sourcePixelsArePreserved(){byte[] p=page(3,210,6,false),before=p.clone();count(p);assertArrayEquals(before,p);}
}
