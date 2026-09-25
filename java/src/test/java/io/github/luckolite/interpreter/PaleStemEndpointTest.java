// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original pale shafts and clipped semantic stem endpoint controls. */
public class PaleStemEndpointTest {
 static final int W=240,H=220;final byte[] g=new byte[W*H],l=new byte[W*H];float cx=100,cy=100,staffTop=64,staffBottom=128;
 public PaleStemEndpointTest(){Arrays.fill(g,(byte)255);}
 void rect(int a,int b,int c,int d,int v){for(int y=c;y<=d;y++)for(int x=a;x<=b;x++)g[y*W+x]=(byte)v;}
 void setup(){rect(89,91,100,164,235);for(int y=100;y<=130;y++)l[y*W+90]=OmrMeasurePostProcessor.STEM_OR_REST;rect(0,W-1,128,129,35);}
 int beams()throws Exception {var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=hc.getDeclaredConstructors()[0];ctor.setAccessible(true);Object head=ctor.newInstance(180,(int)cx-10,(int)cx+10,(int)cy-6,(int)cy+6,cx,cy);var sc=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var st=sc.getDeclaredConstructor(float.class,float.class,float.class);st.setAccessible(true);Object staff=st.newInstance(staffTop,staffBottom,16f);var m=OmrScoreInterpreter.class.getDeclaredMethod("detectBeamCount",byte[].class,byte[].class,int.class,int.class,hc,sc,List.class);m.setAccessible(true);return(int)m.invoke(null,l,g,W,H,head,staff,List.of(head));}
 @Test public void paleQuarterDoesNotBorrowRuleAtClippedSemanticEnd()throws Exception{setup();assertEquals(0,beams());}
 @Test public void upwardQuarterAlsoUsesTheRawEndpoint()throws Exception{setup();byte[] gg=g.clone(),ll=l.clone();for(int y=0;y<H;y++){System.arraycopy(gg,y*W,g,(H-1-y)*W,W);System.arraycopy(ll,y*W,l,(H-1-y)*W,W);}cx=80;cy=H-1-100;staffTop=H-1-128;staffBottom=H-1-64;assertEquals(0,beams());}
 @Test public void realBeamAtPaleEndpointIsPreserved()throws Exception{setup();rect(90,155,159,164,35);assertEquals(1,beams());}
 @Test public void twoRealBeamsAtPaleEndpointArePreserved()throws Exception{setup();rect(90,155,158,164,35);rect(90,155,146,151,35);assertEquals(2,beams());}
 @Test public void darkShaftKeepsOrdinaryQuarter()throws Exception{setup();rect(89,91,100,164,35);assertEquals(0,beams());}
 @Test public void broadGrayRegionDoesNotInventAPaleStem()throws Exception{setup();rect(83,97,112,157,235);rect(0,W-1,128,129,35);assertEquals(1,beams());}
 @Test public void disconnectedShaftDoesNotSuppressExistingEvidence()throws Exception{setup();rect(89,91,140,149,255);assertEquals(1,beams());}
 @Test public void blankPageHasNoBeams()throws Exception{assertEquals(0,beams());}
 @Test public void pixelsAndLabelsAreUnchanged()throws Exception{setup();byte[] old=g.clone(),ll=l.clone();beams();assertArrayEquals(old,g);assertArrayEquals(ll,l);}
}
