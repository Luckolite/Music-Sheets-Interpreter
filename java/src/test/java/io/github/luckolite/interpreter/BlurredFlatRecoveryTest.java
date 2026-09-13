// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original gray halos and unequal pale strokes, independent of score pixels. */
public class BlurredFlatRecoveryTest {
    static final int W=100,H=100;final byte[] gray=new byte[W*H];
    public BlurredFlatRecoveryTest(){Arrays.fill(gray,(byte)255);}
    void ink(int l,int r,int t,int b,int value){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)gray[y*W+x]=(byte)value;}
    void flat(int spine){ink(30,32,25,64,spine);ink(30,41,49,50,0);ink(30,41,60,61,0);ink(40,41,49,61,0);}
    void halo(){ink(29,35,24,65,230);ink(29,44,47,63,230);}
    Object make(String name,Object...args)throws Exception{var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);}
    boolean detect()throws Exception{var seed=make("Component",55,30,41,49,64,34f,56f);var glyph=make("AccidentalCandidate",seed,(byte)3);var head=make("Component",120,53,69,49,61,61f,55f);var m=Arrays.stream(OmrScoreInterpreter.class.getDeclaredMethods()).filter(x->x.getName().equals("rawFlatFromBowl")).findFirst().orElseThrow();m.setAccessible(true);return (boolean)m.invoke(null,gray,W,H,List.of(glyph),head,16f);}
    @Test public void mediumSpineSurvivesLighterHalo()throws Exception{halo();flat(210);assertTrue(detect());}
    @Test public void paleSpineSurvivesLighterHalo()throws Exception{halo();flat(220);assertTrue(detect());}
    @Test public void unequalSharpSpinesNeverBecomeFlat()throws Exception{flat(220);ink(40,41,25,64,230);assertFalse(detect());}
    @Test public void unequalNaturalSpinesNeverBecomeFlat()throws Exception{ink(30,32,25,51,220);ink(40,41,38,64,230);ink(30,41,38,39,0);ink(30,41,50,51,0);assertFalse(detect());}
    @Test public void fadingTailStillMakesTheGlyphIncomplete()throws Exception{flat(220);ink(30,32,65,88,230);assertFalse(detect());}
    @Test public void absentSpineCannotBeSuppliedByHalo()throws Exception{halo();flat(255);ink(29,35,24,46,255);assertFalse(detect());}
    @Test public void isolatedFlatKeepsExistingBehavior()throws Exception{flat(220);assertTrue(detect());}
    @Test public void pixelsRemainUnchanged()throws Exception{halo();flat(220);var before=gray.clone();detect();assertArrayEquals(before,gray);}
}
