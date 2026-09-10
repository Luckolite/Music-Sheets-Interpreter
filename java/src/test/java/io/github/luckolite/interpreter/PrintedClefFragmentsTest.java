// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrintedClefFragmentsTest {
    private Object component(int area,int x1,int x2,int y1,int y2,float x,float y) throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        return ctor.newInstance(area,x1,x2,y1,y2,x,y);
    }
    private Object join(Object body,Object tail) throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var staff=ctor.newInstance(1922f,1977f,13.75f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("joinTrebleCurl",body.getClass(),List.class,type);
        method.setAccessible(true);return method.invoke(null,body,List.of(body,tail),staff);
    }
    @Test public void reconnectsPhoneClefButNotNeighboringKeyOrDistantInk() throws Exception {
        var body=component(800,202,238,1897,1975,219.67f,1938.76f);
        assertNotEquals(body,join(body,component(81,219,230,1965,1999,227.1f,1982.99f)));
        assertSame(body,join(body,component(81,249,260,1965,1999,253,1982)));
        assertSame(body,join(body,component(81,219,230,2000,2024,227,2012)));
    }
    @Test public void neverBuildsClefsOutOfNarrowBracketsOrSmallAccidentals() throws Exception {
        var tail=component(81,219,230,1965,1999,227.1f,1982.99f);
        var bracket=component(800,219,222,1897,1975,220,1939);
        assertSame(bracket,join(bracket,tail));
        var accidental=component(300,210,230,1920,1975,220,1947);
        assertSame(accidental,join(accidental,tail));
    }
}
