// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class SixthPrintedRuleTest {
    private static final int W=420,H=260;
    private static byte[][] page(boolean lowerBeam,int extraWidth,boolean oneSide) {
        byte[] gray=new byte[W*H],labels=new byte[W*H];Arrays.fill(gray,(byte)245);
        int top=lowerBeam?116:84;
        for(int line=0;line<5;line++)for(int x=80;x<340;x++) {
            int y=top+line*16;gray[y*W+x]=40;labels[y*W+x]=4;
        }
        int extra=lowerBeam?100:164;
        for(int x=210-extraWidth;x<=210+extraWidth;x++)if(!oneSide||x<210)gray[extra*W+x]=(byte)190;
        for(int y=118;y<=130;y++)for(int x=200;x<=220;x++)
            if((x-210)*(x-210)/100f+(y-124)*(y-124)/36f<=1){gray[y*W+x]=0;labels[y*W+x]=2;}
        return new byte[][]{labels,gray};
    }
    private static float[] pitch(byte[][] data)throws Exception {
        var staffType=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var sc=staffType.getDeclaredConstructors()[0];sc.setAccessible(true);Object staff=sc.newInstance(100f,164f,16f);
        var headType=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var hc=headType.getDeclaredConstructors()[0];hc.setAccessible(true);Object head=hc.newInstance(220,200,220,118,130,210f,124f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("localStaffPitch",byte[].class,byte[].class,int.class,int.class,staffType,headType);
        method.setAccessible(true);return (float[])method.invoke(null,data[0],data[1],W,H,staff,head);
    }
    @Test public void fadedBottomRuleRejectsAnUpperBeamAlias()throws Exception {
        float[] p=pitch(page(false,130,false));assertEquals(164,p[0],.6);assertEquals(5,Math.round((p[0]-124)*2/p[1]));
    }
    @Test public void fadedTopRuleRejectsALowerBeamAlias()throws Exception {
        float[] p=pitch(page(true,130,false));assertEquals(164,p[0],.6);assertEquals(5,Math.round((p[0]-124)*2/p[1]));
    }
    @Test public void realShiftWithoutAnExtraRuleRemainsAvailable()throws Exception {
        float[] p=pitch(page(false,0,false));assertEquals(148,p[0],.6);
    }
    @Test public void shortLedgerInkCannotOverrideACompleteStaff()throws Exception {
        float[] p=pitch(page(false,20,false));assertEquals(148,p[0],.6);
    }
    @Test public void oneSidedMarkCannotOverrideACompleteStaff()throws Exception {
        float[] p=pitch(page(false,130,true));assertEquals(148,p[0],.6);
    }
}
