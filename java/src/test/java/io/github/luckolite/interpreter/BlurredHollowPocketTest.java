// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original low-contrast oval geometry, not a source-score fixture. */
public class BlurredHollowPocketTest {
 private static final int W=180,H=180;
 private boolean open(int wall,int interior,int rows,boolean broken,boolean filled)throws Exception {
  byte[] gray=new byte[W*H],labels=new byte[W*H];Arrays.fill(gray,(byte)250);
  for(int y=81;y<=98;y++)for(int x=80;x<=101;x++) {
   double r=Math.pow((x-90.5)/11,2)+Math.pow((y-89.5)/8.5,2);
   if(r<=1){gray[y*W+x]=(byte)wall;labels[y*W+x]=2;}
  }
  if(!filled)for(int y=88;y<88+rows;y++)for(int x=89;x<=92;x++)gray[y*W+x]=(byte)interior;
  if(broken)for(int y=88;y<88+rows;y++)for(int x=92;x<=101;x++)gray[y*W+x]=(byte)interior;
  var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
  var head=ctor.newInstance(270,80,101,81,98,90.5f,89.5f);
  var method=OmrScoreInterpreter.class.getDeclaredMethod("hasOpenCenter",byte[].class,byte[].class,int.class,int.class,type,float.class);method.setAccessible(true);
  return (boolean)method.invoke(null,labels,gray,W,H,head,14f);
 }
 @Test public void blurredOutlineRetainsSmallEnclosedInterior()throws Exception {assertTrue(open(160,215,3,false,false));}
 @Test public void lowContrastFilledHeadRemainsQuarter()throws Exception {assertFalse(open(160,215,3,false,true));}
 @Test public void tinyTwoRowNoiseIsNotAnOpenOval()throws Exception {assertFalse(open(160,215,2,false,false));}
 @Test public void exteriorOpeningCannotBeAClosedPocket()throws Exception {assertFalse(open(160,215,3,true,false));}
 @Test public void insufficientInteriorContrastCannotProveHollow()throws Exception {assertFalse(open(170,195,3,false,false));}
 @Test public void darkHeadPinholesDoNotUseTheBlurAllowance()throws Exception {assertFalse(open(50,215,3,false,false));}
}
