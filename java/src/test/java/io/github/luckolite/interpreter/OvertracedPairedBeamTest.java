// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original beam group and pale continuation beyond the true stem endpoint. */
public class OvertracedPairedBeamTest {
 static final int W=320,H=260;byte[] gray=new byte[W*H],labels=new byte[W*H];
 public OvertracedPairedBeamTest(){Arrays.fill(gray,(byte)255);}
 void rect(int l,int r,int t,int b,int value,boolean label){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){gray[y*W+x]=(byte)value;if(label)labels[y*W+x]=5;}}
 Object head(int x,int y)throws Exception {var c=Class.forName(OmrScoreInterpreter.class.getName()+"$Component").getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(220,x-10,x+10,y-7,y+7,(float)x,(float)y);}
 int decoded(int beams,boolean partner,boolean tail,boolean breakBeam,boolean opposite)throws Exception {
  rect(90,92,100,167,40,true);if(tail)rect(90,92,168,200,150,false);
  if(partner)rect(130,132,opposite?35:100,opposite?100:167,40,true);
  for(int b=0;b<beams;b++)rect(90,132,150+b*11,156+b*11,40,true);
  if(breakBeam)rect(107,119,145,170,255,false);
  var h=head(100,100);var other=head(140,100);var hc=h.getClass();var sc=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var ctor=sc.getDeclaredConstructor(float.class,float.class,float.class);ctor.setAccessible(true);Object staff=ctor.newInstance(70f,134f,16f);
  var m=OmrScoreInterpreter.class.getDeclaredMethod("detectBeamCount",byte[].class,byte[].class,int.class,int.class,hc,sc,List.class);m.setAccessible(true);return(int)m.invoke(null,labels,gray,W,H,h,staff,partner?List.of(h,other):List.of(h));
 }
 @Test public void paleContinuationCannotHideTwoOwnedBeams()throws Exception {assertEquals(2,decoded(2,true,true,false,false));}
 @Test public void ordinaryShortTraceRetainsDoubleBeam()throws Exception {assertEquals(2,decoded(2,true,false,false,false));}
 @Test public void noCompanionCannotUsePairedRecovery()throws Exception {assertTrue(decoded(2,false,true,false,false)<2);}
 @Test public void singlePrintedBeamIsNotPromoted()throws Exception {assertTrue(decoded(1,true,true,false,false)<2);}
 @Test public void interruptedSpanDoesNotProveTwoBeams()throws Exception {assertTrue(decoded(2,true,true,true,false)<2);}
 @Test public void oppositeStemCannotOwnCommonBeams()throws Exception {assertTrue(decoded(2,true,true,false,true)<2);}
 @Test public void noPrintedBeamStaysQuarter()throws Exception {assertEquals(0,decoded(0,true,true,false,false));}
}
