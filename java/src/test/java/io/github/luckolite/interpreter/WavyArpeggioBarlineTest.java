// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic ink: a thick wavy roll can have a continuous vertical core. */
public class WavyArpeggioBarlineTest {
    private boolean bar(int scale, boolean wave, int paper, int ink) throws Exception {
        int w=260*scale,h=200*scale;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)paper);
        int[] rows={70*scale,84*scale,98*scale,112*scale,126*scale};
        for(int y:rows)for(int x=20*scale;x<240*scale;x++)gray[y*w+x]=(byte)ink;
        for(int y=rows[0]-3*scale;y<=rows[4]+3*scale;y++) {
            double center=120*scale+(wave?2.2*scale*Math.sin((y-rows[0])*Math.PI/(7*scale)):0);
            for(int x=(int)Math.floor(center-2*scale);x<=Math.ceil(center+2*scale);x++)gray[y*w+x]=(byte)ink;
        }
        var method=OmrMeasurePostProcessor.class.getDeclaredMethod("rawBarlineSpansStaff",
                byte[].class,int.class,int.class,int.class,int[].class,float.class,float.class,float.class);
        method.setAccessible(true);
        return (boolean)method.invoke(null,gray,w,h,120*scale,rows,14f*scale,0f,0f);
    }
    @Test public void wavyRollIsNotAMeasureSeparator()throws Exception {assertFalse(bar(1,true,255,0));}
    @Test public void enlargedWavyRollIsNotAMeasureSeparator()throws Exception {assertFalse(bar(2,true,255,0));}
    @Test public void grayPaperWavyRollIsNotAMeasureSeparator()throws Exception {assertFalse(bar(1,true,210,100));}
    @Test public void thickStraightBarIsRetained()throws Exception {assertTrue(bar(1,false,255,0));}
    @Test public void faintStraightBarIsRetained()throws Exception {assertTrue(bar(2,false,210,175));}
}
