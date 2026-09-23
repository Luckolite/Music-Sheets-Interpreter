// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original natural/flat drawings beside an independently continuing note stem. */
public class TouchingNaturalStemTest {
    static final int W=100,H=100;
    final byte[] gray=new byte[W*H];
    public TouchingNaturalStemTest(){Arrays.fill(gray,(byte)255);}
    void rect(int x,int y,int w,int h){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)gray[yy*W+xx]=0;}
    void glyph(boolean natural){
        rect(25,18,2,30);rect(35,30,2,natural?29:18);
        rect(25,30,12,4);rect(25,44,12,4);
        // The neighboring note's stem touches the semantic accidental box.
        rect(38,30,2,45);
    }
    boolean recovered()throws Exception {
        var type=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var cc=type.getDeclaredConstructors()[0];cc.setAccessible(true);
        var seed=cc.newInstance(96,25,38,30,47,31.5f,38.5f);
        var head=cc.newInstance(180,39,51,33,43,45f,38f);
        var ac=Class.forName(OmrScoreInterpreter.class.getName()+"$AccidentalCandidate");
        var ctor=ac.getDeclaredConstructors()[0];ctor.setAccessible(true);
        var candidate=ctor.newInstance(seed,(byte)3);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("rawNaturalFromCrossbars",byte[].class,
                int.class,int.class,List.class,type,float.class);method.setAccessible(true);
        return (boolean)method.invoke(null,gray,W,H,List.of(candidate),head,14f);
    }
    @Test public void naturalBesideForeignStemIsRecovered()throws Exception{glyph(true);assertTrue(recovered());}
    @Test public void flatBesideForeignStemStaysFlat()throws Exception{glyph(false);assertFalse(recovered());}
}
