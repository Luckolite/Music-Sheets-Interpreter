// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original paired grace beams, narrow staff rules, and an oblique grace slash. */
public final class PairedGraceBeamInkTest {
    private static final int W=240,H=180;
    private byte[] page(int beams,int paper,boolean slash,boolean rule) {
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)paper);
        for(int x=100;x<=124;x++)for(int i=0;i<beams;i++)for(int dy=0;dy<4;dy++)
            p[(60+Math.round((x-100)*.125f)+i*8+dy)*W+x]=20;
        if(slash)for(int x=100;x<=124;x++)for(int dy=0;dy<2;dy++)p[(77-Math.round((x-100)*.65f)+dy)*W+x]=20;
        if(rule)for(int x=80;x<=145;x++)p[78*W+x]=60;
        for(int y=60;y<100;y++)p[y*W+100]=20;
        for(int y=63;y<107;y++)p[y*W+124]=20;
        return p;
    }
    private int count(byte[] p){return PairedGraceBeamInk.count(p,W,H,new int[]{100,60,-1},new int[]{124,63,-1},14);}
    @Test public void twoParallelBeamsAreRecovered(){assertEquals(2,count(page(2,255,false,false)));}
    @Test public void shadedTwoBeamPairIsRecovered(){assertEquals(2,count(page(2,180,false,false)));}
    @Test public void thinStaffRuleDoesNotBecomeThirdBeam(){assertEquals(2,count(page(2,180,false,true)));}
    @Test public void singleBeamIsNotUpgraded(){assertEquals(0,count(page(1,255,false,false)));}
    @Test public void diagonalGraceSlashIsNotAParallelBeam(){assertEquals(0,count(page(1,255,true,false)));}
    @Test public void missingSecondStemDoesNotProveAPair(){assertEquals(0,PairedGraceBeamInk.count(page(2,255,false,false),W,H,new int[]{100,60,-1},null,14));}
    @Test public void oppositeStemDirectionsDoNotFormAPair(){assertEquals(0,PairedGraceBeamInk.count(page(2,255,false,false),W,H,new int[]{100,60,-1},new int[]{124,63,1},14));}
    @Test public void reversedInputOrderPreservesCount(){assertEquals(2,PairedGraceBeamInk.count(page(2,255,false,false),W,H,new int[]{124,63,-1},new int[]{100,60,-1},14));}
    @Test public void sourcePixelsArePreserved(){byte[] p=page(2,180,false,false),before=p.clone();count(p);assertArrayEquals(before,p);}
}
