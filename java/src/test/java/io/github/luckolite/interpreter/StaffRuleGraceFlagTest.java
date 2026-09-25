// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;

/** Original miniature grace strokes crossing full staff rules on shaded paper. */
public class StaffRuleGraceFlagTest {
 static final int W=240,H=200; final byte[] g=new byte[W*H],labels=new byte[W*H];
 public StaffRuleGraceFlagTest(){Arrays.fill(g,(byte)255);for(int y=61;y<=140;y++)Arrays.fill(g,y*W,(y+1)*W,(byte)165);}
 void line(int ax,int ay,int bx,int by,int radius,int ink){int n=Math.max(Math.abs(bx-ax),Math.abs(by-ay));for(int i=0;i<=n;i++){int x=Math.round(ax+(bx-ax)*i/(float)n),y=Math.round(ay+(by-ay)*i/(float)n);for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++)g[(y+dy)*W+x+dx]=(byte)ink;}}
 void rules(){for(int y=56;y<=120;y+=16)line(30,y,190,y,0,70);}
 void flag(int offset){line(100,88+offset,107,100+offset,1,35);line(107,100+offset,111,106+offset,0,35);line(111,106+offset,109,112+offset,0,35);}
 void slash(){line(94,106,111,91,1,35);}
 int count(){return SlashedGraceFlagInk.count(g,W,H,94,120,100,16);}
 @Test public void crossingStaffDoesNotAddFlag(){rules();flag(0);slash();assertEquals(1,count());}
 @Test public void returningFlagCannotBorrowLowerStaffRule(){
  for(int y=62;y<=118;y+=14)line(30,y,190,y,0,80);
  line(100,90,100,120,0,50);line(100,92,106,101,1,50);line(106,101,110,109,0,60);line(110,109,107,115,0,60);
  line(94,106,111,94,1,45);
  assertEquals(1,SlashedGraceFlagInk.count(g,W,H,94,120,100,14));
 }
 @Test public void staffRulesWithoutFlagRemainUnrecognized(){rules();slash();assertEquals(0,count());}
 @Test public void twoIndependentFlagsRemainTwo(){flag(-8);flag(6);slash();assertEquals(2,count());}
 @Test public void rulesDoNotEraseTwoIndependentFlags(){rules();flag(-8);flag(6);slash();assertEquals(2,count());}
 @Test public void withoutSlashThisRecoveryAbstains(){rules();flag(0);assertEquals(0,count());}
 @Test public void shadowDoesNotMutatePixels(){rules();flag(0);slash();byte[] before=g.clone();count();assertArrayEquals(before,g);}
 private int decoded(boolean companion)throws Exception {
  rules();flag(0);slash();line(100,88,100,120,0,35);
  for(int y=88;y<=120;y++)labels[y*W+100]=5;
  for(int y=115;y<=125;y++)for(int x=88;x<=101;x++)if(Math.pow((x-94)/6.,2)+Math.pow((y-120)/5.,2)<=1){g[y*W+x]=35;labels[y*W+x]=2;}
  var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=hc.getDeclaredConstructors()[0];ctor.setAccessible(true);
  Object head=ctor.newInstance(85,88,101,115,125,94f,120f);
  var sc=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var ctorS=sc.getDeclaredConstructor(float.class,float.class,float.class);ctorS.setAccessible(true);
  Object staff=ctorS.newInstance(56f,120f,16f);
  var m=OmrScoreInterpreter.class.getDeclaredMethod("detectBeamCount",byte[].class,byte[].class,int.class,int.class,hc,sc,List.class);m.setAccessible(true);
  Object other=ctor.newInstance(85,120,133,115,125,126f,120f);
  return (int)m.invoke(null,labels,g,W,H,head,staff,companion?List.of(head,other):List.of(head));
 }
 @Test public void decoderRetracesStemWithinLocalShadow()throws Exception {assertEquals(1,decoded(false));}
 @Test public void companionHeadStillBlocksSolitaryFlagRepair()throws Exception {assertEquals(0,decoded(true));}
}
