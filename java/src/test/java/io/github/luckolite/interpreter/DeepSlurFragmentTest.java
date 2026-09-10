// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original compact curves and real-note counterexamples, with partial head masks. */
public final class DeepSlurFragmentTest {
    private static final int W=240,H=240;
    private static final float GAP=16;
    private static final class Drawing {
        final byte[] gray=new byte[W*H];
        final boolean[] mask=new boolean[W*H];
        Drawing(){Arrays.fill(gray,(byte)255);}
        void ink(int x,int y){gray[y*W+x]=0;}
        void curve(boolean upper,boolean tilted) {
            for(int x=90;x<=121;x++) {
                double t=(x-90)/31d;
                int center=(int)Math.round(100+16*4*t*(1-t)+(tilted?16*t:0));
                for(int d=-1;d<=1;d++) {
                    int y=upper?220-center-d:center+d;
                    ink(x,y);
                    if(x>=91&&x<=100)mask[y*W+x]=true;
                }
            }
        }
        void mirror() {
            byte[] before=gray.clone();boolean[] selected=mask.clone();
            for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
                gray[y*W+x]=before[y*W+W-1-x];mask[y*W+x]=selected[y*W+W-1-x];
            }
        }
        Object component() throws Exception {
            int area=0,left=W,right=0,top=H,bottom=0;long sx=0,sy=0;
            for(int y=0;y<H;y++)for(int x=0;x<W;x++)if(mask[y*W+x]) {
                area++;left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);sx+=x;sy+=y;
            }
            assertTrue(area>0);
            Class<?> type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
            Constructor<?> ctor=type.getDeclaredConstructor(int.class,int.class,int.class,int.class,int.class,float.class,float.class);
            ctor.setAccessible(true);return ctor.newInstance(area,left,right,top,bottom,sx/(float)area,sy/(float)area);
        }
        boolean rejected(boolean raw) throws Exception {
            Object head=component();Method method=OmrScoreInterpreter.class.getDeclaredMethod("flatStemlessFragment",byte[].class,int.class,int.class,head.getClass(),float.class);
            method.setAccessible(true);return (boolean)method.invoke(null,raw?gray:null,W,H,head,GAP);
        }
        void oval(boolean filled,boolean ledger) {
            for(int y=99;y<=117;y++)for(int x=89;x<=123;x++) {
                double radius=Math.pow((x-106)/16d,2)+Math.pow((y-108)/8d,2);
                if(radius<=1&&(filled||radius>=.55)) {
                    ink(x,y);if(x>=91&&x<=100)mask[y*W+x]=true;
                }
            }
            if(ledger)for(int x=84;x<=128;x++)ink(x,108);
        }
    }
    @Test public void deepLowerCurveIsNotANote()throws Exception {var d=new Drawing();d.curve(false,false);assertTrue(d.rejected(true));}
    @Test public void deepUpperCurveIsNotANote()throws Exception {var d=new Drawing();d.curve(true,false);assertTrue(d.rejected(true));}
    @Test public void rightSideIslandAlsoRevealsReturningCurve()throws Exception {var d=new Drawing();d.curve(false,false);d.mirror();assertTrue(d.rejected(true));}
    @Test public void missingRawImageCannotProveASlur()throws Exception {var d=new Drawing();d.curve(false,false);assertFalse(d.rejected(false));}
    @Test public void highlyUnequalEndpointsAreNotACompleteBowl()throws Exception {var d=new Drawing();d.curve(false,true);assertFalse(d.rejected(true));}
    @Test public void attachedStemPreservesARealHead()throws Exception {
        var d=new Drawing();d.curve(false,false);for(int y=73;y<=115;y++)d.ink(100,y);assertFalse(d.rejected(true));
    }
    @Test public void filledOvalIsPreserved()throws Exception {var d=new Drawing();d.oval(true,false);assertFalse(d.rejected(true));}
    @Test public void hollowOvalIsPreserved()throws Exception {var d=new Drawing();d.oval(false,false);assertFalse(d.rejected(true));}
    @Test public void ledgerThroughHollowOvalDoesNotProveASlur()throws Exception {var d=new Drawing();d.oval(false,true);assertFalse(d.rejected(true));}
    @Test public void connectedInkLeavingCropCannotProveACompleteBowl()throws Exception {
        var d=new Drawing();d.curve(false,false);for(int x=121;x<180;x++)d.ink(x,100);assertFalse(d.rejected(true));
    }
}
