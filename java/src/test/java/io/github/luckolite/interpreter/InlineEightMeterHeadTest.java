// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic meter numerals; no score image or model output is included. */
public class InlineEightMeterHeadTest {
    private static final int WIDTH=160,HEIGHT=150;
    private final byte[] gray=new byte[WIDTH*HEIGHT];

    private void rectangle(int left,int top,int right,int bottom,int value) {
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++)gray[y*WIDTH+x]=(byte)value;
    }

    private Object component(int top,int bottom)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var constructor=type.getDeclaredConstructor(int.class,int.class,int.class,int.class,int.class,float.class,float.class);
        constructor.setAccessible(true);
        return constructor.newInstance(90,77,87,top,bottom,82f,(top+bottom)/2f);
    }

    private boolean fragment(Object head)throws Exception {
        var staffType=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var constructor=staffType.getDeclaredConstructor(float.class,float.class,float.class);
        constructor.setAccessible(true);
        Object staff=constructor.newInstance(40f,112f,18f);
        Method method=OmrScoreInterpreter.class.getDeclaredMethod("inlineEightMeterFragment",
                byte[].class,int.class,int.class,head.getClass(),List.class);
        method.setAccessible(true);
        return (boolean)method.invoke(null,gray,WIDTH,HEIGHT,head,List.of(staff));
    }

    private void notation(boolean barline) {
        Arrays.fill(gray,(byte)250);
        for(int y=40;y<=112;y+=18)rectangle(0,y,WIDTH-1,y,40);
        if(barline)rectangle(55,40,55,112,30);
        // Two enclosed counters of a printed 8 beneath a separate numerator.
        rectangle(77,84,87,94,30);rectangle(80,87,84,90,250);
        rectangle(77,98,87,108,30);rectangle(80,101,84,104,250);
        rectangle(80,94,84,98,30);
    }

    @Test public void stackedEightBesideBarlineIsNotTwoNoteheads()throws Exception {
        notation(true);
        assertTrue(fragment(component(84,94)));
        assertTrue(fragment(component(84,108)));
        assertTrue(fragment(component(98,108)));
    }

    @Test public void countersWithoutBarlineAreNotMeter()throws Exception {
        notation(false);
        assertFalse(fragment(component(84,94)));
    }

    @Test public void oneCounterIsNotMeter()throws Exception {
        notation(true);rectangle(77,98,87,108,250);
        assertFalse(fragment(component(84,94)));
    }

    @Test public void externalStemProtectsAHead()throws Exception {
        notation(true);rectangle(87,20,87,84,20);
        assertFalse(fragment(component(84,94)));
    }
}
