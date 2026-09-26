// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original steep crosses, corner-only semantic bounds and nearby oval controls. */
public class CrossCornerHeadTest {
 static final int W=280,H=240;final byte[] g=new byte[W*H];
 public CrossCornerHeadTest(){Arrays.fill(g,(byte)250);}
 void cross(boolean both){for(int y=-9;y<=9;y++)for(int x=-11;x<=11;x++)if(Math.abs(y-x)<1.7||both&&Math.abs(y+x)<1.7)g[(150+y)*W+120+x]=30;}
 boolean detect(float cx)throws Exception{var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var c=hc.getDeclaredConstructors()[0];c.setAccessible(true);Object h=c.newInstance(35,Math.round(cx)-3,Math.round(cx)+3,147,153,cx,150f);var st=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var sc=st.getDeclaredConstructor(float.class,float.class,float.class);sc.setAccessible(true);Object s=sc.newInstance(118f,182f,16f);var m=OmrScoreInterpreter.class.getDeclaredMethod("isUnpitchedCrossHead",byte[].class,int.class,int.class,hc,st);m.setAccessible(true);return(boolean)m.invoke(null,g,W,H,h,s);}
 @Test public void steepCrossWithOuterCornerMaskIsUnpitched()throws Exception{cross(true);assertTrue(detect(129.5f));}
 @Test public void steepCenteredCrossIsUnpitched()throws Exception{cross(true);assertTrue(detect(120));}
 @Test public void nearbySeparateHeadCannotBorrowCross()throws Exception{cross(true);assertFalse(detect(135));}
 @Test public void oneDiagonalDoesNotProveCross()throws Exception{cross(false);assertFalse(detect(129.5f));}
 @Test public void filledOvalCornerDoesNotConvergeTwice()throws Exception{for(int y=-8;y<=8;y++)for(int x=-12;x<=12;x++)if(x*x/144f+y*y/64f<=1)g[(150+y)*W+120+x]=30;assertFalse(detect(129.5f));}
 @Test public void hollowOvalSidesDivergeTowardCenter()throws Exception{for(int y=-8;y<=8;y++)for(int x=-12;x<=12;x++){float d=x*x/144f+y*y/64f;if(d<=1&&d>=.5)g[(150+y)*W+120+x]=30;}assertFalse(detect(129.5f));}
 @Test public void smallRoundGraceRemainsPitched()throws Exception{for(int y=-4;y<=4;y++)for(int x=-6;x<=6;x++)if(x*x/36f+y*y/16f<=1)g[(150+y)*W+120+x]=30;assertFalse(detect(120));}
 @Test public void sourceInkIsImmutable()throws Exception{cross(true);byte[] old=g.clone();detect(129.5f);assertArrayEquals(old,g);}
}

