// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original thick-rule and parabolic-arc rasters, not copied notation. */
public class TieStrokeCenterlineTest {
 final int W=230,H=210;final byte[] labels=new byte[W*H],gray=new byte[W*H];
 boolean detect(int left,int right,float cy)throws Exception{
  var m=OmrScoreInterpreter.class.getDeclaredMethod("hasPrintedTieArc",byte[].class,byte[].class,int.class,int.class,int.class,int.class,float.class,float.class);m.setAccessible(true);
  return (boolean)m.invoke(null,labels,gray,W,H,left,right,cy,20f);
 }
 void rect(int x,int y,int radius){for(int yy=y-radius;yy<=y+radius;yy++){gray[yy*W+x]=20;labels[yy*W+x]=5;}}
 @Test public void curvedBoundaryOfVariableWidthStraightRuleIsNotTie()throws Exception{
  Arrays.fill(gray,(byte)250);
  for(int x=60;x<=82;x++){float t=(x-60)/22f;int radius=Math.round(2+2*4*t*(1-t));rect(x,110,radius);}
  assertFalse(detect(60,82,100));
 }
 @Test public void upperReturningStrokeRetainsTie()throws Exception{arc(-1);assertTrue(detect(60,155,100));}
 @Test public void lowerReturningStrokeRetainsTie()throws Exception{arc(1);assertTrue(detect(60,155,100));}
 void arc(int side){Arrays.fill(gray,(byte)250);for(int x=60;x<=155;x++){float t=(x-60)/95f;rect(x,Math.round(100+side*20*(.35f+.7f*4*t*(1-t))),1);}}
}
