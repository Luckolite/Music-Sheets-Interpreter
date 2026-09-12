// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rest-count strokes and nearby playable note shapes. */
public class MultimeasureRestCountTest {
    private static final int W=440,H=240;
    private final byte[] pixels=new byte[W*H];
    private void rect(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)pixels[yy*W+xx]=0;}
    private void ellipse(int x,int y,int w,int h,boolean hollow){for(int yy=y;yy<=y+h;yy++)for(int xx=x;xx<=x+w;xx++) {
        double d=Math.pow((xx-x-w/2.0)/(w/2.0),2)+Math.pow((yy-y-h/2.0)/(h/2.0),2);
        if(d<=1&&(!hollow||d>=.45))pixels[yy*W+xx]=0;
    }}
    private void curve(int dx,double x0,double y0,double x1,double y1,double x2,double y2,double x3,double y3) {
        for(int i=0;i<=200;i++){double t=i/200.0,u=1-t;
            int x=dx+(int)Math.round(u*u*u*x0+3*u*u*t*x1+3*u*t*t*x2+t*t*t*x3);
            int y=(int)Math.round(u*u*u*y0+3*u*u*t*y1+3*u*t*t*y2+t*t*t*y3);
            ellipse(x-3,y-3,6,6,false);
        }
    }
    private void setup(boolean bar,boolean bothCaps) {
        Arrays.fill(pixels,(byte)255);
        for(int y=100;y<=180;y+=20)rect(10,y,420,1);
        if(bar){rect(90,136,260,10);rect(88,123,5,35);if(bothCaps)rect(347,123,5,35);}
    }
    private void count(){count(0);}
    private void count(int dx){curve(dx,204,50,233,38,240,62,217,65);curve(dx,217,65,244,63,239,94,204,85);}
    private boolean suppressed(float x,float y,int minX,int maxX,int minY,int maxY)throws Exception {
        var ht=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var hc=ht.getDeclaredConstructors()[0];hc.setAccessible(true);
        var head=hc.newInstance(75,minX,maxX,minY,maxY,x,y);
        var st=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var sc=st.getDeclaredConstructor(float.class,float.class,float.class);sc.setAccessible(true);var staff=sc.newInstance(100f,180f,20f);
        var m=OmrScoreInterpreter.class.getDeclaredMethod("isHeavyRestCount",byte[].class,int.class,int.class,ht,st);m.setAccessible(true);
        byte[] before=pixels.clone();boolean result=(boolean)m.invoke(null,pixels,W,H,head,staff);assertArrayEquals(before,pixels);return result;
    }
    private boolean countSuppressed()throws Exception{return suppressed(218,83,206,232,76,90);}
    @Test public void lowerCountLobeAboveCappedRestIsSilent()throws Exception {setup(true,true);count();assertTrue(countSuppressed());}
    @Test public void upperCountLobeAboveCappedRestIsSilent()throws Exception {setup(true,true);count();assertTrue(suppressed(218,51,207,232,44,58));}
    @Test public void anUncappedHorizontalBeamDoesNotSuppressAnything()throws Exception {setup(false,false);rect(90,136,260,10);count();assertFalse(countSuppressed());}
    @Test public void oneEndCapIsInsufficient()throws Exception {setup(true,false);count();assertFalse(countSuppressed());}
    @Test public void staffRulesAreNotAHeavyRest()throws Exception {setup(false,false);count();assertFalse(countSuppressed());}
    @Test public void aWideWholeCueNoteAboveRestRemainsPlayable()throws Exception {setup(true,true);ellipse(200,70,32,17,true);assertFalse(countSuppressed());}
    @Test public void aStemmedCueNoteAboveRestRemainsPlayable()throws Exception {setup(true,true);ellipse(207,77,23,12,false);rect(227,28,3,58);assertFalse(countSuppressed());}
    @Test public void aCountBesideTheRestIsNotRemovedByThisRule()throws Exception {setup(true,true);count(150);assertFalse(suppressed(368,83,356,382,76,90));}
}
