// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original curved glyph beside a quarter shaft on shaded paper. */
public class ReturningFlagContrastTest {
 static final int W=280,H=240;final byte[] g=new byte[W*H],l=new byte[W*H];
 public ReturningFlagContrastTest(){Arrays.fill(g,(byte)185);rect(89,91,80,125,30);}
 void rect(int a,int b,int c,int d,int v){for(int y=c;y<=d;y++)for(int x=a;x<=b;x++)g[y*W+x]=(byte)v;}
 void hook(){int[] xs={100,110,117,120,119,112,109,110,115,110,104,100};int[] ys={50,59,64,72,80,90,91,77,73,69,65,62};for(int y=49;y<=92;y++)for(int x=100;x<=121;x++){boolean in=false;for(int i=0,j=xs.length-1;i<xs.length;j=i++)if((ys[i]>y)!=(ys[j]>y)&&x<(xs[j]-xs[i])*(y-ys[i])/(double)(ys[j]-ys[i])+xs[i])in=!in;if(in)g[(210-y)*W+x-10]=30;}}
 int[] trace()throws Exception{var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var c=hc.getDeclaredConstructors()[0];c.setAccessible(true);Object head=c.newInstance(180,90,110,74,86,100f,80f);var m=OmrScoreInterpreter.class.getDeclaredMethod("stemToReturningFlag",byte[].class,byte[].class,int.class,int.class,hc,float.class,int[].class);m.setAccessible(true);return(int[])m.invoke(null,l,g,W,H,head,20f,new int[]{90,125,1});}
 @Test public void shadedPaperDoesNotJoinAnIndependentCurvedGlyph()throws Exception{hook();rect(89,91,126,160,165);assertArrayEquals(new int[]{90,125,1},trace());}
 @Test public void realConnectedDarkContinuationRetainsFlag()throws Exception{hook();rect(89,91,126,160,30);assertTrue(trace()[1]>150);}
 @Test public void brightPaperKeepsTheExistingAbsoluteThreshold()throws Exception{Arrays.fill(g,(byte)250);rect(89,91,80,160,165);hook();assertTrue(trace()[1]>150);}
 @Test public void fullyDetachedCurvedGlyphCannotAttach()throws Exception{hook();assertArrayEquals(new int[]{90,125,1},trace());}
 @Test public void ordinaryQuarterRemainsUnextended()throws Exception{assertArrayEquals(new int[]{90,125,1},trace());}
 @Test public void sourcePixelsRemainUnchanged()throws Exception{hook();rect(89,91,126,160,165);byte[] a=g.clone(),b=l.clone();trace();assertArrayEquals(a,g);assertArrayEquals(b,l);}
}
