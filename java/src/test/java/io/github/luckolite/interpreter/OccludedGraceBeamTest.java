// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometry: compact parallel beams blurred together beside one stem. */
public final class OccludedGraceBeamTest {
    private static final int W=240,H=180;
    private static final int[] A={100,60,-1},B={124,63,-1};
    private byte[] page(int count,boolean fused) {
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)220);
        for(int x=100;x<=124;x++) {
            int top=60+Math.round((x-100)*.125f);
            for(int b=0;b<count;b++)for(int dy=0;dy<4;dy++)p[(top+b*8+dy)*W+x]=20;
            if(fused&&x>=117)for(int y=top;y<top+(count-1)*8+4;y++)p[y*W+x]=20;
        }
        return p;
    }
    private int count(byte[] p){return PairedGraceBeamInk.count(p,W,H,A,B,14);}
    @Test public void twoClearCoresSurviveOneBlurredStemEdge(){assertEquals(2,count(page(2,true)));}
    @Test public void reversedInputPreservesTheClearSide(){assertEquals(2,PairedGraceBeamInk.count(page(2,true),W,H,B,A,14));}
    @Test public void clearUnblurredPairRetainsCount(){assertEquals(2,count(page(2,false)));}
    @Test public void oneBeamDoesNotBecomeTwo(){assertEquals(0,count(page(1,true)));}
    @Test public void fullSizePairsDoNotUseTheNarrowFallback(){assertEquals(0,PairedGraceBeamInk.countFullSize(page(2,true),W,H,A,B,14));}
    @Test public void threeAmbiguousBeamsAreNotDeclaredTwo(){assertEquals(0,count(page(3,true)));}
    @Test public void twoIsolatedColumnSpotsAreNotParallelBeams(){byte[] p=page(2,true);for(int y=55;y<82;y++)for(int x=100;x<117;x++)if(x!=105&&x!=106&&x!=107&&x!=113&&x!=114&&x!=115)p[y*W+x]=(byte)220;assertEquals(0,count(p));}
    @Test public void sourcePixelsArePreserved(){byte[] p=page(2,true),copy=p.clone();count(p);assertArrayEquals(copy,p);}
}
