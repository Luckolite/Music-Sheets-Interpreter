// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original component geometry for rasterized, staff-separated clef fragments. */
public class ClefCurlRasterBoundaryTest {
    private Object component(int area,int x1,int x2,int y1,int y2,float x,float y) throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        return ctor.newInstance(area,x1,x2,y1,y2,x,y);
    }
    private Object body() throws Exception {
        return component(1400,40,82,70,161,61,122);
    }
    private Object join(Object body,Object tail) throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var staff=ctor.newInstance(100f,160f,15f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("joinTrebleCurl",body.getClass(),List.class,type);
        method.setAccessible(true);return method.invoke(null,body,List.of(body,tail),staff);
    }
    @Test public void roundsTheJoinDistanceToTheNearestRasterRow() throws Exception {
        var b=body();var t=component(260,44,64,168,188,54,178);
        assertNotEquals(b,join(b,t));
    }
    @Test public void doesNotReachAnAdditionalRasterRow() throws Exception {
        var b=body();assertSame(b,join(b,component(260,44,64,169,189,54,179)));
    }
    @Test public void roundedDistanceDoesNotJoinAdjacentKeyGlyph() throws Exception {
        var b=body();assertSame(b,join(b,component(260,87,107,168,188,97,178)));
    }
    @Test public void roundedDistanceStillNeedsALargeTrebleBody() throws Exception {
        var b=component(180,48,61,115,161,55,138);
        assertSame(b,join(b,component(90,49,59,168,183,54,175)));
    }
}
