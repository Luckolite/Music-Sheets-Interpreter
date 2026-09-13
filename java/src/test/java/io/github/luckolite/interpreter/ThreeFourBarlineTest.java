// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original paired open bowls above a four counter, with continuous right-hand ink. */
public class ThreeFourBarlineTest {
    private static final int W=240,H=140,X=100;
    private final byte[] gray=new byte[W*H];
    private final int[] rules={40,54,68,82,96};
    public ThreeFourBarlineTest(){Arrays.fill(gray,(byte)255);for(int y:rules)rect(10,230,y,y,0);rect(X,X+2,40,96,0);}
    private void rect(int a,int b,int t,int z,int value){for(int y=t;y<=z;y++)for(int x=a;x<=b;x++)gray[y*W+x]=(byte)value;}
    private void bowls(){rect(88,94,43,49,0);rect(88,94,57,63,0);rect(90,101,40,41,0);rect(90,101,65,66,0);}
    private void four(){rect(91,102,81,89,0);for(int row=0;row<5;row++)rect(98-Math.min(6,row+2)+1,98,83+row,83+row,255);}
    private boolean digit(){return OmrMeasurePostProcessor.threeOverFourCounters(gray,W,H,X,38,98,14);}
    private int bar()throws Exception{var m=OmrMeasurePostProcessor.class.getDeclaredMethod("rawBarlineColumn",byte[].class,int.class,int.class,int.class,int[].class,float.class,float.class,float.class);m.setAccessible(true);return (int)m.invoke(null,gray,W,H,X,rules,14f,0f,0f);}
    @Test public void openBowlsAndLowerCounterRejectTheFalseBar()throws Exception{bowls();four();assertEquals(Integer.MIN_VALUE,bar());assertTrue(digit());}
    @Test public void staffClippedLowerCounterStillCorroboratesBothBowls(){
        int w=300,h=180,x=160;byte[] ink=new byte[w*h];Arrays.fill(ink,(byte)255);
        for(int y:new int[]{50,71,92,113,134})for(int c=10;c<290;c++)ink[y*w+c]=0;
        for(int y=50;y<=134;y++)for(int c=x;c<=x+3;c++)ink[y*w+c]=0;
        for(int top:new int[]{54,75})for(int y=top;y<top+10;y++)for(int c=142;c<=151;c++)ink[y*w+c]=0;
        for(int y=112;y<=122;y++)for(int c=147;c<=163;c++)ink[y*w+c]=0;
        for(int row=0;row<4;row++)for(int c=159-new int[]{8,9,10,10}[row];c<=158;c++)ink[(115+row)*w+c]=(byte)255;
        assertTrue(OmrMeasurePostProcessor.threeOverFourCounters(ink,w,h,x,47,137,21));
    }
    @Test public void plainBarRemainsABar()throws Exception{assertFalse(digit());assertNotEquals(Integer.MIN_VALUE,bar());}
    @Test public void doubleBarRemainsABar()throws Exception{rect(94,95,40,96,0);assertFalse(digit());assertNotEquals(Integer.MIN_VALUE,bar());}
    @Test public void oneCounterCannotRejectABar()throws Exception{four();assertFalse(digit());assertNotEquals(Integer.MIN_VALUE,bar());}
    @Test public void upperBowlsAloneDoNotProveStackedDigits(){bowls();assertFalse(digit());}
    @Test public void onlyOneUpperBowlCannotProveThree(){four();rect(88,94,43,49,0);assertFalse(digit());}
    @Test public void filledSpacesAreNotOpenBowls(){bowls();four();rect(94,100,43,49,0);rect(94,100,57,63,0);assertFalse(digit());}
    @Test public void sourcePixelsRemainUnchanged(){bowls();four();var before=gray.clone();digit();assertArrayEquals(before,gray);}
}
