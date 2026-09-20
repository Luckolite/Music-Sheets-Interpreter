// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Golden generated-geometry outputs captured before deferring note-ownership scans. */
public class BarlineCandidateOptimizationTest {
    @Test public void sparseDenseSlopedAndMaskOnlyBoundariesRemainExact() throws Exception {
        var method=OmrMeasurePostProcessor.class.getDeclaredMethod("findBoundaries",byte[].class,
                byte[].class,int.class,int.class,int[].class,float.class,int.class,int.class,float.class);
        method.setAccessible(true);
        var digest=MessageDigest.getInstance("SHA-256");
        Random random=new Random(203709);
        for(int run=0;run<96;run++) {
            int w=180,h=120;float gap=10,slope=(run%3-1)*.03f;
            int[] rows={35,45,55,65,75};
            byte[] labels=new byte[w*h],gray=new byte[w*h];Arrays.fill(gray,(byte)255);
            for(int x=8;x<w-8;x++)for(int row:rows) {
                int y=Math.round(row+slope*(x-w/2f));labels[y*w+x]=4;gray[y*w+x]=0;
            }
            // Alternate real rules, head-owned stems, pale semantic rules and isolated noise.
            for(int x:new int[]{35,88,142}) {
                int shade=run%4==0?215:0;
                for(int y=30;y<=80;y++){labels[y*w+x]=1;gray[y*w+x]=(byte)shade;}
                if((run+x)%3==0)for(int y=22;y<=30;y++)for(int xx=x-9;xx<=x;xx++) {
                    labels[y*w+xx]=2;gray[y*w+xx]=0;
                }
            }
            for(int i=0;i<(run%4)*90;i++) {
                int x=2+random.nextInt(w-4),y=2+random.nextInt(h-4);
                labels[y*w+x]=(byte)(1+random.nextInt(5));gray[y*w+x]=(byte)random.nextInt(220);
            }
            byte[] oldLabels=labels.clone(),oldGray=gray.clone();
            Object result=method.invoke(null,labels,run%5==0?null:gray,w,h,rows,gap,8,w-9,slope);
            digest.update((run+":"+result+"\n").getBytes(StandardCharsets.UTF_8));
            assertArrayEquals(oldLabels,labels);assertArrayEquals(oldGray,gray);
        }
        StringBuilder hash=new StringBuilder();
        for(byte b:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",b&255));
        assertEquals("27a358134a9bc955b87a0ce1505c616ee330f73c51d25a4976ae7fac085029fc",hash.toString());
    }
}
