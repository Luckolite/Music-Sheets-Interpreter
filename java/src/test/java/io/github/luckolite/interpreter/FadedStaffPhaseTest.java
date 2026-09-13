// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class FadedStaffPhaseTest {
    private static final int W=240,H=240;
    private static byte[][] page(boolean below,boolean omittedRule) {
        byte[] gray=new byte[W*H],labels=new byte[W*H];
        Arrays.fill(gray,(byte)195);
        int first=below?112:80;
        for(int i=0;i<5;i++)for(int x=12;x<W-12;x++) {
            int at=(first+16*i)*W+x;
            gray[at]=100;labels[at]=4;
        }
        if(omittedRule)for(int x=12;x<W-12;x++)gray[(below?96:160)*W+x]=110;
        return new byte[][]{labels,gray};
    }
    private static float[] rules(boolean below,boolean omittedRule) {
        byte[][] p=page(below,omittedRule);
        assertTrue(StaffPitchTrack.needsContrast(p[1],W,H,120,160,16));
        return StaffPitchTrack.localRules(p[0],p[1],W,H,120,110,130,160,16);
    }
    @Test public void unlabeledTopRuleRejectsDownwardPhaseShiftOnShadedPaper() {
        assertNull(rules(true,true));
    }
    @Test public void unlabeledBottomRuleRejectsUpwardPhaseShiftOnShadedPaper() {
        assertNull(rules(false,true));
    }
    @Test public void completeLowerStaffStillCorrectsTheReference() {
        float[] found=rules(true,false);assertNotNull(found);assertEquals(176,found[0],.01f);
    }
    @Test public void completeUpperStaffStillCorrectsTheReference() {
        float[] found=rules(false,false);assertNotNull(found);assertEquals(144,found[0],.01f);
    }
    @Test public void unambiguousShadedStaffKeepsItsOriginalPhase() {
        byte[][] p=page(false,false);
        float[] found=StaffPitchTrack.localRules(p[0],p[1],W,H,120,110,130,144,16);
        assertNotNull(found);assertEquals(144,found[0],.01f);assertEquals(16,found[1],.01f);
    }
}
