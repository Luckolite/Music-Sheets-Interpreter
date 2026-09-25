// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original long bowed curves crossing separate printed rules; no source raster. */
public class LongStaffCrossingTieTest {
    private boolean arc(int mode,boolean below)throws Exception {
        int w=300,h=210,left=45,right=235;float cy=110,gap=16;
        byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);
        int sign=below?1:-1;
        for(int distance:new int[]{8,24,40})for(int dy=-2;dy<=2;dy++)for(int x=0;x<w;x++) {
            int y=Math.round(cy)+sign*distance+dy;
            if(Math.abs(dy)<=1)gray[y*w+x]=0;
            labels[y*w+x]=4;
        }
        if(mode!=1)for(int x=left;x<=right;x++) {
            float t=(x-left)/(float)(right-left);
            if(mode==3 && t>.55f)continue;
            if(mode==4 && t>.42f && t<.58f)continue;
            float bend=mode==2?16*t:16*4*t*(1-t);
            int y=Math.round(cy+sign*(15+bend));
            gray[y*w+x]=(byte)(mode==5?200:0);
            if(labels[y*w+x]!=4)labels[y*w+x]=5;
        }
        var method=OmrScoreInterpreter.class.getDeclaredMethod("hasContinuousTieArc",byte[].class,byte[].class,
                int.class,int.class,int.class,int.class,float.class,float.class);
        method.setAccessible(true);return (boolean)method.invoke(null,labels,gray,w,h,left,right,cy,gap);
    }
    @Test public void longReturningCurveCrossesRulesAbove()throws Exception{assertTrue(arc(0,false));}
    @Test public void longReturningCurveCrossesRulesBelow()throws Exception{assertTrue(arc(0,true));}
    @Test public void staffRulesAloneDoNotTie()throws Exception{assertFalse(arc(1,false));}
    @Test public void slopedBeamDoesNotReturn()throws Exception{assertFalse(arc(2,false));}
    @Test public void oneShoulderIsInsufficient()throws Exception{assertFalse(arc(3,false));}
    @Test public void missingMiddleIsNotStaffOcclusion()throws Exception{assertFalse(arc(4,false));}
    @Test public void uniformlyPaleCurveCannotReplaceDarkCore()throws Exception{assertFalse(arc(5,false));}
}
