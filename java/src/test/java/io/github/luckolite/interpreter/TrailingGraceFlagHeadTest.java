// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original reduced-head geometry, independent of any score image. */
public class TrailingGraceFlagHeadTest {
    private Object component(int area,int minX,int maxX,int minY,int maxY)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var constructor=type.getDeclaredConstructor(int.class,int.class,int.class,int.class,int.class,float.class,float.class);
        constructor.setAccessible(true);
        return constructor.newInstance(area,minX,maxX,minY,maxY,
                (minX+maxX)*.5f,(minY+maxY)*.5f);
    }

    private List<?> rejected(List<Object> heads)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var constructor=type.getDeclaredConstructor(float.class,float.class,float.class);
        constructor.setAccessible(true);
        Object staff=constructor.newInstance(292f,366f,18.5f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("trailingGraceFlagHeads",List.class,List.class);
        method.setAccessible(true);
        return (List<?>)method.invoke(null,heads,List.of(staff));
    }

    @Test public void alignedSmallTailUnderUpperGraceHeadIsNotAnotherPitch()throws Exception {
        Object upper=component(120,405,418,258,269);
        Object tail=component(99,405,416,289,304);
        assertEquals(List.of(tail),rejected(List.of(upper,tail)));
    }

    @Test public void equalHeadsOfAChordStayPitched()throws Exception {
        Object upper=component(105,405,416,258,269);
        Object lower=component(99,405,416,289,300);
        assertTrue(rejected(List.of(upper,lower)).isEmpty());
    }

    @Test public void unalignedGraceNotesStayPitched()throws Exception {
        Object upper=component(120,405,418,258,269);
        Object lower=component(60,416,426,293,303);
        assertTrue(rejected(List.of(upper,lower)).isEmpty());
    }
}
