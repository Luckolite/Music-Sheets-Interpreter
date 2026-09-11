// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric strokes distinguish a flag from two natural crossbars. */
public class NaturalCrossbarSeparationTest {
 private final byte[] ink=new byte[100*100];
 private void rect(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)ink[yy*100+xx]=5;}
 private void bar(int y,float slope,int thickness){for(int x=25;x<=37;x++)rect(x,Math.round(y+(x-25)*slope),1,thickness);}
 private boolean natural()throws Exception {
  int area=0,x0=100,y0=100,x1=-1,y1=-1;long sx=0,sy=0;
  for(int y=0;y<100;y++)for(int x=0;x<100;x++)if(ink[y*100+x]!=0){area++;sx+=x;sy+=y;x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);}
  var component=Class.forName(OmrScoreInterpreter.class.getName()+"$Component").getDeclaredConstructors()[0];component.setAccessible(true);
  Object c=component.newInstance(area,x0,x1,y0,y1,sx/(float)area,sy/(float)area);
  var candidate=Class.forName(OmrScoreInterpreter.class.getName()+"$AccidentalCandidate");var ctor=candidate.getDeclaredConstructors()[0];ctor.setAccessible(true);
  var method=OmrScoreInterpreter.class.getDeclaredMethod("isNaturalGlyph",byte[].class,int.class,int.class,candidate,float.class);method.setAccessible(true);
  return (boolean)method.invoke(null,ink,100,100,ctor.newInstance(c,(byte)5),14f);
 }
 private void spines(){rect(25,18,2,28);rect(36,29,2,28);}
 @Test public void oneBroadConnectorCannotActAsTwoCrossbars()throws Exception{spines();bar(30,0,10);assertFalse(natural());}
 @Test public void oneDiagonalFlagConnectorIsNotNatural()throws Exception{spines();bar(26,.65f,8);assertFalse(natural());}
 @Test public void twoNarrowCrossbarsRemainNatural()throws Exception{spines();bar(30,0,2);bar(43,0,2);assertTrue(natural());}
 @Test public void slightlySlantedCrossbarsRemainNatural()throws Exception{spines();bar(31,-.20f,3);bar(44,-.20f,3);assertTrue(natural());}
 @Test public void steepCrossbarsRemainNatural()throws Exception{rect(25,18,2,36);rect(36,29,2,37);bar(38,-.55f,3);bar(51,-.55f,3);assertTrue(natural());}
 private boolean rawNatural(boolean slur, boolean faintBreak)throws Exception {
  spines();bar(30,0,2);bar(43,0,2);
  if(faintBreak)for(int y=34;y<=35;y++)for(int x=25;x<27;x++)ink[y*100+x]=0;
  if(slur)rect(25,14,13,1);
  byte[] gray=new byte[ink.length];java.util.Arrays.fill(gray,(byte)255);
  for(int i=0;i<ink.length;i++)if(ink[i]!=0)gray[i]=0;
  var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
  var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
  Object seed=ctor.newInstance(70,25,37,18,45,30f,32f);
  Object head=ctor.newInstance(40,50,58,34,42,54f,38f);
  var method=OmrScoreInterpreter.class.getDeclaredMethod("rawNaturalAtSeed",byte[].class,int.class,int.class,type,type,float.class);
  method.setAccessible(true);return (boolean)method.invoke(null,gray,100,100,seed,head,14f);
 }
 @Test public void disconnectedSlurDoesNotHideRawNatural()throws Exception{assertTrue(rawNatural(true,false));}
 @Test public void faintSpineBreakDoesNotDiscardValidRawNatural()throws Exception{assertTrue(rawNatural(false,true));}
}
