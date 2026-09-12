// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original raster examples of a threshold-isolated staff/stem crossing. */
public class FadedStemDotTest {
 static final int W=320,H=220;
 final byte[] gray=new byte[W*H];
 public FadedStemDotTest(){Arrays.fill(gray,(byte)255);}
 void rule(){for(int x=20;x<300;x++)gray[112*W+x]=(byte)150;}
 void stem(int shade,int first,int last){for(int y=first;y<=last;y++)for(int x=127;x<=129;x++)gray[y*W+x]=(byte)shade;}
 void core(){for(int y=-3;y<=3;y++)for(int x=-3;x<=3;x++)if(x*x+y*y<=9)gray[(112+y)*W+128+x]=100;}
 int dots()throws Exception {
  Class<?> type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
  Object head=ctor.newInstance(150,92,108,98,110,100f,104f);
  var m=OmrScoreInterpreter.class.getDeclaredMethod("countAugmentationDots",List.class,type,float.class,byte[].class,int.class,int.class,boolean.class);m.setAccessible(true);
  return (Integer)m.invoke(null,List.of(),head,16f,gray,W,H,false);
 }
 @Test public void shadedStemCrossingIsNotADurationDot()throws Exception{rule();stem(150,70,150);core();assertEquals(0,dots());}
 @Test public void lighterStemCrossingIsNotADurationDot()throws Exception{rule();stem(190,70,150);core();assertEquals(0,dots());}
 @Test public void actualDotOnAStaffRuleSurvives()throws Exception{rule();core();assertEquals(1,dots());}
 @Test public void actualIsolatedDotSurvives()throws Exception{core();assertEquals(1,dots());}
 @Test public void NearbyShortInkDoesNotEstablishAThroughStroke()throws Exception{rule();stem(150,101,108);core();assertEquals(1,dots());}
 @Test public void actualDotOnGrayPaperSurvives()throws Exception{Arrays.fill(gray,(byte)200);core();assertEquals(1,dots());}
 @Test public void actualDotOnDarkerPaperSurvives()throws Exception{Arrays.fill(gray,(byte)175);core();assertEquals(1,dots());}
 @Test public void rasterIsNeverChanged()throws Exception{rule();stem(150,70,150);core();byte[] before=gray.clone();dots();assertArrayEquals(before,gray);}
 @Test public void crossingNearLowerStemTipIsNotADot()throws Exception{rule();stem(190,70,122);core();assertEquals(0,dots());}
 @Test public void crossingNearUpperStemTipIsNotADot()throws Exception{rule();stem(190,102,150);core();assertEquals(0,dots());}
 @Test public void aGapAboveRealDotBreaksStemContinuation()throws Exception{rule();stem(190,70,105);stem(190,116,122);core();assertEquals(1,dots());}
 @Test public void aGapBelowRealDotBreaksStemContinuation()throws Exception{rule();stem(190,102,108);stem(190,119,150);core();assertEquals(1,dots());}
}
