// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original classified silhouettes, testing cue-clef ownership without any score imagery. */
public class CueClefFragmentsTest {
    private final Class<?> hc,sc;
    private final Constructor<?> head,staff;
    public CueClefFragmentsTest()throws Exception{
        hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");sc=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        head=hc.getDeclaredConstructors()[0];head.setAccessible(true);
        staff=sc.getDeclaredConstructor(float.class,float.class,float.class);staff.setAccessible(true);
    }
    private Object h(int area,int l,int r,int t,int b)throws Exception{return head.newInstance(area,l,r,t,b,(l+r)*.5f,(t+b)*.5f);}
    private Object treble(Object body,List<?> parts)throws Exception{
        Method m=OmrScoreInterpreter.class.getDeclaredMethod("joinCueTreble",hc,List.class,sc);m.setAccessible(true);
        return m.invoke(null,body,parts,staff.newInstance(100f,180f,20f));
    }
    @Test public void separatedSmallTrebleCurlRetainsItsGLineAnchor()throws Exception{
        Object body=h(370,110,133,103,167),tail=h(800,100,142,141,201),top=h(70,112,117,96,121);
        assertNotNull(treble(body,List.of(body,tail,top)));
    }
    @Test public void bodyWithoutLowerCurlIsNotAClef()throws Exception{Object body=h(370,110,133,103,167);assertNull(treble(body,List.of(body)));}
    @Test public void nearbyAccidentalCannotSupplyTheCurl()throws Exception{
        Object body=h(370,110,133,103,167),other=h(800,150,185,141,201);assertNull(treble(body,List.of(body,other)));
    }
    @Test public void detachedNotationBelowCannotSupplyTheCurl()throws Exception{
        Object body=h(370,110,133,103,167),other=h(800,100,142,177,220);assertNull(treble(body,List.of(body,other)));
    }
    @Test public void cueBassOnlyJoinsItsBoundedLowerLeftTail()throws Exception{
        Object body=h(220,110,125,103,139),tail=h(15,94,100,145,149),dot=h(20,130,134,114,119);
        Method m=OmrScoreInterpreter.class.getDeclaredMethod("joinCueBassTail",hc,List.class,sc);m.setAccessible(true);
        Object result=m.invoke(null,body,List.of(body,tail,dot),staff.newInstance(108f,160f,13f));
        Method minX=hc.getDeclaredMethod("minX"),maxX=hc.getDeclaredMethod("maxX");minX.setAccessible(true);maxX.setAccessible(true);
        assertEquals(94,minX.invoke(result));assertEquals(125,maxX.invoke(result));
    }
}
