// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original short curves intersecting a printed staff rule. */
public class StaffCrossingTieTest {
    private static final int W=220,H=180;

    private boolean detect(boolean curve,boolean slopedBeam)throws Exception {
        byte[] labels=new byte[W*H],gray=new byte[W*H];
        Arrays.fill(gray,(byte)255);
        for(int y=60;y<=124;y+=16)for(int x=0;x<W;x++) {
            gray[y*W+x]=0;labels[y*W+x]=OmrMeasurePostProcessor.STAFF;
        }
        for(int y=40;y<=124;y++){gray[y*W+113]=0;labels[y*W+113]=OmrMeasurePostProcessor.STEM_OR_REST;}
        if(curve||slopedBeam)for(int x=101;x<=124;x++) {
            float t=(x-101)/23f;
            int y=Math.round(slopedBeam?64-6*t:64-6*4*t*(1-t));
            gray[y*W+x]=0;
            if(y!=60)labels[y*W+x]=OmrMeasurePostProcessor.SYMBOL;
        }
        var method=OmrScoreInterpreter.class.getDeclaredMethod("hasContinuousTieArc",
                byte[].class,byte[].class,int.class,int.class,int.class,int.class,float.class,float.class);
        method.setAccessible(true);
        return (boolean)method.invoke(null,labels,gray,W,H,100,125,68f,16f);
    }

    @Test public void shortReturningTieCanCrossAStaffRule()throws Exception {
        assertTrue(detect(true,false));
    }

    @Test public void staffAndBarlineAloneCannotBecomeATie()throws Exception {
        assertFalse(detect(false,false));
    }

    @Test public void slopingBeamCrossingAStaffRuleIsNotAReturningTie()throws Exception {
        assertFalse(detect(false,true));
    }
}
