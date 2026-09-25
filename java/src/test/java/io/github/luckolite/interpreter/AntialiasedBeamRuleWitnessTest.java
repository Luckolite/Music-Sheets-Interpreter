// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric beam/staff patterns with a three-pixel continuing rule. */
public class AntialiasedBeamRuleWitnessTest {
    private static final int W=360,H=200;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    private void rect(int left,int right,int top,int bottom,int label){for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++){gray[y*W+x]=20;labels[y*W+x]=(byte)label;}}
    private void setup(int ruleThickness){Arrays.fill(gray,(byte)245);for(int y=84;y<=140;y+=14)rect(0,W-1,y,y+ruleThickness-1,4);rect(80,250,80,86,5);}
    private Object staff()throws Exception{var c=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff").getDeclaredConstructor(float.class,float.class,float.class);c.setAccessible(true);return c.newInstance(84f,140f,14f);}
    private int bands()throws Exception{var s=staff();var m=OmrScoreInterpreter.class.getDeclaredMethod("thickNonHeadBands",byte[].class,byte[].class,int.class,int.class,int.class,int.class,int.class,s.getClass());m.setAccessible(true);return (int)m.invoke(null,gray,labels,W,H,170,74,106,s);}
    private boolean finite()throws Exception{var s=staff();var m=OmrScoreInterpreter.class.getDeclaredMethod("finiteBeamOverRule",byte[].class,int.class,int.class,int.class,int.class,int.class,s.getClass(),int.class);m.setAccessible(true);return (boolean)m.invoke(null,gray,W,H,170,80,86,s,165);}
    @Test public void threePixelRuleProvesFiniteSevenPixelBeam()throws Exception{setup(3);assertTrue(finite());assertEquals(1,bands());}
    @Test public void twoPixelRuleStillProvesBeam()throws Exception{setup(2);assertTrue(finite());assertEquals(1,bands());}
    @Test public void fourPixelThickContinuationIsNotThinEvidence()throws Exception{setup(4);assertFalse(finite());}
    @Test public void fullWidthThickBandHasNoProvenEndpoints()throws Exception{setup(3);rect(0,W-1,80,86,4);assertFalse(finite());assertEquals(0,bands());}
    @Test public void oneFiniteEndIsInsufficient()throws Exception{setup(3);rect(0,80,80,86,4);assertFalse(finite());assertEquals(0,bands());}
    @Test public void blankPaperIsNotAContinuingRule()throws Exception{setup(0);assertFalse(finite());}
    @Test public void sourceArraysRemainUntouched()throws Exception{setup(3);byte[] a=gray.clone(),b=labels.clone();assertEquals(1,bands());assertArrayEquals(a,gray);assertArrayEquals(b,labels);}
}
