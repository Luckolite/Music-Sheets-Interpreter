// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

public class TrailingHeadExtentTest {
    private boolean clipped(int headLeft,int headWidth)throws Exception {
        int w=400,h=200;byte[] labels=new byte[w*h];
        for(int y=108;y<116;y++)for(int x=headLeft;x<headLeft+headWidth;x++)labels[y*w+x]=2;
        var method=OmrMeasurePostProcessor.class.getDeclaredMethod("clippedClosingHead",byte[].class,
                int.class,int.class,int.class,int.class,int[].class,float.class,float.class);
        method.setAccessible(true);
        return (boolean)method.invoke(null,labels,w,h,200,240,new int[]{80,96,112,128,144},16f,0f);
    }
    @Test public void aPartlyClippedHeadRetainsThePrintedStaffEnding()throws Exception {
        assertTrue(clipped(190,14));
    }
    @Test public void aCompleteHeadWithRoomAfterItDoesNotExpandTheEnding()throws Exception {
        assertFalse(clipped(178,14));
    }
    @Test public void inkBeyondThePrintedStaffIsNotAReasonToExtend()throws Exception {
        assertFalse(clipped(241,14));
    }
}
