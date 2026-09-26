// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original displaced staff-rule frame with ordinary shafts and true beams. */
public class LocalBeamStaffFrameTest {
 static final int W=320,H=240;final byte[] g=new byte[W*H],l=new byte[W*H];
 static final Class<?> ST,HC;static{try{ST=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");HC=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");}catch(Exception e){throw new RuntimeException(e);}}
 public LocalBeamStaffFrameTest(){Arrays.fill(g,(byte)255);for(int y=96;y<=160;y+=16)rect(20,295,y,y+4,30,4);rect(89,91,110,163,30,1);}
 void rect(int a,int b,int c,int d,int value,int label){for(int y=c;y<=d;y++)for(int x=a;x<=b;x++){g[y*W+x]=(byte)value;l[y*W+x]=(byte)label;}}
 void thinRules(){for(int y=96;y<=160;y+=16)rect(20,295,y+2,y+4,255,0);rect(89,91,110,163,30,1);}
 Object staff()throws Exception{var c=ST.getDeclaredConstructor(float.class,float.class,float.class);c.setAccessible(true);return c.newInstance(64f,128f,16f);}
 Object frame(Object s,float bottom,float gap)throws Exception{var m=OmrScoreInterpreter.class.getDeclaredMethod("beamStaffFrame",ST,float[].class,int.class);m.setAccessible(true);return m.invoke(null,s,new float[]{bottom,gap},W);}
 int count(Object s)throws Exception{var c=HC.getDeclaredConstructors()[0];c.setAccessible(true);Object h=c.newInstance(180,90,110,104,116,100f,110f);var m=OmrScoreInterpreter.class.getDeclaredMethod("detectBeamCount",byte[].class,byte[].class,int.class,int.class,HC,ST,List.class);m.setAccessible(true);return(int)m.invoke(null,l,g,W,H,h,s,List.of(h));}
 @Test public void displacedRulesDoNotBecomeBeams()throws Exception{Object s=staff();assertTrue(count(s)>0);assertEquals(0,count(frame(s,160,16)));}
 @Test public void realWideBeamSurvivesLocalRuleFrame()throws Exception{thinRules();rect(89,185,157,165,30,1);assertEquals(1,count(frame(staff(),160,16)));}
 @Test public void twoIndependentThickBeamsSurvive()throws Exception{thinRules();rect(89,185,143,150,30,1);rect(89,185,157,165,30,1);assertEquals(2,count(frame(staff(),160,16)));}
 @Test public void alignedFrameIsKeptByIdentity()throws Exception{Object s=staff();assertSame(s,frame(s,128,16));}
 @Test public void SmallDisplacementKeepsLegacyFrame()throws Exception{Object s=staff();assertSame(s,frame(s,132,16));}
 @Test public void incompatibleScaleKeepsLegacyFrame()throws Exception{Object s=staff();assertSame(s,frame(s,160,22));}
 @Test public void establishedCurveIsNeverReplaced()throws Exception{Object s=staff();var f=ST.getDeclaredField("pitchTrack");f.setAccessible(true);Object track=StaffPitchTrack.linear(W,128,16,.02f);f.set(s,track);assertSame(s,frame(s,160,16));assertSame(track,f.get(s));}
 @Test public void invalidLocalGeometryKeepsLegacyFrame()throws Exception{Object s=staff();assertSame(s,frame(s,Float.NaN,16));assertSame(s,frame(s,160,Float.NaN));assertSame(s,frame(s,Float.POSITIVE_INFINITY,16));}
 @Test public void sourceStaffAndPixelsAreImmutable()throws Exception{Object s=staff();byte[] gg=g.clone(),ll=l.clone();Object adjusted=frame(s,160,16);assertNotSame(s,adjusted);count(adjusted);var b=ST.getDeclaredField("bottom");b.setAccessible(true);assertEquals(128,b.getFloat(s),0);assertArrayEquals(gg,g);assertArrayEquals(ll,l);}
}

