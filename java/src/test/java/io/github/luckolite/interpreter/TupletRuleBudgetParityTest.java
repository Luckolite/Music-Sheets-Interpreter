// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.Method;
import java.util.*;
import java.security.MessageDigest;

/** Original binary sloped rules, crossings, thickness and coverage boundary masks. */
public class TupletRuleBudgetParityTest {
    record Fixture(byte[] pixels,int width,int height,float gap) {}
    static Method cleanup;
    static Fixture fixture(int n) {
        int w=new int[]{31,79,120,241}[n%4],h=120;
        float gap=new float[]{4,8,14.5f,22}[n%4];
        byte[] pixels=new byte[w*h];Arrays.fill(pixels,(byte)255);
        int mode=n%8,angle=(n/4)%9-4;
        float slope=angle*.025f;
        if(mode!=0)for(int x=0;x<w;x++) {
            if(mode==2&&x>=Math.floor(w*.89)||mode==3&&x>=Math.ceil(w*.90)||mode==4&&x%7==0)continue;
            int y=Math.round(55+slope*(x-w*.5f));
            int thickness=mode==5?Math.max(2,Math.round(gap*.3f))+1:1;
            for(int dy=0;dy<thickness;dy++)pixels[(y+dy)*w+x]=0;
        }
        if(mode==6)for(int x=w/3;x<w/3+3;x++)for(int y=25;y<85;y++)pixels[y*w+x]=0;
        if(n>=32){Random r=new Random(460907+n);for(int i=0;i<(n%4)*100;i++)pixels[r.nextInt(pixels.length)]=0;}
        return new Fixture(pixels,w,h,gap);
    }
    static byte[] clean(Fixture f)throws Exception {
        byte[] copy=f.pixels.clone();cleanup.invoke(null,copy,f.width,f.height,f.gap);return copy;
    }
    @org.junit.Test public void coverageEdgesCrossingsAndSlopedCleanupBytesRemainExact() throws Exception {
        cleanup=TupletNumeralInk.class.getDeclaredMethod("removeSlopedRules",byte[].class,int.class,int.class,float.class);
        cleanup.setAccessible(true);var hash=MessageDigest.getInstance("SHA-256");int removed=0;
        for(int n=0;n<96;n++) {
            Fixture f=fixture(n);byte[] output=clean(f);hash.update(output);
            for(int i=0;i<output.length;i++)if(output[i]!=f.pixels[i])removed++;
        }
        org.junit.Assert.assertEquals(6886,removed);
        // Golden binary masks captured before impossible candidates were skipped.
        org.junit.Assert.assertEquals("61e5ad15849e28f99e9eebe895351fa15e087efc682936a71468cd5053b495a6",
                HexFormat.of().formatHex(hash.digest()));
    }
}
