// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original flat bowls with a stem-labelled spine; no commercial score pixels. */
public class SplitFlatSpineTest {
    private Object make(String name,Object...args)throws Exception {
        var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];
        c.setAccessible(true);return c.newInstance(args);
    }
    private boolean detect(String shape,float headY,boolean rule,boolean seedPresent)throws Exception {
        int w=100,h=100;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=25;y<=64;y++)for(int x=30;x<=32;x++)gray[y*w+x]=0;
        if(!shape.equals("stem")) {
            for(int x=30;x<=41;x++)for(int y:new int[]{49,50,60,61})gray[y*w+x]=0;
            for(int y=49;y<=61;y++)for(int x=40;x<=41;x++)gray[y*w+x]=0;
        }
        if(shape.equals("sharp"))for(int y=25;y<=64;y++)for(int x=40;x<=41;x++)gray[y*w+x]=0;
        if(shape.equals("natural")) {
            for(int y=25;y<=64;y++)for(int x=30;x<=41;x++)gray[y*w+x]=(byte)255;
            for(int y=25;y<=51;y++)for(int x=30;x<=32;x++)gray[y*w+x]=0;
            for(int y=38;y<=64;y++)for(int x=40;x<=41;x++)gray[y*w+x]=0;
            for(int x=30;x<=41;x++)for(int y:new int[]{38,39,50,51})gray[y*w+x]=0;
        }
        if(rule)for(int x=0;x<w;x++)for(int y=55;y<=56;y++)gray[y*w+x]=0;
        Object seed=make("Component",55,30,41,49,64,34f,56f);
        Object candidate=make("AccidentalCandidate",seed,(byte)3);
        Object head=make("Component",120,53,69,Math.round(headY-6),Math.round(headY+6),61f,headY);
        for(var m:OmrScoreInterpreter.class.getDeclaredMethods())if(m.getName().equals("rawFlatFromBowl")) {
            m.setAccessible(true);return (boolean)m.invoke(null,gray,w,h,seedPresent?List.of(candidate):List.of(),head,16f);
        }
        throw new IllegalStateException();
    }
    @Test public void lostSpineIsRecoveredFromPrintedInk()throws Exception {assertTrue(detect("flat",55,false,true));}
    @Test public void staffRuleDoesNotHideFlatSpine()throws Exception {assertTrue(detect("flat",55,true,true));}
    @Test public void flatDoesNotChangeNeighboringHigherPitch()throws Exception {assertFalse(detect("flat",39,false,true));}
    @Test public void stemWithoutBowlIsNotFlat()throws Exception {assertFalse(detect("stem",55,true,true));}
    @Test public void sharpIsNotFlat()throws Exception {assertFalse(detect("sharp",55,false,true));}
    @Test public void naturalIsNotFlat()throws Exception {assertFalse(detect("natural",55,false,true));}
    @Test public void unlabelledInkAloneIsNotEnough()throws Exception {assertFalse(detect("flat",55,false,false));}
}
