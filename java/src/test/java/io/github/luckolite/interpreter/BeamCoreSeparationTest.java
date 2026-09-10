// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.*;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original grayscale beam bands bridged by an antialiased staff rule. */
public class BeamCoreSeparationTest {
    static final int W=200,H=180;
    static class Page {
        byte[] gray=new byte[W*H],labels=new byte[W*H];
        Page(){Arrays.fill(gray,(byte)230);for(int y=82;y<=146;y+=16)rect(0,y-2,W-1,y+2,125,4);}
        void rect(int l,int t,int r,int b,int ink,int label){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){gray[y*W+x]=(byte)ink;labels[y*W+x]=(byte)label;}}
        void beams(){rect(60,73,150,79,15,5);rect(60,85,150,91,15,5);}
        int count()throws Exception{
            Class<?> staff=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
            var constructor=staff.getDeclaredConstructor(float.class,float.class,float.class);constructor.setAccessible(true);
            var method=OmrScoreInterpreter.class.getDeclaredMethod("thickNonHeadBands",byte[].class,byte[].class,int.class,int.class,int.class,int.class,int.class,staff);method.setAccessible(true);
            return (int)method.invoke(null,gray,labels,W,H,90,70,100,constructor.newInstance(82f,146f,16f));
        }
    }
    @Test public void lighterStaffBridgeDoesNotFuseTwoDarkBeams()throws Exception{var p=new Page();p.beams();assertEquals(2,p.count());}
    @Test public void oneThickBeamCrossingAStaffRemainsOne()throws Exception{var p=new Page();p.rect(60,77,150,87,15,5);assertEquals(1,p.count());}
    @Test public void aNarrowSlurTerminalIsNotASecondBeam()throws Exception{var p=new Page();p.rect(60,73,150,79,15,5);p.rect(60,85,150,87,15,5);assertEquals(1,p.count());}
    @Test public void staffInkAloneDoesNotCreateABeam()throws Exception{assertEquals(0,new Page().count());}
    @Test public void equallyDarkConnectionDoesNotInventAnInvisibleGap()throws Exception{var p=new Page();p.beams();p.rect(60,80,150,84,15,5);assertEquals(1,p.count());}
    @Test public void anAdjacentNoteheadIsExcludedFromBothThresholds()throws Exception{var p=new Page();p.beams();p.rect(60,85,150,91,15,2);assertEquals(1,p.count());}
    @Test public void lighterBeamCoresUseTheirLocalContrast()throws Exception{var p=new Page();p.rect(60,73,150,79,75,5);p.rect(60,85,150,91,75,5);assertEquals(2,p.count());}
    @Test public void sourcePixelsRemainUnchanged()throws Exception{var p=new Page();p.beams();byte[] g=p.gray.clone(),l=p.labels.clone();p.count();assertArrayEquals(g,p.gray);assertArrayEquals(l,p.labels);}
    @Test public void aLighterStripeAwayFromAnyRuleCannotSplitABeam()throws Exception{var p=new Page();p.rect(60,70,150,88,15,5);p.rect(60,75,150,76,125,5);assertEquals(1,p.count());}
}
