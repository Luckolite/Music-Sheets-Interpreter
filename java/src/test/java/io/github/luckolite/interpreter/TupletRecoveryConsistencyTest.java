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
    private static byte[] staffScene(boolean bracket) {
        byte[] gray=scene(false);
        for(int y=140;y<=180;y+=10)for(int x=60;x<=190;x++)gray[y*400+x]=0;
        if(bracket) {
            for(int y=143;y<=149;y++)for(int x:new int[]{100,150})gray[y*400+x]=0;
            for(int x=100;x<=150;x++)if(x<=115||x>=134)gray[143*400+x]=0;
        }
        return gray;
    }
    @Test public void erasedStaffRulesCannotHideTheRestContext() throws Exception {
        assertNull(detect(staffScene(false)));
    }
    @Test public void explicitBracketHooksProvideStaffContextProof() throws Exception {
        Class<?> glyphClass=Arrays.stream(TripletRhythmDetector.class.getDeclaredClasses())
                .filter(type->type.getSimpleName().equals("Glyph")).findFirst().orElseThrow();
        var constructor=glyphClass.getDeclaredConstructor(int.class,int.class,int.class,int.class);
        constructor.setAccessible(true);
        Object glyph=constructor.newInstance(119,145,130,166);
        var method=TripletRhythmDetector.class.getDeclaredMethod("validNumeralContext",
                byte[].class,int.class,int.class,glyphClass,float.class,float.class,float.class,int.class);
        method.setAccessible(true);
        assertFalse((boolean)method.invoke(null,staffScene(false),400,240,glyph,100f,150f,12f,3));
        assertTrue((boolean)method.invoke(null,staffScene(true),400,240,glyph,100f,150f,12f,3));
    }
}
