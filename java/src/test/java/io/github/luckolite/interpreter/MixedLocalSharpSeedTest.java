// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
/** A connected mixed-label candidate is evidence only when raw spines and crossbars agree. */
public class MixedLocalSharpSeedTest {
    private final byte[] gray=new byte[100*100];
    public MixedLocalSharpSeedTest(){java.util.Arrays.fill(gray,(byte)255);}
    private void box(int l,int r,int t,int b){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)gray[y*100+x]=0;}
    private void sharp(){box(33,34,30,69);box(41,42,30,69);box(30,45,40,43);box(30,45,56,59);}
    private Object make(String name,Object...args)throws Exception{var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(args);}
    private boolean detect(float headY)throws Exception {
        Object seed=make("AccidentalCandidate",make("Component",240,30,45,30,69,38f,50f),(byte)0);
        Object head=make("Component",100,55,71,Math.round(headY-6),Math.round(headY+6),63f,headY);
        for(var m:OmrScoreInterpreter.class.getDeclaredMethods())if(m.getName().equals("rawSharpFromSeed")){m.setAccessible(true);return(boolean)m.invoke(null,gray,100,100,List.of(seed),head,16f);}
        throw new AssertionError("Missing recovery method");
    }
    @Test public void completeMixedCandidateCanSeedPrintedSharp()throws Exception{sharp();assertTrue(detect(50));}
    @Test public void unrelatedNearbyInkDoesNotEraseCompleteBoundedSharp()throws Exception{sharp();box(30,31,10,20);assertTrue(detect(50));}
    @Test public void mixedCandidateWithoutSpinesCannotCreateSharp()throws Exception{box(30,45,40,43);box(30,45,56,59);assertFalse(detect(50));}
    @Test public void neighbouringPitchCannotTakeMixedSharp()throws Exception{sharp();assertFalse(detect(59));}
    @Test public void croppedSubfragmentCannotOverrideItsLongConnectedGlyph()throws Exception {
        sharp();box(33,34,5,94);
        Object crop=make("AccidentalCandidate",make("Component",240,30,45,30,69,38f,50f),(byte)3);
        Object whole=make("AccidentalCandidate",make("Component",340,30,45,5,94,36f,50f),(byte)0);
        Object head=make("Component",100,55,71,44,56,63f,50f);
        for(var m:OmrScoreInterpreter.class.getDeclaredMethods())if(m.getName().equals("rawSharpFromSeed")) {
            m.setAccessible(true);assertFalse((boolean)m.invoke(null,gray,100,100,List.of(crop,whole),head,16f));return;
        }
        throw new AssertionError("Missing recovery method");
    }
    @Test public void overridingAFlatRequiresMoreThanCroppedStemCompatibility()throws Exception {
        sharp();box(33,34,5,94);
        Object crop=make("AccidentalCandidate",make("Component",240,30,45,30,69,38f,50f),(byte)3);
        Object head=make("Component",100,55,71,44,56,63f,50f);
        boolean sawLegacy=false,sawComplete=false;
        for(var m:OmrScoreInterpreter.class.getDeclaredMethods()) {
            if(m.getName().equals("rawSharpFromSeed")){m.setAccessible(true);assertTrue((boolean)m.invoke(null,gray,100,100,List.of(crop),head,16f));sawLegacy=true;}
            if(m.getName().equals("completeRawSharpFromSeed")){m.setAccessible(true);assertFalse((boolean)m.invoke(null,gray,100,100,List.of(crop),head,16f));sawComplete=true;}
        }
        assertTrue(sawLegacy&&sawComplete);
    }
    @Test public void sourceInkIsNotChanged()throws Exception{sharp();var before=gray.clone();detect(50);assertArrayEquals(before,gray);}
}
