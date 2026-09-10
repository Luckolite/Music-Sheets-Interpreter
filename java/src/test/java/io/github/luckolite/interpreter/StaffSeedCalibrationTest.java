// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original sloping rule geometry with independently chosen imperfect semantic seeds. */
public class StaffSeedCalibrationTest {
    static final int W=1200,H=350;
    static float bottom(int x){return 200+20*x/(float)W;}
    static byte[] page(int rules,int offset,boolean sparse){
        byte[] g=new byte[W*H];Arrays.fill(g,(byte)240);
        for(int x=30;x<W-30;x++) {
            if(sparse&&x>400)continue;
            for(int line=0;line<rules;line++) {
                int y=Math.round(bottom(x)-line*16+offset);g[y*W+x]=45;g[(y+1)*W+x]=45;
            }
        }
        return g;
    }
    static void correct(StaffPitchTrack t){
        assertNotNull(t);
        for(int x:new int[]{200,440,720,1000}) {
            assertEquals(bottom(x)+.5,t.at(x)[0],1.8);
            assertEquals(16,t.at(x)[1],.6);
            assertEquals(7,Math.round((t.at(x)[0]-(bottom(x)-56))*2/t.at(x)[1]));
        }
    }
    @Test public void compressedSeedSpacingCanUseTheCompletePrintedStaff(){correct(StaffPitchTrack.detect(page(5,0,false),W,H,154,208,13.5f));}
    @Test public void expandedSeedSpacingCanUseTheCompletePrintedStaff(){correct(StaffPitchTrack.detect(page(5,0,false),W,H,139,215,19));}
    @Test public void aDifferentDistantStaffCannotReplaceTheSeed(){assertNull(StaffPitchTrack.detect(page(5,60,false),W,H,154,208,13.5f));}
    @Test public void fourRulesCannotAuthorizeRecalibration(){assertNull(StaffPitchTrack.detect(page(4,0,false),W,H,154,208,13.5f));}
    @Test public void aSmallPrintedFragmentIsNotAFullStaffTrack(){assertNull(StaffPitchTrack.detect(page(5,0,true),W,H,154,208,13.5f));}
    @Test public void implausiblyDifferentSpacingIsRejected(){assertNull(StaffPitchTrack.detect(page(5,0,false),W,H,164,208,11));}
    @Test public void rawPageIsNotChanged(){byte[] g=page(5,0,false),copy=g.clone();StaffPitchTrack.detect(g,W,H,154,208,13.5f);assertArrayEquals(copy,g);}
}
