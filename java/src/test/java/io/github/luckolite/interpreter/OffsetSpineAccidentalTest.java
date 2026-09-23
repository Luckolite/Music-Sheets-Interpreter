// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic offset-stem accidentals; no source score pixels. */
public class OffsetSpineAccidentalTest {
    private static final int W=80,H=90;
    private final byte[] labels=new byte[W*H];
    private void ink(int left,int right,int top,int bottom) {
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++)labels[y*W+x]=3;
    }
    private int classify(boolean natural)throws Exception {
        Arrays.fill(labels,(byte)0);
        ink(30,32,natural?20:27,natural?60:67);
        ink(42,44,natural?27:20,natural?67:60);
        ink(30,44,34,35);ink(30,44,52,53);
        var owner=OmrScoreInterpreter.class;
        var component=Class.forName(owner.getName()+"$Component").getDeclaredConstructors()[0];
        component.setAccessible(true);
        Object box=component.newInstance(300,30,44,20,67,37f,43f);
        var candidate=Class.forName(owner.getName()+"$AccidentalCandidate").getDeclaredConstructors()[0];
        candidate.setAccessible(true);
        Object glyph=candidate.newInstance(box,(byte)3);
        var method=Arrays.stream(owner.getDeclaredMethods())
                .filter(m->m.getName().equals("offsetSpineAccidental")).findFirst().orElseThrow();
        method.setAccessible(true);
        return (int)method.invoke(null,labels,W,H,glyph,16f);
    }
    @Test public void naturalHasRightSpineLowerAtBothEnds()throws Exception {
        assertEquals(ScoreNoteEvent.ACCIDENTAL_NATURAL,classify(true));
    }
    @Test public void sharpHasRightSpineHigherAtBothEnds()throws Exception {
        assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP,classify(false));
    }
}
