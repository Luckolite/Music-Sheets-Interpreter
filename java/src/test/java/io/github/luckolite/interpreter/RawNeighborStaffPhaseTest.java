// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original five-rule raster, with a large locally occluded strip. */
public class RawNeighborStaffPhaseTest {
    private byte[] page(boolean sixth,boolean oneSide,boolean broken) {
        int w=800,h=280;byte[] g=new byte[w*h];Arrays.fill(g,(byte)190);
        for(int line=0;line<(sixth?6:5);line++)for(int x=20;x<780;x++) {
            if(oneSide&&x>400||broken&&line==2&&x>170&&x<630)continue;
            int y=Math.round(181-line*14+(x-400)*.025f);
            g[y*w+x]=35;g[(y+1)*w+x]=35;
        }
        // A beam overlaps the lowest rule only in the central strip.
        for(int y=181;y<=188;y++)for(int x=330;x<=470;x++)g[y*800+x]=30;
        return g;
    }
    @Test public void bracketsOccludedPrintedStaff(){float[] p=NeighboringStaffPhase.rawBracket(page(false,false,false),800,280,400,201,14);assertNotNull(p);assertEquals(181.5,p[0],1);assertEquals(14,p[1],.5);}
    @Test public void noExtrapolationFromOneSide(){assertNull(NeighboringStaffPhase.rawBracket(page(false,true,false),800,280,400,201,14));}
    @Test public void sixthRuleRejectsAmbiguity(){float[] p=NeighboringStaffPhase.rawBracket(page(true,false,false),800,280,400,201,14);assertNull(Arrays.toString(p),p);}
    @Test public void incompleteInterveningRulesReject(){assertNull(NeighboringStaffPhase.rawBracket(page(false,false,true),800,280,400,201,14));}
    @Test public void nearAccurateSeedIsNotRescaled(){assertNull(NeighboringStaffPhase.rawBracket(page(false,false,false),800,280,400,182,14));}
}
