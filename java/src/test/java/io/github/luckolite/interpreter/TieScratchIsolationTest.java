// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generated returning curves and straight-rule negatives. */
public class TieScratchIsolationTest {
    private boolean arc(boolean present, boolean below, boolean strict) throws Exception {
        int width=300,height=210,left=45,right=235;
        float center=110,gap=16;
        byte[] gray=new byte[width*height],labels=new byte[gray.length];
        Arrays.fill(gray,(byte)255);
        int side=below?1:-1;
        for(int distance:new int[]{8,24,40})for(int dy=-2;dy<=2;dy++)for(int x=0;x<width;x++) {
            int y=Math.round(center)+side*distance+dy;
            if(Math.abs(dy)<=1)gray[y*width+x]=0;
            labels[y*width+x]=4;
        }
        if(present)for(int x=left;x<=right;x++) {
            float t=(x-left)/(float)(right-left);
            int y=Math.round(center+side*(15+16*4*t*(1-t)));
            gray[y*width+x]=0;
            if(labels[y*width+x]!=4)labels[y*width+x]=5;
        }
        Method method=OmrScoreInterpreter.class.getDeclaredMethod("hasContinuousTieArc",
                byte[].class,byte[].class,int.class,int.class,int.class,int.class,float.class,
                float.class,Class.forName(OmrScoreInterpreter.class.getName()+"$Component"),int.class,int.class,
                boolean.class,boolean.class);
        method.setAccessible(true);
        return (boolean)method.invoke(null,labels,gray,width,height,left,right,center,gap,
                null,205,side,strict,false);
    }
    @Test public void candidateSearchDoesNotCarrySupportIntoStraightRules() throws Exception {
        for(int i=0;i<20;i++)for(boolean below:new boolean[]{false,true}) {
            assertTrue(arc(true,below,false));
            assertFalse(arc(false,below,false));
            assertTrue(arc(true,below,true));
            assertFalse(arc(false,below,true));
        }
    }
    @Test public void separateCallsKeepScratchIsolatedAcrossThreads() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(4);
        try {
            var jobs=new ArrayList<Callable<Void>>();
            for(int i=0;i<80;i++) {
                final int n=i;
                jobs.add(()->{boolean present=(n&1)==0;
                    assertEquals(present,arc(present,(n&2)!=0,(n&4)!=0));return null;});
            }
            for(Future<Void> result:pool.invokeAll(jobs))result.get();
        } finally {pool.shutdownNow();}
    }
}
