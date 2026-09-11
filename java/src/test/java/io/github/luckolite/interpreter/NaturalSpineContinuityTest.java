// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric glyphs distinguish connected spines from unrelated specks. */
public class NaturalSpineContinuityTest {
 private final byte[] ink=new byte[100*100];
 private void rect(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)ink[yy*100+xx]=5;}
 private boolean natural()throws Exception {
  int area=0,x0=100,y0=100,x1=-1,y1=-1;long sx=0,sy=0;
  for(int y=0;y<100;y++)for(int x=0;x<100;x++)if(ink[y*100+x]!=0){area++;sx+=x;sy+=y;x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);}
  var component=Class.forName(OmrScoreInterpreter.class.getName()+"$Component").getDeclaredConstructors()[0];component.setAccessible(true);
  Object c=component.newInstance(area,x0,x1,y0,y1,sx/(float)area,sy/(float)area);
  var candidate=Class.forName(OmrScoreInterpreter.class.getName()+"$AccidentalCandidate");var ctor=candidate.getDeclaredConstructors()[0];ctor.setAccessible(true);
  var method=OmrScoreInterpreter.class.getDeclaredMethod("isNaturalGlyph",byte[].class,int.class,int.class,candidate,float.class);method.setAccessible(true);
  return (boolean)method.invoke(null,ink,100,100,ctor.newInstance(c,(byte)5),12f);
 }
 private void real(){rect(25,18,2,27);rect(35,29,2,27);rect(25,29,12,3);rect(25,42,12,3);}
 @Test public void aBlobAndDetachedSpeckCannotSupplyNaturalSpine()throws Exception{rect(25,20,5,14);rect(30,24,7,9);rect(30,39,2,2);assertFalse(natural());}
 @Test public void twoContinuousOffsetSpinesAreNatural()throws Exception{real();assertTrue(natural());}
 @Test public void aSmallScanBreakStillAllowsNatural()throws Exception{real();for(int y=35;y<37;y++)for(int x=35;x<37;x++)ink[y*100+x]=0;assertTrue(natural());}
 @Test public void distantInkCannotExtendANaturalSpine()throws Exception{rect(25,18,2,25);rect(35,29,2,14);rect(25,29,12,3);rect(25,40,12,3);rect(35,59,2,2);assertFalse(natural());}
}
