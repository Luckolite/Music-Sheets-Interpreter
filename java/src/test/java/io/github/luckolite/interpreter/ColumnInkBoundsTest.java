// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.util.Random;
import static org.junit.Assert.*;

public class ColumnInkBoundsTest {
    @Test public void everySplitMatchesFullRescanIncludingEmptyAndThresholdPixels() {
        Random random=new Random(1934);
        for(int run=0;run<100;run++) {
            int width=1+random.nextInt(60),height=1+random.nextInt(40);
            int[] pixels=new int[width*height];
            for(int i=0;i<pixels.length;i++) {
                int red=run%4==0?255:run%4==1?144:run%4==2?145:random.nextInt(256);
                pixels[i]=0xff000000|red<<16|random.nextInt(65536);
            }
            var bounds=new ColumnInkBounds(pixels,width,height,145);
            for(int cut=0;cut<=width;cut++) {
                assertEquals(scan(pixels,width,height,0,cut),bounds.leftOf(cut));
                assertEquals(scan(pixels,width,height,cut,width),bounds.rightOf(cut));
            }
        }
    }
    private static ColumnInkBounds.Bounds scan(int[] pixels,int width,int height,int start,int end) {
        int left=end,right=start,top=height,bottom=0;
        for(int y=0;y<height;y++)for(int x=start;x<end;x++)if(((pixels[y*width+x]>>>16)&255)<145) {
            left=Math.min(left,x);right=Math.max(right,x+1);top=Math.min(top,y);bottom=Math.max(bottom,y+1);
        }
        return right<=left?null:new ColumnInkBounds.Bounds(left,top,right,bottom);
    }
}

