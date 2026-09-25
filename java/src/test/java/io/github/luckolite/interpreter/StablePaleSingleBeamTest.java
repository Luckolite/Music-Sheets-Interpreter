// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original stable pale endpoints and overtraced/double-beam counterexamples. */
public class StablePaleSingleBeamTest {
 static final int W=260,H=240;final byte[] g=new byte[W*H],l=new byte[W*H];
 public StablePaleSingleBeamTest(){Arrays.fill(g,(byte)255);}
 void rect(int a,int b,int c,int d,int v){for(int y=c;y<=d;y++)for(int x=a;x<=b;x++)g[y*W+x]=(byte)v;}
 void setup(){rect(109,111,80,164,190);rect(40,190,80,86,35);}
 boolean proof()throws Exception{var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=hc.getDeclaredConstructors()[0];ctor.setAccessible(true);Object head=ctor.newInstance(180,90,110,158,170,100f,164f);var sc=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var st=sc.getDeclaredConstructor(float.class,float.class,float.class);st.setAccessible(true);Object staff=st.newInstance(96f,160f,16f);var m=OmrScoreInterpreter.class.getDeclaredMethod("singleBeamAtPaleEndpoint",byte[].class,byte[].class,int.class,int.class,hc,sc);m.setAccessible(true);return(boolean)m.invoke(null,l,g,W,H,head,staff);}
 @Test public void stablePaleShaftWithOneBeamOnBothSidesIsProved()throws Exception{setup();assertTrue(proof());}
 @Test public void independentSecondaryBeamIsNotReduced()throws Exception{setup();rect(110,190,94,100,35);assertFalse(proof());}
 @Test public void oneSidedBeamCannotOverridePositiveCount()throws Exception{setup();rect(40,107,80,86,255);assertFalse(proof());}
 @Test public void paleOvertraceAboveTheRealBeamCannotOverride()throws Exception{setup();rect(109,111,66,79,235);assertFalse(proof());}
 @Test public void ordinaryDarkStemKeepsExistingDecoder()throws Exception{setup();rect(109,111,80,164,35);assertFalse(proof());}
 @Test public void disconnectedShaftCannotOverride()throws Exception{setup();rect(109,111,110,125,255);assertFalse(proof());}
 @Test public void thinRulesCannotProvideBeamProof()throws Exception{setup();rect(40,190,80,86,255);rect(40,190,80,82,35);assertFalse(proof());}
 @Test public void pixelsAndLabelsArePreserved()throws Exception{setup();byte[] gg=g.clone(),ll=l.clone();proof();assertArrayEquals(gg,g);assertArrayEquals(ll,l);}
}
