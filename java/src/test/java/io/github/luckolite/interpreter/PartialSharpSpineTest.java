// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original partial sharp spines near a disconnected slur stroke. */
public final class PartialSharpSpineTest {
    private final byte[] gray=new byte[100*100];
    public PartialSharpSpineTest(){Arrays.fill(gray,(byte)255);}
    private void rect(int l,int r,int t,int b,int value){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)gray[y*100+x]=(byte)value;}
    private Object make(String name,Object...args)throws Exception{var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);}
    private boolean recover(boolean slur,boolean natural,boolean clipped,boolean spines)throws Exception {
        rect(30,45,40,43,0);rect(30,45,56,59,0);
        if(spines){rect(33,34,clipped?0:30,natural?59:69,210);rect(41,42,natural?40:30,69,210);}
        if(slur)rect(29,46,18,21,0);
        Object upper=make("AccidentalCandidate",make("Component",72,30,45,34,46,37.5f,40f),(byte)3);
        Object lower=make("AccidentalCandidate",make("Component",64,30,45,56,59,37.5f,57.5f),(byte)3);
        Object head=make("Component",100,55,71,44,56,63f,50f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("rawSharpFromSeed",byte[].class,int.class,int.class,List.class,head.getClass(),float.class);m.setAccessible(true);
        return (boolean)m.invoke(null,gray,100,100,List.of(upper,lower),head,16f);
    }
    @Test public void partialSpineStillSeedsSharp()throws Exception{assertTrue(recover(false,false,false,true));}
    @Test public void disconnectedSlurDoesNotClipSharp()throws Exception{assertTrue(recover(true,false,false,true));}
    @Test public void naturalEndpointsAreNotSharp()throws Exception{assertFalse(recover(true,true,false,true));}
    @Test public void clippedSpineIsNotCompletedByCrop()throws Exception{assertFalse(recover(false,false,true,true));}
    @Test public void crossbarsAloneRemainInsufficient()throws Exception{assertFalse(recover(true,false,false,false));}
}
