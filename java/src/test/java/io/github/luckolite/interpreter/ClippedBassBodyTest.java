// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic two-dot/tail pattern, either complete or cut off by the raster edge. */
public class ClippedBassBodyTest {
    private static boolean check(int x)throws Exception {
        int w=180,h=160;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int yy=42;yy<68;yy++)for(int xx=x;xx<x+18;xx++)gray[yy*w+xx]=0;
        for(int y:new int[]{46,62})for(int yy=y;yy<y+5;yy++)for(int xx=x+26;xx<x+31;xx++)gray[yy*w+xx]=0;
        for(int yy=77;yy<91;yy++)for(int xx=x+2;xx<x+7;xx++)gray[yy*w+xx]=0;
        Class<?> c=Class.forName(OmrScoreInterpreter.class.getName()+"$Component"),s=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");
        var cc=c.getDeclaredConstructors()[0];cc.setAccessible(true);var sc=s.getDeclaredConstructors()[0];sc.setAccessible(true);
        Object body=cc.newInstance(150,x,x+17,42,67,x+9f,55f),staff=sc.newInstance(40f,104f,16f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("rawBassClef",c,byte[].class,int.class,int.class,s);m.setAccessible(true);
        return (boolean)m.invoke(null,body,gray,w,h,staff);
    }
    @Test public void croppedBorderPatternCannotEstablishBassClef()throws Exception {assertFalse(check(0));}
    @Test public void completeInteriorBodyRetainsBassClef()throws Exception {assertTrue(check(40));}
}
