// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import java.lang.reflect.Method;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original wide, capped multimeasure-rest bars; no score pixels. */
public final class WideRestBarCountTest {
    static final int W=1000,H=140;
    static byte[] page(boolean left,boolean right,int thickness){
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)255);
        for(int y=70;y<70+thickness;y++)for(int x=60;x<=940;x++)p[y*W+x]=0;
        for(int y=64;y<70+thickness+6;y++)for(int x:new int[]{60,61,939,940})
            if(x<500?left:right)p[y*W+x]=0;
        return p;
    }
    static boolean bar(byte[] p)throws Exception {
        Method m=OmrScoreInterpreter.class.getDeclaredMethod("heavyRestBarAtRow",byte[].class,int.class,int.class,int.class,int.class,float.class);m.setAccessible(true);
        return (boolean)m.invoke(null,p,W,H,500,73,12f);
    }
    @Test public void wideCappedBarHasNoArbitrarySixtyGapLimit()throws Exception{assertTrue(bar(page(true,true,7)));}
    @Test public void detachedHorizontalStrokeHasNoCaps()throws Exception{assertFalse(bar(page(false,false,7)));}
    @Test public void oneStemIsNotEnough()throws Exception{assertFalse(bar(page(true,false,7)));}
    @Test public void thinStaffRuleIsNotHeavyRest()throws Exception{assertFalse(bar(page(true,true,1)));}
    @Test public void oversizedBlackRectangleIsNotRest()throws Exception{assertFalse(bar(page(true,true,20)));}
    @Test public void preservesImage()throws Exception{byte[] p=page(true,true,7),copy=p.clone();bar(p);assertArrayEquals(copy,p);}
}
