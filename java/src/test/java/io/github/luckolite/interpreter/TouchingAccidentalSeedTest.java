// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original sharp and natural geometry with a seed containing a staff-rule fringe. */
public class TouchingAccidentalSeedTest {
    final byte[] gray=new byte[10000];
    public TouchingAccidentalSeedTest(){Arrays.fill(gray,(byte)255);}
    void rect(int l,int r,int t,int b){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)gray[y*100+x]=0;}
    Object make(String name,Object...args)throws Exception{var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);}
    boolean recover(int fringe,boolean natural,boolean clipped,float center)throws Exception{
        rect(30,45,40,43);rect(30,45,56,59);
        rect(33,34,clipped?0:30,natural?59:69);rect(41,42,natural?40:30,69);
        Object seed=make("AccidentalCandidate",make("Component",150,30,55+fringe,30,69,center,50f),(byte)0);
        Object head=make("Component",150,55,75,44,56,65f,50f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("rawSharpFromSeed",byte[].class,int.class,int.class,List.class,head.getClass(),float.class);method.setAccessible(true);
        return (boolean)method.invoke(null,gray,100,100,List.of(seed),head,16f);
    }
    @Test public void staffFringeMayTouchHeadBoundingEdge()throws Exception{assertTrue(recover(0,false,false,42f));}
    @Test public void tinyFringeOverlapStillNeedsCompleteRawSharp()throws Exception{assertTrue(recover(2,false,false,42f));}
    @Test public void largerOverlapIsRejected()throws Exception{assertFalse(recover(4,false,false,42f));}
    @Test public void naturalIsNotOverridden()throws Exception{assertFalse(recover(0,true,false,42f));}
    @Test public void clippedSpinesRemainRejected()throws Exception{assertFalse(recover(0,false,true,42f));}
    @Test public void glyphCenterCannotBeInsideHead()throws Exception{assertFalse(recover(0,false,false,55f));}
}
