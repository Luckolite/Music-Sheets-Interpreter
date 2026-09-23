// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original natural and flat drawings crossing several full-width staff rules. */
public class StaffRuleNaturalEndpointTest {
    static final int W=120,H=110;
    final byte[] gray=new byte[W*H];
    public StaffRuleNaturalEndpointTest(){Arrays.fill(gray,(byte)255);}
    void rect(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)gray[yy*W+xx]=0;}
    void glyph(boolean natural){
        rect(25,18,2,30);rect(35,30,2,natural?29:18);
        rect(25,30,12,4);rect(25,44,12,4);
        for(int y:new int[]{18,34,48,62}){rect(0,y,17,3);rect(21,y,W-21,3);}
    }
    boolean recovered()throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var seed=ctor.newInstance(96,25,36,30,47,30.5f,38.5f);
        var head=ctor.newInstance(180,48,60,33,43,54f,38f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("rawNaturalAtSeed",byte[].class,
                int.class,int.class,type,type,float.class);m.setAccessible(true);
        return (boolean)m.invoke(null,gray,W,H,seed,head,14f);
    }
    @Test public void naturalSurvivesFullWidthStaffRules()throws Exception {glyph(true);assertTrue(recovered());}
    @Test public void flatWithStaffRulesStaysFlat()throws Exception {glyph(false);assertFalse(recovered());}
}
