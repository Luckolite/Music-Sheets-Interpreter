// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original short stem, low-contrast scan tail and detached annotation strokes. */
public class TremoloStemContrastTest {
 static final int W=400,H=240;byte[] gray=new byte[W*H];
 public TremoloStemContrastTest(){Arrays.fill(gray,(byte)185);rect(189,191,90,145,60);}
 void rect(int l,int r,int t,int b,int value){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)gray[y*W+x]=(byte)value;}
 void stroke(int y,int value){for(int x=178;x<=202;x++){int at=Math.round(y+(x-190)*.2f);rect(x,x,at-2,at+2,value);}}
 int strokes()throws Exception {var c=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=c.getDeclaredConstructors()[0];ctor.setAccessible(true);Object h=ctor.newInstance(220,189,210,83,97,200f,90f);var m=OmrScoreInterpreter.class.getDeclaredMethod("tremoloStrokeCounts",byte[].class,int.class,int.class,c,float.class,List.class);m.setAccessible(true);return((int[])m.invoke(null,gray,W,H,h,16f,List.of(h)))[0];}
 @Test public void paperTailDoesNotAttachSeparateLetterStroke()throws Exception{rect(189,191,146,187,165);stroke(177,60);assertEquals(0,strokes());}
 @Test public void darkContinuousLongStemRetainsItsStroke()throws Exception{rect(189,191,146,187,60);stroke(177,60);assertEquals(1,strokes());}
 @Test public void genuineStrokeOnShortStemSurvives()throws Exception{stroke(130,60);assertEquals(1,strokes());}
 @Test public void independentlyContrastedGrayStemIsRetained()throws Exception{rect(189,191,90,187,140);stroke(177,90);assertEquals(1,strokes());}
 @Test public void brightPaperKeepsLegacyThreshold()throws Exception{Arrays.fill(gray,(byte)250);rect(189,191,90,187,165);stroke(177,100);assertEquals(1,strokes());}
 @Test public void plainShortStemHasNoTremolo()throws Exception{assertEquals(0,strokes());}
 @Test public void pixelsStayUnchanged()throws Exception{rect(189,191,146,187,165);stroke(177,60);var before=gray.clone();strokes();assertArrayEquals(before,gray);}
}
