// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Method;
import java.util.*;
import java.security.MessageDigest;

/** Generated bright pockets, printed stems and stipple; no external score raster. */
public class BarlineThresholdMembershipTest {
    record Fixture(byte[] gray,int width,int height,int column,int top,int bottom,float gap,int outline) {}
    static Method method;
    static boolean detect(Fixture f)throws Exception {
        return (boolean)method.invoke(null,f.gray,f.width,f.height,f.column,f.top,f.bottom,f.gap,f.outline);
    }
    static Fixture fixture(int n) {
        int w=220,h=180,col=n%7==0?2:n%7==1?218:94;
        byte[] gray=new byte[w*h];int paper=n%5==0?196:238;Arrays.fill(gray,(byte)paper);
        int mode=n%6,stem=mode==3?102:93;
        if(mode!=0)for(int y=120;y<=140;y++)for(int x=65;x<=95;x++) {
            if((x-80)*(x-80)/169.0+(y-130)*(y-130)/100.0<=1)gray[y*w+x]=45;
            if((x-80)*(x-80)/121.0+(y-130)*(y-130)/49.0<=1)gray[y*w+x]=(byte)(mode==4?238:n%3==0?145:178);
        }
        for(int y=60;y<=140;y++)for(int x=stem;x<=stem+2;x++)gray[y*w+x]=45;
        if(mode==5)for(int y=60;y<=140;y+=20)for(int x=10;x<210;x++)gray[y*w+x]=45;
        if(n>=64) {
            Random r=new Random(88719+n);
            for(int i=0;i<350;i++)gray[r.nextInt(gray.length)]=(byte)r.nextInt(256);
        }
        return new Fixture(gray,w,h,col,60,140,n%8==0?7:20,n%2==0?120:150);
    }
    @Test public void clippedGrayNoisyAndPrintedHeadComponentsStayExact() throws Exception {
        Class<?> c=Class.forName("io.github.luckolite.interpreter.ClosedHeadBarlineGuard");
        method=c.getDeclaredMethod("attachedAt",byte[].class,int.class,int.class,int.class,int.class,int.class,float.class,int.class);
        method.setAccessible(true);
        var hash=MessageDigest.getInstance("SHA-256");int positives=0;
        for(int n=0;n<128;n++) {
            Fixture f=fixture(n);byte[] original=f.gray.clone();
            boolean found=detect(f);hash.update((byte)(found?1:0));if(found)positives++;
            assertArrayEquals("Flood fill must not modify the source page",original,f.gray);
        }
        // Captured from the previous decoder on original generated geometry.
        assertEquals(33,positives);
        assertEquals("caf0b8a48b57a2591a05b30a95aac10cb45dc06fd5947a545e2286ce61865915",HexFormat.of().formatHex(hash.digest()));
    }
}
