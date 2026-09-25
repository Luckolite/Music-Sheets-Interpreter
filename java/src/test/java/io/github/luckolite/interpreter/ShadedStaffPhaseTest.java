// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original five-rule printed staff with an independently misplaced semantic seed. */
public class ShadedStaffPhaseTest {
    private float[] phase(int shift,boolean sixth,boolean white)throws Exception{
        int w=420,h=300;float gap=16,bottom=164;byte[] g=new byte[w*h],l=new byte[w*h];Arrays.fill(g,(byte)(white?255:185));
        for(int line=0;line<5+(sixth?1:0);line++)for(int x=30;x<390;x++){
            int y=Math.round(bottom+shift*gap-line*gap);g[y*w+x]=40;g[(y+1)*w+x]=40;
        }
        for(int line=0;line<5;line++)for(int x=30;x<390;x++)l[(164-line*16)*w+x]=4;
        Class<?> sc=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff"),hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var s=sc.getDeclaredConstructor(float.class,float.class,float.class);s.setAccessible(true);
        var c=hc.getDeclaredConstructors()[0];c.setAccessible(true);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("localStaffPitch",byte[].class,byte[].class,int.class,int.class,sc,hc);m.setAccessible(true);
        return (float[])m.invoke(null,l,g,w,h,s.newInstance(100f,bottom,gap),c.newInstance(180,201,219,140,154,210f,147f));
    }
    @Test public void rawFiveRulesRecoverTwoLineDownwardAlias()throws Exception{assertEquals(196.5f,phase(2,false,false)[0],.6f);}
    @Test public void rawFiveRulesRecoverUpwardAlias()throws Exception{assertEquals(148.5f,phase(-1,false,false)[0],.6f);}
    @Test public void sixthParallelRuleCannotChooseAnAmbiguousPhase()throws Exception{assertEquals(164,phase(2,true,false)[0],.6f);}
}
