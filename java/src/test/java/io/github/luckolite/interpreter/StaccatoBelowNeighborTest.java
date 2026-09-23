// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** An original rising beamed run with an articulation under the next head. */
public class StaccatoBelowNeighborTest {
    private static final float GAP=16f;
    private Object component(int x,int y)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        return ctor.newInstance(60,x-5,x+5,y-5,y+5,(float)x,(float)y);
    }
    private boolean staccato(Object dot,Object current,List<?> heads)throws Exception {
        var type=current.getClass();
        var method=OmrScoreInterpreter.class.getDeclaredMethod("staccatoBelowNextHead",
                type,type,List.class,float.class);method.setAccessible(true);
        return (boolean)method.invoke(null,dot,current,heads,GAP);
    }
    @Test public void dotDirectlyBelowNextRisingHeadIsItsStaccato()throws Exception {
        var current=component(100,100);var next=component(125,94);var dot=component(125,109);
        assertTrue(staccato(dot,current,List.of(current,next)));
        assertFalse(staccato(dot,current,List.of(current)));
    }
    @Test public void ordinaryDurationDotBesideHeadRemains()throws Exception {
        var current=component(100,100);var next=component(140,100);var dot=component(119,100);
        assertFalse(staccato(dot,current,List.of(current,next)));
    }
    @Test public void closeDotAtSameHeightAsNextHeadRemains()throws Exception {
        var current=component(100,100);var next=component(125,100);var dot=component(125,100);
        assertFalse(staccato(dot,current,List.of(current,next)));
    }
}
