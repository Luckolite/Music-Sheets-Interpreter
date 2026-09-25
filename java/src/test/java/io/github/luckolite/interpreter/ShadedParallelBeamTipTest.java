// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
public class ShadedParallelBeamTipTest {
    static final int W=240,H=180,G=16;
    byte[] page(int paper,boolean second){byte[] p=new byte[W*H];Arrays.fill(p,(byte)paper);
        for(int x=70;x<=180;x++)for(int y=81;y<=85;y++)p[y*W+x]=35;
        if(second)for(int x=70;x<=180;x++)for(int y=95;y<=99;y++)p[y*W+x]=35;
        return p;}
    @Test public void genuineWhiteDoubleBeamRemains(){assertTrue(ParallelBeamTip.matches(page(255,true),W,H,80,90,G));}
    @Test public void genuineShadedDoubleBeamRemains(){assertTrue(ParallelBeamTip.matches(page(153,true),W,H,80,90,G));}
    @Test public void oneBeamOnShadedPaperCannotSplit(){assertFalse(ParallelBeamTip.matches(page(153,false),W,H,80,90,G));}
    @Test public void palePaperBandsAreNotSecondBeamCores(){var p=page(153,false);for(int x=70;x<=180;x++)for(int y=95;y<=99;y++)p[y*W+x]=(byte)146;assertFalse(ParallelBeamTip.matches(p,W,H,80,90,G));}
    @Test public void sourceInkIsPreserved(){var p=page(153,true);var before=p.clone();ParallelBeamTip.matches(p,W,H,80,90,G);assertArrayEquals(before,p);}
}
