// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original long chord stem ending on a beam beyond a bounded first trace. */
public final class CappedStemBeamTest {
    private static final int W=240,H=250;
    private byte[] page(boolean beam,boolean disconnected) {
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)180);
        for(int y=45;y<=215;y++)if(!disconnected||y<78||y>88)p[y*W+150]=20;
        if(beam)for(int y=45;y<=49;y++)for(int x=105;x<=150;x++)p[y*W+x]=20;
        return p;
    }
    private int[] extend(byte[] p,int end){return CappedStemBeam.extend(p,W,H,new int[]{150,end,-1},215,14,140);}
    @Test public void cappedStemCanReachItsVerifiedBeam(){assertArrayEquals(new int[]{150,45,-1},extend(page(true,false),90));}
    @Test public void stemWithoutBeamDoesNotExtend(){assertEquals(90,extend(page(false,false),90)[1]);}
    @Test public void detachedBeamDoesNotBridgeWhiteGap(){assertEquals(90,extend(page(true,true),90)[1]);}
    @Test public void ordinaryShortTraceIsUnchanged(){assertEquals(170,extend(page(true,false),170)[1]);}
    @Test public void sourcePixelsArePreserved(){byte[] p=page(true,false),before=p.clone();extend(p,90);assertArrayEquals(before,p);}
}
