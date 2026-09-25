// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original thin continuous rules with isolated darker/raster-broadened patches. */
public class ThinRuleTremoloTest {
 static final int W=400,H=240;byte[] gray=new byte[W*H];
 public ThinRuleTremoloTest(){Arrays.fill(gray,(byte)245);rect(189,191,90,172,40);}
 void rect(int l,int r,int t,int b,int v){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)gray[y*W+x]=(byte)v;}
 void rule(){rect(60,340,140,142,140);rect(181,183,139,143,50);rect(197,199,139,143,50);}
 int strokes()throws Exception {var c=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=c.getDeclaredConstructors()[0];ctor.setAccessible(true);Object h=ctor.newInstance(220,189,210,83,97,200f,90f);var m=OmrScoreInterpreter.class.getDeclaredMethod("tremoloStrokeCounts",byte[].class,int.class,int.class,c,float.class,List.class);m.setAccessible(true);return((int[])m.invoke(null,gray,W,H,h,16f,List.of(h)))[0];}
 @Test public void continuousThinRuleCannotGainTremoloFromWingBlotches()throws Exception {rule();assertEquals(0,strokes());}
 @Test public void genuineThickSlantedStrokeSurvivesAStaffRule()throws Exception {rule();for(int x=178;x<=202;x++){int y=Math.round(140+(x-190)*.2f);rect(x,x,y-2,y+2,30);}assertEquals(1,strokes());}
 @Test public void separateThickStrokeStillHasItsOwnAttackRate()throws Exception {for(int x=178;x<=202;x++){int y=Math.round(140+(x-190)*.2f);rect(x,x,y-2,y+2,30);}assertEquals(1,strokes());}
 @Test public void blankStemCannotSupplyTremolo()throws Exception {assertEquals(0,strokes());}
 @Test public void isolatedWingPairWithoutRuleIsNotGloballyErased()throws Exception {rule();rect(60,157,139,142,245);rect(223,340,139,142,245);assertEquals(1,strokes());}
 @Test public void oneSidedRuleDoesNotProveGlobalContinuation()throws Exception {rule();rect(223,340,139,142,245);assertEquals(1,strokes());}
 @Test public void pixelsAreNeverChanged()throws Exception {rule();var before=gray.clone();strokes();assertArrayEquals(before,gray);}
}
