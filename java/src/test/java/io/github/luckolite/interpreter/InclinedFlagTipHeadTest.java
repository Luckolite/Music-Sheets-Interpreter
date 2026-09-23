// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original diagonal flag stroke and independently printed oval. */
public class InclinedFlagTipHeadTest {
    private static final int WIDTH=220,HEIGHT=190;
    private final byte[] gray=new byte[WIDTH*HEIGHT];

    private Object head()throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var constructor=type.getDeclaredConstructor(int.class,int.class,int.class,int.class,int.class,float.class,float.class);
        constructor.setAccessible(true);
        return constructor.newInstance(169,91,109,93,107,100f,100f);
    }

    private boolean rejected(Object head)throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var constructor=type.getDeclaredConstructor(float.class,float.class,float.class);
        constructor.setAccessible(true);
        Object staff=constructor.newInstance(71f,145f,18.5f);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("inclinedFlagTipHeads",
                byte[].class,int.class,int.class,List.class,List.class);
        method.setAccessible(true);
        return !((List<?>)method.invoke(null,gray,WIDTH,HEIGHT,List.of(head),List.of(staff))).isEmpty();
    }

    @Test public void longDiagonalStrokeIsNotANotehead()throws Exception {
        Arrays.fill(gray,(byte)255);
        for(int d=0;d<=42;d++) {
            int x=100-d,y=100+Math.round(.45f*d);
            for(int offset=-2;offset<=2;offset++)gray[(y+offset)*WIDTH+x]=0;
        }
        assertTrue(rejected(head()));
    }

    @Test public void ovalWithoutDiagonalStrokeRemainsANotehead()throws Exception {
        Arrays.fill(gray,(byte)255);
        for(int y=92;y<=108;y++)for(int x=89;x<=111;x++)
            if(Math.pow((x-100)/11d,2)+Math.pow((y-100)/8d,2)<=1)gray[y*WIDTH+x]=0;
        assertFalse(rejected(head()));
    }
}
