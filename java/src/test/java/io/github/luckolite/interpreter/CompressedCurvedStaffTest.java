// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original five-rule raster and incomplete/remote-rule counterexamples. */
public final class CompressedCurvedStaffTest {
    static final int W=1200,H=350;
    static byte[] page(int rules,int paper,int ink,boolean sparse) {
        byte[] g=new byte[W*H];Arrays.fill(g,(byte)paper);
        for(int x=25;x<W-25;x++) {
            if(sparse&&x%120>25)continue;
            for(int i=0;i<rules;i++) {
                int y=Math.round(200-.02f*x-i*14);
                g[y*W+x]=(byte)ink;g[(y+1)*W+x]=(byte)ink;
            }
        }
        return g;
    }
    static void assertTrack(byte[] g) {
        var track=StaffPitchTrack.detect(g,W,H,146,188,10.5f);
        assertNotNull(track);
        for(int x:new int[]{240,600,960}) {
            assertEquals(200-.02f*x+.5f,track.at(x)[0],1.2f);
            assertEquals(14,track.at(x)[1],.5f);
            assertEquals(-4,Math.round((track.at(x)[0]-(200-.02f*x+28))*2/track.at(x)[1]));
        }
    }
    @Test public void completePrintedRulesRepairCompressedSeed(){assertTrack(page(5,255,45,false));}
    @Test public void shadedPrintedRulesRepairCompressedSeed(){assertTrack(page(5,170,60,false));}
    @Test public void fourRulesCannotInventFifthRule(){assertNull(StaffPitchTrack.detect(page(4,255,45,false),W,H,146,188,10.5f));}
    @Test public void isolatedLedgerGroupsCannotRecalibrateSeed(){assertNull(StaffPitchTrack.detect(page(5,255,45,true),W,H,146,188,10.5f));}
    @Test public void remoteCompleteStaffCannotReplaceSeed(){assertNull(StaffPitchTrack.detect(page(5,255,45,false),W,H,86,128,10.5f));}
}
