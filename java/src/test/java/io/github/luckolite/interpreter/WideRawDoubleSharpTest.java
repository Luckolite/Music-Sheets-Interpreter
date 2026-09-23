// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original double-sharp and neighboring-stem geometry. */
public class WideRawDoubleSharpTest {
    static final int W=150,H=120;
    final byte[] gray=new byte[W*H];
    public WideRawDoubleSharpTest(){
        Arrays.fill(gray,(byte)255);
        for(int y:new int[]{46,63})for(int x=8;x<140;x++)gray[y*W+x]=0;
        for(int y=35;y<=73;y++)gray[y*W+65]=0;
    }
    void rect(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)gray[yy*W+xx]=0;}
    boolean matches()throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var cc=type.getDeclaredConstructors()[0];cc.setAccessible(true);
        var seed=cc.newInstance(240,64,89,43,68,77f,55f);
        var head=cc.newInstance(250,92,112,47,63,101f,55f);
        var ac=Class.forName(OmrScoreInterpreter.class.getName()+"$AccidentalCandidate");
        var ctor=ac.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var candidate=ctor.newInstance(seed,(byte)5);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("rawWideDoubleSharp",byte[].class,
                int.class,int.class,List.class,type,float.class);method.setAccessible(true);
        return (boolean)method.invoke(null,gray,W,H,List.of(candidate),head,16f);
    }
    void doubleSharp(boolean lower){
        rect(69,49,9,4);rect(79,49,9,4);
        rect(75,53,7,5);
        if(lower){rect(70,58,8,5);rect(79,58,8,5);}
    }
    @Test public void pairedNotchesRecoverDoubleSharpBesideStem()throws Exception {doubleSharp(true);assertTrue(matches());}
    @Test public void oneNotchIsInsufficient()throws Exception {doubleSharp(false);assertFalse(matches());}
    @Test public void naturalCrossbarsAreNotDoubleSharp()throws Exception {
        rect(70,39,3,23);rect(81,48,3,26);rect(70,50,14,3);rect(70,60,14,3);
        assertFalse(matches());
    }
}
