// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original long double beam with a narrow endpoint segmentation island. */
public class PairedBeamCornerTest {
    static final int W=320,H=180;
    final byte[] gray=new byte[W*H];
    public PairedBeamCornerTest(){Arrays.fill(gray,(byte)255);band(70);band(82);}
    void band(int top){for(int y=top;y<top+6;y++)for(int x=40;x<=260;x++)gray[y*W+x]=0;}
    boolean match(){return StemOwnedBeamTip.pairedCorner(gray,W,H,new int[]{40,70,-1},new int[]{260,70,-1},44,78,8,15,80,16);}
    @Test public void twoContinuousBandsOwnNarrowCorner(){assertTrue(match());}
    @Test public void aSingleBandCannotOwnTallCorner(){for(int y=82;y<88;y++)Arrays.fill(gray,y*W,(y+1)*W,(byte)255);assertFalse(match());}
    @Test public void disconnectedBandsDoNotEstablishOwnership(){for(int y=60;y<100;y++)for(int x=100;x<150;x++)gray[y*W+x]=(byte)255;assertFalse(match());}
    @Test public void fullOvalIsNotACorner(){assertFalse(StemOwnedBeamTip.pairedCorner(gray,W,H,new int[]{40,70,-1},new int[]{260,70,-1},44,78,22,15,220,16));}
    @Test public void independentOpposingStemsAreNotOwned(){assertFalse(StemOwnedBeamTip.pairedCorner(gray,W,H,new int[]{40,70,-1},new int[]{260,70,1},44,78,8,15,80,16));}
    @Test public void sourcePixelsArePreserved(){var before=gray.clone();match();assertArrayEquals(before,gray);}
}
