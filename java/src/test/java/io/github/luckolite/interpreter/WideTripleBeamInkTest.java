// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original aligned beam cores, antialias seams and thin-rule controls. */
public class WideTripleBeamInkTest {
 static final int W=280,H=230;byte[] g=new byte[W*H];final int[] a={100,80,-1},b={144,80,-1};
 public WideTripleBeamInkTest(){Arrays.fill(g,(byte)220);}
 void page(int n,int thirdThickness,float slope){for(int x=100;x<=144;x++)for(int beam=0;beam<n;beam++)for(int dy=0;dy<(beam==2?thirdThickness:6);dy++)g[(80+beam*9+dy+Math.round((x-100)*slope))*W+x]=20;}
 int count(){return WideTripleBeamInk.count(g,W,H,a,b,12);}
 @Test public void threeFullCoresRecoverTheWidePair(){page(3,6,0);assertEquals(3,count());}
 @Test public void twoBeamsCannotBecomeThree(){page(2,6,0);assertEquals(0,count());}
 @Test public void thinStaffRuleCannotBeThirdBeam(){page(3,2,0);assertEquals(0,count());}
 @Test public void slopingTripleRemainsThree(){page(3,6,.1f);assertEquals(3,WideTripleBeamInk.count(g,W,H,a,new int[]{144,84,-1},12));}
 @Test public void reversedStemsAreEquivalent(){page(3,6,0);assertEquals(3,WideTripleBeamInk.count(g,W,H,b,a,12));}
 @Test public void brokenIndependentColumnRejects(){page(3,6,0);for(int y=75;y<110;y++)for(int x=120;x<125;x++)g[y*W+x]=(byte)220;assertEquals(0,count());}
 @Test public void oppositeDirectionsAreRejected(){page(3,6,0);assertEquals(0,WideTripleBeamInk.count(g,W,H,a,new int[]{144,80,1},12));}
 @Test public void widerOrNarrowerPairsCannotUseThisRecovery(){page(3,6,0);assertEquals(0,WideTripleBeamInk.count(g,W,H,a,new int[]{149,80,-1},12));assertEquals(0,WideTripleBeamInk.count(g,W,H,a,new int[]{130,80,-1},12));}
 @Test public void blankPaperDoesNotInventBeams(){assertEquals(0,count());}
 @Test public void invalidInputsAreRejected(){assertEquals(0,WideTripleBeamInk.count(g,W,H,a,b,Float.NaN));assertEquals(0,WideTripleBeamInk.count(g,W,H,new int[]{1},b,12));assertEquals(0,WideTripleBeamInk.count(new byte[1],W,H,a,b,12));}
 @Test public void pixelsAndEndpointsStayUnchanged(){page(3,6,0);byte[] before=g.clone();int[] old=a.clone();count();assertArrayEquals(before,g);assertArrayEquals(old,a);}
 int decoded()throws Exception {
  Arrays.fill(g,(byte)255);byte[] labels=new byte[W*H];for(int x:new int[]{110,155})for(int y=90;y<=145;y++){g[y*W+x]=30;labels[y*W+x]=5;}
  for(int x=110;x<=155;x++){for(int j=0;j<3;j++)for(int y=90+j*10;y<=96+j*10;y++){g[y*W+x]=30;labels[y*W+x]=5;}if(x!=110&&x!=155)for(int y=107;y<=109;y++)g[y*W+x]=(byte)150;}
  var hc=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");var ctor=hc.getDeclaredConstructors()[0];ctor.setAccessible(true);Object h1=ctor.newInstance(210,90,110,138,152,100f,145f),h2=ctor.newInstance(210,135,155,138,152,145f,145f);var sc=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var cs=sc.getDeclaredConstructor(float.class,float.class,float.class);cs.setAccessible(true);Object staff=cs.newInstance(100f,148f,12f);var m=OmrScoreInterpreter.class.getDeclaredMethod("detectBeamCount",byte[].class,byte[].class,int.class,int.class,hc,sc,List.class);m.setAccessible(true);return(int)m.invoke(null,labels,g,W,H,h1,staff,List.of(h1,h2));
 }
 @Test public void decoderDoesNotStopAtTwoFusedCores()throws Exception {assertEquals(3,decoded());}
}
