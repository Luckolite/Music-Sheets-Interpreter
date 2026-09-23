// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic flat bowl sharing a stroke with an upper beamed note. */
public class FlatBowlUnderBeamTest {
    private static final int W=150,H=150;
    private final byte[] gray=new byte[W*H],labels=new byte[W*H];
    public FlatBowlUnderBeamTest(){Arrays.fill(gray,(byte)255);}
    private void ink(int left,int right,int top,int bottom,int label) {
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++) {
            gray[y*W+x]=0;labels[y*W+x]=(byte)label;
        }
    }
    private Object make(String name,Object...args)throws Exception {
        var c=Class.forName(OmrScoreInterpreter.class.getName()+"$"+name).getDeclaredConstructors()[0];
        c.setAccessible(true);return c.newInstance(args);
    }
    private boolean detect()throws Exception {
        Object seed=make("Component",160,75,89,89,109,82f,99f);
        Object glyph=make("AccidentalCandidate",seed,(byte)3);
        Object head=make("Component",120,91,110,93,107,100f,100f);
        var method=Arrays.stream(OmrScoreInterpreter.class.getDeclaredMethods())
                .filter(m->m.getName().equals("flatBowlUnderBeamedStem")).findFirst().orElseThrow();
        method.setAccessible(true);
        return (boolean)method.invoke(null,labels,gray,W,H,List.of(glyph),head,16f);
    }
    private void flat() {
        ink(76,78,62,109,1);
        ink(76,89,89,90,3);ink(76,89,104,105,3);ink(88,89,89,105,3);
        ink(76,78,89,109,3);
        ink(76,100,59,61,5);
    }
    @Test public void upperBeamedStemDoesNotEraseCompleteFlatBowl()throws Exception {
        flat();assertTrue(detect());
    }
    @Test public void aLongStemWithoutUpperBeamIsInsufficient()throws Exception {
        flat();ink(76,100,59,61,0);assertFalse(detect());
    }
    @Test public void secondUpperSpineIsNotAFlat()throws Exception {
        flat();ink(86,88,72,109,3);assertFalse(detect());
    }
}
