// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original ledger-note shafts with independent long thick beam evidence. */
public class LongPaleBeamTest {
 static final int W=280,H=260;final byte[] g=new byte[W*H],l=new byte[W*H];
 public LongPaleBeamTest(){Arrays.fill(g,(byte)255);}
 void rect(int a,int b,int c,int d,int v){for(int y=c;y<=d;y++)for(int x=a;x<=b;x++)g[y*W+x]=(byte)v;}
 int beams()throws Exception {var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=hc.getDeclaredConstructors()[0];ctor.setAccessible(true);Object head=ctor.newInstance(180,90,110,64,76,100f,70f);var sc=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var st=sc.getDeclaredConstructor(float.class,float.class,float.class);st.setAccessible(true);Object staff=st.newInstance(96f,160f,16f);var m=OmrScoreInterpreter.class.getDeclaredMethod("detectBeamCount",byte[].class,byte[].class,int.class,int.class,hc,sc,List.class);m.setAccessible(true);return(int)m.invoke(null,l,g,W,H,head,staff,List.of(head));}
 void shaft(int bottom){rect(89,91,70,bottom,235);}
 @Test public void longLedgerStemHasIndependentSingleBeam()throws Exception{shaft(207);rect(20,90,201,207,35);assertEquals(1,beams());}
 @Test public void longBlankStemDoesNotInventBeam()throws Exception{shaft(207);assertEquals(0,beams());}
 @Test public void longStemAtThinRuleDoesNotInventBeam()throws Exception{shaft(207);rect(20,230,205,207,35);assertEquals(0,beams());}
 @Test public void longStemAtShortTenutoDoesNotInventBeam()throws Exception{shaft(207);rect(72,90,201,207,35);assertEquals(0,beams());}
 @Test public void excessiveStemDoesNotBorrowFarBeam()throws Exception{shaft(243);rect(20,90,237,243,35);assertEquals(0,beams());}
 @Test public void disconnectedLongShaftDoesNotBorrowBeam()throws Exception{shaft(207);rect(89,91,125,140,255);rect(20,90,201,207,35);assertEquals(0,beams());}
 @Test public void pixelsAndLabelsArePreserved()throws Exception{shaft(207);rect(20,90,201,207,35);byte[] gg=g.clone(),ll=l.clone();beams();assertArrayEquals(gg,g);assertArrayEquals(ll,l);}
}
