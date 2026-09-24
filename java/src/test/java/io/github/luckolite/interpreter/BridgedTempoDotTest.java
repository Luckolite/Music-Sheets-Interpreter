// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class BridgedTempoDotTest {
    private double unit(boolean dot,boolean bridge,boolean faint) {
        int w=300,h=160;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=42;y<=79;y++)for(int x=113;x<=114;x++)gray[y*w+x]=0;
        for(int y=74;y<=84;y++)for(int x=99;x<=115;x++)
            if(Math.pow((x-107)/8d,2)+Math.pow((y-79)/5d,2)<=1)gray[y*w+x]=0;
        if(dot)for(int y=77;y<=81;y++)for(int x=121;x<=125;x++)gray[y*w+x]=(byte)(faint?185:0);
        if(bridge)for(int x=115;x<=121;x++)gray[79*w+x]=(byte)185;
        for(int y:new int[]{70,78})for(int yy=y;yy<y+3;yy++)for(int x=140;x<154;x++)gray[yy*w+x]=0;
        for(int y=60;y<=80;y++)for(int x=170;x<=173;x++)gray[y*w+x]=0;
        var result=TempoChangeDetector.detect(List.of(new MeasureNumberReconciler.NumberToken(96,170f/w,60f/h,180f/w,80f/h)),gray,w,h,
                List.of(new MeasureRegion(.2f,.9f,.65f,.95f)));
        assertEquals(1,result.size());return result.get(0).beatUnit();
    }
    @Test public void paleBridgeDoesNotErasePrintedDot(){assertEquals(1.5,unit(true,true,false),.001);}
    @Test public void unprintedDotIsNotInvented(){assertEquals(1,unit(false,false,false),.001);}
    @Test public void isolatedFaintDotIsNotErased(){assertEquals(1.5,unit(true,false,true),.001);}
}
