// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original long beam bodies, independently shaded thin rules and small slopes. */
public class LongBeamPaleRuleTest {
    static final int W=800,H=200;
    final byte[] gray=new byte[W*H],labels=new byte[W*H];
    void rect(int l,int r,int t,int b,int shade){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)gray[y*W+x]=(byte)shade;}
    void setup(int rule,boolean slope){Arrays.fill(gray,(byte)250);for(int y=84;y<=148;y+=16)rect(0,W-1,y,y+1,rule);for(int x=100;x<=700;x++){int dy=slope?Math.round((x-100)/300f):0;rect(x,x,79+dy,85+dy,35);}rect(390,424,93,99,35);}
    int count()throws Exception{var st=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var sc=st.getDeclaredConstructor(float.class,float.class,float.class);sc.setAccessible(true);var m=OmrScoreInterpreter.class.getDeclaredMethod("thickNonHeadBands",byte[].class,byte[].class,int.class,int.class,int.class,int.class,int.class,st);m.setAccessible(true);return (int)m.invoke(null,gray,labels,W,H,400,76,105,sc.newInstance(84f,148f,16f));}
    @Test public void longBeamOverDarkRuleKeepsBothBands()throws Exception{setup(0,false);assertEquals(2,count());}
    @Test public void paleContinuingRuleStillProvesFiniteBeam()throws Exception{setup(212,false);assertEquals(2,count());}
    @Test public void smallBeamSlopeRetainsPaleRuleWitness()throws Exception{setup(212,true);assertEquals(2,count());}
    @Test public void absentRuleDoesNotProveFiniteBeam()throws Exception{setup(250,false);assertEquals(1,count());}
    @Test public void nearWhiteTextureDoesNotProveRule()throws Exception{setup(230,false);assertEquals(1,count());}
    @Test public void fullWidthThickRuleRemainsExcluded()throws Exception{setup(0,false);rect(0,W-1,79,85,35);assertEquals(1,count());}
    @Test public void oneVisibleEndIsInsufficient()throws Exception{setup(0,false);rect(0,100,79,85,35);assertEquals(1,count());}
    private boolean edge(int first,int last)throws Exception{var st=Class.forName(OmrScoreInterpreter.class.getName()+"$Staff");var sc=st.getDeclaredConstructor(float.class,float.class,float.class);sc.setAccessible(true);var m=OmrScoreInterpreter.class.getDeclaredMethod("finiteBeamOverRule",byte[].class,int.class,int.class,int.class,int.class,int.class,st,int.class);m.setAccessible(true);return (boolean)m.invoke(null,gray,W,H,400,first,last,sc.newInstance(84f,148f,16f),165);}
    @Test public void upperImageEdgeRejectsOutOfRangeWitness()throws Exception{setup(212,true);assertFalse(edge(-1,5));}
    @Test public void lowerImageEdgeRejectsOutOfRangeWitness()throws Exception{setup(212,true);assertFalse(edge(H-5,H));}
    @Test public void inputsArePreserved()throws Exception{setup(212,true);var a=gray.clone();var b=labels.clone();count();assertArrayEquals(a,gray);assertArrayEquals(b,labels);}
}
