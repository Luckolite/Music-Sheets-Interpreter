// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original paired beams owned by a pale shaft; no score-derived fixture. */
public class FadedBeamIslandOwnershipTest {
 static final int W=280,H=240;final byte[] gray=new byte[W*H];
 public FadedBeamIslandOwnershipTest(){Arrays.fill(gray,(byte)255);page(190);}
 void page(int stem){for(int y=109;y<=160;y++)gray[y*W+140]=(byte)stem;ellipse(132,160,10,9);for(int x=60;x<=190;x++){int cy=Math.round(110+(x-150)*.15f);for(int dy=-10;dy<=10;dy++)if(dy<=-3||dy>=3)gray[(cy+dy)*W+x]=0;}}
 void ellipse(int x,int y,int rx,int ry){for(int yy=y-ry;yy<=y+ry;yy++)for(int xx=x-rx;xx<=x+rx;xx++)if(Math.pow((xx-x)/(double)rx,2)+Math.pow((yy-y)/(double)ry,2)<=1)gray[yy*W+xx]=0;}
 int rejected(boolean owner,boolean large)throws Exception {var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var cc=hc.getDeclaredConstructors()[0];cc.setAccessible(true);var main=cc.newInstance(310,122,142,151,169,132f,160f);var fragment=large?cc.newInstance(150,131,149,96,110,140f,103f):cc.newInstance(30,137,143,100,106,140f,103f);var sc=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var cs=sc.getDeclaredConstructor(float.class,float.class,float.class);cs.setAccessible(true);var staff=cs.newInstance(80f,144f,16f);var m=OmrScoreInterpreter.class.getDeclaredMethod("mergedBeamInteriorHeads",byte[].class,int.class,int.class,List.class,List.class);m.setAccessible(true);return ((List<?>)m.invoke(null,gray,W,H,owner?List.of(main,fragment):List.of(fragment),List.of(staff))).size();}
 @Test public void tinyBeamIslandUsesPaleIndependentOwner()throws Exception {assertEquals(1,rejected(true,false));}
 @Test public void noOwnerIsRejectedAsEvidence()throws Exception {assertEquals(0,rejected(false,false));}
 @Test public void largerHeadCannotUseTheNewPaleFallback()throws Exception {assertEquals(0,rejected(true,true));}
 @Test public void roundedHeadBulgePreservesRealNote()throws Exception {ellipse(140,103,13,16);assertEquals(0,rejected(true,false));}
 @Test public void disconnectedOwnerDoesNotSupplyStem()throws Exception {for(int y=130;y<145;y++)gray[y*W+140]=(byte)255;assertEquals(0,rejected(true,false));}
 @Test public void paperBrightnessDoesNotSupplyStem()throws Exception {page(225);assertEquals(0,rejected(true,false));}
 @Test public void thinRulesAreNotPairedBeamEvidence()throws Exception {Arrays.fill(gray,(byte)255);ellipse(132,160,10,9);for(int y=109;y<=160;y++)gray[y*W+140]=(byte)190;for(int y:new int[]{101,114})for(int x=60;x<=190;x++)gray[y*W+x]=0;assertEquals(0,rejected(true,false));}
 @Test public void pixelsAreNeverChanged()throws Exception {byte[] before=gray.clone();rejected(true,false);assertArrayEquals(before,gray);}
}
