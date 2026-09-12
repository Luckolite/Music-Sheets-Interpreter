// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original note and dot drawings; no score images or model output. */
public class CompactAugmentationDotTest {
    private static final class Drawing {
        final int scale,width,height;
        final byte[] gray;
        Drawing(int scale) {
            this.scale=scale;width=140*scale;height=100*scale;gray=new byte[width*height];Arrays.fill(gray,(byte)255);
            for(int y=55*scale;y<=65*scale;y++)for(int x=40*scale;x<=60*scale;x++)
                if(Math.pow((x-50.0*scale)/(10*scale),2)+Math.pow((y-60.0*scale)/(5*scale),2)<=1)gray[y*width+x]=0;
        }
        void dot(int cx,int cy,int radius) {
            for(int y=(cy-radius)*scale;y<=(cy+radius)*scale;y++)for(int x=(cx-radius)*scale;x<=(cx+radius)*scale;x++)
                if(Math.pow(x-cx*scale,2)+Math.pow(y-cy*scale,2)<=Math.pow(radius*scale,2))gray[y*width+x]=0;
        }
        int count() throws Exception {
            var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
            var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
            var head=ctor.newInstance(150*scale*scale,40*scale,60*scale,55*scale,65*scale,50f*scale,60f*scale);
            var method=OmrScoreInterpreter.class.getDeclaredMethod("countAugmentationDots",List.class,type,float.class,byte[].class,int.class,int.class,boolean.class);method.setAccessible(true);
            return (int)method.invoke(null,List.of(),head,20f*scale,gray,width,height,false);
        }
    }
    @Test public void isolatedCompactDotIsCounted() throws Exception {var d=new Drawing(1);d.dot(69,60,3);assertEquals(1,d.count());}
    @Test public void compactDotScalesWithStaffSpacing() throws Exception {var d=new Drawing(2);d.dot(69,60,3);assertEquals(1,d.count());}
    @Test public void edgeIslandIsNotADot() throws Exception {var d=new Drawing(1);d.dot(64,60,2);assertEquals(0,d.count());}
    @Test public void tinyCompactSpeckIsNotADot() throws Exception {var d=new Drawing(1);d.dot(69,60,1);assertEquals(0,d.count());}
    @Test public void detachedMarkAboveHeadIsNotAnAugmentationDot() throws Exception {var d=new Drawing(1);d.dot(69,41,3);assertEquals(0,d.count());}
    @Test public void ordinarilySpacedDotStillCounts() throws Exception {var d=new Drawing(1);d.dot(78,60,3);assertEquals(1,d.count());}
    @Test public void noDotRemainsUndotted() throws Exception {assertEquals(0,new Drawing(1).count());}
    @Test public void aSolidSquareFragmentIsNotACompactDot() throws Exception {var d=new Drawing(1);for(int y=58;y<=62;y++)for(int x=67;x<=71;x++)d.gray[y*d.width+x]=0;assertEquals(0,d.count());}
    @Test public void aHeavyDotWithRoundedCornersStillCounts() throws Exception {var d=new Drawing(1);for(int y=58;y<=62;y++)for(int x=67;x<=71;x++)d.gray[y*d.width+x]=0;d.gray[58*d.width+67]=(byte)255;d.gray[62*d.width+71]=(byte)255;assertEquals(1,d.count());}
    @Test public void analysisPreservesTheDrawing() throws Exception {var d=new Drawing(1);d.dot(69,60,3);var before=d.gray.clone();d.count();assertArrayEquals(before,d.gray);}
}
