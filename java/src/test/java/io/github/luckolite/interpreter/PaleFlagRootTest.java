// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original light shaft, filled flag root, and detached text-like hook controls. */
public class PaleFlagRootTest {
 static final int W=260,H=220;final byte[] g=new byte[W*H],labels=new byte[W*H];float cx=100,cy=150;
 public PaleFlagRootTest(){Arrays.fill(g,(byte)255);}
 void rect(int l,int r,int t,int b,int v){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)g[y*W+x]=(byte)v;}
 void line(int ax,int ay,int bx,int by,int rad){int n=Math.max(Math.abs(bx-ax),Math.abs(by-ay));for(int i=0;i<=n;i++){int x=Math.round(ax+(bx-ax)*i/(float)n),y=Math.round(ay+(by-ay)*i/(float)n);rect(x-rad,x+rad,y-rad,y+rad,35);}}
 void setup(boolean root,boolean hook,boolean shaft){if(shaft)rect(109,111,80,150,240);if(root)for(int y=80;y<=96;y++)rect(110,110+Math.round((y-80)*.8f),y,y,35);if(hook){line(122,95,125,107,1);line(125,107,119,118,1);}}
 int beams()throws Exception {var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=hc.getDeclaredConstructors()[0];ctor.setAccessible(true);Object head=ctor.newInstance(180,(int)cx-10,(int)cx+10,(int)cy-6,(int)cy+6,cx,cy);var sc=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var st=sc.getDeclaredConstructor(float.class,float.class,float.class);st.setAccessible(true);Object staff=st.newInstance(70f,134f,16f);var m=OmrScoreInterpreter.class.getDeclaredMethod("detectBeamCount",byte[].class,byte[].class,int.class,int.class,hc,sc,List.class);m.setAccessible(true);return(int)m.invoke(null,labels,g,W,H,head,staff,List.of(head));}
 @Test public void centeredPaleShaftWithIndependentFlagIsEighth()throws Exception {setup(true,true,true);assertEquals(1,beams());}
 @Test public void downwardPaleShaftWithIndependentFlagIsEighth()throws Exception {setup(true,true,true);byte[] old=g.clone();for(int y=0;y<H;y++)System.arraycopy(old,y*W,g,(H-1-y)*W,W);cx=120;cy=H-1-150;assertEquals(1,beams());}
 @Test public void detachedTextLikeHookCannotSupplyFlag()throws Exception {setup(false,true,true);assertEquals(0,beams());}
 @Test public void noShaftCannotBorrowAFlag()throws Exception {setup(true,true,false);assertEquals(0,beams());}
 @Test public void blankFlagRegionPreservesQuarter()throws Exception {setup(false,false,true);assertEquals(0,beams());}
 @Test public void broadGrayPatchDoesNotSupplyShaftContrast()throws Exception {setup(true,true,true);rect(102,120,108,135,240);assertEquals(0,beams());}
 @Test public void disconnectedShaftCannotReachItsFlag()throws Exception {setup(true,true,true);rect(108,112,121,129,255);assertEquals(0,beams());}
 @Test public void inputPixelsAndLabelsRemainUnchanged()throws Exception {setup(true,true,true);byte[] old=g.clone(),l=labels.clone();beams();assertArrayEquals(old,g);assertArrayEquals(l,labels);}
}
