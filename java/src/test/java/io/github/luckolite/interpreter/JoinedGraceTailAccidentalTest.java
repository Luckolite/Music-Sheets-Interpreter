// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original reduced note and tail shapes; no private score or model output. */
public class JoinedGraceTailAccidentalTest {
    private Object component(int area,int left,int right,int top,int bottom)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var constructor=type.getDeclaredConstructor(int.class,int.class,int.class,int.class,int.class,float.class,float.class);
        constructor.setAccessible(true);
        return constructor.newInstance(area,left,right,top,bottom,
                (left+right)*.5f,(top+bottom)*.5f);
    }

    private boolean joined(Object glyph,List<Object> heads)throws Exception {
        var interpreter=OmrScoreInterpreter.class;
        var type=Class.forName(interpreter.getName()+"$AccidentalCandidate");
        var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
        Object candidate=constructor.newInstance(glyph,(byte)0);
        var staffType=Class.forName(interpreter.getName()+"$Staff");
        var staffConstructor=staffType.getDeclaredConstructor(float.class,float.class,float.class);
        staffConstructor.setAccessible(true);
        Object staff=staffConstructor.newInstance(292f,366f,18.5f);
        var method=interpreter.getDeclaredMethod("joinedGraceTailAccidental",type,List.class,List.class);
        method.setAccessible(true);
        return (boolean)method.invoke(null,candidate,heads,List.of(staff));
    }

    @Test public void flagJoinedAroundReducedHeadIsNotASharp()throws Exception {
        Object glyph=component(285,654,671,264,305);
        Object grace=component(120,660,672,258,269);
        assertTrue(joined(glyph,List.of(grace)));
    }

    @Test public void separatePrintedAccidentalIsPreserved()throws Exception {
        Object glyph=component(285,654,671,264,305);
        Object grace=component(120,694,706,258,269);
        assertFalse(joined(glyph,List.of(grace)));
    }

    @Test public void fullSizedHeadDoesNotEraseAnAccidental()throws Exception {
        Object glyph=component(285,654,671,264,305);
        Object head=component(320,660,682,258,276);
        assertFalse(joined(glyph,List.of(head)));
    }
}
