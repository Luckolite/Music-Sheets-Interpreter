// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original zigzag and hook raster; the terminal stroke has no curled foot. */
public class StraightTailQuarterRestTest {
    static final int W=600,H=280;
    static class Page {
        final byte[] gray=new byte[W*H];
        final int gap;
        Page(int gap,boolean hook,boolean zigzag) {
            this.gap=gap;Arrays.fill(gray,(byte)240);
            for(int y=100;y<=100+4*gap;y+=gap)for(int x=20;x<W-20;x++)gray[y*W+x]=0;
            int[][] rows={{1,2},{2,3},{3,4},{4,5},{5,6},{6,7},{7,8},
                    {7,10},{7,11},{6,11},{6,11},{5,11},{5,11},
                    {4,10},{4,9},{5,9},{6,9},{7,10},{8,11},
                    {6,12},{4,13},{3,13},{2,13},{2,5},{2,5},
                    {2,5},{2,5},{3,5},{3,5},{3,5},{3,5}};
            for(int row=0;row<rows.length;row++) {
                int l=rows[row][0],r=rows[row][1];
                if(!hook&&row>=19){l=7;r=10;}
                if(!zigzag&&row<19){l=5;r=8;}
                int y1=100+Math.round(gap*.6f+row*gap*3f/31);
                int y2=100+Math.round(gap*.6f+(row+1)*gap*3f/31);
                for(int y=y1;y<=y2;y++)for(int x=150+Math.round(l*gap/16f);x<=150+Math.round(r*gap/16f);x++)gray[y*W+x]=0;
            }
        }
        List<ScoreRestEvent> detect(){return SixteenthRestDetector.detect(gray,W,H,List.of(new MeasureRegion(.04f,.95f,.2f,.9f)),List.of(new SixteenthRestDetector.Staff(100,100+4*gap,gap,0,1)),List.of());}
    }
    @Test public void straightTailIsOneBeat(){var p=new Page(16,true,true);var rests=p.detect();assertEquals(1,rests.size());assertEquals(1,rests.get(0).durationBeats(),.0001);}
    @Test public void largerEngravingIsOneBeat(){var rests=new Page(22,true,true).detect();assertEquals(1,rests.size());assertEquals(1,rests.get(0).durationBeats(),.0001);}
    @Test public void upperZigzagWithoutLowerHookIsNotARest(){assertTrue(new Page(16,false,true).detect().isEmpty());}
    @Test public void hookWithoutUpperZigzagIsNotARest(){assertTrue(new Page(16,true,false).detect().isEmpty());}
    @Test public void recognitionPreservesTheRaster(){var p=new Page(16,true,true);var before=p.gray.clone();p.detect();assertArrayEquals(before,p.gray);}
}
