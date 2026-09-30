// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original layered numeral geometry; no scanned score pixels. */
public class TupletRecoveryConsistencyTest {
    private static byte[] scene(boolean unstable) {
        String[] rows = {"...######...","..#########.",".........###","........####",
                "........####",".......####.","......####..",".....####...",
                "....##......","...###......","...###......","....##......",
                "....###.....","...####.....","...####.....","...####.....",
                "...####.....","..####......",".#####......","#####.......",
                "####........",".##........."};
        byte[] gray = new byte[400*240]; Arrays.fill(gray,(byte)255);
        for(int y=0;y<rows.length;y++)for(int x=0;x<12;x++) {
            int at=(145+y)*400+119+x;
            if(rows[y].charAt(x)=='#')gray[at]=125;
            else if(unstable&&y>=2&&y<rows.length-2&&x<2)gray[at]=(byte)150;
        }
        return gray;
    }
    private static Object detect(byte[] gray) throws Exception {
        var method=TripletRhythmDetector.class.getDeclaredMethod("findPrintedThree",
                byte[].class,int.class,int.class,float.class,float.class,float.class,
                float.class,float.class,boolean.class,float.class,float.class);
        method.setAccessible(true);
        return method.invoke(null,gray,400,240,100f,150f,96f,96f,12f,true,Float.NaN,Float.NaN);
    }
    @Test public void oneThresholdCannotInventAnOpenNumeral() throws Exception {
        assertNull(detect(scene(true)));
    }
    @Test public void stableRecoveredNumeralStillWorks() throws Exception {
        assertNotNull(detect(scene(false)));
    }
}
