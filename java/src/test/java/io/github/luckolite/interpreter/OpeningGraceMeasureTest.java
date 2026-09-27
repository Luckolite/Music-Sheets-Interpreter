// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original measure rectangles; no private score coordinates. */
public class OpeningGraceMeasureTest {
    int assign(float x,List<MeasureRegion> measures)throws Exception {
        var cls=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var c=cls.getDeclaredConstructors()[0];c.setAccessible(true);Object staff=c.newInstance(100f,164f,16f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("openingGraceMeasure",List.class,float.class,cls,int.class,int.class);
        m.setAccessible(true);return (int)m.invoke(null,measures,x,staff,500,400);
    }
    static List<MeasureRegion> bars(){return List.of(new MeasureRegion(.2f,.5f,.2f,.5f),new MeasureRegion(.6f,.9f,.2f,.5f));}
    @Test public void compactPrefixBelongsToFirstBar()throws Exception{assertEquals(0,assign(72,bars()));}
    @Test public void distantHeaderInkIsNotAdmitted()throws Exception{assertEquals(-1,assign(40,bars()));}
    @Test public void internalBarGapIsNotBridged()throws Exception{assertEquals(-1,assign(282,bars()));}
    @Test public void anotherSystemCannotOwnThePrefix()throws Exception{assertEquals(-1,assign(72,List.of(new MeasureRegion(.2f,.9f,.6f,.9f))));}
}
