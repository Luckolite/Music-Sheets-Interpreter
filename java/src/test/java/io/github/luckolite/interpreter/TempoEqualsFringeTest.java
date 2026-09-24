// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TempoEqualsFringeTest {
    private double unit(boolean dotted) {
        int w=300,h=160;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);
        for(int y=42;y<=79;y++)for(int x=113;x<=114;x++)gray[y*w+x]=0;
        for(int y=74;y<=84;y++)for(int x=99;x<=115;x++)
            if(Math.pow((x-107)/8.0,2)+Math.pow((y-79)/5.0,2)<=1)gray[y*w+x]=0;
        int equals=dotted?140:120;
        for(int y:new int[]{70,78})for(int yy=y;yy<y+3;yy++)for(int x=equals;x<equals+14;x++)gray[yy*w+x]=0;
        if(dotted)for(int y=77;y<=81;y++)for(int x=121;x<=125;x++)gray[y*w+x]=0;
        else for(int y=78;y<=80;y++)for(int x=118;x<120;x++)gray[y*w+x]=(byte)180;
        for(int y=60;y<=80;y++)for(int x=170;x<173;x++)gray[y*w+x]=0;
        var tokens=List.of(new MeasureNumberReconciler.NumberToken(125,170f/w,60f/h,180f/w,80f/h));
        var result=TempoChangeDetector.detect(tokens,gray,w,h,List.of(new MeasureRegion(.2f,.9f,.65f,.95f)));
        assertEquals(1,result.size());return result.get(0).beatUnit();
    }
    @Test public void equalsFringeDoesNotDotTheQuarter(){assertEquals(1,unit(false),.001);}
    @Test public void completeDotBeforeEqualsIsPreserved(){assertEquals(1.5,unit(true),.001);}
}
