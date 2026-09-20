// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.util.Random;
import static org.junit.Assert.*;

public class MeterCropRasterTest {
    @Test public void bulkPreparationMatchesOriginalPerPixelOperations() {
        Random random=new Random(4207);
        for(int run=0;run<100;run++)for(boolean mask:new boolean[]{false,true}) {
            int width=1+random.nextInt(50),height=65;
            int[] actual=new int[width*height];
            for(int i=0;i<actual.length;i++)actual[i]=0xff000000|random.nextInt(0x1000000);
            int[] expected=actual.clone();
            float gap=8+random.nextFloat()*4,first=3+random.nextFloat()*3;
            original(expected,width,height,first,gap,mask);
            MeterCropRaster.prepare(actual,width,height,9,first+9,gap,mask);
            assertArrayEquals(expected,actual);
        }
    }
    private static void original(int[] p,int w,int h,float first,float gap,boolean mask) {
        if(mask)for(int y=0;y<h;y++)for(int x=0;x<w;x++)
            p[y*w+x]=MeterOcrEvidence.ink(luminance(p[y*w+x]))?0xff000000:0xffffffff;
        int radius=Math.max(1,Math.round(gap*.06f));
        for(int line=0;line<5;line++) {
            int y=Math.round(first+9+line*gap)-9;
            for(int x=0;x<w;x++) {
                boolean bridge=column(p,w,x,Math.max(0,y-radius-2),Math.max(0,y-radius-1))
                    &&column(p,w,x,Math.min(h-1,y+radius+1),Math.min(h-1,y+radius+2));
                for(int yy=Math.max(0,y-radius);yy<=Math.min(h-1,y+radius);yy++)p[yy*w+x]=bridge?0xff000000:0xffffffff;
            }
        }
        for(int x=0;x<w;x++) {
            if(x>=w*.15f&&x<w*.90f)continue;
            int dark=0;for(int y=0;y<h;y++)if(luminance(p[y*w+x])<135)dark++;
            if(dark>h*.85f)for(int y=0;y<h;y++)p[y*w+x]=0xffffffff;
        }
    }
    private static boolean column(int[] p,int w,int x,int top,int bottom) {
        if(top>bottom)return false;
        for(int xx=Math.max(0,x-1);xx<=Math.min(w-1,x+1);xx++)
            for(int y=top;y<=bottom;y++)if(luminance(p[y*w+xx])<135)return true;
        return false;
    }
    private static int luminance(int c){return (((c>>>16)&255)*30+((c>>>8)&255)*59+(c&255)*11)/100;}
}

