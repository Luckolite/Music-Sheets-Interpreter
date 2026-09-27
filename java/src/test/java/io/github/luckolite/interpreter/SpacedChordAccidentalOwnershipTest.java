// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original staggered natural drawn from rectangles; no score pixels. */
public class SpacedChordAccidentalOwnershipTest {
 final byte[] labels=new byte[300*150];
 void rect(int l,int r,int t,int b){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)labels[y*300+x]=3;}
 Object make(String n,Object...a)throws Exception{var ct=Class.forName(OmrScoreInterpreter.class.getName()+"$"+n).getDeclaredConstructors()[0];ct.setAccessible(true);return ct.newInstance(a);}
 Object head(int x,int y)throws Exception{return make("Component",300,x-11,x+11,y-8,y+8,(float)x,(float)y);}
 int recognize(int x,int y,int otherX,int otherY)throws Exception{
  rect(60,63,35,75);rect(74,77,55,95);rect(60,77,55,58);rect(60,77,72,75);
  Object seed=make("AccidentalCandidate",make("Component",350,60,77,35,95,68.5f,65f),(byte)3),head=head(x,y);
  rect(90,93,55,95);rect(104,107,75,115);rect(90,107,75,78);rect(90,107,92,95);
  Object partner=make("AccidentalCandidate",make("Component",350,90,107,55,115,98.5f,85f),(byte)3);
  List<Object> heads=new ArrayList<>();heads.add(head);if(otherX>0)heads.add(head(otherX,otherY));
  var m=OmrScoreInterpreter.class.getDeclaredMethod("detectWrittenAccidental",byte[].class,int.class,int.class,List.class,head.getClass(),float.class,List.class);m.setAccessible(true);
  return (int)m.invoke(null,labels,300,150,List.of(seed,partner),head,20f,heads);
 }
 @Test public void staggeredNaturalCanReachChordColumn()throws Exception{assertEquals(0,recognize(145,65,145,85));}
 @Test public void nearerPitchOwnsNaturalEvenWhenOtherHeadIsWithinLegacyTolerance()throws Exception{assertEquals(2,recognize(105,80,105,65));}
 @Test public void interveningAttackBlocksRemoteAccidental()throws Exception{assertEquals(2,recognize(145,65,110,65));}
 @Test public void arbitraryLongWhitespaceCannotLinkAccidental()throws Exception{assertEquals(2,recognize(220,65,-1,0));}
 @Test public void distantDifferentPitchRemainsUnmarked()throws Exception{assertEquals(2,recognize(145,45,-1,0));}
 @Test public void staggeredPartnerMayBelongToAWiderChordInterval()throws Exception{
  rect(60,63,35,75);rect(74,77,55,95);rect(60,77,55,58);rect(60,77,72,75);
  Object seed=make("AccidentalCandidate",make("Component",350,60,77,35,95,68.5f,65f),(byte)3),head=head(125,65);
  rect(90,93,85,125);rect(104,107,105,145);rect(90,107,105,108);rect(90,107,122,125);
  Object partner=make("AccidentalCandidate",make("Component",350,90,107,85,145,98.5f,115f),(byte)3);
  var m=OmrScoreInterpreter.class.getDeclaredMethod("detectWrittenAccidental",byte[].class,int.class,int.class,List.class,head.getClass(),float.class,List.class);m.setAccessible(true);
  assertEquals(0,(int)m.invoke(null,labels,300,150,List.of(seed,partner),head,20f,List.of(head,head(125,115))));
 }
 @Test public void aSharedShaftCanJoinDisplacedChordHeads()throws Exception {
  Object a=head(130,70),b=head(151,80);
  for(int y=65;y<=125;y++)labels[y*300+140]=1;
  var m=OmrScoreInterpreter.class.getDeclaredMethod("sharedAccidentalChordShaft",byte[].class,int.class,int.class,a.getClass(),a.getClass(),float.class);m.setAccessible(true);
  assertTrue((boolean)m.invoke(null,labels,300,150,a,b,20f));
 }
 @Test public void nearbyHeadsWithoutAnOuterShaftAreSeparateAttacks()throws Exception {
  Object a=head(130,70),b=head(151,80);
  for(int y=65;y<=88;y++)labels[y*300+140]=2;
  var m=OmrScoreInterpreter.class.getDeclaredMethod("sharedAccidentalChordShaft",byte[].class,int.class,int.class,a.getClass(),a.getClass(),float.class);m.setAccessible(true);
  assertFalse((boolean)m.invoke(null,labels,300,150,a,b,20f));
 }
}
